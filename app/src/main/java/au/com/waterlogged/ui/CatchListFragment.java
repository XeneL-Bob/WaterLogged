package au.com.waterlogged.ui;

import android.Manifest;
import android.app.DatePickerDialog;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.HorizontalScrollView;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.LiveData;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import au.com.waterlogged.MainActivity;
import au.com.waterlogged.R;
import au.com.waterlogged.model.*;
import au.com.waterlogged.services.*;
import au.com.waterlogged.services.ConditionsProvider.*;

import com.google.android.material.dialog.MaterialAlertDialogBuilder;

import java.text.DateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.EnumMap;
import java.util.List;

/** List controller: Room observable queries supply ordering and refresh after all writes. */
public class CatchListFragment extends Fragment {
    private CatchAdapter adapter;
    private LiveData<List<CatchRecord>> source;
    private TextView summary;
    private LinearLayout favouriteCards;
    private long day = 0;
    private long rangeEnd = 0;
    private String query = "", statusFilter = "", methodFilter = "", sessionFilter = "";
    private TideChartView currentTide;
    private TextView sessionStatus;
    private final EnumMap<Field, TextView> currentQuality = new EnumMap<>(Field.class);
    private final android.os.Handler ageHandler =
            new android.os.Handler(android.os.Looper.getMainLooper());
    private final Runnable updateAges =
            new Runnable() {
                public void run() {
                    if (currentMetadata != null)
                        for (java.util.Map.Entry<Field, TextView> e : currentQuality.entrySet())
                            e.getValue()
                                    .setText(
                                            currentMetadata.brief(
                                                    e.getKey(), System.currentTimeMillis()));
                    if (home && currentStatus != null) ageHandler.postDelayed(this, 60000);
                }
            };
    private boolean home;
    private boolean smallest;
    private TextView currentArea, currentStatus;
    private com.google.android.material.button.MaterialButton currentRefresh;
    private final EnumMap<Field, TextView> currentValues = new EnumMap<>(Field.class);
    private ConditionMetadata currentMetadata;
    private LocationCapture currentLocation;
    private long currentRequest;
    private ActivityResultLauncher<String[]> locationPermission;

    public static CatchListFragment create(boolean home) {
        CatchListFragment f = new CatchListFragment();
        Bundle b = new Bundle();
        b.putBoolean("home", home);
        f.setArguments(b);
        return f;
    }

    public static CatchListFragment forSession(String id) {
        CatchListFragment fragment = create(false);
        fragment.getArguments().putString("session", id);
        return fragment;
    }

    @Override
    public void onCreate(Bundle state) {
        super.onCreate(state);
        home = requireArguments().getBoolean("home");
        if (state != null) day = state.getLong("day");
        sessionFilter = requireArguments().getString("session", "");
        if (state != null) {
            rangeEnd = state.getLong("rangeEnd");
            query = state.getString("query", "");
            statusFilter = state.getString("status", "");
            methodFilter = state.getString("method", "");
            sessionFilter = state.getString("session", sessionFilter);
        }
        smallest =
                requireContext()
                        .getSharedPreferences("preferences", 0)
                        .getBoolean("smallest", false);
        locationPermission =
                registerForActivityResult(
                        new ActivityResultContracts.RequestMultiplePermissions(),
                        result -> {
                            if (home && currentStatus != null) refreshCurrent(true);
                        });
    }

