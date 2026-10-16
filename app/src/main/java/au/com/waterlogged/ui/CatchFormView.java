package au.com.waterlogged.ui;

import android.content.Context;
import android.os.Bundle;
import android.text.InputType;
import android.text.TextWatcher;
import android.view.View;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.Spinner;
import android.widget.TextView;

import au.com.waterlogged.model.*;
import au.com.waterlogged.services.ConditionMetadata;
import au.com.waterlogged.services.ConditionsProvider.Field;

import com.google.android.material.button.MaterialButton;

import java.text.DateFormat;
import java.util.Date;
import java.util.EnumMap;
import java.util.LinkedHashMap;
import java.util.Map;

/** Item view: contains widgets and presentation only; the fragment handles device actions. */
public class CatchFormView extends ScrollView {
    public final EditText title,
            location,
            length,
            weight,
            quantity,
            notes,
            bait,
            method,
            tide,
            wind,
            windDirection,
            temperature,
            swell,
            waves,
            gust;
    public final Spinner environment, disposition, tideDirection, moon;
    public final MaterialButton date,
            time,
            gps,
            map,
            camera,
            removePhoto,
            share,
            save,
            delete,
            refreshConditions,
            tideStation,
            chooseLocation;
    public final TextView coordinates, status;
    public final TextView conditionsStatus;
    public final ImageView photo;
    public final ImageView speciesImage;
    public final TextView speciesName, speciesScientific;
    private final Map<String, EditText> fields = new LinkedHashMap<>();
    private final Map<Field, TextView> sources = new EnumMap<>(Field.class);
    public final Map<Field, MaterialButton> automaticButtons = new EnumMap<>(Field.class);
    public final TideChartView tideChart;
    public final TextView tideDescription;

