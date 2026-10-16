package au.com.waterlogged.ui;

import android.app.Dialog;
import android.os.Bundle;
import android.view.ViewGroup;
import android.widget.LinearLayout;

import androidx.annotation.NonNull;
import androidx.fragment.app.DialogFragment;

public class PhotoDialog extends DialogFragment {
    public static PhotoDialog create(String path) {
        PhotoDialog f = new PhotoDialog();
        Bundle b = new Bundle();
        b.putString("path", path);
        f.setArguments(b);
        return f;
    }

    @NonNull
    @Override
    public Dialog onCreateDialog(Bundle state) {
        Dialog d =
                new Dialog(requireContext(), android.R.style.Theme_Material_NoActionBar_Fullscreen);
        LinearLayout root = Ui.column(requireContext());
        Ui.pad(root, 16);
        root.setBackgroundColor(android.graphics.Color.rgb(0, 20, 34));
        root.addView(
                Ui.muted(requireContext(), "Pinch to zoom · drag to move · double tap to reset"));
        ZoomImageView photo = new ZoomImageView(requireContext());
        photo.setContentDescription("Full size catch photo. Pinch to zoom.");
        root.addView(photo, new LinearLayout.LayoutParams(-1, 0, 1));
        root.addView(Ui.button(requireContext(), "Close photo", this::dismiss));
        d.setContentView(root);
        PhotoLoader.load(photo, requireArguments().getString("path"));
        return d;
    }

    @Override
    public void onStart() {
        super.onStart();
        if (getDialog() != null && getDialog().getWindow() != null)
            getDialog()
                    .getWindow()
                    .setLayout(
                            ViewGroup.LayoutParams.MATCH_PARENT,
                            ViewGroup.LayoutParams.MATCH_PARENT);
    }
}