    @Nullable
    @Override
    public View onCreateView(
            @NonNull LayoutInflater inflater,
            @Nullable ViewGroup container,
            @Nullable Bundle state) {
        LinearLayout root = Ui.column(requireContext());
        LinearLayout header = Ui.column(requireContext());
        Ui.pad(header, 16);
        if (home) {
            LinearLayout hero = Ui.card(requireContext(), "");
            hero.setBackgroundResource(R.drawable.ocean_header);
            int hour = Calendar.getInstance().get(Calendar.HOUR_OF_DAY);
            hero.addView(
                    Ui.text(
                            requireContext(),
                            hour < 12
                                    ? "Good morning!"
                                    : hour < 18 ? "Good afternoon!" : "Good evening!",
                            28,
                            true));
            hero.addView(
                    Ui.text(requireContext(), getString(R.string.welcome_message), 17, false));
            header.addView(hero);
            LinearLayout actions = Ui.row(requireContext());
            Ui.weighted(
                    actions,
                    Ui.button(
                            requireContext(),
                            "+ New Catch",
                            () -> host().navigate(new SpeciesFragment(), true)));
            Ui.weighted(actions, Ui.button(requireContext(), "My Log", () -> host().showLog()));
            Ui.weighted(
                    actions,
                    Ui.button(
                            requireContext(),
                            "Species",
                            () -> host().navigate(new SpeciesFragment(), false)));
            header.addView(actions);
            sessionStatus = Ui.muted(requireContext(), "No fishing session active.");
            header.addView(sessionStatus);
            header.addView(
                    Ui.button(
                            requireContext(),
                            "Fishing sessions · start / end trip",
                            () -> host().navigate(new SessionsFragment(), true)));
            LinearLayout conditions = Ui.card(requireContext(), "≈  Conditions");
            LinearLayout metrics = Ui.row(requireContext());
            String[] labels = {"Tide", "Wind", "Temp", "Total waves", "Swell", "Moon"};
            Field[] fields = {
                Field.TIDE_HEIGHT,
                Field.WIND_SPEED,
                Field.TEMPERATURE,
                Field.WAVE_HEIGHT,
                Field.SWELL,
                Field.MOON
            };
            for (int m = 0; m < labels.length; m++) {
                if (m == 3) {
                    conditions.addView(metrics);
                    metrics = Ui.row(requireContext());
                }
                LinearLayout metric = Ui.column(requireContext());
                TextView value = Ui.text(requireContext(), "—", 16, true);
                value.setTextColor(Ui.SKY);
                metric.addView(value);
                metric.addView(Ui.muted(requireContext(), labels[m]));
                TextView quality = Ui.muted(requireContext(), "");
                quality.setTextSize(11);
                metric.addView(quality);
                currentQuality.put(fields[m], quality);
                Ui.weighted(metrics, metric);
                currentValues.put(fields[m], value);
            }
            conditions.addView(metrics);
            currentTide = new TideChartView(requireContext());
            conditions.addView(
                    currentTide, new LinearLayout.LayoutParams(-1, Ui.dp(requireContext(), 180)));
            currentArea = Ui.text(requireContext(), "Your nearby conditions", 17, true);
            conditions.addView(currentArea);
            currentStatus =
                    Ui.muted(
                            requireContext(),
                            "Use your location to see conditions. Requests use rounded GPS; exact"
                                    + " marks stay on this device.");
            conditions.addView(currentStatus);
            currentRefresh =
                    Ui.button(
                            requireContext(),
                            "Use my location",
                            () ->
                                    locationPermission.launch(
                                            new String[] {
                                                Manifest.permission.ACCESS_FINE_LOCATION,
                                                Manifest.permission.ACCESS_COARSE_LOCATION
                                            }));
            conditions.addView(currentRefresh);
            conditions.addView(
                    Ui.button(
                            requireContext(),
                            "Choose location / test point",
                            () ->
                                    LocationPicker.show(
                                            requireContext(),
                                            null,
                                            null,
                                            (lat, lon) -> {
                                                currentLocation.cancel();
                                                loadCurrent(lat, lon, true);
                                            })));
            conditions.addView(
                    Ui.button(requireContext(), "Data sources & times", this::showCurrentSources));
            header.addView(conditions);
            LinearLayout location = Ui.card(requireContext(), "⌖  Your fishing marks");
            location.addView(
                    Ui.muted(
                            requireContext(),
                            "Capture GPS in New Catch, or enter a location name. Marks are saved"
                                    + " privately on this device."));
            header.addView(location);
            LinearLayout favourites = Ui.card(requireContext(), "★  Favourite Species");
            HorizontalScrollView strip = new HorizontalScrollView(requireContext());
            strip.setHorizontalScrollBarEnabled(false);
            favouriteCards = Ui.row(requireContext());
            strip.addView(favouriteCards);
            favourites.addView(strip);
            favourites.addView(
                    Ui.button(
                            requireContext(),
                            "Choose a species",
                            () -> host().navigate(new SpeciesFragment(), true)));
            header.addView(favourites);
            header.addView(Ui.text(requireContext(), "Your catches", 22, true));
        } else header.addView(Ui.text(requireContext(), "Your fishing diary", 26, true));
        LinearLayout filters = Ui.row(requireContext());
        Ui.weighted(filters, Ui.button(requireContext(), "Choose day", () -> pickDay()));
        Ui.weighted(
                filters,
                Ui.button(
                        requireContext(),
                        "All dates",
                        () -> {
                            day = 0;
                            rangeEnd = 0;
                            observe();
                        }));
        header.addView(filters);
        header.addView(Ui.button(requireContext(), "Search & filters", this::showFilters));
        summary = Ui.muted(requireContext(), "Loading catches…");
        header.addView(summary);
        RecyclerView list = new RecyclerView(requireContext());
        list.setLayoutManager(new LinearLayoutManager(requireContext()));
        adapter =
                new CatchAdapter(
                        header, r -> host().navigate(CatchItemFragment.existing(r.id), true));
        list.setAdapter(adapter);
        root.addView(list, new LinearLayout.LayoutParams(-1, -1));
        return root;
    }

