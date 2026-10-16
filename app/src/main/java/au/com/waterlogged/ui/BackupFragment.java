package au.com.waterlogged.ui;

import android.net.Uri;
import android.os.Bundle;
import android.text.InputType;
import android.view.*;
import android.widget.*;

import androidx.activity.result.*;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.*;
import androidx.appcompat.app.AlertDialog;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.*;

import au.com.waterlogged.MainActivity;
import au.com.waterlogged.model.WaterRepository;
import au.com.waterlogged.services.BackupService;

import com.google.android.material.dialog.MaterialAlertDialogBuilder;

import java.util.Arrays;

public final class BackupFragment extends Fragment {
    public static class Job extends ViewModel {
        final MutableLiveData<String> status =
                new MutableLiveData<>("Choose a backup or CSV action.");
        boolean busy, coordinates;
        char[] password;

        void clearPassword() {
            if (password != null) Arrays.fill(password, '\0');
            password = null;
        }

        @Override
        protected void onCleared() {
            clearPassword();
        }
    }

    private Job job;
    private Runnable bindStatus;
    private ActivityResultLauncher<String> saveBackup, saveCsv;
    private ActivityResultLauncher<String[]> openBackup;

    @Override
    public void onCreate(Bundle state) {
        super.onCreate(state);
        job = new ViewModelProvider(this).get(Job.class);
        saveBackup =
                registerForActivityResult(
                        new ActivityResultContracts.CreateDocument("application/octet-stream"),
                        uri -> {
                            if (uri == null) {
                                job.clearPassword();
                                job.busy = false;
                                job.status.setValue("Backup cancelled.");
                                return;
                            }
                            if (job.password == null) {
                                job.busy = false;
                                job.status.setValue(
                                        "Password was not retained after restart. Please create"
                                                + " another backup.");
                                return;
                            }
                            char[] password = job.password;
                            job.password = null;
                            android.content.Context app = requireApp();
                            run(
                                    (db, photos) -> {
                                        try {
                                            return BackupService.exportBackup(
                                                    app, db, photos, uri, password);
                                        } finally {
                                            Arrays.fill(password, '\0');
                                        }
                                    });
                        });
        saveCsv =
                registerForActivityResult(
                        new ActivityResultContracts.CreateDocument("text/csv"),
                        uri -> {
                            if (uri == null) {
                                job.busy = false;
                                job.status.setValue("CSV export cancelled.");
                                return;
                            }
                            android.content.Context app = requireApp();
                            boolean coordinates = job.coordinates;
                            run((db, p) -> BackupService.csv(app, db, uri, coordinates));
                        });
        openBackup =
                registerForActivityResult(
                        new ActivityResultContracts.OpenDocument(),
                        uri -> {
                            job.busy = false;
                            job.status.setValue("Choose a password to unlock the backup.");
                            if (uri != null) passwordDialog(false, uri);
                            else job.status.setValue("Restore cancelled.");
                        });
    }

    private android.content.Context requireApp() {
        return requireContext().getApplicationContext();
    }

