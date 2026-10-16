# Verification record

## Pilot 0.4.1 — 16 October 2026

Final combined build/check succeeded: `assembleDebug`, `testDebugUnitTest`, `lintDebug`, `connectedDebugAndroidTest`, with live conditions enabled and the opt-in manual journey disabled. **14 local tests passed. Connected XML: 32 tests, 31 passed, zero failures/errors, one skipped. Lint: zero errors, 44 warnings.** The skipped journey passed in the preceding 0.4 work and cleaned its fixtures; it is not claimed as rerun here.

Branding now uses original underwater logbook vectors in the launcher and List toolbar, with adaptive and API 33+ monochrome resources. Native launcher and toolbar screenshots were inspected. The subtitle is “Your fishing diary, above and below the surface.” Help/Settings now distinguish disabled automatic Android backup from the available encrypted export/restore feature. Version 0.4.1/code 5, Room schema remains 4.

Added **RubricUiTest (2 tests)**:

- New catches hide Delete; reopened catches show it. Current form edits write through the 900 ms debounce while the screen stays open, with one UUID despite repeated saves. Invalid/empty/negative/decimal/overflow quantities show feedback. Saved title, length, quantity and multiline notes reopen correctly. The date dialog opens/cancels.
- Delete cancellation preserves a fixture; confirmation removes it, shows the List fragment and prevents a delayed auto-save from recreating it. Unrelated catch IDs are checked before/after.
- A no-mark form disables Show map. A saved public mark produces the expected geo intent targeting Google Maps. Share preview opens a chooser intent containing the general place/length and excluding exact coordinates and private notes. Test interception prevents any message from being sent.

The first targeted share test asserted before Android delivered its accessibility-click callback. Waiting for the chooser callback fixed that test timing issue; production sharing was unchanged. The empty-map assertion was also made independent of asynchronous automatic GPS delivery. The final full run includes both corrected checks.

Additional native checks on **Pixel 7 / Android 17 API 37, emulator-5554 only**:

- Offline help displayed in WebView. The exact Assignment reference URL loaded the RTA Preparing for a tenancy page inside that WebView.
- A clearly labelled temporary catch captured a fresh simulated public Mooloolaba fix: **-26.682500, 153.120100**, reported accuracy **5 m**. Show map opened Google Maps with the same coordinates and Water Logged catch label. This is emulator GPS evidence, not a physical accuracy claim.
- The temporary GPS catch was removed through Delete confirmation. Native diary inspection retained the two original rows (Snapper and Dusky flathead). No uninstall or Clear storage was used.
- Launcher artwork and revised Home greeting were inspected. Screenshots remain under ignored `work/`.

The report template copy contains the adapted self-assessment, two MVC diagrams and all three schema-4 tables. Its generator verifies complete exported-column coverage and preserves untouched template parts. The original Downloads file remains unchanged. Word/read-only conversion plus the packaged document rasterizer is used for visual QA because LibreOffice is absent on this host.

Remaining: physical phone/outdoor use, Android API 26 runtime, denied/approximate permissions, broader screen/launcher variants and recipient-app delivery. Lint maintenance warnings remain; two monochrome warnings concern API 26 resources whose API 33 overrides already include monochrome layers. [FINAL_REVIEW.md](FINAL_REVIEW.md) and [RUBRIC_MAPPING.md](RUBRIC_MAPPING.md) distinguish implemented features, verified paths and submission checks.

An intermediate search edit failed Room compilation because of missing whitespace between SQL tokens. This was corrected before the final successful combined run. No failed build was installed.

Final audit also corrected both diary search queries to match the catalogue common name after the catch title is renamed. The existing Room filter/sort test now verifies that case under both orderings.

## Pilot 0.4.0 — 16 October 2026

The latest full build/test command succeeded. **14 local tests passed**. The connected XML records **30 tests: 29 passed, zero failures/errors, one skipped**. The skipped test is the opt-in manual emulator journey; it passed in the preceding run and cleaned up its labelled fixtures, so it is deliberately not repeated against missing fixtures. Android lint reported **zero errors and 41 warnings**. All four Room schema versions are retained.

