"""Fill the supplied CSC202 report template from verified Water Logged design evidence.

Run using the bundled document Python runtime. The original template is never changed.
Template distillation and render QA are recorded under ignored work/final-report.
"""
from copy import deepcopy
from hashlib import sha256
from io import BytesIO
from pathlib import Path
import json
import re
from zipfile import ZipFile, ZIP_DEFLATED
from xml.sax.saxutils import escape

from docx import Document
from docx.enum.table import WD_TABLE_ALIGNMENT, WD_CELL_VERTICAL_ALIGNMENT
from docx.enum.text import WD_ALIGN_PARAGRAPH
from docx.oxml import OxmlElement
from docx.oxml.ns import qn
from docx.shared import Inches, Pt, RGBColor
from PIL import Image, ImageDraw, ImageFont

ROOT = Path(__file__).resolve().parents[1]
TEMPLATE = Path(r'C:\Users\xenel\Downloads\2026_CSC202_T3_assignment_Report (1).docx')
OUT = ROOT / 'docs' / 'submission'
OUT.mkdir(parents=True, exist_ok=True)
FIG = OUT / 'diagrams'
FIG.mkdir(exist_ok=True)
EXPECTED = '4d166c7998afe5917609f37bfe1a92f197b6ea083b8be39c3f44f01e3f81290f'
assert sha256(TEMPLATE.read_bytes()).hexdigest() == EXPECTED, 'Template changed; re-distill first'

NARRATIVE = {
    'cover': 'Water Logged is an Android Studio fishing diary using the approved CSC202 Task 3 mapping. This report assesses its implemented features and explains the MVC-style design and Room database.',
    'scope': 'Tutor approval of the substitution and app name is confirmed by the student. Property entries become catches; inspection date becomes catch time; rent becomes length; bedrooms become quantity; type becomes environment; application status becomes disposition. Length supplies the assessed numeric sort.',
    'evaluation': 'This provisional self-assessment uses the approved mapping and remains subject to marking. New/View share CatchItemFragment; Home/My Log share CatchListFragment. Room supplies storage, refresh and SQL ordering. Help retains the exact RTA dummy link alongside fishing guidance.',
    'verification': 'Version 0.4.1 passed the build, 14 local tests and 31 instrumented tests on Pixel 7 / Android 17 API 37. One previously passed journey check was skipped after fixture cleanup. Lint: zero errors, 44 warnings. Native checks cover GPS/map, WebView, camera, offline saves, backup/restore and 200% text. Physical-phone and API 26 checks remain pending.',
    'list': 'CatchListFragment controls day, filters and ordering, observes repository results and passes rows to CatchAdapter. RecyclerView and bound cards form the View. CatchRecord, Species and WaterRepository form the Model; WaterDatabase exposes Room DAOs. MainActivity handles navigation and toolbar actions. A selected catch passes its UUID to Item UI. New opens species selection, then the same Item fragment in new-entry mode.',
    'list2': 'WaterRepository serialises background writes and returns main-thread callbacks. CatchDao uses ORDER BY caughtAt DESC and ORDER BY lengthCm IS NULL ASC, lengthCm ASC, with deterministic tie-breakers; unknown lengths come last. Filtering also stays in SQL. Room LiveData refreshes the list after writes. SharedPreferences retains the chosen sort.',
    'item': 'CatchItemFragment shares New/View behaviour, distinguishing species arguments from an existing UUID. CatchFormView owns widgets, presentation and typed extraction; InputRules checks numeric conversion/bounds. Optional length/weight may be empty; title, location and quantity are required. Invalid input preserves the last valid row. DatePickerDialog and TimePickerDialog edit timestamps. Delete appears only when reopening.',
    'item2': 'Stable UUIDs prevent duplicate saves. Valid changes save after 900 ms, on exit and backgrounding. Copies isolate queued writes; draft state survives recreation. Delete cancels pending saves before removing the row/photo. LocationCapture handles foreground permissions, fresh fixes and timeout; Show map uses the saved mark. TakePicture and FileProvider store private photos; PhotoDialog/ZoomImageView enlarge, pinch, pan and reset. CatchSummary previews the sharesheet payload.',
    'item3': 'ConditionsProvider separates retrieval from control. Nearby observations precede labelled models; ConditionMetadata preserves source, applicable/retrieval times and manual ownership. Reopening keeps saved snapshots. Weather/geocoder requests use rounded coordinates. Shares exclude exact marks, photos and notes, although place names can reveal a spot. The design is MVC-style: fragments also create views and manage Android lifecycle.',
    'database': 'WaterDatabase version 4 stores catches, species and sessions in water_logged.db, using TEXT primary keys. Catch speciesId/sessionId are logical links without foreign keys/cascades; stored titles remain independently readable. CaughtAt/lengthCm are indexed. Unique nullable sessions.activeSlot allows one active trip and multiple ended trips. Tested migrations 1→2→3→4 preserve records without destructive fallback.',
    'database2': 'There are no SQL DEFAULT clauses. New catches initialise UUID/current time, Inshore, quantity 1, Released, empty text, Unknown trend, Not recorded moon and Manual / not recorded source; optional fields are null. New sessions initialise UUID/current time, activeSlot 1, Inshore and empty text. Species come from the catalogue/custom entry; favourite uses INTEGER 0/1.',
    'storage': 'Private photos use photoPath references rather than blobs. Automatic Android backup/transfer is disabled. Encrypted .wlog exports include catches, photos, favourites, sessions and snapshots; restore validates and merges missing UUIDs. CSV is unencrypted, excluding coordinates by default. Clearing storage/uninstalling removes the diary without an external backup. Physical-device checks, wider coverage, catch-limit and marine-park alerts remain future work.'
}

