# Water Logged

A personal Android fishing diary based on the supplied Water Logged mockups. 

**Current version: pilot 0.4.1.** Encrypted backup/restore, CSV export, fishing sessions, diary search/filters, automatic-value reset, source-quality labels, saved tide charts and reusable custom species are available. Nearby observations take priority over labelled models. The bundled catalogue contains 32 species/30 licensed images, plus the end user's private custom species.

See [the new feature guide](docs/BACKUP_AND_SESSIONS.md) for usage, restore behaviour, privacy and pilot limits. Update the existing installation with Android Studio Run; do not uninstall it as an update procedure.

## Open in Android Studio

1. Start Android Studio. It is already installed on this computer at `D:\Android Studio`.
2. From the welcome screen choose **Open**. If another project is open, choose **File → Open**.
3. Select the **WaterLogged** folder containing this README and `settings.gradle`. Do not select just the `app` subfolder. This is a complete Android Studio project; no New Project wizard is needed.
4. Allow the Gradle sync to finish. An internet connection is needed for Room and any other uncached dependencies.
5. If prompted, use Android Studio's bundled JDK and install **Android SDK Platform 37.0**, **SDK Build Tools 36.0.0**, and Android Platform Tools through SDK Manager. These SDK components are already present on this computer.
6. Open **Tools -> Device Manager** and create an Android 8.0/API 26 or newer emulator, or connect an Android phone with USB debugging enabled. A real phone is best for camera and GPS testing.
7. Select the `app` run configuration and your device, then press **Run ▶**.
8. Start with **New -> Species -> catch details**. Allow foreground location access to prefill the general location name and available conditions. If permission is denied or GPS/data is unavailable, enter a location name and any conditions manually. Check the catch time and quantity, then tap **Save to Log**. Length and weight are optional.

If your fish is not in the 32-entry catalogue, choose **Log another species** and enter its name manually. Tap a reference image for its author, licence and source. Pearl perch and spotted mackerel currently use generic icons because suitable verified free images have not been bundled.

The Gradle wrapper is included. On Windows, a terminal inside this folder can run:

```powershell
.\gradlew.bat assembleDebug testDebugUnitTest lintDebug
.\gradlew.bat connectedDebugAndroidTest
```

The second command needs a running emulator or connected device. Ordinary instrumented tests use deterministic fixtures; the live-provider test is skipped unless explicitly enabled. To include the real weather/swell/tide request at public Mooloolaba example coordinates, run:

```powershell
.\gradlew.bat connectedDebugAndroidTest "-Pandroid.testInstrumentationRunnerArguments.liveConditions=true"
```

The live check also needs internet access. The project sets `android.injected.androidTest.leaveApksInstalledAfterRun=true` so connected test runs leave the app and its local data installed. Do not upload `local.properties`, `.idea`, `.gradle`, build folders, signing keys or the local `work` folder to GitHub; they are ignored.

This connected folder now has a local Git repository. Android Studio uses these same project files directly. Commit and push reviewed changes to save future source checkpoints; a GitHub source backup does not contain the user's private catch diary.

## What is implemented

- Home dashboard and My Log use the same List fragment, with every saved catch showing its title, date/time and length.
- New and existing catches use one shared Item fragment.
- Local Room tables for catches, species and fishing sessions; serial background writes and observable list updates.
- Password-protected logical backups containing photographs and snapshots; transactional merge restore preserves existing catches and restores favourite selections. CSV excludes coordinates by default.
- Start/end fishing sessions, reusable location/bait/method defaults, per-catch snapshots, duration and kept/released totals; trips with no catches remain in session history.
- Diary searches species/scientific names, aliases, location and notes; date-range/status/method/session filters combine with both DAO sort orders.
- Per-field automatic-value reset, Observed/Forecast/Manual/Cached labels, and source-day tide curves saved with catch snapshots.
- A date picker, time picker, editable title/location, four environment choices, optional length/weight, positive whole-number quantity, four catch statuses, bait, method and notes.
- Two remembered SQL sort orders: newest first and length lowest first, with unknown lengths last.
- Diary filtering by local calendar day; catches are never cleared automatically at midnight.
- Foreground GPS capture, permission handling, a 25-second timeout, stored accuracy and timestamp, and map launch.
- Full-resolution camera capture into private app storage, thumbnail display and a DialogFragment with pinch zoom, pan and reset.
- Share preview with title, location name, timestamp, length, quantity and status. Exact GPS coordinates, photos and notes are excluded.
- Debounced auto-save for valid edits, save on leaving/backgrounding, stable IDs across rotation, confirmed deletion and list refresh.
- WebView help available offline, official Queensland fishing links, and the original assignment's dummy URL as a separate reference button.
- 32 searchable species with persistent favourites and common/scientific-name aliases; 30 freely licensed offline reference images, including photographs and historical illustrations, with per-image credits.
- Automatic nearby location name, station weather, wave observations and model fallbacks; separately labelled swell, offline moon phase and nearest supported Queensland tide predictions.
- Per-field source area/gauge, distance, valid time and retrieval time; manual corrections survive refresh. Existing catches reopen without fetching new conditions.
- Explicit Room migrations from versions 1, 2 and 3 to version 4 preserve catches, snapshots, photo paths and favourites; session associations and saved tide curves are nullable additions.
- Blue backgrounds, white text, bright blue actions and licensed offline reference images. The supplied mockups guide the structure and colours; generic icons are used where a verified image is unavailable.