    public CatchFormView(Context c, boolean existing) {
        super(c);
        setFillViewport(true);
        LinearLayout body = Ui.column(c);
        Ui.pad(body, 16);
        addView(body);
        LinearLayout banner = Ui.card(c, "");
        LinearLayout speciesRow = Ui.row(c);
        speciesImage = new ImageView(c);
        speciesImage.setScaleType(ImageView.ScaleType.FIT_CENTER);
        speciesRow.addView(
                speciesImage, new LinearLayout.LayoutParams(Ui.dp(c, 110), Ui.dp(c, 90)));
        LinearLayout speciesLabels = Ui.column(c);
        speciesLabels.setPadding(Ui.dp(c, 12), 0, 0, 0);
        speciesName = Ui.text(c, "Your catch", 22, true);
        speciesScientific = Ui.muted(c, "Add the details below");
        speciesScientific.setTypeface(null, android.graphics.Typeface.ITALIC);
        speciesLabels.addView(speciesName);
        speciesLabels.addView(speciesScientific);
        Ui.weighted(speciesRow, speciesLabels);
        banner.addView(speciesRow);
        body.addView(banner);
        LinearLayout identity = Ui.card(c, "Catch details");
        body.addView(identity);
        title = Ui.field(c, identity, "Species / title *", false);
        title.setFilters(
                new android.text.InputFilter[] {new android.text.InputFilter.LengthFilter(100)});
        location = Ui.field(c, identity, "Location name *", false);
        location.setHint("e.g. Mooloolaba, QLD");
        location.setFilters(
                new android.text.InputFilter[] {new android.text.InputFilter.LengthFilter(200)});
        source(c, identity, Field.LOCATION);
        identity.addView(Ui.muted(c, "A general location name will appear in shared summaries."));
        LinearLayout datetime = Ui.row(c);
        date = Ui.button(c, "Date", () -> {});
        time = Ui.button(c, "Time", () -> {});
        Ui.weighted(datetime, date);
        Ui.weighted(datetime, time);
        identity.addView(datetime);
        environment =
                Ui.spinner(
                        c,
                        identity,
                        "Fishing environment",
                        new String[] {"Inshore", "Offshore", "Freshwater", "Estuary"});
        LinearLayout measurements = Ui.row(c);
        LinearLayout a = Ui.column(c), b = Ui.column(c), q = Ui.column(c);
        length = Ui.field(c, a, "Length (cm)", true);
        weight = Ui.field(c, b, "Weight (kg)", true);
        quantity = Ui.field(c, q, "Quantity *", false);
        quantity.setInputType(InputType.TYPE_CLASS_NUMBER);
        Ui.weighted(measurements, a);
        Ui.weighted(measurements, b);
        Ui.weighted(measurements, q);
        identity.addView(measurements);
        disposition =
                Ui.spinner(
                        c,
                        identity,
                        "Catch status",
                        new String[] {"Released", "Kept", "Bait", "Other"});
        bait = Ui.field(c, identity, "Bait / lure (optional)", false);
        method = Ui.field(c, identity, "Method (optional)", false);
        notes = Ui.field(c, identity, "Notes (optional, up to 1000 characters)", false);
        notes.setSingleLine(false);
        notes.setMinLines(3);
        notes.setGravity(android.view.Gravity.TOP);
        notes.setInputType(
                InputType.TYPE_CLASS_TEXT
                        | InputType.TYPE_TEXT_FLAG_MULTI_LINE
                        | InputType.TYPE_TEXT_FLAG_CAP_SENTENCES);
        notes.setFilters(
                new android.text.InputFilter[] {new android.text.InputFilter.LengthFilter(1000)});

        LinearLayout photos = Ui.card(c, "Catch photo");
        body.addView(photos);
        photo = new ImageView(c);
        photo.setScaleType(ImageView.ScaleType.CENTER_CROP);
        photo.setContentDescription("Catch photo. Tap to zoom.");
        photos.addView(photo, new LinearLayout.LayoutParams(-1, Ui.dp(c, 185)));
        photos.addView(
                Ui.muted(c, "Tap the photo to zoom. Photos stay in the app's private storage."));
        LinearLayout photoActions = Ui.row(c);
        camera = Ui.button(c, "Take photo", () -> {});
        removePhoto = Ui.button(c, "Remove photo", () -> {});
        Ui.weighted(photoActions, camera);
        Ui.weighted(photoActions, removePhoto);
        photos.addView(photoActions);
        body.removeView(photos);
        body.addView(photos, 1);

        LinearLayout place = Ui.card(c, "GPS location");
        body.addView(place);
        coordinates = Ui.muted(c, "No GPS location captured");
        place.addView(coordinates);
        LinearLayout locationActions = Ui.row(c);
        gps = Ui.button(c, "Get GPS", () -> {});
        map = Ui.button(c, "Show map", () -> {});
        Ui.weighted(locationActions, gps);
        Ui.weighted(locationActions, map);
        place.addView(locationActions);
        chooseLocation = Ui.button(c, "Choose location / test point", () -> {});
        place.addView(chooseLocation);
        place.addView(
                Ui.muted(
                        c,
                        "Editing the location name changes its label. Get GPS or Choose location"
                                + " changes the point used for nearby data."));
        place.addView(
                Ui.muted(
                        c,
                        "Check marine park boundaries and local rules before fishing. Opening a map"
                                + " sends this mark to your chosen map app."));
        place.addView(
                Ui.muted(
                        c,
                        "Automatic conditions use GPS rounded to 2 decimals (about 1 km). Exact"
                                + " marks stay on this device. Location names use the same rounded"
                                + " area."));

        LinearLayout conditions = Ui.card(c, "Conditions at catch time");
        body.addView(conditions);
        conditions.addView(
                Ui.muted(
                        c,
                        "Nearby observations where available, with labelled model fallbacks. Tide,"
                                + " total waves and swell are different measurements. Saved catches"
                                + " keep their original snapshot."));
        refreshConditions = Ui.button(c, "Refresh conditions", () -> {});
        conditions.addView(refreshConditions);
        tideStation = Ui.button(c, "Tide gauge: nearest automatically", () -> {});
        conditions.addView(tideStation);
        conditionsStatus =
                Ui.muted(
                        c,
                        "Get GPS to prefill weather, swell and tides. Moon phase works offline.");
        conditionsStatus.setAccessibilityLiveRegion(View.ACCESSIBILITY_LIVE_REGION_POLITE);
        conditions.addView(conditionsStatus);
        tide = Ui.field(c, conditions, "Tide height (m, optional)", true);
        tide.setInputType(
                InputType.TYPE_CLASS_NUMBER
                        | InputType.TYPE_NUMBER_FLAG_DECIMAL
                        | InputType.TYPE_NUMBER_FLAG_SIGNED);
        source(c, conditions, Field.TIDE_HEIGHT);
        tideChart = new TideChartView(c);
        conditions.addView(tideChart, new LinearLayout.LayoutParams(-1, Ui.dp(c, 180)));
        tideDescription = Ui.muted(c, "");
        conditions.addView(tideDescription);
        tideDirection =
                Ui.spinner(
                        c,
                        conditions,
                        "Tide level trend (not tidal current)",
                        new String[] {"Unknown", "Rising", "Falling", "Slack", "Turning"});
        source(c, conditions, Field.TIDE_TREND);
        wind = Ui.field(c, conditions, "Wind speed (km/h, optional)", true);
        source(c, conditions, Field.WIND_SPEED);
        gust = Ui.field(c, conditions, "Wind gust (km/h, optional)", true);
        source(c, conditions, Field.WIND_GUST);
        windDirection = Ui.field(c, conditions, "Wind direction (optional)", false);
        source(c, conditions, Field.WIND_DIRECTION);
        temperature = Ui.field(c, conditions, "Air temperature (°C, optional)", true);
        temperature.setInputType(
                InputType.TYPE_CLASS_NUMBER
                        | InputType.TYPE_NUMBER_FLAG_DECIMAL
                        | InputType.TYPE_NUMBER_FLAG_SIGNED);
        source(c, conditions, Field.TEMPERATURE);
        swell = Ui.field(c, conditions, "Swell height (m, optional)", true);
        source(c, conditions, Field.SWELL);
        waves = Ui.field(c, conditions, "Total significant wave height / Hs (m, optional)", true);
        source(c, conditions, Field.WAVE_HEIGHT);
        moon =
                Ui.spinner(
                        c,
                        conditions,
                        "Moon phase",
                        new String[] {
                            "Not recorded",
                            "New moon",
                            "Waxing crescent",
                            "First quarter",
                            "Waxing gibbous",
                            "Full moon",
                            "Waning gibbous",
                            "Last quarter",
                            "Waning crescent"
                        });
        source(c, conditions, Field.MOON);
        status = Ui.muted(c, "Valid entries auto-save as you edit and when you leave.");
        status.setAccessibilityLiveRegion(View.ACCESSIBILITY_LIVE_REGION_POLITE);
        body.addView(status);
        share = Ui.button(c, "Share catch summary", () -> {});
        body.addView(share);
        save = Ui.button(c, "Save to Log", () -> {});
        body.addView(save);
        delete = Ui.button(c, "Delete catch", () -> {});
        delete.setVisibility(existing ? View.VISIBLE : View.GONE);
        body.addView(delete);
        EditText[] edits = {
            title,
            location,
            length,
            weight,
            quantity,
            notes,
            bait,
            method,
            tide,
            wind,
            windDirection,
            temperature,
            swell,
            waves,
            gust
        };
        String[] keys = {
            "title",
            "location",
            "length",
            "weight",
            "quantity",
            "notes",
            "bait",
            "method",
            "tide",
            "wind",
            "windDirection",
            "temperature",
            "swell",
            "waves",
            "gust"
        };
        for (int i = 0; i < edits.length; i++) fields.put(keys[i], edits[i]);
    }

