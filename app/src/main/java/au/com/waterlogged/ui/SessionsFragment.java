package au.com.waterlogged.ui;

import android.os.Bundle;
import android.view.*;
import android.widget.*;

import androidx.annotation.*;
import androidx.appcompat.app.AlertDialog;
import androidx.fragment.app.Fragment;

import au.com.waterlogged.MainActivity;
import au.com.waterlogged.model.*;

import com.google.android.material.dialog.MaterialAlertDialogBuilder;

import java.text.DateFormat;
import java.util.Date;

public final class SessionsFragment extends Fragment {
    private LinearLayout rows;
    private FishingSession active;

    @Nullable
    @Override
    public View onCreateView(
            @NonNull LayoutInflater i, @Nullable ViewGroup c, @Nullable Bundle state) {
        ScrollView scroll = new ScrollView(requireContext());
        LinearLayout root = Ui.column(requireContext());
        Ui.pad(root, 16);
        scroll.addView(root);
        root.addView(Ui.text(requireContext(), "Your fishing sessions", 24, true));
        root.addView(
                Ui.muted(
                        requireContext(),
                        "Start a trip to reuse location, bait and method. Each catch gets its own"
                                + " time and conditions. Trips with no catches are kept too."));
        root.addView(
                Ui.button(
                        requireContext(),
                        "Start fishing",
                        () -> {
                            if (active != null)
                                message("End the active session before starting another.");
                            else edit(new FishingSession(), true);
                        }));
        rows = Ui.column(requireContext());
        root.addView(rows);
        return scroll;
    }

    @Override
    public void onViewCreated(@NonNull View v, @Nullable Bundle state) {
        WaterRepository.get(requireContext())
                .sessions()
                .observe(
                        getViewLifecycleOwner(),
                        items -> {
                            rows.removeAllViews();
                            active = null;
                            if (items.isEmpty())
                                rows.addView(
                                        Ui.muted(
                                                requireContext(),
                                                "No sessions yet. Start fishing to record your next"
                                                        + " trip."));
                            for (SessionSummary summary : items) {
                                FishingSession s = summary.session;
                                if (s.activeSlot != null) active = s;
                                LinearLayout card =
                                        Ui.card(
                                                requireContext(),
                                                (s.endedAt == null ? "● Fishing · " : "Trip · ")
                                                        + s.locationName);
                                card.addView(
                                        Ui.muted(
                                                requireContext(),
                                                DateFormat.getDateTimeInstance()
                                                                .format(new Date(s.startedAt))
                                                        + "\n"
                                                        + summary.description(
                                                                System.currentTimeMillis())
                                                        + "\nBait: "
                                                        + s.bait
                                                        + " · Method: "
                                                        + s.method));
                                card.addView(
                                        Ui.button(
                                                requireContext(),
                                                "View session catches",
                                                () ->
                                                        ((MainActivity) requireActivity())
                                                                .navigate(
                                                                        CatchListFragment
                                                                                .forSession(s.id),
                                                                        true)));
                                if (s.endedAt == null) {
                                    card.addView(
                                            Ui.button(
                                                    requireContext(),
                                                    "Log a catch",
                                                    () ->
                                                            ((MainActivity) requireActivity())
                                                                    .navigate(
                                                                            new SpeciesFragment(),
                                                                            true)));
                                    card.addView(
                                            Ui.button(
                                                    requireContext(),
                                                    "Edit session defaults",
                                                    () -> edit(s, false)));
                                    card.addView(
                                            Ui.button(
                                                    requireContext(),
                                                    "End session",
                                                    () ->
                                                            new MaterialAlertDialogBuilder(
                                                                            requireContext())
                                                                    .setTitle(
                                                                            "End this fishing"
                                                                                    + " session?")
                                                                    .setMessage(
                                                                            summary.description(
                                                                                    System
                                                                                            .currentTimeMillis()))
                                                                    .setNegativeButton(
                                                                            "Keep fishing", null)
                                                                    .setPositiveButton(
                                                                            "End session",
                                                                            (d, w) ->
                                                                                    WaterRepository
                                                                                            .get(
                                                                                                    requireContext())
                                                                                            .endSession(
                                                                                                    s.id,
                                                                                                    (unused,
                                                                                                            error) -> {
                                                                                                        if (isAdded())
                                                                                                            message(
                                                                                                                    error
                                                                                                                                    == null
                                                                                                                            ? "Session"
                                                                                                                                  + " ended"
                                                                                                                                  + " and saved."
                                                                                                                            : error
                                                                                                                                    .getMessage());
                                                                                                    }))
                                                                    .show()));
                                }
                                rows.addView(card);
                            }
                        });
    }

