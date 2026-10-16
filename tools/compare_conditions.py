"""Read-only source audit at a public test point; never reads the app's catch database.

python tools/compare_conditions.py --at 2026-10-15T22:20:00+10:00
Raw public responses and a comparison are written to ignored work/conditions-audit/.
"""
import argparse
import concurrent.futures
import datetime as dt
import json
import math
from pathlib import Path
import urllib.parse
import urllib.request

parser = argparse.ArgumentParser()
parser.add_argument('--latitude', type=float, default=-26.6825)
parser.add_argument('--longitude', type=float, default=153.1201)
parser.add_argument('--at', default=dt.datetime.now(dt.timezone.utc).isoformat())
args = parser.parse_args()
if not (math.isfinite(args.latitude) and -90 <= args.latitude <= 90
        and math.isfinite(args.longitude) and -180 <= args.longitude <= 180):
    parser.error('Use finite latitude -90 to 90 and longitude -180 to 180.')
at = dt.datetime.fromisoformat(args.at)
if at.tzinfo is None:
    parser.error('--at must include a timezone, for example +10:00')
queensland_at = at.astimezone(dt.timezone(dt.timedelta(hours=10)))
out = Path(__file__).resolve().parents[1] / 'work/conditions-audit'
out.mkdir(parents=True, exist_ok=True)
lat, lon = round(args.latitude, 2), round(args.longitude, 2)
date = at.astimezone(dt.timezone.utc).date()
query = dict(latitude=lat, longitude=lon, timezone='GMT', timeformat='unixtime',
             start_date=str(date), end_date=str(date + dt.timedelta(days=1)))
base = 'https://www.data.qld.gov.au/api/3/action/'
assets=Path(__file__).resolve().parents[1]/'app/src/main/assets'
def nearest(asset):
    stations=json.loads((assets/asset).read_text(encoding='utf-8'))['stations']
    def distance(s):
        a,b=map(math.radians,(args.latitude,s['latitude']))
        delta=math.radians(s['longitude']-args.longitude)
        return 6371.0088*2*math.asin(min(1,math.sqrt(math.sin((b-a)/2)**2+math.cos(a)*math.cos(b)*math.sin(delta/2)**2)))
    station=min(stations,key=distance)
    return station if distance(station)<=50 else None
weather_station=nearest('weather_stations.json')
wave_station=nearest('wave_buoys.json')
tide_station=nearest('qld_tide_stations.json')
urls = {
    'weather-model': 'https://api.open-meteo.com/v1/forecast?' + urllib.parse.urlencode(dict(query, hourly='temperature_2m,wind_speed_10m,wind_direction_10m', current='temperature_2m,wind_speed_10m,wind_direction_10m', wind_speed_unit='kmh')),
    'marine-model': 'https://marine-api.open-meteo.com/v1/marine?' + urllib.parse.urlencode(dict(query, hourly='wave_height,wind_wave_height,swell_wave_height,swell_wave_period')),
}
if weather_station:urls['bom-station']=weather_station['url']
if wave_station:urls['qld-wave']=base+'datastore_search?'+urllib.parse.urlencode(dict(resource_id='2bbef99e-9974-49b9-a316-57402b00609c',limit=500,sort='Seconds desc',filters=json.dumps({'Site':wave_station['name']})))
if tide_station:urls['tide-metadata']=base+'package_show?id='+tide_station['id']

def get(item):
    name, url = item
    try:
        request = urllib.request.Request(url, headers={'User-Agent': 'WaterLogged/0.3 source-verification', 'Accept': 'application/json'})
        with urllib.request.urlopen(request, timeout=20) as response:
            data = json.load(response)
        (out / (name + '.json')).write_text(json.dumps(data, indent=2), encoding='utf-8')
        return name, data
    except Exception as error:
        return name, {'error': str(error)}

with concurrent.futures.ThreadPoolExecutor(max_workers=4) as pool:
    responses = dict(pool.map(get, urls.items()))

summary = {'requestedAt': at.isoformat(), 'retrievedAt': dt.datetime.now(dt.timezone.utc).isoformat(),
           'roundedQuery': [lat, lon], 'selectedStations': {'weather':weather_station,'waves':wave_station,'tide':tide_station}, 'sources': {}}
for name, data in responses.items():
    if 'hourly' in data:
        times = data['hourly']['time']
        index = min(range(len(times)), key=lambda i: abs(times[i] - at.timestamp()))
        summary['sources'][name] = {'point': [data.get('latitude'), data.get('longitude')],
                                   'validAt': dt.datetime.fromtimestamp(times[index], dt.timezone.utc).isoformat(),
                                   'values': {k: v[index] for k, v in data['hourly'].items() if k != 'time'},
                                   'current': data.get('current')}
    elif 'observations' in data:
        rows = data['observations']['data']
        matching=[r for r in rows if dt.datetime.strptime(r['aifstime_utc'],'%Y%m%d%H%M%S').replace(tzinfo=dt.timezone.utc)<=at]
        summary['sources'][name] = {'header': data['observations']['header'], 'atOrBeforeCatch': matching[:3]}
    elif name == 'tide-metadata' and data.get('success'):
        resources = data['result']['resources']
        resource = next((r for r in resources if str(queensland_at.year) in r['name'] and r['format'].upper() == 'CSV'), None)
        if resource is None:
            summary['sources'][name] = {'error': 'No tide resource for the requested Queensland year.'}
            continue
        day = queensland_at.strftime('%d/%m/%Y')
        url = base + 'datastore_search?' + urllib.parse.urlencode(dict(resource_id=resource['id'], limit=200, filters=json.dumps({'Date': day})))
        _, tide = get(('tide-rows', url))
        summary['sources'][name] = {'resource': resource['id'], 'day': day, 'response': tide}
    else:
        summary['sources'][name] = data
(out / 'comparison.json').write_text(json.dumps(summary, indent=2), encoding='utf-8')
for name, data in summary['sources'].items():
    if name == 'tide-metadata' and data.get('response', {}).get('success'):
        rows = data['response']['result']['records']
        def stamp(row):
            return dt.datetime.strptime(row['Date'] + ' ' + row['Time'], '%d/%m/%Y %H:%M').replace(tzinfo=dt.timezone(dt.timedelta(hours=10)))
        rows.sort(key=lambda r: abs((stamp(r) - at).total_seconds()))
        print(name, json.dumps(rows[:3]))
    elif name == 'qld-wave' and data.get('success'):
        matching=[r for r in data['result']['records'] if dt.datetime.fromisoformat(r['DateTime']).replace(tzinfo=dt.timezone(dt.timedelta(hours=10)))<=at]
        print(name, json.dumps({'total': data['result']['total'], 'atOrBeforeCatch': matching[:3]}))
    else:
        print(name, json.dumps(data))
print('Audit saved to', out)
