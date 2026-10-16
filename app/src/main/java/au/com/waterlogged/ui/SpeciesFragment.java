package au.com.waterlogged.ui;

import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.LiveData;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import au.com.waterlogged.MainActivity;
import au.com.waterlogged.R;
import au.com.waterlogged.model.*;

import java.util.ArrayList;
import java.util.List;

public class SpeciesFragment extends Fragment {
    private SpeciesAdapter adapter;
    private LiveData<List<Species>> source;
    private TextView count;

    @Nullable
    @Override
    public View onCreateView(
            @NonNull LayoutInflater i, @Nullable ViewGroup container, @Nullable Bundle state) {
        LinearLayout root = Ui.column(requireContext());
        Ui.pad(root, 16);
        EditText search =
                Ui.field(requireContext(), root, "Search common or scientific name", false);
        search.setHint("Try snapper, Pagrus, or flat…");
        search.setInputType(android.text.InputType.TYPE_CLASS_TEXT);
        root.addView(
                Ui.muted(
                        requireContext(),
                        "★ Favourite species appear first. Tap a star to change your favourites."));
        count = Ui.muted(requireContext(), "Loading offline species catalogue…");
        root.addView(count);
        root.addView(
                Ui.muted(
                        requireContext(),
                        "Tap a reference image for its author, licence and source. Confirm fish"
                                + " identification independently."));
        root.addView(
                Ui.button(
                        requireContext(),
                        "Log another species",
                        () ->
                                ((MainActivity) requireActivity())
                                        .navigate(CatchItemFragment.fresh("", ""), true)));
        root.addView(Ui.button(requireContext(), "Add a custom species", this::addCustomSpecies));
        RecyclerView list = new RecyclerView(requireContext());
        list.setLayoutManager(new LinearLayoutManager(requireContext()));
        adapter = new SpeciesAdapter(root);
        list.setAdapter(adapter);
        Ui.pad(list, 16);
        list.setClipToPadding(false);
        Ui.pad(root, 0);
        search.addTextChangedListener(
                new TextWatcher() {
                    public void beforeTextChanged(CharSequence s, int a, int c, int f) {}

                    public void onTextChanged(CharSequence s, int a, int b, int c) {
                        observe(s.toString().trim());
                    }

                    public void afterTextChanged(Editable e) {}
                });
        return list;
    }

    @Override
    public void onViewCreated(@NonNull View v, @Nullable Bundle state) {
        observe("");
    }

    @Override
    public void onResume() {
        super.onResume();
        ((MainActivity) requireActivity()).chrome("Species Search", false, false, R.id.nav_species);
    }

    private void observe(String term) {
        if (source != null) source.removeObservers(getViewLifecycleOwner());
        source = WaterRepository.get(requireContext()).species(term);
        source.observe(
                getViewLifecycleOwner(),
                items -> {
                    adapter.items.clear();
                    adapter.items.addAll(items);
                    adapter.notifyDataSetChanged();
                    count.setText(
                            items.isEmpty()
                                    ? "No matches. Try a common name or part of a scientific name."
                                    : items.size() + " species · offline reference catalogue");
                });
    }

    private class SpeciesAdapter extends RecyclerView.Adapter<SpeciesHolder> {
        final List<Species> items = new ArrayList<>();
        final View header;

        SpeciesAdapter(View header) {
            this.header = header;
        }

        @Override
        public int getItemViewType(int position) {
            return position == 0 ? 0 : 1;
        }

        @Override
        public int getItemCount() {
            return items.size() + 1;
        }

        @NonNull
        @Override
        public SpeciesHolder onCreateViewHolder(@NonNull ViewGroup p, int type) {
            if (type == 0) {
                header.setLayoutParams(new RecyclerView.LayoutParams(-1, -2));
                return new SpeciesHolder(header, null, null, null, null);
            }
            LinearLayout card = Ui.card(requireContext(), "");
            LinearLayout row = Ui.row(requireContext());
            ImageView photo = new ImageView(requireContext());
            photo.setScaleType(ImageView.ScaleType.FIT_CENTER);
            row.addView(
                    photo,
                    new LinearLayout.LayoutParams(
                            Ui.dp(requireContext(), 88), Ui.dp(requireContext(), 76)));
            LinearLayout labels = Ui.column(requireContext());
            labels.setPadding(Ui.dp(requireContext(), 10), 0, 0, 0);
            TextView name = Ui.text(requireContext(), "", 18, true),
                    latin = Ui.muted(requireContext(), "");
            latin.setTypeface(null, android.graphics.Typeface.ITALIC);
            labels.addView(name);
            labels.addView(latin);
            Ui.weighted(row, labels);
            TextView favourite = Ui.text(requireContext(), "☆", 28, true);
            favourite.setTextColor(Ui.SKY);
            favourite.setGravity(android.view.Gravity.CENTER);
            favourite.setMinWidth(Ui.dp(requireContext(), 48));
            favourite.setMinHeight(Ui.dp(requireContext(), 48));
            favourite.setFocusable(true);
            row.addView(favourite);
            card.addView(row);
            card.setFocusable(true);
            RecyclerView.LayoutParams lp = new RecyclerView.LayoutParams(-1, -2);
            lp.setMargins(0, 0, 0, Ui.dp(requireContext(), 10));
            card.setLayoutParams(lp);
            return new SpeciesHolder(card, photo, name, latin, favourite);
        }