# Every exported schema field is covered exactly once by these grouped rows.
DICTIONARY = {
 'catches': [
  ('id', 'TEXT NN PK', 'Stable catch UUID'),
  ('speciesId', 'TEXT NN', 'Catalogue or custom species association'),
  ('title / locationName', 'TEXT NN', 'Editable species title and general place'),
  ('caughtAt', 'INTEGER NN', 'Catch time in epoch milliseconds'),
  ('environment / disposition', 'TEXT NN', 'Water type and kept/released/bait/other'),
  ('lengthCm / weightKg', 'REAL', 'Optional measured length and weight'),
  ('quantity', 'INTEGER NN', 'Number of fish in this entry'),
  ('bait / method / notes', 'TEXT NN', 'Fishing details and multiline notes'),
  ('latitude / longitude', 'REAL', 'Exact private catch mark'),
  ('accuracyMetres', 'REAL', 'Device location accuracy'),
  ('locationCapturedAt', 'INTEGER', 'Location fix epoch milliseconds'),
  ('photoPath', 'TEXT', 'Private camera file path'),
  ('tideHeightM', 'REAL', 'Predicted/manual height above LAT'),
  ('tideDirection', 'TEXT NN', 'Water-level trend, not current speed'),
  ('windKmh / windGustKmh', 'REAL', 'Sustained wind and separate gust'),
  ('windDirection', 'TEXT NN', 'Compass wind direction'),
  ('airTemperatureC', 'REAL', 'Air temperature'),
  ('swellM / waveHeightM', 'REAL', 'Swell component and total significant waves'),
  ('moonPhase', 'TEXT NN', 'Calculated or manually selected phase'),
  ('conditionsSource', 'TEXT NN', 'Snapshot source summary'),
  ('conditionsMetadata', 'TEXT', 'Per-field provenance/ownership JSON'),
  ('tideStationId', 'TEXT', 'Gauge override; null selects nearest'),
  ('sessionId', 'TEXT', 'Optional trip association'),
  ('tideSeriesJson', 'TEXT', 'Saved daily tide samples and provenance')
 ],
 'species': [
  ('id', 'TEXT NN PK', 'Stable catalogue key or custom UUID key'),
  ('commonName / scientificName / aliases', 'TEXT NN', 'Display and case-insensitive partial search'),
  ('favourite', 'INTEGER NN', 'User preference as 0/1')
 ],
 'sessions': [
  ('id', 'TEXT NN PK', 'Trip UUID'),
  ('startedAt', 'INTEGER NN', 'Trip start epoch milliseconds'),
  ('endedAt', 'INTEGER', 'Trip end; null while active'),
  ('activeSlot', 'INTEGER UNIQUE', '1 while active; null after ending'),
  ('locationName / bait / method / environment', 'TEXT NN', 'Defaults copied to each new catch'),
  ('latitude / longitude', 'REAL', 'Optional default trip point')
 ]
}
schema = json.loads((ROOT/'app/schemas/au.com.waterlogged.model.WaterDatabase/4.json').read_text())
for entity in schema['database']['entities']:
    covered = [n.strip() for row in DICTIONARY[entity['tableName']] for n in row[0].split('/')]
    assert sorted(covered) == sorted(f['columnName'] for f in entity['fields'])
    assert not any(f.get('defaultValue') not in (None, 'undefined') for f in entity['fields'])

