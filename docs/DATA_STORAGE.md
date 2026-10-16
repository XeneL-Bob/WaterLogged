# How Water Logged saves data

Water Logged has no account, catch-upload server, analytics or cloud sync. It fetches public environmental information, then stores the selected snapshot with the catch on the user's device.

## Saving a catch

1. New Catch creates a stable random UUID and a catch timestamp. Species selection fills the title; the user can edit it.
2. Get GPS records exact latitude/longitude, fix time and reported accuracy. Choose location stores the explicitly selected point; its accuracy is unknown and the UI labels it manual.
3. Nearby sources are selected locally. Automatic values retain their source label, applicable time, retrieval time and manual-override status.
4. Valid edits auto-save after approximately 900 ms without editing, when leaving, and when backgrounding. Save to Log validates and saves immediately before navigating. Invalid input leaves the last valid stored row intact.
5. A copy of the record is written by the repository's serial background worker to Room. The same UUID updates one row rather than making duplicates. Room's observable query refreshes the diary.

The `catches` table includes the species ID/title, general place name, catch date/time, environment, length, weight, quantity, disposition, bait, method, notes, exact coordinates, location accuracy/time, private photo path, tide height/trend, wind speed/direction/gust, temperature, swell, total wave height, gauge choice and condition provenance.

Version 4 adds nullable `sessionId` and `tideSeriesJson` to catches. The tide JSON preserves the selected gauge day's prediction samples, source and retrieval time. The `sessions` table stores a UUID, start/end times, defaults for location/bait/method/environment, an optional fixed point and a unique active slot. Trips with no catches remain saved. Session totals are calculated from associated catch rows; they are not separate possession counters. Custom species use stable `custom:` IDs in the existing `species` table, retaining names, scientific names, aliases and favourites locally.

`conditionsMetadata` is JSON in the same catch row. Each automatic field stores its selected value, source, `validAt`, `retrievedAt` and whether the value is manual. Editing a field, including clearing it, marks that field manual. Refresh does not overwrite that correction. Changing position or time clears automatically owned values from the old context before requesting replacements. Old callbacks cannot update a newer request or a destroyed view.

Opening an old catch does not retrieve today's weather. Its stored values and provenance remain unchanged until the user edits or explicitly refreshes it. Refresh uses the catch's saved timestamp and position. Changing a place's display name alone changes neither its mark nor its source selection.

## Storage locations

| Data | Android app storage |
|---|---|
| Catches and species/favourites | `databases/water_logged.db` under the app's private data directory |
| Pending/recent SQLite writes | SQLite may use adjacent `water_logged.db-wal` and `water_logged.db-shm` files; these are part of the database, not separate backups |
| User camera photographs | `files/photos/` in private app storage; the database stores the file path |
| Temporary provider responses | `cache/conditions/`; response filenames are hashes of the request URL |
| Sort preference | Private Android shared preferences |
| Unsaved form draft during activity recreation | Android saved-instance state; it is not a cloud backup or a replacement for a successful database save |
| Public fish reference images, licences and station catalogues | Bundled APK assets, available offline; these are public reference material, not the user's catch photos |

For this application ID, the private root is normally `/data/user/0/au.com.waterlogged/`. Android may present the equivalent `/data/data/au.com.waterlogged/` path. Other ordinary apps do not have access through Android's app sandbox. This pilot adds no separate database encryption. An authorised debugging session can inspect a debug build's private storage.

Schema version 4 uses explicit migrations: version 1 → 2 adds provenance/gauge fields; version 2 → 3 adds nullable total-wave and gust fields; version 3 → 4 adds nullable session/curve fields and the session table. These preserve catches, notes, coordinates, photo paths and favourites. Catalogue updates do not reset favourites or rewrite catch titles. An explicit backup restore does restore favourite choices from the selected backup.

## Information sent outside the app

- Open-Meteo weather/marine and Android Geocoder receive coordinates rounded to two decimal places, roughly a kilometre. They do not receive the exact saved mark from this app.
- BOM requests identify a public supported weather station. Queensland wave requests identify a public buoy; tide requests identify a public gauge and date. Matching to the user's exact point happens locally.
- Providers can still see normal network information such as the requesting IP address and requested public area/station. Rounded coordinates are a privacy reduction, not anonymity.
- Show map deliberately passes the selected exact coordinates to the user's chosen map app.
- Share previews a text summary containing title, general place name, catch time, length, quantity and status. Exact coordinates, private photo paths, photos and notes are excluded. A detailed place name can still reveal a fishing spot.
- Bundled species images need no runtime image downloads. Opening their source/credit link hands that public URL to a browser.
- Encrypted backup and CSV export deliberately write a user-selected document. A selected cloud provider receives that file. Backups contain exact marks/photos inside encrypted contents; CSV is unencrypted and excludes coordinates unless explicitly enabled. Location names/notes remain in CSV.

Android automatic backup and device transfer remain disabled. Uninstalling/Clear storage removes the private diary and photos; an independently exported encrypted backup can be restored afterward. Keep the backup password separately: it is never saved and cannot be recovered. GitHub backs up code, not catches. [BACKUP_AND_SESSIONS.md](BACKUP_AND_SESSIONS.md) explains encryption, merge rules, photo handling and size limits. Backup/restore use temporary private cache files and clean them after normal completion; process termination can leave private cache files for Android to clear.

Deleting a catch removes its row and associated private photo. Replacing/removing a photo cleans up the previous private file after the new record is saved. Public environmental cache files may remain until cache eviction; deleting a catch does not need to delete public weather responses. Cache expiry never changes the saved catch snapshot.