    private void source(Context c, LinearLayout parent, Field f) {
        TextView label = Ui.muted(c, "");
        label.setTextSize(12);
        parent.addView(label);
        sources.put(f, label);
        MaterialButton automatic = Ui.button(c, "Use automatic value again", () -> {});
        automatic.setTextSize(12);
        automatic.setVisibility(GONE);
        parent.addView(automatic);
        automaticButtons.put(f, automatic);
    }

    public void showSources(ConditionMetadata metadata) {
        for (Map.Entry<Field, TextView> e : sources.entrySet()) {
            e.getValue().setText(metadata.label(e.getKey()));
            automaticButtons
                    .get(e.getKey())
                    .setVisibility(
                            !metadata.automatic(e.getKey())
                                            && (metadata.manual(e.getKey())
                                                    || !java.util.Arrays.asList(
                                                                    "", "Unknown", "Not recorded")
                                                            .contains(conditionValue(e.getKey())))
                                    ? VISIBLE
                                    : GONE);
        }
    }

    public void showTide(CatchRecord record) {
        tideChart.show(record.tideSeriesJson, record.caughtAt);
        tideDescription.setText(record.tideSeriesJson == null ? "" : tideChart.description());
    }

    public String conditionValue(Field f) {
        if (f == Field.MOON) return Ui.selected(moon);
        if (f == Field.TIDE_TREND) return Ui.selected(tideDirection);
        return Ui.value(conditionEdit(f));
    }