def diagram(name, boxes, arrows, size=(1400,850), notes=()):
    """Render deterministic engineering diagrams, also retaining editable SVG sources."""
    im=Image.new('RGB',size,'white'); draw=ImageDraw.Draw(im)
    regular=ImageFont.truetype(r'C:\Windows\Fonts\arial.ttf',25)
    bold=ImageFont.truetype(r'C:\Windows\Fonts\arialbd.ttf',29)
    svg=[f'<svg xmlns="http://www.w3.org/2000/svg" width="{size[0]}" height="{size[1]}" viewBox="0 0 {size[0]} {size[1]}"><rect width="100%" height="100%" fill="white"/>']
    for key,(x,y,w,h,title,lines) in boxes.items():
        draw.rounded_rectangle((x,y,x+w,y+h),radius=10,fill='#edf4f8',outline='#647b88',width=2)
        svg.append(f'<rect x="{x}" y="{y}" width="{w}" height="{h}" rx="10" fill="#edf4f8" stroke="#647b88" stroke-width="2"/>')
        for j,line in enumerate([title]+lines):
            yy=y+22+j*36; font=bold if j==0 else regular
            draw.text((x+w/2,yy),line,font=font,fill='#111111',anchor='mt')
            svg.append(f'<text x="{x+w/2}" y="{yy+25}" text-anchor="middle" font-family="Arial" font-size="{29 if j==0 else 25}" font-weight="{"bold" if j==0 else "normal"}">{escape(line)}</text>')
    for points,label,lx,ly in arrows:
        draw.line(points,fill='#283f4d',width=3)
        x,y=points[-1];px,py=points[-2];dx,dy=x-px,y-py;length=(dx*dx+dy*dy)**0.5;ux,uy=dx/length,dy/length
        triangle=[(x,y),(x-13*ux+6*uy,y-13*uy-6*ux),(x-13*ux-6*uy,y-13*uy+6*ux)]
        draw.polygon(triangle,fill='#283f4d')
        draw.text((lx,ly),label,font=regular,fill='#111111',anchor='mm',stroke_width=5,stroke_fill='white')
        draw.text((lx,ly),label,font=regular,fill='#111111',anchor='mm')
        svg.append('<polyline points="'+' '.join(f'{a},{b}' for a,b in points)+'" fill="none" stroke="#283f4d" stroke-width="3"/>')
        svg.append('<polygon points="'+' '.join(f'{a},{b}' for a,b in triangle)+'" fill="#283f4d"/>')
        svg.append(f'<text x="{lx}" y="{ly+8}" text-anchor="middle" font-family="Arial" font-size="25" fill="#111111" stroke="white" stroke-width="7" paint-order="stroke">{escape(label)}</text>')
    for x,y,t in notes:
        draw.text((x,y),t,font=regular,fill='#111111',anchor='mt')
        svg.append(f'<text x="{x}" y="{y+25}" text-anchor="middle" font-family="Arial" font-size="25">{escape(t)}</text>')
    svg.append('</svg>');im.save(FIG/(name+'.png'));(FIG/(name+'.svg')).write_text('\n'.join(svg),encoding='utf-8')