## Branding in 0.4.1

The launcher and toolbar now use original underwater logbook vector artwork being: a white diary, cyan waterline and bubbles. Adaptive launcher resources include circle/rounded-square support and an Android 13+ monochrome variant. The welcome subtitle is “Your fishing diary, above and below the surface.” Fish reference placeholders retain their separate fish icon.

## Automatic conditions and implementation decisions

| Automatic field | Source and location matching |
|---|---|
| General location name | Android Geocoder returns a locality/region from rounded GPS, with an approximate-coordinate area as a fallback. |
| Air temperature, sustained wind, gust and direction | Prefer the nearest supported [BOM observation station](https://www.bom.gov.au/catalogue/data-feeds.shtml) within 50 km, using a valid observation at or before the catch and no more than 60 minutes old. The current pilot has 16 verified Queensland stations. Missing values fall back to a labelled [Open-Meteo model](https://open-meteo.com/en/docs). |
| Swell height | [Open-Meteo marine API](https://open-meteo.com/en/docs/marine-weather-api), using a nearby marine model point. This is the swell component of waves. |
| Total significant wave height / Hs | Prefer a valid observation from the nearest supported [Queensland wave buoy](https://www.data.qld.gov.au/dataset/coastal-data-system-near-real-time-wave-data) within 50 km, at or before the catch and no more than 90 minutes old. There are 27 bundled buoy locations. Otherwise use labelled model total waves. This field is separate from swell and tide. |
| Moon phase | Calculated on the device for the catch timestamp using the [SunCalc 1.9.0 algorithm](https://github.com/mourner/suncalc/tree/v1.9.0); works without GPS or internet. |
| Queensland tide height and level trend | [MSQ/BOM predictions](https://www.msq.qld.gov.au/Tides/Open-data) through Queensland Open Data. The nearest supported gauge within 50 km is selected from a catalogue of 78 verified gauges. |

To use the upgrade:

1. Open **New Catch** and allow location access. The app requests a fresh foreground GPS fix and retrieves available conditions for the catch's time.
2. Review the prefilled values. Each automatic field shows its source, applicable time and retrieval time; weather/marine and tide labels also show the source point or gauge and its distance.
3. Use **Tide gauge** to choose another nearby gauge if it better represents your waterway. Straight-line proximity alone cannot confirm that a gauge describes your inlet or river.
4. Correct any field manually. **Refresh conditions** requests fresh provider responses while keeping manual corrections, including fields deliberately cleared. If the network fails, matching cached data can be used with its original retrieval time and a cached label.
5. Save the catch. Reopening it displays its saved snapshot without fetching replacements. Home separately offers nearby conditions through **Use my location** / **Refresh my location**, with a **Data sources & times** control.

The main decisions behind this implementation are:

- **Separate observations, forecasts and measurements.** Weather can come from a named station, total waves can come from a named buoy. Model fallbacks and swell forecasts are labelled. Queensland tide heights are predictions above Lowest Astronomical Tide (LAT). Rising/Falling/Turning describes water level, not tidal current; wind gusts are separate from sustained wind.
- **Use the catch's position and time.** Changing GPS or catch date/time invalidates automatic readings and requests replacements for the new context. Manual corrections survive. Hourly weather/marine values must be within 30 minutes of the catch time and within 50 km of the rounded position; tide samples must be within five minutes. Freshwater skips swell and tides.
- **Protect exact fishing marks.** Weather, marine and location-name requests use GPS rounded to two decimal places, roughly a kilometre. Exact coordinates remain in private local storage. Tide matching happens locally, and requests identify a public gauge and date. Location access is foreground only, with no background tracking.
- **Preserve saved evidence.** Values, per-field provenance and tide curves are stored with each catch independently of cache expiry. Explicit migrations through schema 4 preserve existing data. Reopening a saved catch never replaces its snapshot with today's conditions.
- **Handle partial results and outages.** Sources return independently with a 30-second overall completion timeout. Missing or unsupported values remain available for manual entry. A private cache reuses station responses for 5 minutes, buoy responses for 10 minutes, weather/marine models for 15 minutes, resource metadata for one day and tide-day predictions for 30 days. A weather/observation fallback response up to one day old is labelled as cached and keeps its original retrieval time; observation age limits still apply.

Data credits and licences are available in offline Help and by tapping a species reference image. Open-Meteo and Queensland tide/wave data are attributed under CC BY 4.0; SunCalc's BSD-2-Clause notice is included. Fish images retain their individual CC BY, CC BY-SA, CC0 or public-domain terms in [species credits](docs/SPECIES_CREDITS.md). This personal/student pilot uses free non-commercial Open-Meteo and BOM feeds. A commercial release needs appropriate [Open-Meteo service terms](https://open-meteo.com/en/terms) and [BOM data-service permission/licensing](https://www.bom.gov.au/catalogue/data-feeds.shtml).

See [the detailed conditions implementation notes](docs/CONDITIONS.md) for provider matching, timestamp handling, caching and extension points.

The [public-data maintenance tools](tools/README.md) explain repeatable source comparisons, catalogue updates and image/licence review. They operate on public reference data and do not access private catches.

## Accuracy investigation and changing the test location

The Mooloolaba screenshots compare airport station wind and a NOAA swell forecast with the app's original model readings. The original 21.3 km/h wind and 1.02 m swell were reproduced from Open-Meteo. MSQ confirms the app's 1.134 m tide prediction at 22:30 AEST on 15 October 2026, the roughly 2 m curve in the screenshot is swell, not a tide plot. No calibration factor or forced matching value has been applied. [The investigation](docs/DATA_ACCURACY.md) records the source values, exact sample times, reasons for differences and the fixes.

On Home or a catch, tap **Choose location / test point → Mooloolaba example → Use this point**. The public test point is **-26.6825, 153.1201**. Selecting another point recalculates the nearest supported weather station, wave buoy and automatic tide gauge. A location-name edit changes only the label. **Get GPS** / **Refresh my location** returns to the device's current position. [Location testing instructions](docs/TESTING_LOCATIONS.md) cover another Brisbane point, emulator GPS, matching timestamps/units and the read-only diagnostic script.

## How catches are logged and stored

Each catch has a stable UUID. Valid edits save after about 900 ms, when leaving/backgrounding, or immediately through Save to Log. The record and its per-field condition source/time/manual-override metadata are written to private Room storage. User camera photos are separate private files. Exact marks stay local unless the user deliberately opens them in a map app; weather/geocoder queries use rounded coordinates, and station/buoy/gauge requests use public identifiers.

Old catches keep their saved snapshot. Manual corrections survive refresh and context changes until explicitly returned to automatic. Public response-cache expiry does not alter saved catches. Android automatic backup/device transfer remain disabled; the user can now export and restore an encrypted diary file. Uninstalling or clearing storage removes the private diary unless a usable external backup has been kept. GitHub saves source code, not catches. [Storage details](docs/DATA_STORAGE.md) and [backup/session instructions](docs/BACKUP_AND_SESSIONS.md) explain the behaviour.

## MVP boundaries

Tide predictions currently cover supported Queensland gauges. Weather/marine requests for older dates outside the forecast provider's available history show an unavailable message; they do not substitute today's conditions. National tide coverage and long-term historical retrieval remain future work.

Catch-limit warnings, seasonal-closure checks and marine-park boundary alerts are not implemented. The app links to official guidance and reminds users to check their fishing area. It uses documented data feeds and bundled freely licensed reference images. There is no account, cloud sync, analytics or background location tracking.

GPS marks and photos remain inside Android's private app storage. Automatic Android backup and device transfer are disabled; encrypted export/restore is available in Settings. This is app sandbox protection, not additional database encryption. Uninstalling or clearing app storage removes the log. A shared location **name** can still reveal a private spot, and Show map sends the selected coordinates to the user's map application.

## Project structure

```text
app/src/main/java/au/com/waterlogged/
  MainActivity.java          Navigation and toolbar
  model/                    Room entities, DAOs, repository, validation, share formatter
  ui/                       Fragments, form view, adapters, photo viewer, visual helpers
  services/                 GPS, live conditions providers, parsers, provenance and private cache
app/src/main/res/            Theme, main layout, menus, icons and fish illustrations
app/src/main/assets/         Offline help, licensed fish images/credits and station/buoy/gauge catalogues
app/src/test/                Validation, sharing, coordinate and moon tests
app/src/androidTest/         Room, migration, parsers, snapshot UI and opt-in live-provider tests
docs/                       Rubric mapping, report notes, and device checklist
```

The code uses Java 17 with Android Views, AndroidX fragments and Room 2.6.1. Gradle 9.5.0 and Android Gradle Plugin 9.3.3 match this computer's installed Android toolchain. Minimum Android API: 26. Compile SDK: 37.0. Target SDK: 36. Room exports schemas under `app/schemas/`; versions 1–4 are retained. Java source is consistently formatted, with comments explaining snapshot, privacy and asynchronous-view decisions.

## Verification status

On **16 October 2026**, pilot **0.4.1** passed the debug build, **14 local tests** and **31 instrumented tests**, with one previously passed opt-in journey deliberately skipped after fixture cleanup. Lint reports **zero errors and 44 warnings**. Added rubric checks cover New/View controls, invalid numeric input, debounce saves, delete/cancel/list refresh and exact map/share intent payloads. Native emulator checks confirmed fresh Mooloolaba GPS capture, Google Maps launch and the required RTA WebView page. Earlier native checks covered camera/offline entries, encrypted backup/restore and 200% text. Physical-phone/API 26 checks remain pending. See [verification details](docs/VERIFICATION.md).

Real-phone/outdoor checks remain pending; I chose emulator testing for this upgrade. Routine tests never uninstall or clear the diary. The manual journey test is opt-in through `fieldJourney=true` only when its labelled fixture trip/catches/custom species have been created; ordinary runs skip it. The live-provider test is separately opt-in through `liveConditions=true`.

On **15 October 2026**, pilot **0.3.0** passed the actual debug build, Room generation, **11 local tests and 19 instrumented tests** on the Pixel 7 / Android 17 API 37 emulator. Android lint reported **zero errors and 35 warnings**. The live check retrieved Sunshine Coast Airport observations and Mooloolaba tide predictions; it used labelled model total waves when the buoy observation was stale. Tests also cover both version 1 and version 2 upgrades to schema 3, catalogue updates preserving favourites, stale/future observations and native decoding of all 30 bundled images. Home, source details and the species list were inspected in the running app at the public Mooloolaba point. See [verification details](docs/VERIFICATION.md).

Camera, map/share target apps, permission variants, Android 8 and broader screen/font configurations still need the hands-on checks in [the device checklist](docs/DEVICE_CHECKLIST.md). These results establish functioning retrieval and tested preservation rules, not that a forecast exactly describes the fishing spot.

Earlier verification:

On 14 October 2026, the real debug APK build, Room generation, 11 local tests, Android lint (zero errors, 36 warnings) and 12 instrumented tests passed on the connected Pixel 7 / Android 17 API 37 emulator. Tests cover DAO sorting/search, migration, data parsing, manual overrides, reopening without fetching, stale callbacks, activity recreation and an opt-in real provider request using public Mooloolaba example coordinates. See [verification details](docs/VERIFICATION.md).

The emulator verification used a simulated Mooloolaba GPS position. A real phone uses its own live location. The inspected home-screen image shows actual running app output, the original supplied mockups remain visual references.

The first instrumented run used Android's default uninstall-after-testing behaviour, clearing emulator app data. The app was reinstalled and the project now sets `android.injected.androidTest.leaveApksInstalledAfterRun=true`, verified on subsequent runs. No separate phone installation was touched. Migration preservation was tested separately using a version 1 fixture database.

## GitHub and the assignment

See `docs/RUBRIC_MAPPING.md` for the technical mapping and specific case-study variations.


## Primary technical references

- [Android Gradle Plugin 9.3 compatibility](https://developer.android.com/build/releases/agp-9-3-0-release-notes)
- [Room dependencies and Java annotation processing](https://developer.android.com/jetpack/androidx/releases/room)
- [Room DAO queries](https://developer.android.com/training/data-storage/room/accessing-data)

Bundled third-party reference images are individually attributed in `docs/SPECIES_CREDITS.md` and the app's offline credits.