    @Nullable
    @Override
    public View onCreateView(
            @NonNull LayoutInflater inflater,
            @Nullable ViewGroup container,
            @Nullable Bundle state) {
        ScrollView scroll = new ScrollView(requireContext());
        LinearLayout root = Ui.column(requireContext());
        Ui.pad(root, 16);
        scroll.addView(root);
        root.addView(Ui.text(requireContext(), "Keep a copy of your diary", 24, true));
        root.addView(
                Ui.muted(
                        requireContext(),
                        "Encrypted backups include catches, private photos, sessions, favourites"
                            + " and saved condition sources/curves. You choose the destination;"
                            + " selecting a cloud provider sends the file there. The password is"
                            + " never saved and cannot be recovered."));
        Button backup =
                Ui.button(
                        requireContext(),
                        "Create encrypted backup",
                        () -> passwordDialog(true, null));
        root.addView(backup);
        root.addView(
                Ui.muted(
                        requireContext(),
                        "Restore adds missing records and keeps existing catches. Favourite choices"
                            + " are restored from the backup. Trips active in a backup return as"
                            + " ended at its creation time."));
        Button restore =
                Ui.button(
                        requireContext(),
                        "Restore encrypted backup",
                        () -> {
                            job.busy = true;
                            job.status.setValue("Choose a backup file.");
                            openBackup.launch(
                                    new String[] {
                                        "application/octet-stream", "application/zip", "*/*"
                                    });
                        });
        root.addView(restore);
        root.addView(Ui.text(requireContext(), "Spreadsheet export", 20, true));
        root.addView(
                Ui.muted(
                        requireContext(),
                        "CSV is unencrypted and includes location names and notes. It is for"
                            + " analysis, not full diary recovery. Exact coordinates are excluded"
                            + " unless you select the option below."));
        CheckBox coordinates = new CheckBox(requireContext());
        coordinates.setText("Include exact coordinates in CSV");
        coordinates.setTextColor(Ui.WHITE);
        coordinates.setChecked(job.coordinates);
        root.addView(coordinates);
        Button csv =
                Ui.button(
                        requireContext(),
                        "Export catches as CSV",
                        () -> {
                            job.coordinates = coordinates.isChecked();
                            job.busy = true;
                            job.status.setValue("Choose where to save the CSV.");
                            saveCsv.launch("water-logged-catches.csv");
                        });
        root.addView(csv);
        TextView status = Ui.muted(requireContext(), "");
        status.setAccessibilityLiveRegion(View.ACCESSIBILITY_LIVE_REGION_POLITE);
        root.addView(status);
        bindStatus =
                () ->
                        job.status.observe(
                                getViewLifecycleOwner(),
                                text -> {
                                    status.setText(text);
                                    backup.setEnabled(!job.busy);
                                    restore.setEnabled(!job.busy);
                                    csv.setEnabled(!job.busy);
                                    coordinates.setEnabled(!job.busy);
                                });
        return scroll;
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle state) {
        bindStatus.run();
        bindStatus = null;
    }

    private void passwordDialog(boolean creating, Uri uri) {
        if (job.busy) return;
        LinearLayout content = Ui.column(requireContext());
        Ui.pad(content, 20);
        EditText password = Ui.field(requireContext(), content, "Backup password", false);
        password.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_PASSWORD);
        password.setSaveEnabled(false);
        EditText confirmation =
                creating ? Ui.field(requireContext(), content, "Confirm password", false) : null;
        if (confirmation != null) {
            confirmation.setInputType(password.getInputType());
            confirmation.setSaveEnabled(false);
        }
        AlertDialog dialog =
                new MaterialAlertDialogBuilder(requireContext())
                        .setTitle(creating ? "Protect your backup" : "Unlock and merge backup")
                        .setView(content)
                        .setNegativeButton("Cancel", null)
                        .setPositiveButton(creating ? "Choose destination" : "Restore", null)
                        .create();
        dialog.setOnShowListener(
                d ->
                        dialog.getButton(AlertDialog.BUTTON_POSITIVE)
                                .setOnClickListener(
                                        v -> {
                                            String value = password.getText().toString();
                                            if (value.length() < 8) {
                                                password.setError("Use at least 8 characters.");
                                                return;
                                            }
                                            if (creating
                                                    && !value.equals(
                                                            confirmation.getText().toString())) {
                                                confirmation.setError("Passwords do not match.");
                                                return;
                                            }
                                            char[] chars = value.toCharArray();
                                            password.setText("");
                                            if (confirmation != null) confirmation.setText("");
                                            dialog.dismiss();
                                            android.content.Context app = requireApp();
                                            if (creating) {
                                                job.password = chars;
                                                job.busy = true;
                                                job.status.setValue(
                                                        "Choose where to save the encrypted"
                                                                + " backup.");
                                                saveBackup.launch(
                                                        "water-logged-"
                                                                + System.currentTimeMillis()
                                                                + ".wlog");
                                            } else
                                                run(
                                                        (db, photos) -> {
                                                            try {
                                                                return BackupService.restore(
                                                                        app, db, photos, uri,
                                                                        chars);
                                                            } finally {
                                                                Arrays.fill(chars, '\0');
                                                            }
                                                        });
                                        }));
        dialog.show();
    }

    private void run(WaterRepository.Task<String> task) {
        job.busy = true;
        job.status.setValue("Working… keep the app open until this completes.");
        Job target = job;
        WaterRepository.get(requireContext())
                .execute(
                        task,
                        (result, error) -> {
                            target.busy = false;
                            target.status.setValue(
                                    error == null
                                            ? result
                                            : "Operation failed: "
                                                    + (error.getMessage() == null
                                                            ? "Please check the file and password."
                                                            : error.getMessage()));
                        });
    }

    @Override
    public void onResume() {
        super.onResume();
        ((MainActivity) requireActivity()).chrome("Backup & export", true, false, 0);
    }
}
