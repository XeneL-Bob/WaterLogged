"""Finish a partially downloaded catalogue, respecting Commons rate limits."""
import html
import json
from pathlib import Path
import re
import time
import urllib.error
import urllib.parse
import urllib.request

root=Path(__file__).resolve().parents[1]
assets=root/'app/src/main/assets'
path=assets/'species_catalogue.json'
catalogue=json.loads(path.read_text(encoding='utf-8'))
audit=root/'work/reference-catalogues'

def get(url):
    for attempt in range(3):
        try:
            request=urllib.request.Request(url,headers={'User-Agent':'WaterLogged/0.3 offline species catalogue'})
            with urllib.request.urlopen(request,timeout=20) as r: return r.read()
        except urllib.error.HTTPError as e:
            if e.code!=429 or attempt==2: raise
            wait=max(60,int(e.headers.get('Retry-After','60')))
            print('Provider rate limit; waiting',wait,'seconds before retry.',flush=True)
            time.sleep(wait)
    raise RuntimeError('No response')

for entry in catalogue['species']:
    if 'photoAsset' in entry: continue
    try:
        cached=audit/(entry['id']+'.json')
        if cached.exists(): data=json.loads(cached.read_text(encoding='utf-8'))
        else:
            query={'action':'query','format':'json','generator':'search','gsrnamespace':6,
                   'gsrsearch':'intitle:"'+entry['scientificName']+'" filetype:bitmap','gsrlimit':8,
                   'prop':'imageinfo','iiprop':'url|extmetadata','iiurlwidth':640}
            data=json.loads(get('https://commons.wikimedia.org/w/api.php?'+urllib.parse.urlencode(query)))
            cached.write_text(json.dumps(data,indent=2),encoding='utf-8')
        for page in sorted(data.get('query',{}).get('pages',{}).values(),key=lambda p:p.get('index',99)):
            if any(w in page['title'].lower() for w in ('map','range','skeleton','otolith','stamp','larva','distribution','.svg','parasite','nematod','philometra')): continue
            info=page.get('imageinfo',[{}])[0];meta=info.get('extmetadata',{})
            value=lambda k: html.unescape(re.sub('<[^>]+>','',meta.get(k,{}).get('value',''))).strip()
            licence=value('LicenseShortName')
            if not (licence.startswith('CC BY ') or licence.startswith('CC BY-SA ') or licence in ('CC0','Public domain','PDM')): continue
            raw=get(info.get('thumburl',info.get('url','')).split('?')[0])
            if not(raw.startswith(b'\xff\xd8') or raw.startswith(b'\x89PNG')) or len(raw)>2_000_000: continue
            suffix='.png' if raw.startswith(b'\x89PNG') else '.jpg'
            relative='species/'+entry['id']+suffix
            (assets/relative).write_bytes(raw)
            entry.update(photoAsset=relative,photoTitle=page['title'][5:],photoAuthor=value('Artist'),
                         photoLicence=licence,photoLicenceUrl=value('LicenseUrl'),photoSource=info['descriptionurl'],
                         photoDescription=value('ImageDescription'),photoChanges='Resized by Wikimedia thumbnail service; displayed without further editing.')
            break
        print(entry['commonName'],entry.get('photoLicence','no eligible photograph'),flush=True)
        path.write_text(json.dumps(catalogue,indent=2,ensure_ascii=False),encoding='utf-8')
    except Exception as e: print(entry['id'],e,flush=True)
    time.sleep(3)
print('Verified photos:',sum('photoAsset' in e for e in catalogue['species']),flush=True)
