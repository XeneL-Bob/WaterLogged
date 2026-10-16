package au.com.waterlogged.ui;

import android.Manifest;
import android.app.DatePickerDialog;
import android.app.TimePickerDialog;
import android.content.ActivityNotFoundException;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.FileProvider;
import androidx.fragment.app.Fragment;

import au.com.waterlogged.MainActivity;
import au.com.waterlogged.model.*;
import au.com.waterlogged.services.*;
import au.com.waterlogged.services.ConditionsProvider.*;
import au.com.waterlogged.services.LocationCapture;

import com.google.android.material.dialog.MaterialAlertDialogBuilder;

import java.io.File;
import java.io.IOException;
import java.util.Calendar;
import java.util.EnumMap;
import java.util.List;
import java.util.Locale;

/**
 * Shared New/View controller, with a stable UUID so repeated auto-saves never duplicate entries.
 */
public class CatchItemFragment extends Fragment {
    private CatchFormView form;
    private CatchRecord record;
    private boolean existing, ready, leaving, suppressSave;
    private String pendingPhoto;
    private long editRevision;
    private LocationCapture locationCapture;
    private ConditionsProvider conditionsProvider;
    private ConditionMetadata metadata;
    private final EnumMap<Field, String> seenConditions = new EnumMap<>(Field.class);
    private boolean filling, locationRequested, loadingConditions;
    private long conditionsRequest;
    private String lastEnvironment;
    private final Handler handler = new Handler(Looper.getMainLooper());
    private final Runnable debounce = () -> autoSave();
    private ActivityResultLauncher<String[]> permissions;
    private ActivityResultLauncher<Uri> camera;

    public static CatchItemFragment fresh(String species, String name) {
        CatchItemFragment f = new CatchItemFragment();
        Bundle b = new Bundle();
        b.putString("species", species);
        b.putString("name", name);
        f.setArguments(b);
        return f;
    }

    public static CatchItemFragment existing(String id) {
        CatchItemFragment f = new CatchItemFragment();
        Bundle b = new Bundle();
        b.putString("id", id);
        f.setArguments(b);
        return f;
    }

    @Override
    public void onCreate(Bundle state) {
        super.onCreate(state);
        existing = requireArguments().containsKey("id");
        if (state != null) {
            record = (CatchRecord) state.getSerializable("record");
            pendingPhoto = state.getString("pendingPhoto");
            locationRequested = state.getBoolean("locationRequested");
        }
        permissions =
                registerForActivityResult(
                        new ActivityResultContracts.RequestMultiplePermissions(),
                        result -> {
                            if (form != null) captureLocation();
                        });
        camera =
                registerForActivityResult(
                        new ActivityResultContracts.TakePicture(),
                        success -> {
                            if (pendingPhoto == null) return;
                            File file = new File(pendingPhoto);
                            if (success && file.length() > 0 && record != null) {
                                WaterRepository.get(requireContext())
                                        .discardUncommittedPhoto(record.id, record.photoPath);
                                record.photoPath = pendingPhoto;
                                if (form != null) form.showMetadata(record);
                                changed();
                            } else file.delete();
                            pendingPhoto = null;
                        });
    }

    @Nullable
    @Override
    public View onCreateView(
            @NonNull LayoutInflater i, @Nullable ViewGroup parent, @Nullable Bundle state) {
        form = new CatchFormView(requireContext(), existing);
        return form;
    }

    @Override
    public void onViewCreated(@NonNull View v, @Nullable Bundle state) {
        locationCapture = new LocationCapture(requireContext());
        conditionsProvider = createConditionsProvider();
        if (record != null) bind(state);
        else if (existing) {
            form.save.setEnabled(false);
            WaterRepository.get(requireContext())
                    .find(
                            requireArguments().getString("id"),
                            (found, error) -> {
                                if (form == null || !isAdded()) return;
                                if (error != null || found == null) {
                                    message("This catch could not be loaded.");
                                    suppressSave = true;
                                    ((MainActivity) requireActivity()).back();
                                    return;
                                }
                                record = found;
                                bind(state);
                            });
        } else {
            CatchRecord draft = new CatchRecord();
            draft.speciesId = requireArguments().getString("species", "");
            draft.title = requireArguments().getString("name", "");
            CatchFormView target = form;
            form.save.setEnabled(false);
            loadSessionDefaults(
                    (session, error) -> {
                        if (form != target || !isAdded()) return;
                        if (error != null) {
                            message(
                                    "Session defaults unavailable; this catch can still be"
                                            + " logged.");
                        }
                        if (session != null) {
                            session.applyTo(draft);
                            ConditionMetadata defaults = new ConditionMetadata(null);
                            defaults.edited(Field.LOCATION, session.locationName);
                            draft.conditionsMetadata = defaults.json();
                            locationRequested = draft.latitude != null;
                        }
                        record = draft;
                        bind(state);
                        if (session != null && draft.latitude != null) requestConditions();
                    });
        }
    }

