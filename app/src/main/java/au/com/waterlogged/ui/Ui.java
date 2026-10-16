package au.com.waterlogged.ui;

import android.content.Context;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.text.InputType;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.Spinner;
import android.widget.TextView;

import com.google.android.material.button.MaterialButton;

/** Shared visual primitives; controllers keep persistence and navigation out of these views. */
public final class Ui {
    public static final int WHITE = Color.rgb(245, 250, 255),
            MUTED = Color.rgb(185, 216, 239),
            SKY = Color.rgb(114, 199, 255);

    private Ui() {}

    public static int dp(Context c, float n) {
        return (int) (n * c.getResources().getDisplayMetrics().density + .5f);
    }

    public static LinearLayout column(Context c) {
        LinearLayout v = new LinearLayout(c);
        v.setOrientation(LinearLayout.VERTICAL);
        return v;
    }

    public static LinearLayout row(Context c) {
        LinearLayout v = new LinearLayout(c);
        v.setOrientation(LinearLayout.HORIZONTAL);
        v.setGravity(android.view.Gravity.CENTER_VERTICAL);
        return v;
    }

    public static void pad(View v, int n) {
        int p = dp(v.getContext(), n);
        v.setPadding(p, p, p, p);
    }

    public static TextView text(Context c, String s, int size, boolean bold) {
        TextView t = new TextView(c);
        t.setText(s);
        t.setTextColor(WHITE);
        t.setTextSize(size);
        if (bold) t.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
        t.setPadding(0, dp(c, 4), 0, dp(c, 4));
        return t;
    }

    public static TextView muted(Context c, String s) {
        TextView t = text(c, s, 14, false);
        t.setTextColor(MUTED);
        return t;
    }

    public static GradientDrawable background(int start, int end, int radius, Context c) {
        GradientDrawable b =
                new GradientDrawable(GradientDrawable.Orientation.TL_BR, new int[] {start, end});
        b.setCornerRadius(dp(c, radius));
        b.setStroke(dp(c, 1), Color.rgb(28, 104, 151));
        return b;
    }

    public static LinearLayout card(Context c, String title) {
        LinearLayout card = column(c);
        pad(card, 14);
        card.setBackground(background(Color.rgb(9, 69, 111), Color.rgb(4, 48, 79), 16, c));
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(-1, -2);
        lp.setMargins(0, 0, 0, dp(c, 12));
        card.setLayoutParams(lp);
        if (!title.isEmpty()) card.addView(text(c, title, 18, true));
        return card;
    }

    public static MaterialButton button(Context c, String label, Runnable action) {
        MaterialButton b = new MaterialButton(c);
        b.setText(label);
        b.setAllCaps(false);
        b.setTextColor(WHITE);
        b.setTextSize(14);
        b.setCornerRadius(dp(c, 14));
        b.setMinHeight(dp(c, 48));
        b.setOnClickListener(v -> action.run());
        return b;
    }

    public static EditText field(Context c, LinearLayout parent, String label, boolean decimal) {
        parent.addView(muted(c, label));
        EditText e = new EditText(c);
        e.setSingleLine(true);
        e.setTextColor(WHITE);
        e.setTextSize(16);
        e.setHintTextColor(MUTED);
        e.setMinHeight(dp(c, 48));
        pad(e, 10);
        e.setBackground(background(Color.rgb(4, 49, 81), Color.rgb(4, 49, 81), 10, c));
        e.setInputType(
                decimal
                        ? InputType.TYPE_CLASS_NUMBER | InputType.TYPE_NUMBER_FLAG_DECIMAL
                        : InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_FLAG_CAP_SENTENCES);
        e.setContentDescription(label);
        e.setImportantForAutofill(View.IMPORTANT_FOR_AUTOFILL_NO);
        parent.addView(e, new LinearLayout.LayoutParams(-1, -2));
        return e;
    }

    public static Spinner spinner(Context c, LinearLayout parent, String label, String[] values) {
        parent.addView(muted(c, label));
        Spinner spinner = new Spinner(c);
        ArrayAdapter<String> a =
                new ArrayAdapter<>(c, android.R.layout.simple_spinner_item, values);
        a.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinner.setAdapter(a);
        spinner.setMinimumHeight(dp(c, 48));
        spinner.setContentDescription(label);
        parent.addView(spinner, new LinearLayout.LayoutParams(-1, -2));
        return spinner;
    }

    public static void select(Spinner spinner, String value) {
        for (int i = 0; i < spinner.getCount(); i++)
            if (spinner.getItemAtPosition(i).equals(value)) {
                spinner.setSelection(i);
                break;
            }
    }

    public static String selected(Spinner spinner) {
        return String.valueOf(spinner.getSelectedItem());
    }

    public static String value(EditText e) {
        return e.getText().toString().trim();
    }

    public static void fill(EditText e, Object n) {
        e.setText(n == null ? "" : n.toString());
    }

    public static void weighted(LinearLayout parent, View child) {
        LinearLayout.LayoutParams lp =
                new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1);
        lp.setMargins(0, 0, dp(parent.getContext(), 6), 0);
        parent.addView(child, lp);
    }

    public static String measurement(Double value, String unit) {
        return value == null
                ? "—"
                : String.format(
                        java.util.Locale.getDefault(),
                        "%s %s",
                        new java.text.DecimalFormat("0.#").format(value),
                        unit);
    }
}
