"""Refresh small public reference catalogues; never touches catches or favourites.

Uses documented BOM observation JSON, Queensland Open Data and Wikimedia Commons.
Only CC BY, CC BY-SA, CC0 and public-domain images are imported. Review the manifest
and contact sheet before distributing an updated catalogue.
"""
import concurrent.futures
import datetime
import html
import json
from pathlib import Path
import re
import urllib.parse
import urllib.request

root = Path(__file__).resolve().parents[1]
assets = root / 'app/src/main/assets'
audit = root / 'work/reference-catalogues'
audit.mkdir(parents=True, exist_ok=True)

def get(url):
    req = urllib.request.Request(url, headers={'User-Agent': 'WaterLogged/0.3 reference-catalogue (non-commercial student pilot)'})
    with urllib.request.urlopen(req, timeout=20) as response:
        return response.read()

def get_json(url):
    return json.loads(get(url))

def save(name, data):
    (assets / name).write_text(json.dumps(data, indent=2, ensure_ascii=False), encoding='utf-8')

# Deliberately bounded coverage; outside these verified stations the app uses a model.
weather_ids = [94569, 94584, 94566, 94578, 94575, 94568, 94592, 94387, 94388,
               94380, 94374, 94367, 94294, 94287, 94551, 94555, 94384, 94289]
def weather_station(wmo):
    try:
        url = f'https://www.bom.gov.au/fwo/IDQ60801/IDQ60801.{wmo}.json'
        data = get_json(url)
        row = data['observations']['data'][0]
        return {'id': str(wmo), 'name': row['name'], 'latitude': row['lat'], 'longitude': row['lon'], 'url': url}
    except Exception as error:
        print('Weather station', wmo, error, flush=True)

with concurrent.futures.ThreadPoolExecutor(max_workers=4) as pool:
    stations = [s for s in pool.map(weather_station, weather_ids) if s]
if stations:
    save('weather_stations.json', {'checkedOn': str(datetime.date.today()), 'source': 'Bureau of Meteorology documented public observation feeds', 'stations': stations})
    print('Weather stations:', len(stations), flush=True)

# One latest row per buoy provides its public name and position. Catch matching is local.
resource = '2bbef99e-9974-49b9-a316-57402b00609c'
url = 'https://www.data.qld.gov.au/api/3/action/datastore_search?' + urllib.parse.urlencode({'resource_id': resource, 'limit': 2000, 'sort': 'Seconds desc'})
rows = get_json(url)['result']['records']
buoys = {}
for row in rows:
    try:
        lat, lon = float(row['Latitude']), float(row['Longitude'])
        if -90 <= lat <= 90 and -180 <= lon <= 180 and row['Site'] not in buoys:
            buoys[row['Site']] = {'id': row['SiteNumber'], 'name': row['Site'], 'latitude': lat, 'longitude': lon}
    except (ValueError, KeyError):
        pass
save('wave_buoys.json', {'resourceId': resource, 'source': 'Queensland Coastal Data System', 'licence': 'CC-BY-4.0', 'stations': list(buoys.values())})
print('Wave buoys:', len(buoys), flush=True)

# Stable original IDs preserve favourites and catch associations on existing installs.
fish = [
 ('flathead', 'Dusky flathead', 'Platycephalus fuscus', 'flathead lizard flattie'),
 ('snapper', 'Snapper', 'Chrysophrys auratus', 'Pagrus auratus pink snapper squire'),
 ('bream', 'Yellowfin bream', 'Acanthopagrus australis', 'bream'),
 ('barramundi', 'Barramundi', 'Lates calcarifer', 'barra'),
 ('tailor', 'Tailor', 'Pomatomus saltatrix', 'bluefish'),
 ('trevally', 'Giant trevally', 'Caranx ignobilis', 'trevally GT'),
 ('mangrove_jack', 'Mangrove jack', 'Lutjanus argentimaculatus', 'mangrove red snapper jack'),
 ('golden_snapper', 'Golden snapper', 'Lutjanus johnii', 'fingermark johns snapper'),
 ('red_emperor', 'Red emperor', 'Lutjanus sebae', 'emperor red snapper'),
 ('coral_trout', 'Common coral trout', 'Plectropomus leopardus', 'leopard coral grouper trout'),
 ('redthroat_emperor', 'Redthroat emperor', 'Lethrinus miniatus', 'sweetlip sweetlips'),
 ('spangled_emperor', 'Spangled emperor', 'Lethrinus nebulosus', 'spangled spanglie'),
 ('pearl_perch', 'Pearl perch', 'Glaucosoma scapulare', 'pearlie'),
 ('mulloway', 'Mulloway', 'Argyrosomus japonicus', 'jewfish jew silver jew'),
 ('kingfish', 'Yellowtail kingfish', 'Seriola lalandi', 'kingie yellowtail amberjack'),
 ('amberjack', 'Greater amberjack', 'Seriola dumerili', 'amberjack AJ'),
 ('cobia', 'Cobia', 'Rachycentron canadum', 'black kingfish'),
 ('spanish_mackerel', 'Spanish mackerel', 'Scomberomorus commerson', 'narrow barred spanish mackerel'),
 ('spotted_mackerel', 'Spotted mackerel', 'Scomberomorus munroi', 'spotty mackerel'),
 ('school_mackerel', 'School mackerel', 'Scomberomorus queenslandicus', 'doggie mackerel'),
 ('mackerel_tuna', 'Mackerel tuna', 'Euthynnus affinis', 'mack tuna kawakawa'),
 ('longtail_tuna', 'Longtail tuna', 'Thunnus tonggol', 'northern bluefin tuna'),
 ('yellowfin_tuna', 'Yellowfin tuna', 'Thunnus albacares', 'yellow fin'),
 ('sand_whiting', 'Sand whiting', 'Sillago ciliata', 'summer whiting'),
 ('trumpeter_whiting', 'Trumpeter whiting', 'Sillago maculata', 'winter whiting'),
 ('luderick', 'Luderick', 'Girella tricuspidata', 'blackfish'),
 ('mullet', 'Sea mullet', 'Mugil cephalus', 'flathead grey mullet'),
 ('australian_bass', 'Australian bass', 'Percalates novemaculeata', 'bass Macquaria novemaculeata Percalates novemaculeatus'),
 ('golden_perch', 'Golden perch', 'Macquaria ambigua', 'yellowbelly callop'),
 ('murray_cod', 'Murray cod', 'Maccullochella peelii', 'cod'),
 ('saratoga', 'Southern saratoga', 'Scleropages leichardti', 'saratoga spotted barramundi'),
 ('silver_perch', 'Silver perch', 'Bidyanus bidyanus', 'silver bream grunter'),
]