    protected ConditionsProvider createConditionsProvider() {
        return new LiveConditionsProvider(requireContext());
    }

    protected void loadSessionDefaults(WaterRepository.Result<FishingSession> callback) {
        WaterRepository.get(requireContext()).sessionDefaults(callback);
    }

    private void bind(Bundle state) {
        form.bind(record);
        if (state != null && state.containsKey("title")) form.restoreDraft(state);
        ready = true;
        form.save.setEnabled(true);
        metadata = new ConditionMetadata(record.conditionsMetadata);
        lastEnvironment = Ui.selected(form.environment);
        rememberValues();
        form.showSources(metadata);
        form.showTide(record);
        for (java.util.Map.Entry<Field, com.google.android.material.button.MaterialButton> e :
                form.automaticButtons.entrySet())
            e.getValue().setOnClickListener(v -> useAutomatic(e.getKey()));
        updateGaugeLabel();
        form.conditionsStatus.setText(
                existing
                        ? "Saved snapshot · reopening this catch does not refresh it. Refresh uses"
                                + " the catch's saved time and GPS."
                        : "Finding conditions for your catch…");
        WaterRepository.get(requireContext())
                .findSpecies(
                        record.speciesId,
                        (species, error) -> {
                            if (form != null && species != null)
                                form.speciesScientific.setText(species.scientificName);
                        });
        form.date.setOnClickListener(v -> pickDate());
        form.time.setOnClickListener(v -> pickTime());
        form.gps.setOnClickListener(
                v ->
                        permissions.launch(
                                new String[] {
                                    Manifest.permission.ACCESS_FINE_LOCATION,
                                    Manifest.permission.ACCESS_COARSE_LOCATION
                                }));
        form.map.setOnClickListener(v -> openMap());
        form.camera.setOnClickListener(v -> takePhoto());
        form.removePhoto.setOnClickListener(
                v -> {
                    WaterRepository.get(requireContext())
                            .discardUncommittedPhoto(record.id, record.photoPath);
                    record.photoPath = null;
                    form.showMetadata(record);
                    changed();
                });
        form.photo.setOnClickListener(
                v -> {
                    if (record.photoPath != null)
                        PhotoDialog.create(record.photoPath)
                                .show(getParentFragmentManager(), "photo");
                });
        form.share.setOnClickListener(v -> share());
        form.save.setOnClickListener(
                v -> leave(() -> ((MainActivity) requireActivity()).showLog()));
        form.delete.setOnClickListener(v -> confirmDelete());
        form.refreshConditions.setOnClickListener(
                v -> {
                    if (record.latitude == null) requestLocation();
                    else requestConditions(true);
                });
        form.chooseLocation.setOnClickListener(
                v ->
                        LocationPicker.show(
                                requireContext(),
                                record.latitude,
                                record.longitude,
                                (lat, lon) -> {
                                    conditionsRequest++;
                                    locationCapture.cancel();
                                    invalidate(true, false);
                                    record.latitude = lat;
                                    record.longitude = lon;
                                    record.accuracyMetres = null;
                                    record.locationCapturedAt = System.currentTimeMillis();
                                    record.tideStationId = null;
                                    form.gps.setEnabled(true);
                                    updateGaugeLabel();
                                    form.showMetadata(record);
                                    changed();
                                    requestConditions(true);
                                }));
        form.tideStation.setOnClickListener(v -> chooseGauge());
        form.watch(
                new TextWatcher() {
                    public void beforeTextChanged(
                            CharSequence s, int start, int count, int after) {}

                    public void onTextChanged(CharSequence s, int start, int before, int count) {
                        changed();
                    }

                    public void afterTextChanged(Editable e) {}
                },
                this::changed);
        if (!existing) {
            applyMoon();
            if (!locationRequested) {
                locationRequested = true;
                form.post(
                        () -> {
                            if (form != null && isAdded()) requestLocation();
                        });
            } else if (record.latitude != null
                    && state != null
                    && state.getBoolean("conditionsPending")) requestConditions();
        } else if (state != null
                && state.getBoolean("conditionsPending")
                && record.latitude != null) requestConditions();
    }

