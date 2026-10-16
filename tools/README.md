# Public-data maintenance tools

These Python helpers work with public provider data and bundled reference assets. They never read the private catch database or upload fishing marks. `work/` contains ignored local downloads, audit responses and review images; do not commit it.

## Compare conditions

`compare_conditions.py` accepts latitude, longitude and a timezone-aware `--at` timestamp. It chooses nearby supported stations/buoys/gauges from the same bundled catalogues as the app, rounds model query coordinates and writes public responses plus `comparison.json` under `work/conditions-audit/`.

```powershell
python tools/compare_conditions.py --latitude -26.6825 --longitude 153.1201 --at "2026-10-15T22:20:00+10:00"
```

This is a diagnostic view of provider responses, not an alternative app parser. Raw rows may include readings the app rejects for age, distance or missing values. Check the applicable timestamp and measurement before comparing. Re-running replaces the latest comparison; copy an audit folder if you need a separate historical record. Feeds have limited history and may no longer contain an old sample.

## Maintain species and source catalogues

- `update_reference_catalogues.py` refreshes the bounded BOM station list, Queensland buoy locations and the species manifest. Existing verified images are retained. Newly found image candidates require visual review.
- `complete_species_photos.py` resumes missing image downloads and respects provider rate limits.
- `import_commons_image.py SPECIES_ID "Commons file title.jpg"` imports a specifically reviewed freely licensed Commons file. Confirm its species and image-specific licence first.
- `build_image_credits.py` generates offline HTML and Markdown credits from the manifest, including author, licence link, source and resize disclosure.
- `review_species_assets.py` checks unique IDs, attribution, licences and readable bitmap files; it creates the contact sheet at `work/species-review.jpg`. It needs Pillow. The app's instrumented asset test independently decodes images through Android.
- `update_tide_catalog.py` maintains the Queensland tide catalogue; review provider resource changes before shipping it.

Before distributing changed assets, inspect the contact sheet, verify species identity against primary references, check author/licence/source fields and regenerate credits. Search filtering alone cannot establish fish identity. Keep original species IDs to preserve favourites and associations. Missing suitable licences should produce a generic icon, not an unlicensed photograph.

These scripts use Python's standard library except the contact-sheet helper (Pillow). They are development tools; the Android app does not execute them or download species photographs at runtime. App tests/build and source-matching rules are documented in [VERIFICATION.md](../docs/VERIFICATION.md) and [CONDITIONS.md](../docs/CONDITIONS.md).

## Rebuild the filled report

`build_submission_report.py` fills the retained uploaded report template, checks every schema-4 field, preserves unrelated DOCX package parts and generates matching SVG/PNG diagrams. Run it with the bundled document Python runtime after reviewing its version/test facts. The original template path and SHA are recorded in the script; a changed template requires fresh review. Identity remains blank by request. Rendering/page inspection is a separate required QA step; the current machine uses read-only native Word conversion because LibreOffice is absent. Internal QA files remain in ignored `work/final-report`.