    @Override
    public void onViewCreated(@NonNull View v, @Nullable Bundle state) {
        observe();
        if (home)
            WaterRepository.get(requireContext())
                    .activeSession()
                    .observe(
                            getViewLifecycleOwner(),
                            session -> {
                                sessionStatus.setText(
                                        session == null
                                                ? "No fishing session active."
                                                : "● Fishing at "
                                                        + session.locationName
                                                        + " · new catches use this trip's"
                                                        + " defaults");
                            });
        if (home) {
            currentMetadata = new ConditionMetadata(null);
            currentLocation = new LocationCapture(requireContext());
            Reading moon = LiveConditionsProvider.moon(System.currentTimeMillis());
            currentValues.get(Field.MOON).setText(moon.value);
            currentMetadata.remember(Field.MOON, moon);
            if (ContextCompat.checkSelfPermission(
                            requireContext(), Manifest.permission.ACCESS_COARSE_LOCATION)
                    == PackageManager.PERMISSION_GRANTED) refreshCurrent();
        }
        if (home)
            WaterRepository.get(requireContext())
                    .species("")
                    .observe(
                            getViewLifecycleOwner(),
                            list -> {
                                favouriteCards.removeAllViews();
                                for (Species s : list)
                                    if (s.favourite) {
                                        LinearLayout tile = Ui.column(requireContext());
                                        Ui.pad(tile, 5);
                                        ImageView image = new ImageView(requireContext());
                                        SpeciesImages.show(image, s.id);
                                        image.setContentDescription(
                                                s.commonName + " reference image");
                                        image.setScaleType(ImageView.ScaleType.FIT_CENTER);
                                        tile.addView(
                                                image,
                                                new LinearLayout.LayoutParams(
                                                        Ui.dp(requireContext(), 110),
                                                        Ui.dp(requireContext(), 70)));
                                        TextView name =
                                                Ui.text(requireContext(), s.commonName, 13, true);
                                        name.setMaxLines(2);
                                        tile.addView(name);
                                        tile.setFocusable(true);
                                        tile.setOnClickListener(
                                                clicked ->
                                                        host().navigate(
                                                                        CatchItemFragment.fresh(
                                                                                s.id, s.commonName),
                                                                        true));
                                        favouriteCards.addView(tile);
                                    }
                                if (favouriteCards.getChildCount() == 0)
                                    favouriteCards.addView(
                                            Ui.muted(
                                                    requireContext(),
                                                    "Tap a star in Species to add favourites."));
                            });
    }

    private void refreshCurrent() {
        refreshCurrent(false);
    }