    @Override
    public void onResume() {
        super.onResume();
        ((MainActivity) requireActivity())
                .chrome(existing ? "View Catch" : "New Catch", true, false, 0);
    }

    private void changed() {
        if (!ready || suppressSave || leaving || form == null || filling) return;
        for (Field f : Field.values()) {
            String value = form.conditionValue(f);
            if (!value.equals(seenConditions.get(f))) metadata.edited(f, value);
        }
        rememberValues();
        record.conditionsMetadata = metadata.json();
        form.showSources(metadata);
        String environment = Ui.selected(form.environment);
        if (!environment.equals(lastEnvironment)) {
            lastEnvironment = environment;
            invalidate(false, true);
            handler.post(
                    () -> {
                        if (form != null && record.latitude != null) requestConditions();
                    });
        }
        editRevision++;
        form.status.setText("Editing… valid changes auto-save.");
        handler.removeCallbacks(debounce);
        handler.postDelayed(debounce, 900);
    }

    private void rememberValues() {
        for (Field f : Field.values()) seenConditions.put(f, form.conditionValue(f));
    }

    private void requestLocation() {
        permissions.launch(
                new String[] {
                    Manifest.permission.ACCESS_FINE_LOCATION,
                    Manifest.permission.ACCESS_COARSE_LOCATION
                });
    }

    private void applyMoon() {
        if (metadata.canApply(Field.MOON, form.conditionValue(Field.MOON))) {
            Snapshot s = new Snapshot();
            s.readings.put(Field.MOON, LiveConditionsProvider.moon(record.caughtAt));
            applyReadings(s);
        }
    }

    private void applyReadings(Snapshot snapshot) {
        filling = true;
        try {
            for (java.util.Map.Entry<Field, Reading> e : snapshot.readings.entrySet()) {
                if (metadata.canApply(e.getKey(), form.conditionValue(e.getKey()))) {
                    metadata.remember(e.getKey(), e.getValue());
                    form.setCondition(e.getKey(), e.getValue().value);
                }
            }
        } finally {
            filling = false;
        }
        rememberValues();
        record.conditionsMetadata = metadata.json();
        record.conditionsSource = "Per-field sources and timestamps";
        if (snapshot.tideSeries != null) {
            record.tideSeriesJson = snapshot.tideSeries.json();
            form.showTide(record);
        }
        form.showSources(metadata);
        changed();
    }

    private void requestConditions() {
        requestConditions(false);
    }

    private void requestConditions(boolean fresh) {
        requestConditions(fresh, null);
    }

    private void requestConditions(boolean fresh, Field onlyField) {
        if (form == null || !ready || leaving || suppressSave) return;
        if (onlyField == null || onlyField == Field.MOON) applyMoon();
        if (record.latitude == null || record.longitude == null) {
            form.conditionsStatus.setText(
                    "Get GPS to prefill weather, swell and tides. Manual entries are kept.");
            return;
        }
        long request = ++conditionsRequest;
        loadingConditions = true;
        form.conditionsStatus.setText(
                "Retrieving conditions for this catch's time and rounded GPS area… Manual entries"
                        + " are kept.");
        ConditionsProvider.Callback callback =
                snapshot -> {
                    if (form == null
                            || !ready
                            || leaving
                            || suppressSave
                            || request != conditionsRequest) return;
                    Snapshot selected = snapshot;
                    if (onlyField != null) {
                        selected = new Snapshot();
                        selected.complete = snapshot.complete;
                        selected.notices.addAll(snapshot.notices);
                        if (snapshot.readings.containsKey(onlyField))
                            selected.readings.put(onlyField, snapshot.readings.get(onlyField));
                        if (onlyField == Field.TIDE_HEIGHT || onlyField == Field.TIDE_TREND)
                            selected.tideSeries = snapshot.tideSeries;
                    }
                    applyReadings(selected);
                    if (snapshot.complete) loadingConditions = false;
                    String message =
                            snapshot.complete
                                    ? "Snapshot updated. Manual corrections kept."
                                    : "Receiving conditions…";
                    if (!snapshot.notices.isEmpty())
                        message += "\n" + android.text.TextUtils.join("\n", snapshot.notices);
                    form.conditionsStatus.setText(message);
                };
        if (fresh)
            conditionsProvider.fetchFresh(
                    record.latitude,
                    record.longitude,
                    record.caughtAt,
                    !"Freshwater".equals(Ui.selected(form.environment)),
                    record.tideStationId,
                    callback);
        else
            conditionsProvider.fetch(
                    record.latitude,
                    record.longitude,
                    record.caughtAt,
                    !"Freshwater".equals(Ui.selected(form.environment)),
                    record.tideStationId,
                    callback);
    }

