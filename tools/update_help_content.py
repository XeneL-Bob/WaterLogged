"""Keep offline help aligned with the observation-first pilot implementation."""
from pathlib import Path
import re

assets=Path(__file__).resolve().parents[1]/'app/src/main/assets'
path=assets/'help.html';text=path.read_text(encoding='utf-8')
text=text.replace('The six starter illustrations are decorative and should not be used to identify fish.',
                  'The offline catalogue has 32 species and 30 licensed reference images, including photographs and illustrations. Tap a species image for its creator, licence and source. Pearl perch and spotted mackerel use generic icons. Confirm fish identification independently.')
start='<section><h2>Automatic conditions</h2>'
section='''<section><h2>Automatic conditions</h2>
<p>New catches request a fresh foreground GPS fix. Choose location / test point lets you enter coordinates or select the public Mooloolaba example. Editing the place name changes only its label. On Home, Refresh my location captures the device location again.</p>
<p>Weather prefers a nearby supported BOM station observation at or before the catch, at most 60 minutes old. The pilot includes 16 Queensland stations. Missing values use labelled Open-Meteo model estimates. Sustained wind and gusts are separate fields.</p>
<p>Total significant wave height (Hs) prefers a nearby supported Queensland buoy observation at or before the catch, at most 90 minutes old. The pilot includes 27 buoy locations. A missing observation uses model total waves. Swell is a separate modelled component. Hs, swell and tide are different measurements; a swell graph does not establish the tide height.</p>
<p>Queensland tide predictions come from MSQ/BOM through Queensland Open Data. Heights are above Lowest Astronomical Tide (LAT); published times are Queensland UTC+10. The nearest supported gauge within 50 km is selected automatically. Use Tide gauge to choose another nearby gauge on your waterway. Rising/Falling/Turning describes water level, not tidal current. Freshwater skips waves, swell and tides.</p>
<p>Each automatic field shows its source, applicable time and retrieval time. Named station/buoy/gauge labels and model labels show their source distance. Moon phase is calculated offline using SunCalc. Weather/marine model and location-name requests use GPS rounded to two decimals, roughly a kilometre. Exact marks are matched locally to public station IDs and stay in private app storage unless you deliberately open a map.</p>
<p>Correct any field manually, including clearing it. Refresh conditions requests fresh responses without overwriting those corrections. Cached fallback readings keep their retrieval times and are labelled. Changing time or position invalidates automatic values from the old context; manual corrections survive. Reopening an old catch keeps its saved snapshot. Unsupported dates, missing data and outages leave manual entry available.</p>
<p>Valid edits auto-save after a short pause and on leaving/backgrounding; Save to Log saves immediately. Catches and metadata use private Room storage, camera images use private files, and provider responses use a separate disposable cache. Automatic Android backup and device transfer are disabled. An encrypted backup exported in Settings can restore the diary after reinstalling. GitHub backs up project code, not catch records.</p>
<p>Data credits &amp; licences and Species image credits are available below. BOM and Open-Meteo's free feeds in this personal/student pilot are non-commercial services; a commercial release requires appropriate provider permission/plans. Queensland tide/wave data is CC BY 4.0. Bundled images retain their individual licences.</p></section>'''
text,n=re.subn(re.escape(start)+r'.*?</section>',section,text,flags=re.S)
assert n==1,'Expected one automatic conditions section'
path.write_text(text,encoding='utf-8')