| Check | Result | Evidence/scope |
|---|---|---|
| Local tests | 14 passed | Earlier validation/share/geometry/moon tests plus session defaults and chunked encryption round-trip/randomness/wrong-password/tamper/truncation checks |
| Room migrations | 3 passed | Versions 1, 2 and 3 upgraded to version 4; existing fields/favourites preserved and new session/curve fields nullable |
| New storage/features | 5 passed | Combined search/date/status/method filters, both SQL orders, zero-catch trips, session counts, backup photo/snapshot/custom-species/favourite recovery, conflict preservation, repeated-restore deduplication, wrong passwords and archive traversal rejection, CSV coordinate exclusion/formula escaping |
| Earlier Room functionality | 4 passed | Original ordering/storage/search/catalogue tests |
| Species assets | 1 passed | Android decodes all 30 licensed reference images |
| Conditions parsing/provenance | 5 passed | Earlier fixtures plus source-quality/cache labels and stored tide-series round trip |
| Observation parsing | 4 passed | Weather/buoy source selection and stale/future/range checks |
| Conditions UI | 5 passed | Earlier lifecycle tests plus individual automatic reset and offline reopening/recreation of a saved tide chart |
| Live source integration | 1 passed | Actual public Mooloolaba source requests on the emulator |
| Zoom gestures | 1 passed | Native multi-pointer pinch, pan and double-tap reset change/reset the displayed image matrix |
| Manual journey verification/cleanup | Passed separately; skipped on final rerun | Actual labelled camera/offline catches retain distinct IDs/times, session defaults, coordinates, metadata and a decodable photo; only test records/photo/trip/custom species removed, unrelated catch IDs preserved |

The final connected run used `liveConditions=true` and `fieldJourney=false`. The preceding journey run used both flags as true; its XML is retained under ignored `work/verification-v04/field-journey-run.xml`. Without explicit opt-in flags, network and manual-journey tests are skipped; controlled fixture tests still run. Do not enable `fieldJourney=true` without its labelled manual fixtures.

A final wording-only cleanup removed implementation terminology from the search dialog, followed by another successful debug build/lint check. The resulting APK was installed over the emulator app using a data-preserving update. A final diary inspection confirmed the two unrelated original entries remained after fixture cleanup. No functional code changed after the successful full test rerun.

The first added zoom test used 200–500 pixel finger spacing. The device reports a **446-pixel minimum scaling span**, so that sequence only crossed the threshold at its last move and never supplied a subsequent scaling move. The test now uses the framework threshold (546-pixel initial spacing on this emulator) and passes pinch/pan/reset. Production zoom code did not need alteration. An earlier test compilation failure used try-with-resources on RoomDatabase, which does not implement AutoCloseable; its fixture now closes explicitly.

### Native emulator journey

- Started `WaterLogged_emulator_trip` at public Mooloolaba coordinates, with Prawn/Boat defaults.
- Logged a camera catch, accepted the emulator camera's generated image, attached it and opened the full-size viewer.
- Disabled emulator Wi-Fi/mobile data, logged another catch, backgrounded/reopened the app and reopened that saved record. Connectivity was restored.
- Ended the trip: summary showed **two entries, two fish, zero kept, two released**, with elapsed duration. Zero-catch history is covered by a separate Room fixture.
- Created a `.wlog` through the actual Android Downloads document picker. It included the test trip/catches and existing records. Merging it back reported **zero missing catches and zero sessions**, preserving existing UUIDs and the already-ended trip. The temporary external backup was deleted afterward.
- Added a labelled custom species, found it using `emulator_alias`, and verified persistence across activity recreation. The custom/catch flow was inspected at **200% font scale**; the original **1.0** scale was restored.
- The opt-in journey test verified the saved records and removed only owned fixtures, including any catch from the labelled custom-species flow. It verified unrelated catch IDs remained present. No uninstall or Clear storage was used.

Device: Pixel 7 emulator / Android 17 API 37, **emulator-5554 only**. Screenshots are under ignored `work/`. The app and test APK remain installed. The user explicitly chose emulator testing instead of connecting a physical phone.

Remaining limitations: actual outdoor readability/fishing-trip use, physical phone GPS/camera orientation, API 26 runtime, permission variants, map/share target apps and broader screen sizes still need hands-on checks. Successful retrieval tests do not prove forecast accuracy at the fishing spot. The current backup format is a pilot format with documented size limits; CSV output is deliberately unencrypted and is not a restore format.

Details and user steps are in [BACKUP_AND_SESSIONS.md](BACKUP_AND_SESSIONS.md). Standard build/test outputs remain at the paths listed below.

## Pilot 0.3.0 — 15 October 2026

