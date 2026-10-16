package au.com.waterlogged.ui;

import android.content.Context;
import android.text.InputType;
import android.widget.EditText;
import android.widget.LinearLayout;

import androidx.appcompat.app.AlertDialog;

import au.com.waterlogged.model.InputRules;

import com.google.android.material.dialog.MaterialAlertDialogBuilder;

/** An explicit point change, separate from editing a place's display name. */
public final class LocationPicker {
    public interface Selection {
        void picked(double latitude, double longitude);
    }

    private LocationPicker() {}

    public static void show(
            Context context, Double latitude, Double longitude, Selection selection) {
        LinearLayout content = Ui.column(context);
        Ui.pad(content, 20);
        content.addView(
                Ui.muted(
                        context,
                        "Enter a fishing location or use the public Mooloolaba example. This"
                                + " changes the point used to choose nearby data."));
        EditText lat = Ui.field(context, content, "Latitude (-90 to 90)", true);
        EditText lon = Ui.field(context, content, "Longitude (-180 to 180)", true);
        int signed =
                InputType.TYPE_CLASS_NUMBER
                        | InputType.TYPE_NUMBER_FLAG_DECIMAL
                        | InputType.TYPE_NUMBER_FLAG_SIGNED;
        lat.setInputType(signed);
        lon.setInputType(signed);
        Ui.fill(lat, latitude);
        Ui.fill(lon, longitude);
        content.addView(
                Ui.button(
                        context,
                        "Mooloolaba example",
                        () -> {
                            lat.setText("-26.6825");
                            lon.setText("153.1201");
                        }));
        android.widget.ScrollView scroll = new android.widget.ScrollView(context);
        scroll.addView(content);
        AlertDialog dialog =
                new MaterialAlertDialogBuilder(context)
                        .setTitle("Choose location")
                        .setView(scroll)
                        .setNegativeButton("Cancel", null)
                        .setPositiveButton("Use this point", null)
                        .create();
        dialog.setOnShowListener(
                unused ->
                        dialog.getButton(AlertDialog.BUTTON_POSITIVE)
                                .setOnClickListener(
                                        v -> {
                                            try {
                                                lat.setError(null);
                                                lon.setError(null);
                                                Double chosenLat =
                                                        InputRules.optionalNumber(
                                                                Ui.value(lat), -90, 90, "Latitude");
                                                Double chosenLon =
                                                        InputRules.optionalNumber(
                                                                Ui.value(lon),
                                                                -180,
                                                                180,
                                                                "Longitude");
                                                if (chosenLat == null || chosenLon == null)
                                                    throw new IllegalArgumentException(
                                                            "Enter both latitude and longitude.");
                                                selection.picked(chosenLat, chosenLon);
                                                dialog.dismiss();
                                            } catch (IllegalArgumentException error) {
                                                lat.setError(error.getMessage());
                                            }
                                        }));
        dialog.show();
    }
}
