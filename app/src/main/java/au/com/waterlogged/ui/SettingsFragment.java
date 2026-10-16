package au.com.waterlogged.ui;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.ScrollView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import au.com.waterlogged.MainActivity;
import au.com.waterlogged.R;

public class SettingsFragment extends Fragment {
    @Nullable
    @Override
    public View onCreateView(
            @NonNull LayoutInflater i, @Nullable ViewGroup p, @Nullable Bundle state) {
        ScrollView scroll = new ScrollView(requireContext());
        LinearLayout root = Ui.column(requireContext());
        Ui.pad(root, 16);
        scroll.addView(root);
        root.addView(
                Ui.button(
                        requireContext(),
                        "Backup, restore & CSV export",
                        () ->
                                ((MainActivity) requireActivity())
                                        .navigate(new BackupFragment(), true)));
        root.addView(
                Ui.button(
                        requireContext(),
                        "Fishing sessions",
                        () ->
                                ((MainActivity) requireActivity())
                                        .navigate(new SessionsFragment(), true)));
        LinearLayout privacy = Ui.card(requireContext(), "Your marks stay with you");
        privacy.addView(
                Ui.muted(
                        requireContext(),
                        "Catches, exact coordinates and photos are stored in this app on this"
                                + " device. There is no account, analytics service or cloud sync."
                                + " Automatic Android backup and device transfer are disabled."));
        privacy.addView(
                Ui.muted(
                        requireContext(),
                        "Keep an encrypted backup from Settings before uninstalling or clearing"
                            + " storage. Device storage is protected by Android's app sandbox; this"
                            + " pilot does not add database encryption."));
        privacy.addView(
                Ui.muted(
                        requireContext(),
                        "Shared summaries include your location name. Exact GPS, photos and notes"
                            + " are excluded. Opening Show map shares the chosen mark with your map"
                            + " app."));
        root.addView(privacy);
        privacy.addView(
                Ui.muted(
                        requireContext(),
                        "Automatic conditions and location names send GPS rounded to two decimals"
                                + " (about 1 km) to data providers. Tide requests identify a public"
                                + " gauge. Exact marks are kept locally."));
        LinearLayout features = Ui.card(requireContext(), "Water Logged pilot 0.4.1");
        features.addView(
                Ui.muted(
                        requireContext(),
                        "Available: encrypted backup/restore, CSV export, fishing sessions, diary"
                            + " filters, custom species, tide charts, automatic-value reset, catch"
                            + " diary, species search, favourites, automatic nearby weather and"
                            + " swell, Queensland tide predictions, offline moon phase, editable"
                            + " saved snapshots, GPS, camera, zoom, sharing and local storage."));
        features.addView(
                Ui.muted(
                        requireContext(),
                        "Available: 32 offline species, licensed reference images, nearby station"
                            + " and buoy observations, separate waves/swell/gusts and manual point"
                            + " selection. Planned: tides outside Queensland, verified catch limits"
                            + " and marine park boundary alerts."));
        features.addView(
                Ui.muted(
                        requireContext(),
                        "Catch-limit and marine-park alerts are not active. Check the official"
                                + " rules for your state, species and fishing area."));
        root.addView(features);
        root.addView(
                Ui.button(
                        requireContext(),
                        "Help & official fishing rules",
                        () ->
                                ((MainActivity) requireActivity())
                                        .navigate(new HelpFragment(), true)));
        return scroll;
    }

    @Override
    public void onResume() {
        super.onResume();
        ((MainActivity) requireActivity()).chrome("Settings", false, false, R.id.nav_settings);
    }
}