    public void setCondition(Field f, String value) {
        if (f == Field.MOON) Ui.select(moon, value);
        else if (f == Field.TIDE_TREND) Ui.select(tideDirection, value);
        else conditionEdit(f).setText(value);
    }

    private EditText conditionEdit(Field f) {
        switch (f) {
            case LOCATION:
                return location;
            case TEMPERATURE:
                return temperature;
            case WIND_SPEED:
                return wind;
            case WIND_DIRECTION:
                return windDirection;
            case WIND_GUST:
                return gust;
            case SWELL:
                return swell;
            case WAVE_HEIGHT:
                return waves;
            case TIDE_HEIGHT:
                return tide;
            default:
                throw new IllegalArgumentException("Not a text field");
        }
    }

    public void bind(CatchRecord r) {
        SpeciesImages.show(speciesImage, r.speciesId);
        speciesImage.setContentDescription("Species reference image. Tap for credits.");
        speciesImage.setOnClickListener(v -> SpeciesImages.showCredits(getContext(), r.speciesId));
        speciesName.setText(r.title.isEmpty() ? "Your catch" : r.title);
        Ui.fill(title, r.title);
        Ui.fill(location, r.locationName);
        Ui.fill(length, r.lengthCm);
        Ui.fill(weight, r.weightKg);
        Ui.fill(quantity, r.quantity);
        Ui.fill(notes, r.notes);
        Ui.fill(bait, r.bait);
        Ui.fill(method, r.method);
        Ui.fill(tide, r.tideHeightM);
        Ui.fill(wind, r.windKmh);
        Ui.fill(windDirection, r.windDirection);
        Ui.fill(temperature, r.airTemperatureC);
        Ui.fill(swell, r.swellM);
        Ui.fill(waves, r.waveHeightM);
        Ui.fill(gust, r.windGustKmh);
        Ui.select(environment, r.environment);
        Ui.select(disposition, r.disposition);
        Ui.select(tideDirection, r.tideDirection);
        Ui.select(moon, r.moonPhase);
        showMetadata(r);
    }

    public void showMetadata(CatchRecord r) {
        date.setText(DateFormat.getDateInstance(DateFormat.MEDIUM).format(new Date(r.caughtAt)));
        time.setText(DateFormat.getTimeInstance(DateFormat.SHORT).format(new Date(r.caughtAt)));
        boolean has = r.latitude != null && r.longitude != null;
        coordinates.setText(
                has
                        ? String.format(
                                java.util.Locale.getDefault(),
                                "%.6f, %.6f\n%s",
                                r.latitude,
                                r.longitude,
                                r.accuracyMetres == null
                                        ? "Manually selected point"
                                        : "GPS accuracy: " + Ui.measurement(r.accuracyMetres, "m"))
                        : "No location selected");
        map.setEnabled(has);
        removePhoto.setEnabled(r.photoPath != null);
        SpeciesImages.show(photo, r.speciesId);
        if (r.photoPath != null) {
            PhotoLoader.load(photo, r.photoPath);
            photo.setContentDescription("Catch photo. Tap to zoom.");
        }
    }