    /**
     * Changing time/position clears only automatically owned values; manual corrections survive.
     */
    private void invalidate(boolean position, boolean marineOnly) {
        conditionsRequest++;
        loadingConditions = false;
        record.tideSeriesJson = null;
        form.showTide(record);
        filling = true;
        try {
            for (Field f : Field.values()) {
                if (marineOnly
                        && f != Field.SWELL
                        && f != Field.WAVE_HEIGHT
                        && f != Field.TIDE_HEIGHT
                        && f != Field.TIDE_TREND) continue;
                if (!position && f == Field.LOCATION) continue;
                if (metadata.automatic(f)) {
                    form.setCondition(
                            f,
                            f == Field.MOON
                                    ? "Not recorded"
                                    : f == Field.TIDE_TREND ? "Unknown" : "");
                    metadata.remove(f);
                }
            }
        } finally {
            filling = false;
        }
        rememberValues();
        record.conditionsMetadata = metadata.json();
        form.showSources(metadata);
    }

    private void useAutomatic(Field field) {
        if (form == null) return;
        conditionsRequest++;
        loadingConditions = false;
        filling = true;
        try {
            metadata.remove(field);
            form.setCondition(
                    field,
                    field == Field.MOON
                            ? "Not recorded"
                            : field == Field.TIDE_TREND ? "Unknown" : "");
        } finally {
            filling = false;
        }
        rememberValues();
        record.conditionsMetadata = metadata.json();
        changed();
        if (field == Field.MOON) applyMoon();
        if (record.latitude != null && field != Field.MOON) requestConditions(true, field);
        else if (field != Field.MOON)
            form.conditionsStatus.setText(
                    "Choose a location or Get GPS to retrieve this automatic value.");
    }

    private void changedTime() {
        invalidate(false, false);
        form.showMetadata(record);
        applyMoon();
        changed();
        if (record.latitude != null) requestConditions();
    }

    private void updateGaugeLabel() {
        form.tideStation.setText(
                record.tideStationId == null
                        ? "Tide gauge: nearest automatically"
                        : "Tide gauge: "
                                + new TideCatalog(requireContext()).name(record.tideStationId));
    }

    private void chooseGauge() {
        if (record.latitude == null || record.longitude == null) {
            message("Capture GPS first so nearby gauges can be listed.");
            return;
        }
        List<TideCatalog.Station> stations =
                new TideCatalog(requireContext()).nearby(record.latitude, record.longitude);
        String[] names = new String[stations.size() + 1];
        names[0] = "Nearest gauge automatically (within 50 km)";
        int selected = 0;
        for (int i = 0; i < stations.size(); i++) {
            TideCatalog.Station s = stations.get(i);
            names[i + 1] =
                    String.format(
                            Locale.getDefault(),
                            "%s (%.1f km)",
                            s.name,
                            s.distance(record.latitude, record.longitude));
            if (s.id.equals(record.tideStationId)) selected = i + 1;
        }
        new MaterialAlertDialogBuilder(requireContext())
                .setTitle("Choose your waterway's tide gauge")
                .setSingleChoiceItems(
                        names,
                        selected,
                        (dialog, which) -> {
                            record.tideStationId = which == 0 ? null : stations.get(which - 1).id;
                            invalidate(false, true);
                            updateGaugeLabel();
                            changed();
                            requestConditions();
                            dialog.dismiss();
                        })
                .setNegativeButton("Cancel", null)
                .show();
    }

