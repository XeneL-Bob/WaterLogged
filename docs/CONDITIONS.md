# Automatic conditions in pilot 0.3

New Catch requests a fresh foreground GPS fix once. A manually selected point works without GPS permission. Home supports live GPS or an explicit test point. Existing catches reopen without fetching replacements. Editing a location-name label alone does not retarget source selection.

| Field | Preferred source | Labelled fallback |
|---|---|---|
| General place name | Android Geocoder at coordinates rounded to two decimals | Rounded-coordinate area name |
| Temperature, sustained wind, gust, direction | Nearest supported BOM station within 50 km; observation at/before catch, maximum age 60 minutes | Open-Meteo best-match weather grid; nearest hourly value within 30 minutes and point within 50 km |
| Total significant waves / Hs | Nearest supported Queensland buoy within 50 km; observation at/before catch, maximum age 90 minutes | Open-Meteo total significant wave estimate |
| Swell component | Open-Meteo marine model | Manual entry if missing |
| Queensland tide height/trend | Nearest supported MSQ prediction gauge within 50 km, or user-selected nearby gauge | Manual entry if unsupported |
| Moon phase | Offline SunCalc 1.9.0 calculation for catch timestamp | Manual selection |

Current offline reference coverage: 16 verified Queensland weather stations, 27 Queensland buoy locations and 78 Queensland tide gauges. This is nearest supported source selection, not a claim that every Australian sensor is represented. Source positions can be approximate. A nearby station can have different wind exposure; a nearby tide gauge can be on another waterway.

Weather/marine model and geocoder queries use rounded coordinates. Station, buoy and tide requests use public identifiers; exact-point matching happens on device. Each automatic field stores its selected value, source label, applicable time and retrieval time. Model point distances are from the rounded query point; station/buoy/gauge distances are from the selected local point. Provider network requests do not upload the catch record or camera photo.

BOM observations are selected at or before the catch and within one hour. The documented feed has a rolling 72-hour history. Null, missing, out-of-range or sentinel values remain missing, allowing a per-field model fallback. Published km/h values are used directly; the app does not manufacture extra precision by converting rounded knots. Gust and sustained wind are different fields. The personal/student pilot uses BOM's documented free non-commercial feed, not an HTML scraper. Commercial release needs appropriate permission/licensing.

Queensland buoy Hs is an observation of total significant waves: approximately the average of the highest third of wave heights in the recording period. It is neither Hmax nor a swell-only measurement. Published DateTime is interpreted as AEST/UTC+10. Future observations and readings over 90 minutes old are rejected. The public near-real-time feed has approximately seven days of history and can be delayed. Swell remains a model estimate even when total waves come from a buoy.

Open-Meteo requests the catch's UTC date and following day to cover hourly selection across midnight. Hourly model values may be slightly before or after catch time; this is explicit forecast/estimate matching, not observation matching. Unsupported old dates, distant grid cells and invalid values are rejected. Different models, forecast runs, wave definitions and exposure can differ from other websites. No empirical correction factor has been fitted from one screenshot.

MSQ datasets are resolved for the catch's Queensland calendar year. Tide timestamps use UTC+10 year-round; heights are metres above Lowest Astronomical Tide (LAT). A prediction must be within five minutes of the catch. Trend is derived from neighbouring predicted heights, with a nearly level prediction labelled Turning. It does not determine current direction or actual observed water level. At a day's first/last prediction the trend can be unavailable.

Manual edits, including clears, retain ownership through refresh. Changing position or time clears automatically owned values from the old context, then retrieves replacements. Changing environment/gauge invalidates affected marine/tide values. Freshwater skips marine/tide requests. A request generation counter ignores results for an older context or destroyed view. The catch snapshot stays in Room independently of response-cache expiry.

| Cached response | Normal reuse |
|---|---:|
| BOM station observations | 5 minutes |
| Queensland buoy response | 10 minutes |
| Weather/marine models | 15 minutes |
| Tide resource metadata | 1 day |
| Tide-day predictions | 30 days |

Explicit Refresh conditions bypasses normal response reuse, and choosing a point requests fresh data. If retrieval fails, a matching cached response may be reused with its original retrieval timestamp and a cached/refresh-unavailable label. Weather/observation fallback responses can be up to one day old, but per-reading valid-time limits still apply. The cache holds at most 64 responses, uses bounded HTTPS reads and connection/read timeouts, and has a 30-second overall batch completion timeout. Partial results arrive independently. Expiring or deleting cache never changes saved catch rows.

Room schema version 4 adds nullable session/curve fields and a session table, following the earlier metadata/gauge and total-wave/gust migrations. Catch/photo/favourite preservation is tested. Catalogue updates preserve favourite choices and saved titles. Encrypted backups include snapshots, metadata and tide curves; explicit restore recovers favourite selections.

Version 0.4 adds **Use automatic value again** for individual manual fields, without releasing other overrides. Source-quality/age labels distinguish observations, forecasts, predictions, calculations and cached values. Tide charts use the returned interval prediction series and store it with the catch; they do not synthesize a curve from a single tide height. Time/point/gauge/environment changes invalidate the old curve. Reopening a saved catch uses the stored curve without network requests. New catches in a session reuse defaults but request independent conditions for their own time. See [BACKUP_AND_SESSIONS.md](BACKUP_AND_SESSIONS.md).

See [accuracy investigation](DATA_ACCURACY.md), [location testing](TESTING_LOCATIONS.md), [storage/privacy](DATA_STORAGE.md) and [image credits](SPECIES_CREDITS.md). Weather/marine and Queensland data attribution, SunCalc's BSD-2-Clause notice and individual image licences are available in offline Help. Open-Meteo's free service and BOM anonymous feeds are for this non-commercial pilot; provider terms must be addressed before a commercial release.