diagram('list-mvc',{
 'view':(60,25,760,130,'View',['RecyclerView + CatchAdapter','Header, catch cards and displayed values']),
 'nav':(960,25,380,130,'Navigation',['MainActivity','Toolbar and back stack']),
 'controller':(60,275,760,130,'Controller',['CatchListFragment','Home / My Log, filters and selected sort']),
 'repository':(60,545,760,130,'Model',['WaterRepository','CatchRecord + Species']),
 'dao':(960,545,380,170,'Room storage',['WaterDatabase','CatchDao + SpeciesDao','SQL ORDER BY'])
},[
 ([(290,275),(290,155)],'bind rows',285,205),
 ([(600,155),(600,275)],'tap catch',600,205),
 ([(820,330),(1140,330),(1140,155)],'navigate by UUID',1070,365),
 ([(290,405),(290,545)],'observe query',290,475),
 ([(600,545),(600,405)],'LiveData rows',600,475),
 ([(820,590),(960,590)],'query',890,555),
 ([(960,650),(820,650)],'rows',890,690)
],size=(1400,770),notes=[(700,735,'Room changes refresh the list; UI code does not reorder the rows.')])
diagram('item-mvc',{
 'form':(40,20,620,130,'View',['CatchFormView','Inputs, validation feedback and source labels']),
 'photo':(870,20,490,130,'Photo view',['PhotoDialog + ZoomImageView','Enlargement and gestures']),
 'controller':(40,285,620,130,'Controller',['CatchItemFragment','Shared New / View flow, stable UUID']),
 'service':(870,270,490,270,'Device and data services',['LocationCapture; TakePicture','FileProvider; map / share intents','ConditionsProvider interface','LiveConditionsProvider implementation','ConditionMetadata + CatchSummary']),
 'model':(40,575,620,155,'Model',['CatchRecord + InputRules','WaterRepository','WaterDatabase + CatchDao'])
},[
 ([(225,285),(225,150)],'bind / read',225,220),
 ([(660,305),(755,305),(755,85),(870,85)],'open photo',760,220),
 ([(660,375),(870,375)],'coordinate',765,410),
 ([(225,415),(225,575)],'save / find / delete',225,485),
 ([(510,575),(510,415)],'result callbacks',510,535)
],size=(1400,790),notes=[(700,755,'Snapshots and photos belong to the saved catch, independently of a later refresh.')])
diagram('database',{
 'species':(40,40,360,145,'species',['id PK','names, aliases, favourite']),
 'catch':(520,40,360,205,'catches',['id PK','speciesId logical link','sessionId optional link','fields and saved snapshot']),
 'session':(1000,40,360,145,'sessions',['id PK','trip time and defaults'])
},[
 ([(400,110),(520,110)],'0..1 species per catch',420,275),
 ([(1000,110),(880,110)],'0..1 trip per catch',980,275)
],size=(1400,390),notes=[(700,310,'Each species or trip can link to many catches.'),(700,350,'Logical associations only; no foreign-key constraints or cascade deletion.')])

d=Document(TEMPLATE); original_paragraphs=list(d.paragraphs)
for name in ['Title','Heading 1','Heading 2']:
    s=d.styles[name];s.font.color.rgb=RGBColor(0,0,0)
    for child in list(s.element.iter(qn('w:color'))): child.attrib.pop(qn('w:themeColor'),None)
d.styles['Normal'].font.name='Times New Roman';d.styles['Normal'].font.size=Pt(12)

def setp(p,text,style=None,size=12):
    p.clear();p.add_run(text);p.style=style or 'Normal'
    p.paragraph_format.space_after=Pt(6);p.paragraph_format.line_spacing=1.05
    for r in p.runs:r.font.size=Pt(size);r.font.color.rgb=RGBColor(0,0,0)
    return p

def insertp(after,text='',style='Normal',size=12):
    p=d.add_paragraph();setp(p,text,style,size);after._p.addnext(p._p);return p

def figure(after,name,caption):
    p=insertp(after);p.paragraph_format.keep_with_next=True
    p.add_run().add_picture(str(FIG/(name+'.png')),width=Inches(5.7))
    cap=insertp(p,caption,size=10);cap.paragraph_format.space_after=Pt(10)
    return cap

