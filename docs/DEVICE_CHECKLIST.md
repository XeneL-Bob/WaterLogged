# Android verification checklist

Automated build, Room, migration, conditions and emulator checks are recorded in VERIFICATION.md. Unchecked items below still need hands-on verification, particularly on a real phone and API 26. Instrumentation tests keep the app installed using `android.injected.androidTest.leaveApksInstalledAfterRun=true`.

## Version 0.4.1 marked-flow review

- [x] New/View share the Item fragment; Delete visibility, invalid quantities, auto-save, saved fields, date dialog, cancel/confirm deletion and return to List verified by RubricUiTest.
- [x] Saved public mark is correctly encoded in a map intent; native fresh simulated Mooloolaba GPS (5 m reported accuracy) and Google Maps launch verified.
- [x] Share preview/chooser payload excludes precise coordinates and private notes; no message sent.
- [x] Offline help and the exact RTA assignment reference page displayed inside WebView.
- [x] Underwater logbook launcher and toolbar artwork and revised welcome text inspected on API 37.
- [x] Labelled native GPS fixture deleted through the app; original two catch rows retained.
- [ ] Repeat marked device actions on a physical phone/API 26 and check recipient-app delivery.

## Build

- [ ] Gradle sync succeeds with the project's wrapper and Android Studio's bundled JDK.
- [x] `assembleDebug`, `testDebugUnitTest` and `lintDebug` succeed.
- [x] Room's annotation processor generates `WaterDatabase_Impl`; schema versions 1–4 are retained.
- [x] Actual Room queries and upgrades from versions 1, 2 and 3 to version 4 pass on the API 37 emulator, preserving controlled fixture data.
- [x] The 0.4 checks include unit tests, migration, encrypted backups, session counts, filtering, automatic resets and live provider retrieval. Exact final counts are in VERIFICATION.md.
- [ ] The app installs and starts on Android API 26 and a current Android version.

## Home and species

- [x] Running API 37 app displays the 32-entry catalogue and offline reference pictures; all 30 files decode through Android's image decoder.
- [x] Home's manual Mooloolaba point selects Sunshine Coast Airport observations and the Mooloolaba tide prediction, with source distances/times visible. Stale buoy data falls back to labelled model total waves.
- [x] Catalogue update preserves an existing favourite choice; common/scientific aliases and another point's station selection are covered by controlled tests.
- [ ] First launch shows Water Logged, New/Sort/Help and the empty diary message.
- [ ] Home, My Log, Species and Settings navigation all work.
- [ ] Search matches common names, scientific names, aliases and partial text without case sensitivity.
- [ ] Search for `pagrus aur` finds Snapper; `pagr` may also match Acanthopagrus bream, which is correct partial-name behaviour.
- [ ] Toggling a favourite reorders results and persists after restarting.
- [ ] A small screen and larger font size remain readable; all forms scroll without obscured actions.
- [x] Species/custom-catch screens inspected at 200% text on API 37. Scrollable headers keep the catalogue reachable. Broader small-screen/physical-phone testing remains pending.

## Version 0.4 emulator journey

- [x] Start a labelled trip at the public Mooloolaba point; reuse location, bait and method in two distinct catches.
- [x] Capture a photo with the emulator camera, accept it, attach it and open the full-size viewer.
- [x] Disable emulator Wi-Fi/mobile data, save another catch, background/reopen the app and reopen the saved record; restore connectivity afterward.
- [x] End the trip; summary shows two entries/two fish/two released and elapsed duration. Zero-catch trips are separately verified with a Room fixture.
- [x] Create an encrypted backup through the actual Android Documents UI, then merge it back without duplicating existing catches or restarting the ended trip.
- [x] Test CSV output, default coordinate exclusion, quotes and spreadsheet-formula escaping using isolated fixture data.
- [x] Save a local custom species, find it by a regional alias and retain it after activity recreation.
- [x] Verify and remove only the labelled test catches/trip/custom species/photo, preserving unrelated catch IDs. Remove the temporary external backup and restore font scale/connectivity.
- [ ] Repeat the journey on a physical phone and outdoors. The user chose emulator testing for this upgrade.

## Catch details and storage

- [x] Choosing a species opens New Catch with current date/time, selected title and no Delete button.
- [ ] Title/location are editable; date and time buttons open their dialogs.
- [ ] Four environments and four statuses are selectable; notes accept multiple lines.
- [ ] Empty optional length/weight save as unknown. Empty title/location/quantity display feedback.
- [ ] Invalid numeric text, overflow, negative quantities and decimal quantities never crash.
- [x] Valid edits auto-save after a pause and on leaving. Exactly one record is created.
- [ ] Returning to Home/My Log shows the saved title, date/time and length.
- [x] Reopening a record displays its saved fields and allows editing with the same fragment.
- [ ] Rotate while editing valid and invalid fields; text, selections, date/time, photo, UUID and scroll position survive.
- [ ] Rotate or recreate the activity during camera capture; the result is attached to the same catch.
- [ ] Background/restart preserves saved catches. Force-stop after an explicit Save preserves the catch.
- [ ] A chosen day includes midnight at its start and excludes the next day's midnight, using the device time zone.
- [ ] Newest sort and ascending-length sort produce correct results, with unknown lengths last. Sort choice survives restart.
- [x] Delete only appears for reopened records; cancel keeps the catch, confirm removes it and refreshes the log. It never reappears from a delayed save.

## Device actions

- [ ] Deny location permission: app remains usable with manual location-name entry.
- [ ] Grant approximate permission: capture works where the device supports it and shows reported accuracy.
- [x] With precise permission already granted on the emulator, Get GPS records a fresh simulated fix and reported accuracy. Physical capture remains pending.
- [ ] Device location disabled or no fix: actionable message appears, with no crash or permanent spinner.
- [x] Show map is disabled without a mark and opens the captured public mark in Google Maps on the emulator.
- [ ] Camera works on a real phone, including portrait/landscape orientation.
- [ ] Cancelled camera capture does not attach an empty photo.
- [ ] Replacing/removing a photo refreshes the thumbnail and preserves the correct saved file.
- [x] Tapping a photo opens the DialogFragment; pinch/pan/double-tap reset verified on API 37.
- [x] Share preview/chooser payload contains title, general location, timestamp, length, quantity and status; coordinates, photo paths and notes are absent.
- [ ] A chosen messaging app receives the summary through the Android sharesheet.

## Help and privacy

- [x] Bundled offline help renders in WebView; earlier offline catch journey verified separately.
- [ ] QLD rules and the exact RTA assignment reference URL load when connected, or show the unavailable-page message.
- [x] Manifest/service review confirms foreground location only; missing provider values remain unknown.
- [x] Stored conditions remain attached to the original catch rather than changing to today's readings.
- [ ] New Catch obtains a fresh GPS location and prefills available nearby conditions.
- [ ] A denied permission or unavailable data source leaves manual entry usable.
- [x] Correct a prefilled value, then refresh: the manual correction is kept.
- [ ] Change date/time or GPS: automatic values update for the new context; manual values remain.
- [ ] Select a nearby Queensland tide gauge on the correct waterway.
- [ ] Freshwater skips marine/tide requests. Unsupported old dates leave unknown fields blank.
- [ ] Data credits & licences shows source attribution and the SunCalc licence.
- [ ] No user catch data is placed in the source repository or the project ZIP.

Record device/API, build date, failures and fixes beside each completed group. Use actual app screenshots for the report and any demonstration video.