| Check | Result | Scope |
|---|---|---|
| Actual Android debug build | Passed | Gradle 9.5.0, AGP 9.3.3, installed SDK and actual Room 2.6.1 processor; version 0.3.0 / code 3 |
| Local unit tests | 11 passed | Validation, snapshot copy/share privacy, rounded coordinates, distance/compass and lunar phase dates |
| Android lint | 0 errors, 35 warnings | Remaining warnings include English text construction, dependency versions, target API and existing photo/view implementation |
| Room generated schema | Passed | WaterDatabase_Impl generated; schema versions 1, 2 and 3 retained |
| Room sorting/search/storage | 4 passed | Both DAO ORDER BY queries, date filter, aliases/favourites and save/update/delete; catalogue update preserves favourite choices |
| Version 1 and version 2 to 3 migrations | 2 passed | Controlled schema fixtures retain catch fields, notes, exact mark, photo path, snapshot metadata, gauge and favourites; new wave/gust columns nullable |
| Conditions parsing/provenance | 4 passed | Manual corrections/clears, JSON round trip, missing values, wrong-time/distant-grid rejection, UTC+10 tide timestamps/trend and nearest gauge |
| Observation parsing/source selection | 4 passed | Observations at/before catch, stale/future rejection, invalid values, gust versus sustained wind, buoy Hs versus Hmax/swell, changed-point station selection and unsupported inland point |
| Conditions UI/lifecycle | 3 passed | Reopening does not fetch; refresh keeps manual corrections; stale callbacks cannot update replacement views; activity recreation retains drafts/snapshots |
| Offline species assets | 1 passed | Android decodes all 30 image files; catalogue has 32 unique IDs and image credits |
| Live-provider integration | 1 passed | Actual HTTPS at public Mooloolaba coordinates returned location, station weather/gust/direction, model swell/total waves, tide/trend and moon |
| Image attribution review | Passed | Manifest/licence/author/source checks and visual contact-sheet review; 30 eligible images, two generic placeholders |
| Native UI inspection | Passed for inspected screens | Home manual-point dialog, Mooloolaba dashboard, source/timestamp dialog and species list in the running API 37 app |
| Keep-installed setting | Verified | App remains installed after connected tests; no uninstall or Clear storage used during this upgrade |

The final combined run completed with **11 local + 19 instrumented tests**, zero failures/errors/skips. The live test was enabled through `android.testInstrumentationRunnerArguments.liveConditions=true`; without it, that one network-dependent test is deliberately skipped. Ordinary tests use controlled fixtures.

The final live run at approximately **23:35 AEST** used a **23:00 Sunshine Coast Airport observation**: wind 24 km/h, gust 35 km/h and temperature 20.1°C. Swell was a separately labelled 1.12 m hourly model estimate; total waves fell back to the model at 1.36 m because no buoy observation met the 90-minute freshness limit. Mooloolaba tide was a 1.068 m prediction applicable at 23:40. These are timestamped test results, not current readings or replacements for the earlier screenshot comparison in [DATA_ACCURACY.md](DATA_ACCURACY.md).

The running Home check at approximately **23:39 AEST** selected the public Mooloolaba example through the actual dialog and displayed a newer **23:30 station observation**, confirming fresh point selection bypassed the earlier station response cache. Sources displayed Sunshine Coast Airport at 9.4 km, the marine model point at 9.3 km and the Mooloolaba gauge at 1.3 km. Wind displayed 26 km/h; total waves and swell remained distinct. The species screen showed 32 entries and decoded photographs. Screenshots are retained only under ignored local `work/`.

Device: Pixel 7 emulator, Android 17 / API 37, **emulator-5554 only**. The separate phone installation was not accessed. This check does not establish real-phone camera, permission, map or share behaviour.

Executed build tasks: `assembleDebug`, `testDebugUnitTest`, `lintDebug`, `connectedDebugAndroidTest`, with the live-provider argument, offline Gradle dependency resolution and the installed Android Studio JDK. Provider HTTPS requests themselves used the network.

Outputs:

- APK: `app/build/outputs/apk/debug/app-debug.apk`
- Instrumented report: `app/build/reports/androidTests/connected/debug/index.html`
- Instrumented XML: `app/build/outputs/androidTest-results/connected/debug/`
- Local results: `app/build/test-results/testDebugUnitTest/`
- Lint: `app/build/reports/lint-results-debug.html`

Two intermediate failures corrected test expectations: the live smoke test originally expected a model source after observations became preferred, and a stale-buoy fixture still contained a later valid observation. The final run passed with corrected assertions. No value was adjusted to force agreement with another weather website.

## Earlier verification and preservation limits

On 14 October, the previous build passed 11 local tests, 12 instrumented tests and lint with zero errors/36 warnings. It also checked the startup resource-ID regression, version 1 → 2 migration and earlier Home dashboard. A queued Room test cleanup race and an API-27 theme attribute in the API-26 base theme were corrected during that earlier work.

The first instrumented run on 14 October used Android's default uninstall-after-test behaviour and removed the emulator app/data. The app was reinstalled and `gradle.properties` now sets `android.injected.androidTest.leaveApksInstalledAfterRun=true`; subsequent runs, including this upgrade, leave it installed. Catches predating that initial removal cannot be claimed as recovered. Migration preservation is established independently with controlled version 1 and 2 fixtures; a separate phone installation was never touched.

Still pending: a real phone, API 26 runtime, permission-denial/approximate variants, actual camera orientation/zoom gestures, map/share target apps, remote WebView behaviour, larger fonts/smaller screens and remaining [DEVICE_CHECKLIST.md](DEVICE_CHECKLIST.md) items. Forecast accuracy across locations/time needs ongoing comparisons of matching measurements and timestamps; a passing integration test establishes retrieval, not forecast skill.