def format_table(table,widths,font_size=10.5,header=True,padding=65):
    table.autofit=False;table.alignment=WD_TABLE_ALIGNMENT.CENTER
    for col,width in zip(table.columns,widths):col.width=Inches(width)
    pr=table._tbl.tblPr
    borders=pr.find(qn('w:tblBorders'))
    if borders is not None:pr.remove(borders)
    borders=OxmlElement('w:tblBorders');pr.append(borders)
    for side in ['top','left','bottom','right','insideH','insideV']:
        el=OxmlElement('w:'+side);el.set(qn('w:val'),'single');el.set(qn('w:sz'),'4');el.set(qn('w:color'),'D9D9D9');borders.append(el)
    for ri,row in enumerate(table.rows):
        row.height=None
        trpr=row._tr.get_or_add_trPr();no_split=OxmlElement('w:cantSplit');trpr.append(no_split)
        if ri==0 and header:trpr.append(OxmlElement('w:tblHeader'))
        for ci,c in enumerate(row.cells):
            if c._tc.get_or_add_tcPr().find(qn('w:gridSpan')) is None:
                c.width=Inches(widths[ci])
            c.vertical_alignment=WD_CELL_VERTICAL_ALIGNMENT.CENTER
            tcpr=c._tc.get_or_add_tcPr()
            for old in list(tcpr.findall(qn('w:shd')))+list(tcpr.findall(qn('w:tcBorders'))):tcpr.remove(old)
            shd=OxmlElement('w:shd');shd.set(qn('w:fill'),'E8EEF2' if ri==0 and header else ('F7F9FB' if ri%2 else 'FFFFFF'));tcpr.append(shd)
            mar=OxmlElement('w:tcMar')
            for edge in ['top','bottom','left','right']:
                e=OxmlElement('w:'+edge);e.set(qn('w:w'),str(padding));e.set(qn('w:type'),'dxa');mar.append(e)
            tcpr.append(mar)
            for p in c.paragraphs:
                p.paragraph_format.space_after=Pt(0);p.paragraph_format.space_before=Pt(0);p.paragraph_format.line_spacing=1.0
                for r in p.runs:
                    r.font.name='Arial';r.font.size=Pt(font_size);r.font.color.rgb=RGBColor(0,0,0)
                    if ri==0 and header:r.bold=True
                    if r._r.rPr is not None:
                        for color in r._r.rPr.findall(qn('w:color')):color.attrib.pop(qn('w:themeColor'),None)
    # Merged continuation cells are not all exposed by python-docx row.cells.
    for old in list(table._tbl.xpath('.//w:tcBorders|.//w:tblPrEx/w:tblBorders')):
        old.getparent().remove(old)

# Preserve the cover's overall layout, replacing instruction text with useful project scope.
setp(original_paragraphs[4],'Water Logged App','Title',26)
original_paragraphs[4].alignment=WD_ALIGN_PARAGRAPH.CENTER
for node in [original_paragraphs[4]._p, d.styles['Title'].element]:
    for border in list(node.xpath('.//w:pBdr')):
        border.getparent().remove(border)
setp(original_paragraphs[7],NARRATIVE['cover'])
setp(original_paragraphs[8],'CSC202 Task 3    |    Pilot 0.4.1    |    16 October 2026',size=11)
setp(original_paragraphs[9],'Report based on the approved fishing logbook mapping',size=11)
original_paragraphs[9]._p.get_or_add_pPr().remove(original_paragraphs[9]._p.pPr.numPr) if original_paragraphs[9]._p.pPr.numPr is not None else None
metadata=d.tables[0]
metadata.cell(2,1).text='https://github.com/XeneL-Bob/WaterLogged'
metadata.cell(3,1).text='Not supplied'
format_table(metadata,[1.65,4.11],11,False)
setp(original_paragraphs[13],NARRATIVE['scope'],size=11)
setp(original_paragraphs[17],'App Functionality 80 marks','Heading 1',16)
setp(original_paragraphs[18],NARRATIVE['evaluation'],size=11)
setp(original_paragraphs[19],'Code implementation and self assessment',size=12)
marks=d.tables[1]
equivalents=[
 '1.a Catch title, date/time and length', '1.b Toolbar New / Sort / Help',
 '2.a Editable species title / location', '2.b Catch date button and date dialog',
 '2.c Environment, length, quantity, status, notes; safe numeric input',
 '2.d Catch GPS capture and saved map mark', '2.e Catch photo, camera and zoom',
 '2.f Share catch summary', '2.g Auto-save and list refresh',
 '3.a Reopen saved catch details', '3.b Delete and refresh the list',
 '4.a Catch date newest first through DAO', '4.b Length lowest first through DAO',
 '5.a Help implemented with WebView', '5.b Given RTA reference link'
]
marks.cell(0,3).text='Proposed\nmarks'
for index,text in enumerate(equivalents,1):
    marks.cell(index,1).text=text
    marks.cell(index,3).text=marks.cell(index,2).text.strip()