    public CatchRecord read(CatchRecord base) {
        for (EditText e : fields.values()) e.setError(null);
        CatchRecord r = base.copy();
        r.title = Ui.value(title);
        r.locationName = Ui.value(location);
        if (r.title.isEmpty()) {
            title.setError("Enter a species or title");
            throw new IllegalArgumentException("Enter a species or title.");
        }
        if (r.locationName.isEmpty()) {
            location.setError("Enter a location name");
            throw new IllegalArgumentException("Enter a location name.");
        }
        r.lengthCm = number(length, 0.1, 1000, "Length");
        r.weightKg = number(weight, 0.001, 2000, "Weight");
        try {
            r.quantity = InputRules.quantity(Ui.value(quantity));
        } catch (IllegalArgumentException e) {
            quantity.setError(e.getMessage());
            throw e;
        }
        r.environment = Ui.selected(environment);
        r.disposition = Ui.selected(disposition);
        r.notes = Ui.value(notes);
        r.bait = Ui.value(bait);
        r.method = Ui.value(method);
        r.tideHeightM = number(tide, -20, 30, "Tide height");
        r.tideDirection = Ui.selected(tideDirection);
        r.windKmh = number(wind, 0, 400, "Wind speed");
        r.windDirection = Ui.value(windDirection);
        r.airTemperatureC = number(temperature, -90, 65, "Temperature");
        r.swellM = number(swell, 0, 40, "Swell height");
        r.moonPhase = Ui.selected(moon);
        r.waveHeightM = number(waves, 0, 40, "Total wave height");
        r.windGustKmh = number(gust, 0, 500, "Wind gust");
        return r;
    }

    private Double number(EditText e, double min, double max, String label) {
        try {
            return InputRules.optionalNumber(Ui.value(e), min, max, label);
        } catch (IllegalArgumentException error) {
            e.setError(error.getMessage());
            throw error;
        }
    }

    public void watch(TextWatcher watcher, Runnable changed) {
        for (EditText e : fields.values()) e.addTextChangedListener(watcher);
        android.widget.AdapterView.OnItemSelectedListener l =
                new android.widget.AdapterView.OnItemSelectedListener() {
                    public void onItemSelected(
                            android.widget.AdapterView<?> p, View v, int i, long id) {
                        changed.run();
                    }

                    public void onNothingSelected(android.widget.AdapterView<?> p) {}
                };
        environment.setOnItemSelectedListener(l);
        disposition.setOnItemSelectedListener(l);
        tideDirection.setOnItemSelectedListener(l);
        moon.setOnItemSelectedListener(l);
    }

    public void saveDraft(Bundle b) {
        for (Map.Entry<String, EditText> e : fields.entrySet())
            b.putString(e.getKey(), e.getValue().getText().toString());
        b.putInt("scroll", getScrollY());
        b.putInt("environmentChoice", environment.getSelectedItemPosition());
        b.putInt("statusChoice", disposition.getSelectedItemPosition());
        b.putInt("tideChoice", tideDirection.getSelectedItemPosition());
        b.putInt("moonChoice", moon.getSelectedItemPosition());
    }

    public void restoreDraft(Bundle b) {
        for (Map.Entry<String, EditText> e : fields.entrySet())
            if (b.containsKey(e.getKey())) e.getValue().setText(b.getString(e.getKey()));
        environment.setSelection(b.getInt("environmentChoice"));
        disposition.setSelection(b.getInt("statusChoice"));
        tideDirection.setSelection(b.getInt("tideChoice"));
        moon.setSelection(b.getInt("moonChoice"));
        post(() -> scrollTo(0, b.getInt("scroll")));
    }
}