    private void refreshCurrent(boolean fresh) {
        if (currentStatus == null) return;
        currentRefresh.setEnabled(false);
        currentStatus.setText("Finding your current location…");
        long request = ++currentRequest;
        currentLocation.capture(
                new LocationCapture.Callback() {
                    public void success(android.location.Location location) {
                        if (currentStatus == null || request != currentRequest) return;
                        loadCurrent(location.getLatitude(), location.getLongitude(), fresh);
                    }

                    public void failure(String error) {
                        if (currentStatus != null && request == currentRequest) {
                            currentRefresh.setEnabled(true);
                            currentStatus.setText(error);
                        }
                    }
                });
    }

    private void loadCurrent(double latitude, double longitude, boolean fresh) {
        long request = ++currentRequest;
        currentRefresh.setEnabled(false);
        for (Field f : currentValues.keySet()) currentValues.get(f).setText("—");
        for (TextView label : currentQuality.values()) label.setText("");
        currentTide.show(null, System.currentTimeMillis());
        currentMetadata = new ConditionMetadata(null);
        currentArea.setText("Selected area");
        currentStatus.setText("Retrieving nearby observations and estimates…");
        ConditionsProvider.Callback callback =
                snapshot -> {
                    if (currentStatus == null || request != currentRequest) return;
                    for (java.util.Map.Entry<Field, Reading> e : snapshot.readings.entrySet()) {
                        currentMetadata.remember(e.getKey(), e.getValue());
                        TextView quality = currentQuality.get(e.getKey());
                        if (quality != null)
                            quality.setText(
                                    currentMetadata.quality(e.getKey())
                                            + "\n"
                                            + ConditionMetadata.age(
                                                    e.getValue().validAt,
                                                    System.currentTimeMillis()));
                        if (e.getKey() == Field.LOCATION) currentArea.setText(e.getValue().value);
                        TextView value = currentValues.get(e.getKey());
                        if (value != null) {
                            String unit =
                                    e.getKey() == Field.TIDE_HEIGHT
                                                    || e.getKey() == Field.SWELL
                                                    || e.getKey() == Field.WAVE_HEIGHT
                                            ? " m"
                                            : e.getKey() == Field.WIND_SPEED
                                                    ? "\nkm/h"
                                                    : e.getKey() == Field.TEMPERATURE ? "°C" : "";
                            String display = e.getValue().value;
                            try {
                                display =
                                        new java.text.DecimalFormat("0.#")
                                                .format(Double.parseDouble(display));
                            } catch (NumberFormatException ignored) {
                            }
                            value.setText(display + unit);
                        }
                    }
                    if (snapshot.tideSeries != null)
                        currentTide.show(
                                snapshot.tideSeries.json(), System.currentTimeMillis(), "Now");
                    currentStatus.setText(
                            (snapshot.complete
                                            ? "Nearby observations / estimates · inspect sources"
                                                    + " and times below."
                                            : "Receiving nearby conditions…")
                                    + (snapshot.notices.isEmpty()
                                            ? ""
                                            : "\n"
                                                    + android.text.TextUtils.join(
                                                            "\n", snapshot.notices)));
                    if (snapshot.complete) {
                        currentRefresh.setEnabled(true);
                        currentRefresh.setText("Refresh my location");
                    }
                };
        LiveConditionsProvider provider = new LiveConditionsProvider(requireContext());
        if (fresh)
            provider.fetchFresh(
                    latitude, longitude, System.currentTimeMillis(), true, null, callback);
        else provider.fetch(latitude, longitude, System.currentTimeMillis(), true, null, callback);
    }

    private void showCurrentSources() {
        if (currentMetadata == null) return;
        StringBuilder text = new StringBuilder();
        for (Field f : Field.values())
            text.append(f.name().replace('_', ' '))
                    .append("\n")
                    .append(currentMetadata.label(f))
                    .append("\n\n");
        new MaterialAlertDialogBuilder(requireContext())
                .setTitle("Nearby conditions sources")
                .setMessage(text)
                .setPositiveButton("Close", null)
                .show();
    }

    private MainActivity host() {
        return (MainActivity) requireActivity();
    }