marks.cell(16,3).text='80'
format_table(marks,[1.3,3.02,0.72,0.72],10.2)
for row in marks.rows:
    for c in row.cells[2:]:
        for p in c.paragraphs:p.alignment=WD_ALIGN_PARAGRAPH.CENTER
setp(original_paragraphs[20],NARRATIVE['verification'],size=11)
setp(original_paragraphs[21],'Detailed evidence and remaining checks are recorded in docs/VERIFICATION.md and docs/RUBRIC_MAPPING.md. These proposed marks are not awarded grades.',size=10)

setp(original_paragraphs[22],'AI Use Declaration 5 marks','Heading 1',16)
original_paragraphs[22].paragraph_format.page_break_before=True
setp(original_paragraphs[23],'AI assisted the design, implementation, debugging, testing and preparation of this report. This declaration records that assistance; subsequent student edits should be added before submission.',size=11)
ai=d.tables[2]
ai.cell(0,1).text='OpenAI Codex (GPT-6); ChatGPT image generation for earlier concept mockups (image-model version not recorded).'
ai.cell(1,1).text='Codex generated and revised Java UI/controllers, Room entities/DAOs, services, XML resources, tests, Markdown documentation, report text and diagrams. It ran builds and emulator checks, diagnosed launch/data issues and implemented backup/session features. ChatGPT supplied the earlier visual mockups. The new logo is original Android vector artwork.'
ai.cell(2,1).text='“Build Water Logged against CSC202 using fragments, Room, DAO ORDER BY, GPS/maps, camera/zoom, sharing and WebView.” “Investigate Mooloolaba data differences and document private storage.” “Add backup, sessions and filtering; test on the emulator.” “Update the logo, audit the rubric and complete the report template.”'
format_table(ai,[2.05,3.71],10.5,False)
setp(original_paragraphs[32],'Class Design','Heading 1',16)
original_paragraphs[32].paragraph_format.page_break_before=True
setp(original_paragraphs[34],'MVC for List UI 5 marks','Heading 2',14)
setp(original_paragraphs[35],NARRATIVE['list'])
end=figure(original_paragraphs[35],'list-mvc','Figure 1  List UI responsibilities and data flow')
insertp(end,NARRATIVE['list2'])
setp(original_paragraphs[45],'MVC for Item UI 5 marks','Heading 2',14)
original_paragraphs[45].paragraph_format.page_break_before=True
setp(original_paragraphs[46],NARRATIVE['item'])
end=figure(original_paragraphs[46],'item-mvc','Figure 2  Shared New and View Item responsibilities')
end=insertp(end,NARRATIVE['item2']);insertp(end,NARRATIVE['item3'])
setp(original_paragraphs[53],'Database Design 5 marks','Heading 1',16)
original_paragraphs[53].paragraph_format.page_break_before=True
setp(original_paragraphs[54],NARRATIVE['database'])
end=figure(original_paragraphs[54],'database','Figure 3  Three tables and their logical associations')
end=insertp(end,NARRATIVE['database2']);end=insertp(end,'NN means NOT NULL; PK means primary key. Unmarked types are nullable. Grouped field names share the listed type. Every schema 4 column is included below.',size=10)
for table_name,rows in DICTIONARY.items():
    end=insertp(end,table_name,'Heading 2',14)
    end.paragraph_format.keep_with_next=True
    table=d.add_table(rows=1,cols=3)
    for c,t in zip(table.rows[0].cells,['Column','SQLite type','Usage']):c.text=t
    for values in rows:
        for c,t in zip(table.add_row().cells,values):c.text=t
    format_table(table,[2.1,1.05,2.61],10,padding=40)
    end._p.addnext(table._tbl)
    spacer=d.add_paragraph();table._tbl.addnext(spacer._p);end=spacer