    private void edit(FishingSession original, boolean create) {
        LinearLayout content = Ui.column(requireContext());
        Ui.pad(content, 16);
        ScrollView scroll = new ScrollView(requireContext());
        scroll.addView(content);
        EditText place = Ui.field(requireContext(), content, "Session location name *", false),
                bait = Ui.field(requireContext(), content, "Default bait", false),
                method = Ui.field(requireContext(), content, "Default method", false);
        place.setText(original.locationName);
        bait.setText(original.bait);
        method.setText(original.method);
        Spinner environment =
                Ui.spinner(
                        requireContext(),
                        content,
                        "Environment",
                        new String[] {"Inshore", "Offshore", "Freshwater", "Estuary"});
        Ui.select(environment, original.environment);
        Double[] point = {original.latitude, original.longitude};
        TextView chosen =
                Ui.muted(
                        requireContext(),
                        point[0] == null
                                ? "No fixed point; new catches can use live GPS."
                                : "Fixed session point: " + point[0] + ", " + point[1]);
        content.addView(chosen);
        content.addView(
                Ui.button(
                        requireContext(),
                        "Choose session point",
                        () ->
                                LocationPicker.show(
                                        requireContext(),
                                        point[0],
                                        point[1],
                                        (lat, lon) -> {
                                            point[0] = lat;
                                            point[1] = lon;
                                            chosen.setText(
                                                    "Fixed session point: " + lat + ", " + lon);
                                        })));
        content.addView(
                Ui.button(
                        requireContext(),
                        "Use live GPS for new catches",
                        () -> {
                            point[0] = null;
                            point[1] = null;
                            chosen.setText("New catches can use live GPS.");
                        }));
        AlertDialog dialog =
                new MaterialAlertDialogBuilder(requireContext())
                        .setTitle(create ? "Start fishing" : "Session defaults")
                        .setView(scroll)
                        .setNegativeButton("Cancel", null)
                        .setPositiveButton("Save", null)
                        .create();
        dialog.setOnShowListener(
                d ->
                        dialog.getButton(AlertDialog.BUTTON_POSITIVE)
                                .setOnClickListener(
                                        v -> {
                                            if (Ui.value(place).isEmpty()) {
                                                place.setError("Enter a general location name.");
                                                return;
                                            }
                                            FishingSession s = new FishingSession();
                                            s.id = original.id;
                                            s.startedAt =
                                                    create
                                                            ? System.currentTimeMillis()
                                                            : original.startedAt;
                                            s.endedAt = original.endedAt;
                                            s.activeSlot = original.activeSlot;
                                            s.locationName = Ui.value(place);
                                            s.bait = Ui.value(bait);
                                            s.method = Ui.value(method);
                                            s.environment = Ui.selected(environment);
                                            s.latitude = point[0];
                                            s.longitude = point[1];
                                            dialog.getButton(AlertDialog.BUTTON_POSITIVE)
                                                    .setEnabled(false);
                                            WaterRepository.get(requireContext())
                                                    .saveSession(
                                                            s,
                                                            create,
                                                            (unused, error) -> {
                                                                if (!isAdded()) return;
                                                                if (error == null) dialog.dismiss();
                                                                else {
                                                                    dialog.getButton(
                                                                                    AlertDialog
                                                                                            .BUTTON_POSITIVE)
                                                                            .setEnabled(true);
                                                                    message(error.getMessage());
                                                                }
                                                            });
                                        }));
        dialog.show();
    }

    private void message(String text) {
        Toast.makeText(requireContext(), text, Toast.LENGTH_LONG).show();
    }

    @Override
    public void onResume() {
        super.onResume();
        ((MainActivity) requireActivity()).chrome("Fishing sessions", true, false, 0);
    }

    @Override
    public void onDestroyView() {
        rows = null;
        super.onDestroyView();
    }
}