def plain(value):
    return html.unescape(re.sub('<[^>]+>', '', value)).strip()

def photo(entry):
    key, common, scientific, aliases = entry
    result = {'id': key, 'commonName': common, 'scientificName': scientific, 'aliases': aliases,
              'favourite': key in ('flathead', 'snapper', 'bream'),
              'taxonomySource': 'https://www.fish.gov.au/reports/species'}
    existing_path=assets/'species_catalogue.json'
    if existing_path.exists():
        previous=next((s for s in json.loads(existing_path.read_text(encoding='utf-8'))['species'] if s['id']==key),None)
        if previous and previous.get('photoAsset') and (assets/previous['photoAsset']).exists():
            result.update({k:v for k,v in previous.items() if k.startswith('photo') or k=='taxonomySource'})
            return result
    try:
        search = 'Pagrus auratus' if key == 'snapper' else scientific
        query = {'action': 'query', 'format': 'json', 'generator': 'search', 'gsrnamespace': 6,
                 'gsrsearch': 'intitle:"' + search + '" filetype:bitmap', 'gsrlimit': 8,
                 'prop': 'imageinfo', 'iiprop': 'url|extmetadata', 'iiurlwidth': 640}
        data = get_json('https://commons.wikimedia.org/w/api.php?' + urllib.parse.urlencode(query))
        (audit/(key+'.json')).write_text(json.dumps(data,indent=2),encoding='utf-8')
        for page in sorted(data.get('query', {}).get('pages', {}).values(), key=lambda p: p.get('index', 99)):
            if any(word in page['title'].lower() for word in ('map', 'range', 'skeleton', 'otolith', 'stamp', 'larva', 'distribution', '.svg', 'parasite', 'nematod', 'philometra')):
                continue
            info = page.get('imageinfo', [{}])[0]
            meta = info.get('extmetadata', {})
            value = lambda k: plain(meta.get(k, {}).get('value', ''))
            licence = value('LicenseShortName')
            if not (licence.startswith('CC BY ') or licence.startswith('CC BY-SA ') or licence in ('CC0', 'Public domain', 'PDM')):
                continue
            url = info.get('thumburl', info.get('url', '')).split('?')[0]
            raw = get(url)
            if len(raw)>2_000_000 or not (raw.startswith(b'\xff\xd8') or raw.startswith(b'\x89PNG')):
                continue
            suffix = '.png' if raw.startswith(b'\x89PNG') else '.jpg'
            destination = assets/'species'/ (key + suffix)
            destination.parent.mkdir(exist_ok=True)
            destination.write_bytes(raw)
            result.update({'photoAsset': 'species/' + key + suffix, 'photoTitle': page['title'][5:],
                           'photoAuthor': value('Artist'), 'photoLicence': licence, 'photoLicenceUrl': value('LicenseUrl'),
                           'photoSource': info['descriptionurl'], 'photoDescription': value('ImageDescription'),
                           'photoChanges': 'Resized by Wikimedia thumbnail service; displayed without further editing.'})
            break
    except Exception as error:
        print(common, error, flush=True)
    print(common, result.get('photoLicence', 'generic icon; no verified free photo'), flush=True)
    return result

with concurrent.futures.ThreadPoolExecutor(max_workers=3) as pool:
    entries = list(pool.map(photo, fish))
save('species_catalogue.json', {'version': 1, 'checkedOn': str(datetime.date.today()), 'species': entries})
print('Species:', len(entries), 'with photos:', sum('photoAsset' in s for s in entries), flush=True)