insertp(end,NARRATIVE['storage'])

# Reuse the template layout but remove its unused blank answer-space paragraphs.
for p in original_paragraphs:
    if not p.text.strip() and not p._p.xpath('.//w:br|.//w:drawing'):
        # Preserve a few cover spacers so the title/metadata block retains the source rhythm.
        if original_paragraphs.index(p) not in [0,1,2,3,5,6,10,11,12,14,15,16]:
            parent=p._p.getparent()
            if parent is not None:parent.remove(p._p)
settings=d.settings.element
update=OxmlElement('w:updateFields');update.set(qn('w:val'),'true');settings.append(update)
buffer=BytesIO();d.save(buffer)
target=OUT/'WaterLogged_CSC202_Task3_Report.docx'
allowed={'word/document.xml','word/styles.xml','word/settings.xml','word/_rels/document.xml.rels','[Content_Types].xml'}
with ZipFile(TEMPLATE) as source,ZipFile(BytesIO(buffer.getvalue())) as built,ZipFile(target,'w',ZIP_DEFLATED) as out:
    for name in source.namelist():out.writestr(name,built.read(name) if name in allowed else source.read(name))
    for name in built.namelist():
        if name not in source.namelist():out.writestr(name,built.read(name))
with ZipFile(TEMPLATE) as source,ZipFile(target) as result:
    assert all(result.read(name)==source.read(name) for name in source.namelist() if name not in allowed)
assert sha256(TEMPLATE.read_bytes()).hexdigest()==EXPECTED

# Keep readable report design notes and full field definitions aligned with the generated report.
guide=['# Report design and implementation notes','','The completed template is in [submission/WaterLogged_CSC202_Task3_Report.docx](submission/WaterLogged_CSC202_Task3_Report.docx). Name and student ID are intentionally blank at the user’s request. Tutor approval of the fishing substitution and name was confirmed by the user on 16 October 2026. Self-assessed marks are proposals, not awarded grades.','']
for title,keys,picture in [
 ('MVC for List UI',['list','list2'],'list-mvc'),
 ('MVC for Item UI',['item','item2','item3'],'item-mvc'),
 ('Database design',['database','database2','storage'],'database')]:
    guide+=['## '+title,'']
    guide += [NARRATIVE[k]+'\n' for k in keys]
    guide += [f'![{title}](submission/diagrams/{picture}.png)','']
for name,rows in DICTIONARY.items():
    guide += ['### '+name,'','| Column | SQLite type | Usage |','|---|---|---|']
    guide += ['| '+' | '.join(row)+' |' for row in rows]
    guide += ['']
guide+=['NN = NOT NULL, PK = primary key; all other fields are nullable. Schema columns have no SQL DEFAULT clauses. New-object Java values are described above. Reference images are bundled assets linked by stable species IDs, rather than image blobs/resource integers in Room.','',
 '## Submission review','',
 'Fill your name/ID and review the self-assessment and declaration. Verify that the connected GitHub URL is the accepted Classroom 50 submission repository (the local remote alone does not prove Classroom registration). Push the final source and confirm the marker can access it. Rename the report using the course’s required naming convention; retain the submitted repository unchanged for one month as the supplied brief requests. Physical phone/API 26 checks remain pending.','']
(ROOT/'docs/REPORT_GUIDE.md').write_text('\n'.join(guide),encoding='utf-8')
print('Created',target)
print('Narrative words:',sum(len(re.findall(r"\b[\w’]+\b",x)) for x in NARRATIVE.values()))
print('Template preserve-only parts: byte-identical; all schema fields covered')
