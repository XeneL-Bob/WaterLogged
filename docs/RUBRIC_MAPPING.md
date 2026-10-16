# CSC202 requirement mapping
This was a self report I did, subject to change depending on marking.

| Template item | Maximum | Water Logged equivalent | Implementation and verification |
|---|---:|---|---|
| 1.a List of records | 5 | Catch title, date/time and length | CatchListFragment/CatchAdapter; native saved diary rows and Room tests |
| 1.b New / Sort / Help | 2 | All three actions on both list variants | MainActivity/list_actions.xml; native navigation. New opens species selection then Item |
| 2.a Editable title/address | 2 | Species/title and general location | CatchFormView; RubricUiTest edits and reopens fields |
| 2.b Date button/dialog | 5 | Current catch date and DatePickerDialog | CatchItemFragment; native dialog opened/cancelled; additional time picker |
| 2.c Five detail fields | 8 | Environment, length, quantity, disposition, multiline notes | InputRules/CatchFormView; UI/unit tests cover empty/text/negative/decimal/overflow quantities and optional measurements |
| 2.d GPS/map | 15 | Read-only saved mark, Get GPS, Show map | Native fresh simulated Mooloolaba fix: -26.682500, 153.120100, accuracy 5 m; Google Maps opened that mark. UI test checks saved-coordinate intent and disabled map without mark |
| 2.e Image/camera/zoom | 15 | Private photo and enlarged DialogFragment | Actual emulator camera capture/accept/view in 0.4 journey, saved image decoded; ZoomGestureTest verifies pinch/pan/reset. Physical orientation pending |
| 2.f Share | 5 | Preview and Android sharesheet | CatchSummary/CatchItemFragment; chooser/text payload checked, required fields present, precise mark/notes excluded. No message sent |
| 2.g Auto-save/refresh | 2 | Debounce/background/exit saves and observable list | RubricUiTest confirms write while open, exactly one UUID, return to List; earlier offline/background/reopen journey |
| 3.a View details | 2 | Reopen same Item fragment by UUID | UI test checks saved title, size, quantity and multiline notes; snapshot/draft restoration tests |
| 3.b Delete/refresh | 5 | Delete only on reopened catches, cancel/confirm | UI test verifies cancel preserves row, confirm removes it and returns to List, delayed save cannot recreate it; native GPS fixture cleanup |
| 4.a Date DAO sort | 5 | Newest catch first | CatchDao.newest/searchNewest: ORDER BY caughtAt DESC, id ASC; actual Room tests |
| 4.b Numeric DAO sort | 5 | Length lowest first | CatchDao.smallest/searchSmallest: ORDER BY lengthCm IS NULL ASC, lengthCm ASC, caughtAt DESC, id ASC; unknown last; Room tests |
| 5.a WebView | 2 | Offline fishing help | HelpFragment; native content displayed and inspected |
| 5.b Given dummy link | 2 | Exact original RTA URL | Native Assignment reference page displayed Preparing for a tenancy inside WebView at https://www.rta.qld.gov.au/before-renting/preparing-for-a-tenancy |
| AI-use declaration | 5 | Actual tools, affected work and prompts | Filled report declaration; student reviews and adds later assistance/changes |
| MVC List UI | 5 | List controller, adapter/views, entities/repository/DAOs | Figure 1 and class explanations match current implementation |
| MVC Item UI | 5 | Shared controller, form/photo views, model/services | Figure 2 distinguishes controller/lifecycle responsibilities and independent services |
| Database design | 5 | Three Room tables, schema 4 | Figure 3 and every column's type/usage; no SQL DEFAULT clauses, Java initial values explained; generator checks exported schema |

## Approved context changes

| RentRadar concept | Water Logged implementation |
|---|---|
| Property record | Timestamped catch record |
| Title / address | Species/title / fishing location description |
| Inspection date | Catch date/time |
| Property type | Inshore, Offshore, Freshwater, Estuary |
| Weekly rent | Optional length in cm; optional weight also recorded |
| Bedrooms 0–9 | Quantity 1–999, safely validated |
| Application status | Released, Kept, Bait, Other |
| Photo / GPS | Catch photo / exact private fishing mark |
| Date / rent sorting | Date newest first / length lowest first through DAO |
| Help | Fishing guidance plus original dummy URL |


## Evidence and limits

Build, 14 local and 31 instrumented tests passed; one previously passed opt-in journey is deliberately skipped after cleanup. Lint: zero errors, 44 warnings. See [VERIFICATION.md](VERIFICATION.md) for scope and reports. List/Item fragments, Room and DAO SQL ordering are present; no crime/resolved UI artefacts were found.

Physical phone/API 26, outdoor use, denied/approximate permissions and recipient-app delivery remain unverified. No test sent a message or removed unrelated catches. The public GPS fixture was deleted, preserving the two original entries.