    private void autoSave() {
        if (!ready || suppressSave || leaving || form == null) return;
        try {
            record = form.read(record);
            long revision = editRevision;
            WaterRepository.get(requireContext())
                    .save(
                            record.copy(),
                            (v, error) -> {
                                if (form != null && revision == editRevision)
                                    form.status.setText(
                                            error == null
                                                    ? "Saved on this device"
                                                    : "Save failed. Please try Save to Log again.");
                            });
        } catch (IllegalArgumentException e) {
            form.status.setText("Not saved: " + e.getMessage());
        }
    }

    /**
     * Every explicit exit is validated. Invalid edits never replace the last valid database row.
     */
    public void leave(Runnable next) {
        if (suppressSave || leaving) {
            next.run();
            return;
        }
        if (!ready || form == null) return;
        handler.removeCallbacks(debounce);
        try {
            record = form.read(record);
        } catch (IllegalArgumentException e) {
            new MaterialAlertDialogBuilder(requireContext())
                    .setTitle("Check catch details")
                    .setMessage(e.getMessage() + " Your last valid auto-save, if any, is kept.")
                    .setPositiveButton("Keep editing", null)
                    .setNegativeButton(
                            "Discard invalid changes",
                            (d, w) -> {
                                suppressSave = true;
                                WaterRepository.get(requireContext())
                                        .discardUncommittedPhoto(record.id, record.photoPath);
                                next.run();
                            })
                    .show();
            return;
        }
        leaving = true;
        conditionsRequest++;
        loadingConditions = false;
        form.save.setEnabled(false);
        WaterRepository.get(requireContext())
                .save(
                        record.copy(),
                        (v, error) -> {
                            if (!isAdded()) return;
                            if (error != null) {
                                leaving = false;
                                if (form != null) form.save.setEnabled(true);
                                message(
                                        "Could not save. Your catch is still open; please try"
                                                + " again.");
                            } else next.run();
                        });
    }

    private void pickDate() {
        Calendar c = Calendar.getInstance();
        c.setTimeInMillis(record.caughtAt);
        new DatePickerDialog(
                        requireContext(),
                        (v, y, m, d) -> {
                            c.set(y, m, d);
                            record.caughtAt = c.getTimeInMillis();
                            changedTime();
                        },
                        c.get(Calendar.YEAR),
                        c.get(Calendar.MONTH),
                        c.get(Calendar.DAY_OF_MONTH))
                .show();
    }

    private void pickTime() {
        Calendar c = Calendar.getInstance();
        c.setTimeInMillis(record.caughtAt);
        new TimePickerDialog(
                        requireContext(),
                        (v, h, m) -> {
                            c.set(Calendar.HOUR_OF_DAY, h);
                            c.set(Calendar.MINUTE, m);
                            c.set(Calendar.SECOND, 0);
                            c.set(Calendar.MILLISECOND, 0);
                            record.caughtAt = c.getTimeInMillis();
                            changedTime();
                        },
                        c.get(Calendar.HOUR_OF_DAY),
                        c.get(Calendar.MINUTE),
                        android.text.format.DateFormat.is24HourFormat(requireContext()))
                .show();
    }

    private void captureLocation() {
        form.gps.setEnabled(false);
        form.coordinates.setText("Finding a fresh device location…");
        locationCapture.capture(
                new LocationCapture.Callback() {
                    public void success(android.location.Location l) {
                        if (form == null || leaving || suppressSave) return;
                        invalidate(true, false);
                        record.latitude = l.getLatitude();
                        record.longitude = l.getLongitude();
                        record.accuracyMetres = (double) l.getAccuracy();
                        record.locationCapturedAt = l.getTime();
                        form.gps.setEnabled(true);
                        form.showMetadata(record);
                        changed();
                        requestConditions();
                    }

                    public void failure(String s) {
                        if (form != null) {
                            form.gps.setEnabled(true);
                            form.showMetadata(record);
                            form.conditionsStatus.setText(
                                    s
                                            + " Moon phase is available offline; other fields can"
                                            + " be entered manually.");
                            message(s);
                        }
                    }
                });
    }

