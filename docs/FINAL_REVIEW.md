# Final application review

**16 October 2026 — Water Logged pilot 0.4.1.** Assessed Android features are implemented under the fishing substitution the student confirmed as approved. The [rubric mapping](RUBRIC_MAPPING.md) and completed report preserve original maximum marks and state actual evidence. Grades remain the marker's decision.

## Component review

| Component | Result | Practical limit |
|---|---|---|
| Branding/navigation | Original submerged logbook toolbar/launcher vectors, adaptive masks and API 33 monochrome variant; revised welcome line; native appearance inspected | Other launchers/themed-icon appearance not manually checked |
| List/Item/Room | Shared fragments, UUID, validation, auto-save, view/delete/refresh and SQL sorts verified | Broader device/screen coverage pending |
| GPS/maps | Fresh public simulated fix, accuracy and exact mark in Google Maps verified | Physical GPS and denied/approximate/disabled-location paths need phone checks |
| Camera/images | Prior native capture/view/file persistence and current pinch/pan/reset tests pass | Physical orientation, cancellation/replacement and very large images need broader checks |
| Sharing | Preview and chooser payload verified; precise mark/notes/photos excluded | No message sent; recipient-app delivery remains a hands-on check |
| Help | Offline WebView and exact RTA page displayed; stale backup wording corrected | Remote sites depend on network/layout; JavaScript disabled |
| Conditions | Nearby supported station/buoy/gauge, provenance, manual ownership, cache labels and saved snapshots tested; live retrieval passes | Sparse Queensland catalogue, forecast skill and national tides not established |
| Species | 32 species, 30 licensed images, aliases/favourites/custom species; Android decodes images | Two placeholders; not an authoritative identification tool |
| Sessions | Persistent start/end/defaults, zero-catch trips, counts/duration tested | Trip point is a default, not proof of each catch position |
| Backup/CSV | Encrypted round trip, invalid-password/corruption handling, bounded merge and native file-picker flow tested | Pilot format/size limits; forgotten passwords unrecoverable; CSV unencrypted |
| Privacy | Private DB/photos, rounded external coordinates, no account/analytics/cloud sync; automatic backup disabled | No additional DB encryption; map launches, place names and chosen exports have disclosed implications |
| Documentation | Filled template, two MVC figures, three-table diagram and all schema fields | Student identity and submission/repository review remain for student |

## Verification

Debug build and lint succeed: **14 local tests passed; 32 instrumented checks, 31 passed, zero failures/errors, one skipped**. The skipped opt-in journey passed before its fixture cleanup. Lint: **0 errors / 44 warnings**. Exact scope/history: [VERIFICATION.md](VERIFICATION.md).

New rubric UI checks cover invalid numeric input, new/view Delete visibility, date dialog, one-row auto-save, reopened fields, cancel/confirm deletion, no delayed recreation and map/share payloads. Native checks loaded the RTA page and opened Google Maps at the public Mooloolaba point. Only the labelled GPS fixture was removed; the two original catches remained. No real-phone or warning-free claim is made.

Final audit also corrected both diary search queries to match the catalogue common name after the catch title is renamed. The existing Room filter/sort test now verifies that case under both orderings.

## Improvements and submission checks

1. Before submission, repeat GPS/camera/zoom/permissions on a real phone, check outdoor readability and run API 26 if claiming that minimum version. This matters more than adding features.
2. Confirm Classroom 50 registration and marker access, commit/push final files, fill identity, review the self-assessment/declaration and use the required report filename. Retain the submitted repository unchanged for one month as the supplied brief requests.
3. After the pilot, address lint maintenance: translatable text, dependencies, list updates, drawing allocations and EXIF handling. Upgrade one area at a time with regression checks. Two launcher warnings concern the API 26 resource; its API 33 override already has the monochrome layer.
4. For wider release, expand verified station/tide catalogues and compare the same measurement, location, datum and timestamp. Do not force agreement with another site. Obtain commercial provider permissions/plans where required.
5. Add verified regional fisheries rules and marine-park boundaries only with reliable licences, update dates and jurisdiction handling. These remain future features.

## Report preparation

The original Downloads template remains unchanged. The filled copy is [WaterLogged_CSC202_Task3_Report.docx](submission/WaterLogged_CSC202_Task3_Report.docx). Identity is deliberately blank. Template section order, A4 geometry, numbered headings and footers are retained, with filled black headings and approved feature labels. Original package parts outside the authored document/styles/settings/image relationships remain byte-identical. The generator checks every schema field; diagrams retain editable SVG sources.

LibreOffice is absent on this Windows machine. Rendering used installed Microsoft Word in a private hidden read-only instance with macros disabled, followed by the packaged render_docx.py rasterizer. Internal page images stay under ignored work/final-report; every final page is visually inspected before delivery.
