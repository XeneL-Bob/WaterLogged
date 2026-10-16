# Mooloolaba accuracy investigation — 15 October 2026

The supplied WillyWeather screenshots show station wind and a NOAA WaveWatch III swell forecast. They do not show a tide-height plot. Comparing those directly with a model wind value or a tide prediction mixes different measurements and sources.

The app's original readings were reproduced at the public Mooloolaba point **-26.6825, 153.1201**, using rounded coordinates **-26.68, 153.12** for model requests. All local times below are Queensland AEST (UTC+10).

| Reading | Source and applicable time | Result |
|---|---|---:|
| Original app wind | Open-Meteo hourly model, 15 Oct 22:00 | 21.3 km/h |
| Station wind | BOM Sunshine Coast Airport, 15 Oct 22:00 | 28 km/h; 15 knots in the same feed |
| Later station wind | BOM Sunshine Coast Airport, 15 Oct 22:30 | 26 km/h; 14 knots in the same feed |
| Screenshot wind | WillyWeather labels Sunshine Coast Airport, screenshot around 22:34 | 27.8 km/h; gusts 38.9 km/h |
| Original app swell | Open-Meteo marine swell component, 15 Oct 22:00 | 1.02 m |
| Model total waves | Open-Meteo marine total significant waves, 15 Oct 22:00 | 1.36 m |
| Observed total waves | Queensland Mooloolaba buoy Hs, 15 Oct 21:00 | 1.652 m |
| Tide prediction | MSQ Mooloolaba, 15 Oct 22:20 | 1.132 m above LAT |
| Tide prediction | MSQ Mooloolaba, 15 Oct 22:30 | 1.134 m above LAT |

The tide value **1.134 m matches MSQ's published prediction** at 22:30. The roughly 2 m curve in the screenshot is labelled swell. It provides no evidence that the tide should be 2 m.

The wind discrepancy combines model-versus-observation differences, different locations/exposure, sample times and rounding. Fifteen knots converts to 27.78 km/h, consistent with WillyWeather's displayed 27.8; BOM reports whole km/h in its corresponding field. This explains the rounding, but the screenshot does not identify the exact underlying observation timestamp, so an exact sample match is not established. The app now uses the published km/h value directly rather than adding precision through a conversion.

The swell discrepancy is not fixed by changing a tide value or multiplying a forecast. WillyWeather identifies NOAA WaveWatch III as its swell forecast source; the original app uses Open-Meteo's best-match marine model. Their grids, model runs, coastal representation and wave definitions can differ. Total wave height includes more than the swell component, and buoy Hs is an observation of total significant waves, not a swell-only sensor. A single comparison cannot establish which swell model is consistently more accurate.

## Changes made

- Prefer the nearest supported BOM weather station within 50 km when a valid observation exists at or before the catch, no more than 60 minutes old. Current bundled coverage is 16 verified Queensland stations. Elsewhere or during outages, a labelled model estimate remains available.
- Keep sustained wind and gusts in separate fields. Prefer recent station values per field, with model fallbacks for missing values.
- Add a separate total-wave/Hs field. Prefer the nearest supported Queensland buoy within 50 km with a valid reading at or before the catch, no more than 90 minutes old. Current catalogue contains 27 buoy locations. Swell remains a separately labelled forecast.
- Retain MSQ tide predictions, their gauge choice and LAT datum. They are predictions of water level, not measured tide or tidal current.
- Show the source, applicable time and retrieval time for every automatic field. Explicit Refresh bypasses normal response-cache reuse; failed requests may use labelled cached responses, subject to reading-time checks.
- Add explicit point selection. Editing a place name alone does not change the coordinates or retarget the sources.
- Preserve saved catch snapshots, manual corrections and deliberately cleared fields.

The current weather catalogue is bounded pilot coverage, not a claim to include every Australian station. Closest distance also does not establish identical conditions at the fishing spot. Buoys are offshore observations; a nearby tide gauge may be on another waterway. Values are never adjusted merely to agree with another website.

## Reproduce the comparison

Use [TESTING_LOCATIONS.md](TESTING_LOCATIONS.md) for app and emulator steps. The read-only audit helper chooses nearby supported sources from the bundled catalogues:

```powershell
python tools/compare_conditions.py --latitude -26.6825 --longitude 153.1201 --at "2026-10-15T22:20:00+10:00"
```

It writes public source responses to the ignored `work/conditions-audit/` folder and never reads the catch database. BOM's feed has a rolling 72-hour history, the wave feed roughly seven days, and model forecasts have limited history. Re-running later may legitimately have no matching historical observation. The timestamped table above records the original investigation; it is not a claim about current conditions on another day.

Official references: [BOM station feed](https://www.bom.gov.au/products/IDQ60801/IDQ60801.94569.shtml), [BOM documented data feeds and non-commercial terms](https://www.bom.gov.au/catalogue/data-feeds.shtml), [Queensland near-real-time wave data](https://www.data.qld.gov.au/dataset/coastal-data-system-near-real-time-wave-data), [MSQ Mooloolaba predictions](https://www.data.qld.gov.au/dataset/mooloolaba-tide-gauge-predicted-interval-data), [Open-Meteo marine variable definitions](https://open-meteo.com/en/docs/marine-weather-api), [WillyWeather wind/source labels](https://wind.willyweather.com.au/qld/sunshine-coast/mooloolaba.html), [WillyWeather swell/source labels](https://swell.willyweather.com.au/qld/sunshine-coast/mooloolaba-beach.html).
