"""Generate offline credits from the reviewed manifest, not from guesses about filenames."""
import html
import json
from pathlib import Path

root=Path(__file__).resolve().parents[1]
assets=root/'app/src/main/assets'
path=assets/'species_catalogue.json'
catalogue=json.loads(path.read_text(encoding='utf-8'))
sections=[]
markdown=['# Species reference image credits','','Reference images are bundled for offline browsing. They include photographs and historical illustrations. Confirm species identification independently. Each image retains its stated licence; these licences do not describe the app source code.','', '| Species | Creator | Licence and source |','|---|---|---|']
for row in catalogue['species']:
    common=row['commonName'];scientific=row['scientificName']
    if row.get('photoAsset'):
        assert row.get('photoAuthor'), common+' needs a verified creator credit'
        if not row.get('photoLicenceUrl'):
            assert row['photoLicence']=='Public domain'
            row['photoLicenceUrl']=row['photoSource']+'#Licensing'
        row['photoLicenceUrl']=row['photoLicenceUrl'].replace('http://creativecommons.org/','https://creativecommons.org/')
        text=f"<p>{html.escape(row['photoAuthor'])}<br><a href='{html.escape(row['photoLicenceUrl'],quote=True)}'>{html.escape(row['photoLicence'])}</a><br><a href='{html.escape(row['photoSource'],quote=True)}'>Original file and attribution</a><br>{html.escape(row['photoChanges'])}</p>"
        markdown.append('| '+common+' | '+row['photoAuthor'].replace('|','/')+' | ['+row['photoLicence']+']('+row['photoLicenceUrl']+') · [Source]('+row['photoSource']+') |')
    else:
        text='<p>Generic icon. A suitable verified freely licensed image has not been bundled.</p>'
        markdown.append('| '+common+' | — | Generic icon |')
    sections.append('<section><h2>'+html.escape(common)+'</h2><p><em>'+html.escape(scientific)+'</em></p>'+text+'</section>')
path.write_text(json.dumps(catalogue,indent=2,ensure_ascii=False),encoding='utf-8')
page="<!doctype html><html lang='en'><head><meta name='viewport' content='width=device-width, initial-scale=1'><style>body{background:#032c4b;color:white;font:16px/1.6 sans-serif;padding:16px}a{color:#72c7ff}section{border-top:1px solid #246b97;padding:12px 0}h2{font-size:20px}</style></head><body><h1>Species image credits</h1><p>Offline reference photographs and illustrations. Confirm fish identification independently. Tap a source link for its original file and full licence.</p>"+''.join(sections)+'</body></html>'
(assets/'species_credits.html').write_text(page,encoding='utf-8')
(root/'docs/SPECIES_CREDITS.md').write_text('\n'.join(markdown)+'\n',encoding='utf-8')
print('Generated credits for',sum('photoAsset' in s for s in catalogue['species']),'images.')