    @Override
    public void onResume() {
        super.onResume();
        ageHandler.removeCallbacks(updateAges);
        if (home) ageHandler.post(updateAges);
        host().chrome(
                        home ? "Water Logged" : "My Log",
                        false,
                        true,
                        home ? R.id.nav_home : R.id.nav_log);
    }

    private void pickDay() {
        Calendar c = Calendar.getInstance();
        if (day != 0) c.setTimeInMillis(day);
        new DatePickerDialog(
                        requireContext(),
                        (view, y, m, d) -> {
                            Calendar chosen = Calendar.getInstance();
                            chosen.set(y, m, d, 0, 0, 0);
                            chosen.set(Calendar.MILLISECOND, 0);
                            day = chosen.getTimeInMillis();
                            chosen.add(Calendar.DAY_OF_MONTH, 1);
                            rangeEnd = chosen.getTimeInMillis();
                            observe();
                        },
                        c.get(Calendar.YEAR),
                        c.get(Calendar.MONTH),
                        c.get(Calendar.DAY_OF_MONTH))
                .show();
    }

    public void chooseSort() {
        new MaterialAlertDialogBuilder(requireContext())
                .setTitle("Sort catches")
                .setSingleChoiceItems(
                        new String[] {"Catch date (newest first)", "Length (lowest first)"},
                        smallest ? 1 : 0,
                        (d, which) -> {
                            smallest = which == 1;
                            requireContext()
                                    .getSharedPreferences("preferences", 0)
                                    .edit()
                                    .putBoolean("smallest", smallest)
                                    .apply();
                            observe();
                            d.dismiss();
                        })
                .setNegativeButton("Cancel", null)
                .show();
    }

    private void showFilters() {
        LinearLayout content = Ui.column(requireContext());
        Ui.pad(content, 16);
        android.widget.ScrollView scroll = new android.widget.ScrollView(requireContext());
        scroll.addView(content);
        android.widget.EditText search =
                Ui.field(requireContext(), content, "Species, location or notes", false);
        search.setText(query);
        android.widget.EditText method =
                Ui.field(requireContext(), content, "Method contains (optional)", false);
        method.setText(methodFilter);
        android.widget.Spinner status =
                Ui.spinner(
                        requireContext(),
                        content,
                        "Catch status",
                        new String[] {"All", "Kept", "Released", "Bait", "Other"});
        Ui.select(status, statusFilter.isEmpty() ? "All" : statusFilter);
        long[] dates = {day, rangeEnd};
        android.widget.Button
                start = Ui.button(requireContext(), dateLabel("From", dates[0]), () -> {}),
                end =
                        Ui.button(
                                requireContext(),
                                dateLabel("Through", dates[1] == 0 ? 0 : dates[1] - 1),
                                () -> {});
        start.setOnClickListener(v -> filterDate(dates, 0, start));
        end.setOnClickListener(v -> filterDate(dates, 1, end));
        content.addView(start);
        content.addView(end);
        content.addView(
                Ui.muted(
                        requireContext(),
                        "The end date includes the whole local calendar day. Search is"
                                + " case-insensitive and matches partial words."));
        androidx.appcompat.app.AlertDialog dialog =
                new MaterialAlertDialogBuilder(requireContext())
                        .setTitle("Search saved catches")
                        .setView(scroll)
                        .setNegativeButton("Cancel", null)
                        .setNeutralButton(
                                "Clear all",
                                (d, w) -> {
                                    day = 0;
                                    rangeEnd = 0;
                                    query = "";
                                    statusFilter = "";
                                    methodFilter = "";
                                    sessionFilter = "";
                                    observe();
                                })
                        .setPositiveButton("Apply", null)
                        .create();
        dialog.setOnShowListener(
                d ->
                        dialog.getButton(androidx.appcompat.app.AlertDialog.BUTTON_POSITIVE)
                                .setOnClickListener(
                                        v -> {
                                            if (dates[0] != 0
                                                    && dates[1] != 0
                                                    && dates[0] >= dates[1]) {
                                                search.setError(
                                                        "Choose an end date on or after the start"
                                                                + " date.");
                                                return;
                                            }
                                            day = dates[0];
                                            rangeEnd = dates[1];
                                            query = Ui.value(search);
                                            methodFilter = Ui.value(method);
                                            statusFilter = Ui.selected(status);
                                            if (statusFilter.equals("All")) statusFilter = "";
                                            observe();
                                            dialog.dismiss();
                                        }));
        dialog.show();
    }

