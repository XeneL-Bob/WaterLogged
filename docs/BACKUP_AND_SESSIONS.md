# Water Logged 0.4: backup, sessions and diary tools

## Encrypted backup and restore

Open **Settings -> Backup, restore & CSV export**. Choose **Create encrypted backup**, enter and confirm a password of at least eight characters, then select a destination in Android's file picker. Prefer a long unique password. Keep that password separately: the app has no password recovery and does not save it. The `.wlog` file contains catches, photos, favourites, custom species, sessions and saved condition metadata/tide curves. Public provider caches and bundled species images are not copied.

The user chooses local storage or an available document provider. Selecting a cloud provider sends the encrypted file there; the app does not upload it independently. An exported file outside private app storage can survive uninstalling. Private records are otherwise removed by uninstall/Clear storage. GitHub still backs up code, not the diary.

To recover, choose **Restore encrypted backup**, select the file and enter its password. Restore merges missing UUIDs. It never replaces a catch or session already on the device, so repeated imports do not duplicate catches or overwrite newer edits. Favourite selections are explicitly restored from the backup; existing species names/aliases stay as they are. Missing custom species are added. Photos are copied to new private filenames, not to paths supplied by a backup.

Trips that were active when backed up are restored as ended at backup creation time, unless that session already exists locally. Restore does not create a second active trip or interfere with the device's current active session. Reopening restored catches uses saved conditions rather than today's weather.

The file format uses PBKDF2-HMAC-SHA256 with 600,000 iterations, a fresh 16-byte salt and AES-256-GCM authenticated chunks. Each chunk uses a distinct nonce and authenticates the header and position. An authenticated final marker rejects truncated files. Only after the whole file is authenticated are its ZIP/JSON contents parsed. Import rejects unsupported versions, duplicate IDs/entries, invalid fields, missing photos and unexpected archive paths. Room applies the merge in a transaction; failed copies or database writes roll back and clean up newly copied photos.

Pilot limits are 256 MB encrypted payload/expanded contents, 32 MB per photo, 16 MB for the manifest and 20,000 records per type. Backups need temporary free space in private cache for the archive and encrypted output. Normal completion cleans these temporary files; an abrupt process termination can leave private cache files until cleared by Android. A failed destination write can leave an incomplete external file, which restore will reject. Keep the app open until the completion message appears.

This is a versioned logical backup, not a copy of an open SQLite database/WAL. Version 0.4 reads format 1/schema 4; future releases must retain or migrate that reader. Exercise backup and restore with test data before relying on a recovery file.

## CSV export

**Export catches as CSV** produces UTF-8 spreadsheet data. Exact coordinates are excluded unless the user selects **Include exact coordinates in CSV**. Location names and notes remain included, and the CSV is unencrypted. Private photo paths and photograph contents are excluded. Text cells that could be interpreted as spreadsheet formulas are escaped. CSV is for analysis; it cannot restore a diary with photos and snapshots.

## Fishing sessions

Open **Fishing sessions** from Home or Settings. **Start fishing** asks for a general location name, optional bait/method, environment and an optional fixed point. Without a fixed point, new catches can capture live GPS. Only one session can be active; a database uniqueness constraint prevents duplicate active trips.

New catches inherit session defaults and the session UUID, but keep their own new UUID, catch timestamp and freshly requested condition snapshot. A fixed session point is labelled as manual rather than a new GPS fix. The general session place name stays a manual default until changed or returned to automatic. **Edit session defaults** affects subsequent catches, not previously saved ones.

**End session** saves its ending time. Session history includes duration, entries, fish quantities and kept/released totals, including trips with no catches. Counts come from the associated catches, so edits and deletions update totals. These are diary totals, not legal possession-limit calculations. **View session catches** opens a filtered diary.

## Search, automatic values and tide charts

**Search & filters** on Home/My Log searches partial common/scientific names, regional aliases, saved titles, locations and notes. Filters combine a local calendar date range, catch status and method text. The Through date includes the entire day. Both remembered sort choices still use DAO `ORDER BY`; measured lengths sort before unknown lengths. Clear all removes search/date/status/method/session filters.

After manually editing or clearing a condition, **Use automatic value again** restores provider ownership for that field. Other manual corrections remain unchanged. Automatic retrieval uses the saved catch's time and point. With no position or available response, the field remains unknown until a suitable source is retrieved; no value is invented.

Source labels identify Observed, Forecast, Prediction, Calculated, Manual or Cached values and show their age/applicable time. The tide chart uses the selected gauge's actual day of interval predictions, metres above LAT and explicitly labelled AEST times. The vertical marker identifies the catch timestamp; the text alternative gives daily high/low sampled predictions and source. It is a water-level prediction, not tidal current. The curve is stored with a catch and included in backups. Changing time, point, environment or gauge invalidates the old curve before requesting another. Existing catches without curves acquire one only on explicit refresh.

## Custom species and image review

**Species -> Add a custom species** saves a common name, optional scientific name and regional aliases in private Room storage. It is searchable, favouritable, available offline and marked **Custom · unverified**. It is included in encrypted backups. Nothing is submitted to an external review service. **Log another species** remains available for a one-off entry.

The bundled reference catalogue remains 32 species/30 licensed images. Golden snapper now uses Rick Stuart-Smith / Reef Life Survey's more recognisable underwater photograph under CC BY 3.0, with its source and licence retained. Pearl perch and spotted mackerel still use generic icons. See [SPECIES_CREDITS.md](SPECIES_CREDITS.md). Reference photographs are not proof of identification; future additions need visual identity and licence review.

## Verification and remaining device checks

See [VERIFICATION.md](VERIFICATION.md) for executed tests and [DEVICE_CHECKLIST.md](DEVICE_CHECKLIST.md) for pending checks. The user chose emulator testing for this upgrade. Real outdoor readability, an actual fishing trip and a physical phone's camera/GPS behaviour cannot be established on an emulator.

Primary implementation references: [Android document picker](https://developer.android.com/training/data-storage/shared/documents-files), [Android cryptography](https://developer.android.com/privacy-and-security/cryptography), [OWASP PBKDF2 guidance](https://cheatsheetseries.owasp.org/cheatsheets/Password_Storage_Cheat_Sheet.html), [golden snapper image/source licence](https://commons.wikimedia.org/wiki/File:LutjanusJohniiRLS.jpg).
