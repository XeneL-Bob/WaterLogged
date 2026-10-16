"""Validate the bundled image manifest and generate an ignored local contact sheet."""
import json
from pathlib import Path
from PIL import Image, ImageDraw

root=Path(__file__).resolve().parents[1]
assets=root/'app/src/main/assets'
rows=json.loads((assets/'species_catalogue.json').read_text(encoding='utf-8'))['species']
assert len({r['id'] for r in rows})==len(rows), 'Duplicate species IDs'
canvas=Image.new('RGB',(1200,((len(rows)+3)//4)*180),'white')
draw=ImageDraw.Draw(canvas)
count=0
for i,row in enumerate(rows):
    draw.text((i%4*300+4,i//4*180+149),row['commonName'],fill='black')
    if 'photoAsset' not in row: continue
    for field in ('photoAuthor','photoLicence','photoLicenceUrl','photoSource'):
        assert row.get(field), row['id']+' missing '+field
    assert row['photoSource'].startswith('https://'), row['id']+' invalid source'
    assert not any(w in row.get('photoTitle','').lower() for w in ('parasite','nematod','philometra'))
    image=Image.open(assets/row['photoAsset']).convert('RGB')
    assert image.width<=2000 and image.height<=2000, row['id']+' too large'
    image.thumbnail((285,145));canvas.paste(image,(i%4*300,i//4*180));count+=1
destination=root/'work/species-review.jpg';destination.parent.mkdir(exist_ok=True);canvas.save(destination)
print(len(rows),'unique species;',count,'licensed reference images; contact sheet:',destination)