        @Override
        public void onBindViewHolder(@NonNull SpeciesHolder h, int p) {
            if (p == 0) return;
            Species s = items.get(p - 1);
            h.name.setText(s.commonName);
            h.latin.setText(
                    s.id.startsWith("custom:")
                            ? "Custom · unverified\n" + s.scientificName
                            : s.scientificName);
            SpeciesImages.show(h.photo, s.id);
            h.photo.setContentDescription(s.commonName + " reference image. Tap for credits.");
            h.photo.setOnClickListener(v -> SpeciesImages.showCredits(requireContext(), s.id));
            h.star.setText(s.favourite ? "★" : "☆");
            h.star.setContentDescription(
                    (s.favourite ? "Unfavourite " : "Favourite ") + s.commonName);
            h.star.setOnClickListener(
                    v -> WaterRepository.get(requireContext()).toggleFavourite(s.id));
            h.itemView.setOnClickListener(
                    v ->
                            ((MainActivity) requireActivity())
                                    .navigate(CatchItemFragment.fresh(s.id, s.commonName), true));
        }
    }

    private static class SpeciesHolder extends RecyclerView.ViewHolder {
        ImageView photo;
        TextView name, latin, star;

        SpeciesHolder(View v, ImageView p, TextView n, TextView l, TextView s) {
            super(v);
            photo = p;
            name = n;
            latin = l;
            star = s;
        }
    }

    private void addCustomSpecies() {
        LinearLayout content = Ui.column(requireContext());
        Ui.pad(content, 16);
        android.widget.ScrollView scroll = new android.widget.ScrollView(requireContext());
        scroll.addView(content);
        EditText name = Ui.field(requireContext(), content, "Common name *", false),
                scientific =
                        Ui.field(requireContext(), content, "Scientific name (optional)", false),
                aliases =
                        Ui.field(
                                requireContext(),
                                content,
                                "Regional names / aliases (optional)",
                                false);
        content.addView(
                Ui.muted(
                        requireContext(),
                        "Saved privately in your catalogue, marked unverified and available"
                            + " offline. No request or photo is sent to anyone. A generic icon is"
                            + " used until a verified licensed reference is available."));
        androidx.appcompat.app.AlertDialog dialog =
                new com.google.android.material.dialog.MaterialAlertDialogBuilder(requireContext())
                        .setTitle("Add custom species")
                        .setView(scroll)
                        .setNegativeButton("Cancel", null)
                        .setPositiveButton("Save species", null)
                        .create();
        dialog.setOnShowListener(
                d ->
                        dialog.getButton(androidx.appcompat.app.AlertDialog.BUTTON_POSITIVE)
                                .setOnClickListener(
                                        v -> {
                                            if (Ui.value(name).isEmpty()
                                                    || Ui.value(name).length() > 100) {
                                                name.setError("Enter a name up to 100 characters.");
                                                return;
                                            }
                                            if (Ui.value(scientific).length() > 200
                                                    || Ui.value(aliases).length() > 1000) {
                                                aliases.setError(
                                                        "Keep the scientific name under 200 and"
                                                            + " aliases under 1000 characters.");
                                                return;
                                            }
                                            Species s =
                                                    new Species(
                                                            "custom:" + java.util.UUID.randomUUID(),
                                                            Ui.value(name),
                                                            Ui.value(scientific),
                                                            Ui.value(aliases),
                                                            true);
                                            dialog.getButton(
                                                            androidx.appcompat.app.AlertDialog
                                                                    .BUTTON_POSITIVE)
                                                    .setEnabled(false);
                                            WaterRepository.get(requireContext())
                                                    .addSpecies(
                                                            s,
                                                            (unused, error) -> {
                                                                if (!isAdded()) return;
                                                                if (error == null) dialog.dismiss();
                                                                else {
                                                                    dialog.getButton(
                                                                                    androidx
                                                                                            .appcompat
                                                                                            .app
                                                                                            .AlertDialog
                                                                                            .BUTTON_POSITIVE)
                                                                            .setEnabled(true);
                                                                    name.setError(
                                                                            error.getMessage());
                                                                }
                                                            });
                                        }));
        dialog.show();
    }

    @Override
    public void onDestroyView() {
        if (source != null) source.removeObservers(getViewLifecycleOwner());
        adapter = null;
        count = null;
        super.onDestroyView();
    }
}
