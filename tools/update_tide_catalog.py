"""Refresh station coordinates from MSQ's published Queensland prediction files.

Run with Python 3: python tools/update_tide_catalog.py 2026
The app resolves the selected year's resource dynamically; coordinates are bundled offline.
"""
import concurrent.futures
import datetime
import json
from pathlib import Path
import re
import sys
import urllib.parse
import urllib.request

year = int(sys.argv[1]) if len(sys.argv) > 1 else datetime.date.today().year
base = 'https://www.data.qld.gov.au/api/3/action/'
def get_json(url):
    with urllib.request.urlopen(url, timeout=20) as response:
        return json.load(response)

query = urllib.parse.urlencode({'q': '"predicted interval"', 'rows': 100})
packages = get_json(base + 'package_search?' + query)['result']['results']

def station(package):
    if not package['name'].endswith('tide-gauge-predicted-interval-data'):
        return None
    resources = [r for r in package['resources'] if str(year) in r['name'] and r.get('format', '').upper() == 'CSV']
    if not resources:
        return None
    resource = resources[0]
    try:
        request = urllib.request.Request(resource['url'], headers={'Range': 'bytes=0-4095'})
        with urllib.request.urlopen(request, timeout=20) as response:
            header = response.read(4096).decode('utf-8-sig', errors='replace')
        def coordinate(label):
            match = re.search(label + r'[^\r\n=]*=,\s*(-?\d+)\s+(\d+(?:\.\d+)?)', header)
            if not match:
                raise ValueError('Missing ' + label)
            degrees, minutes = map(float, match.groups())
            return (-1 if degrees < 0 else 1) * (abs(degrees) + minutes / 60)
        name = package['title'].split(' tide gauge')[0]
        return {'id': package['name'], 'name': name, 'latitude': coordinate('Latitude'),
                'longitude': coordinate('Longitude'), 'licence': package.get('license_id', ''),
                'source': 'https://www.data.qld.gov.au/dataset/' + package['name']}
    except Exception as error:
        print(package['name'] + ': ' + str(error), flush=True)
        return None

with concurrent.futures.ThreadPoolExecutor(max_workers=4) as pool:
    stations = [s for s in pool.map(station, packages) if s is not None]
stations.sort(key=lambda s: s['name'])
if len(stations) < 10:
    raise SystemExit('Too few verified stations; existing catalogue was not replaced.')
out = Path(__file__).resolve().parents[1] / 'app/src/main/assets/qld_tide_stations.json'
out.write_text(json.dumps({'source': 'Maritime Safety Queensland / Queensland Open Data',
                          'checkedYear': year, 'stations': stations}, indent=2), encoding='utf-8')
print('Verified ' + str(len(stations)) + ' Queensland tide stations.', flush=True)