    private void openMap() {
        if (record.latitude == null || record.longitude == null) return;
        String coords = String.format(Locale.US, "%.6f,%.6f", record.latitude, record.longitude);
        Intent intent =
                new Intent(
                        Intent.ACTION_VIEW,
                        Uri.parse(
                                "geo:"
                                        + coords
                                        + "?q="
                                        + coords
                                        + "("
                                        + Uri.encode("Water Logged catch")
                                        + ")"));
        intent.setPackage("com.google.android.apps.maps");
        try {
            startActivity(intent);
        } catch (ActivityNotFoundException | SecurityException e) {
            intent.setPackage(null);
            try {
                startActivity(intent);
            } catch (ActivityNotFoundException | SecurityException unavailable) {
                message("Install or enable a map app to view this mark.");
            }
        }
    }

    private void takePhoto() {
        try {
            File dir = new File(requireContext().getFilesDir(), "photos");
            if (!dir.exists() && !dir.mkdirs()) throw new IOException();
            File photo = File.createTempFile("catch_", ".jpg", dir);
            pendingPhoto = photo.getAbsolutePath();
            camera.launch(
                    FileProvider.getUriForFile(
                            requireContext(),
                            requireContext().getPackageName() + ".photos",
                            photo));
        } catch (IOException
                | ActivityNotFoundException
                | IllegalArgumentException
                | SecurityException e) {
            if (pendingPhoto != null) new File(pendingPhoto).delete();
            pendingPhoto = null;
            message("Camera could not open. Check that a camera app is installed and enabled.");
        }
    }

    private void share() {
        try {
            CatchRecord r = form.read(record);
            String text = CatchSummary.text(r);
            new MaterialAlertDialogBuilder(requireContext())
                    .setTitle("Share catch summary")
                    .setMessage(
                            text
                                    + "\n\n"
                                    + "Exact coordinates, photos and notes are excluded. Check your"
                                    + " location name before sharing.")
                    .setNegativeButton("Cancel", null)
                    .setPositiveButton(
                            "Share",
                            (d, w) -> {
                                Intent intent =
                                        new Intent(Intent.ACTION_SEND)
                                                .setType("text/plain")
                                                .putExtra(Intent.EXTRA_TEXT, text);
                                try {
                                    startActivity(Intent.createChooser(intent, "Share catch"));
                                } catch (ActivityNotFoundException | SecurityException e) {
                                    message("No sharing app is available.");
                                }
                            })
                    .show();
        } catch (IllegalArgumentException e) {
            message(e.getMessage());
        }
    }

    private void confirmDelete() {
        new MaterialAlertDialogBuilder(requireContext())
                .setTitle("Delete this catch?")
                .setMessage("This removes the catch and its photo from this device.")
                .setNegativeButton("Cancel", null)
                .setPositiveButton(
                        "Delete",
                        (d, w) -> {
                            handler.removeCallbacks(debounce);
                            suppressSave = true;
                            WaterRepository.get(requireContext())
                                    .delete(
                                            record.copy(),
                                            (v, error) -> {
                                                if (!isAdded()) return;
                                                if (error == null)
                                                    ((MainActivity) requireActivity()).showLog();
                                                else {
                                                    suppressSave = false;
                                                    message("Delete failed. Please try again.");
                                                }
                                            });
                        })
                .show();
    }

    private void message(String text) {
        if (isAdded()) Toast.makeText(requireContext(), text, Toast.LENGTH_LONG).show();
    }

    @Override
    public void onPause() {
        handler.removeCallbacks(debounce);
        autoSave();
        super.onPause();
    }

    @Override
    public void onStop() {
        if (locationCapture != null) locationCapture.cancel();
        if (form != null) {
            form.gps.setEnabled(true);
            if (record != null) form.showMetadata(record);
        }
        super.onStop();
    }

    @Override
    public void onSaveInstanceState(@NonNull Bundle out) {
        super.onSaveInstanceState(out);
        if (form != null && ready) {
            try {
                record = form.read(record);
            } catch (IllegalArgumentException ignored) {
            }
            form.saveDraft(out);
        }
        out.putSerializable("record", record);
        out.putString("pendingPhoto", pendingPhoto);
        out.putBoolean("locationRequested", locationRequested);
        out.putBoolean("conditionsPending", loadingConditions);
    }

    @Override
    public void onDestroyView() {
        handler.removeCallbacks(debounce);
        conditionsRequest++;
        loadingConditions = false;
        if (locationCapture != null) locationCapture.cancel();
        form = null;
        ready = false;
        super.onDestroyView();
    }
}