    private String dateLabel(String label, long value) {
        return label
                + ": "
                + (value == 0 ? "any date" : DateFormat.getDateInstance().format(new Date(value)));
    }

    private void filterDate(long[] dates, int index, android.widget.Button button) {
        Calendar c = Calendar.getInstance();
        if (dates[index] != 0) c.setTimeInMillis(dates[index] - (index == 1 ? 1 : 0));
        new DatePickerDialog(
                        requireContext(),
                        (v, y, m, d) -> {
                            Calendar selected = Calendar.getInstance();
                            selected.set(y, m, d, 0, 0, 0);
                            selected.set(Calendar.MILLISECOND, 0);
                            long date = selected.getTimeInMillis();
                            if (index == 1) selected.add(Calendar.DAY_OF_MONTH, 1);
                            dates[index] = selected.getTimeInMillis();
                            button.setText(dateLabel(index == 0 ? "From" : "Through", date));
                        },
                        c.get(Calendar.YEAR),
                        c.get(Calendar.MONTH),
                        c.get(Calendar.DAY_OF_MONTH))
                .show();
    }

    private void observe() {
        if (source != null) source.removeObservers(getViewLifecycleOwner());
        long end = rangeEnd;
        source =
                WaterRepository.get(requireContext())
                        .searchCatches(
                                smallest,
                                day,
                                end,
                                query,
                                statusFilter,
                                methodFilter,
                                sessionFilter);
        source.observe(
                getViewLifecycleOwner(),
                list -> {
                    adapter.submit(list);
                    int total = 0;
                    for (CatchRecord r : list) total += r.quantity;
                    String range =
                            day == 0 && rangeEnd == 0
                                    ? "All dates"
                                    : dateLabel("From", day)
                                            + " · "
                                            + dateLabel(
                                                    "Through", rangeEnd == 0 ? 0 : rangeEnd - 1);
                    summary.setText(
                            range
                                    + "\n"
                                    + (list.isEmpty()
                                            ? "No catches match these filters. Clear filters or log"
                                                    + " a catch."
                                            : list.size() + " entries · " + total + " fish")
                                    + "\n"
                                    + (smallest ? "Length: lowest first" : "Date: newest first")
                                    + (rangeEnd == 0 ? "" : " · date range")
                                    + (sessionFilter.isEmpty() ? "" : " · selected session")
                                    + (query.isEmpty() ? "" : " · search: " + query)
                                    + (statusFilter.isEmpty() ? "" : " · " + statusFilter)
                                    + (methodFilter.isEmpty() ? "" : " · method: " + methodFilter));
                });
    }

    @Override
    public void onSaveInstanceState(@NonNull Bundle b) {
        super.onSaveInstanceState(b);
        b.putLong("day", day);
        b.putLong("rangeEnd", rangeEnd);
        b.putString("query", query);
        b.putString("status", statusFilter);
        b.putString("method", methodFilter);
        b.putString("session", sessionFilter);
    }

    @Override
    public void onStop() {
        ageHandler.removeCallbacks(updateAges);
        if (currentLocation != null) currentLocation.cancel();
        if (currentRefresh != null) currentRefresh.setEnabled(true);
        super.onStop();
    }

    @Override
    public void onDestroyView() {
        ageHandler.removeCallbacks(updateAges);
        currentRequest++;
        if (currentLocation != null) currentLocation.cancel();
        currentStatus = null;
        currentArea = null;
        currentRefresh = null;
        currentValues.clear();
        currentQuality.clear();
        currentTide = null;
        sessionStatus = null;
        if (source != null) source.removeObservers(getViewLifecycleOwner());
        adapter = null;
        summary = null;
        super.onDestroyView();
    }
}
