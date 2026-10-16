# Testing location matching and changed sources

## In the app

1. On Home or a catch, tap **Choose location / test point**.
2. Tap **Mooloolaba example** to fill **-26.6825, 153.1201**, then **Use this point**. No location permission is needed for a manually chosen point.
3. On a catch, confirm its date/time before comparison. Home uses the current time. Wait for retrieval to finish, then inspect **Data sources & times** on Home or the source labels under each catch field.
4. For this point, the nearest supported weather station is Sunshine Coast Airport; the supported wave buoy and tide gauge are named Mooloolaba. A recent observation must exist within the documented age limits. A missing/stale sensor reading produces a labelled model fallback, not a fabricated observation.
5. Compare the same timestamp and quantity: sustained wind versus sustained wind; gust versus gust; buoy total-wave Hs versus significant total waves; swell forecast versus swell forecast; tide metres above LAT versus tide metres above LAT. Record source distances and forecast/observation status.
6. Select another point, for example **-27.39, 153.12** near Brisbane Airport. Weather, buoy and automatic tide selection are recalculated for that point. Nearby supported sources can differ, and inland/unsupported areas can have no marine observation or tide gauge.
7. Tap **Get GPS** in a catch or **Use/Refresh my location** on Home to use the device location again. Editing only the location-name text changes the label, not the lookup point.

Manual corrections, including cleared fields, survive location/time changes. This is intentional: check those values yourself when moving a catch to another place. Selecting a manual point resets the catch's tide-gauge override to nearest automatically; choosing a gauge afterward explicitly overrides it. Saved catches reopen with their saved snapshot.

## Android Studio emulator GPS

Use **Device Manager → emulator → Extended controls → Location** to set latitude/longitude and send a location fix. Depending on the embedded-emulator version, open its standalone window to access Extended controls. The app still needs foreground location permission and a fresh fix.

Alternatively, from Android Studio's terminal:

```powershell
& "$env:LOCALAPPDATA\Android\Sdk\platform-tools\adb.exe" -s emulator-5554 emu geo fix 153.1201 -26.6825
```

The command order is **longitude, latitude**. It changes the emulator only. Tap Get GPS / Refresh my location and send another fix while capture is waiting if necessary. A fresh GPS fix is given a short chance to replace a network-area fix. The explicit in-app test point is the most repeatable source-comparison method because it does not depend on emulator network-location behaviour.

## Save and privacy checks

- Save a catch, close/reopen it, and confirm its readings, timestamps and photo persist without a new data request.
- Correct a prefilled temperature or wind, then refresh; the manual correction must remain. Clear a field deliberately and repeat.
- Change the point/time while a request is still loading; old results must not replace the new context.
- Toggle a species favourite, restart or upgrade, and confirm the preference remains.
- Deny GPS permission and confirm manual point/name entry still works.
- Verify actual map, camera, zoom and share flows on a real phone. Neither this guide nor a successful build establishes those outcomes.

For repeatable source diagnostics, run `tools/compare_conditions.py` with explicit coordinates and a timezone-aware `--at` value. Its output contains public example/provider data and is stored in ignored `work/conditions-audit/`, independently of private catches.

`connectedDebugAndroidTest` uses `android.injected.androidTest.leaveApksInstalledAfterRun=true` to leave the installed app/data in place. The live-source test is opt-in through `-Pandroid.testInstrumentationRunnerArguments.liveConditions=true`; ordinary tests use controlled fixtures. Never use Clear storage or uninstall as a routine update procedure if you want to retain the diary.
