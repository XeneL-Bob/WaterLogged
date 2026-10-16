package au.com.waterlogged.ui;

import android.content.Context;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import au.com.waterlogged.R;
import au.com.waterlogged.model.CatchRecord;

import java.text.DateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

public class CatchAdapter extends RecyclerView.Adapter<CatchAdapter.Holder> {
    public interface Selection {
        void open(CatchRecord record);
    }

    private final List<CatchRecord> records = new ArrayList<>();
    private final Selection selection;
    private final View header;

    public CatchAdapter(View header, Selection selection) {
        this.header = header;
        this.selection = selection;
    }

    public void submit(List<CatchRecord> list) {
        records.clear();
        records.addAll(list);
        notifyDataSetChanged();
    }

    @Override
    public int getItemCount() {
        return records.size() + (header == null ? 0 : 1);
    }

    @Override
    public int getItemViewType(int p) {
        return header != null && p == 0 ? 1 : 0;
    }

    static class Holder extends RecyclerView.ViewHolder {
        ImageView image;
        TextView title, details, conditions;

        Holder(View v) {
            super(v);
        }
    }

    @NonNull
    @Override
    public Holder onCreateViewHolder(@NonNull ViewGroup parent, int type) {
        if (type == 1) {
            if (header.getParent() != null) ((ViewGroup) header.getParent()).removeView(header);
            header.setLayoutParams(new RecyclerView.LayoutParams(-1, -2));
            return new Holder(header);
        }
        Context c = parent.getContext();
        LinearLayout card = Ui.card(c, "");
        LinearLayout row = Ui.row(c);
        ImageView image = new ImageView(c);
        image.setScaleType(ImageView.ScaleType.CENTER_CROP);
        image.setContentDescription("Catch photo or species illustration");
        row.addView(image, new LinearLayout.LayoutParams(Ui.dp(c, 96), Ui.dp(c, 92)));
        LinearLayout labels = Ui.column(c);
        labels.setPadding(Ui.dp(c, 12), 0, 0, 0);
        Holder h = new Holder(card);
        h.image = image;
        h.title = Ui.text(c, "", 19, true);
        h.details = Ui.muted(c, "");
        h.conditions = Ui.muted(c, "");
        labels.addView(h.title);
        labels.addView(h.details);
        labels.addView(h.conditions);
        Ui.weighted(row, labels);
        row.addView(Ui.text(c, "›", 28, false));
        card.addView(row);
        card.setFocusable(true);
        card.setClickable(true);
        RecyclerView.LayoutParams lp = new RecyclerView.LayoutParams(-1, -2);
        lp.setMargins(Ui.dp(c, 16), 0, Ui.dp(c, 16), Ui.dp(c, 10));
        card.setLayoutParams(lp);
        return h;
    }

    @Override
    public void onBindViewHolder(@NonNull Holder h, int position) {
        if (getItemViewType(position) == 1) return;
        CatchRecord r = records.get(position - (header == null ? 0 : 1));
        h.title.setText(r.title);
        h.details.setText(
                DateFormat.getDateTimeInstance(DateFormat.MEDIUM, DateFormat.SHORT)
                                .format(new Date(r.caughtAt))
                        + "\n"
                        + Ui.measurement(r.lengthCm, "cm")
                        + "  ·  "
                        + r.quantity
                        + " "
                        + r.disposition.toLowerCase(java.util.Locale.getDefault()));
        h.conditions.setText(
                r.locationName
                        + "\nTide "
                        + Ui.measurement(r.tideHeightM, "m")
                        + "  ·  Wind "
                        + Ui.measurement(r.windKmh, "km/h"));
        SpeciesImages.show(h.image, r.speciesId);
        if (r.photoPath != null) {
            PhotoLoader.load(h.image, r.photoPath);
            h.image.setContentDescription("Catch photograph: " + r.title);
        }
        h.itemView.setOnClickListener(v -> selection.open(r));
    }

    public static int illustration(String id) {
        switch (id) {
            case "flathead":
                return R.drawable.fish_flathead;
            case "bream":
                return R.drawable.fish_bream;
            case "barramundi":
                return R.drawable.fish_barramundi;
            case "tailor":
                return R.drawable.fish_tailor;
            case "trevally":
                return R.drawable.fish_trevally;
            case "snapper":
                return R.drawable.fish_snapper;
            default:
                return R.drawable.ic_fish;
        }
    }
}
