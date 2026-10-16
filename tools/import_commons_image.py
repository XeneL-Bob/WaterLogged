"""Import a specifically reviewed Commons file, preserving its free licence and attribution.

python tools/import_commons_image.py australian_bass "Australian bass 2.jpg"
"""
import html
import json
from pathlib import Path
import re
import sys
import urllib.parse
import urllib.request

key,title=sys.argv[1:3]
assets=Path(__file__).resolve().parents[1]/'app/src/main/assets'
path=assets/'species_catalogue.json';catalogue=json.loads(path.read_text(encoding='utf-8'))
row=next(s for s in catalogue['species'] if s['id']==key)
def get(url):
    req=urllib.request.Request(url,headers={'User-Agent':'WaterLogged/0.3 reviewed reference image import'})
    with urllib.request.urlopen(req,timeout=20) as r:return r.read()
query={'action':'query','format':'json','titles':'File:'+title,'prop':'imageinfo','iiprop':'url|extmetadata','iiurlwidth':640}
data=json.loads(get('https://commons.wikimedia.org/w/api.php?'+urllib.parse.urlencode(query)))
page=next(iter(data['query']['pages'].values()))
info=page['imageinfo'][0];meta=info['extmetadata']
value=lambda k:html.unescape(re.sub('<[^>]+>','',meta.get(k,{}).get('value',''))).strip()
licence=value('LicenseShortName')
if not (licence.startswith('CC BY ') or licence.startswith('CC BY-SA ') or licence in ('CC0','Public domain','PDM')):
    raise SystemExit('No eligible free licence: '+licence)
raw=get(info.get('thumburl',info['url']).split('?')[0])
if not(raw.startswith(b'\xff\xd8') or raw.startswith(b'\x89PNG')):raise SystemExit('Not a supported bitmap')
suffix='.png' if raw.startswith(b'\x89PNG') else '.jpg'
relative='species/'+key+suffix;(assets/relative).write_bytes(raw)
row.update(photoAsset=relative,photoTitle=title,photoAuthor=value('Artist'),photoLicence=licence,
           photoLicenceUrl=value('LicenseUrl'),photoSource=info['descriptionurl'],photoDescription=value('ImageDescription'),
           photoChanges=('Resized by Wikimedia thumbnail service; displayed without further editing.' if info.get('thumburl') else 'Original image bundled; displayed without further editing.'))
path.write_text(json.dumps(catalogue,indent=2,ensure_ascii=False),encoding='utf-8')
print(key,licence,info['descriptionurl'])
