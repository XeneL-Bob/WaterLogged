package au.com.waterlogged.ui;

import android.content.Context;
import android.graphics.*;
import android.view.View;

import au.com.waterlogged.services.TideSeries;

import java.time.*;
import java.time.format.DateTimeFormatter;
import java.util.Locale;

/** A small native chart with a text equivalent for screen readers. */
public final class TideChartView extends View {
    private final Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private TideSeries series;
    private long caughtAt;
    private String marker = "Catch";

    public TideChartView(Context c) {
        super(c);
        setMinimumHeight(Ui.dp(c, 180));
        setFocusable(true);
        setVisibility(GONE);
    }

    public void show(String json, long time) {
        show(json, time, "Catch");
    }

    public void show(String json, long time, String marker) {
        this.marker = marker;
        try {
            series = TideSeries.fromJson(json);
            caughtAt = time;
            setVisibility(VISIBLE);
            setContentDescription(description());
        } catch (Exception missing) {
            series = null;
            setVisibility(GONE);
        }
        invalidate();
        requestLayout();
    }

    public String description() {
        if (series == null) return "No saved tide prediction curve.";
        int low = 0, high = 0;
        for (int i = 1; i < series.times.length; i++) {
            if (series.heights[i] < series.heights[low]) low = i;
            if (series.heights[i] > series.heights[high]) high = i;
        }
        return String.format(
                Locale.getDefault(),
                "Tide predictions above LAT. Daily low %.2f m at %s; daily high %.2f m at %s."
                        + " Selected %s %s. Times AEST. Source: %s",
                series.heights[low],
                clock(series.times[low]),
                series.heights[high],
                clock(series.times[high]),
                marker.toLowerCase(Locale.ROOT),
                clock(caughtAt),
                series.source);
    }

    private static String clock(long t) {
        return Instant.ofEpochMilli(t)
                .atOffset(ZoneOffset.ofHours(10))
                .format(DateTimeFormatter.ofPattern("HH:mm"));
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        if (series == null) return;
        float left = Ui.dp(getContext(), 38),
                right = getWidth() - Ui.dp(getContext(), 12),
                top = Ui.dp(getContext(), 32),
                bottom = getHeight() - Ui.dp(getContext(), 32);
        double min = Double.POSITIVE_INFINITY, max = Double.NEGATIVE_INFINITY;
        for (double h : series.heights) {
            min = Math.min(min, h);
            max = Math.max(max, h);
        }
        min -= .15;
        max += .15;
        paint.setTextSize(Ui.dp(getContext(), 11));
        paint.setColor(Ui.MUTED);
        paint.setStrokeWidth(1);
        for (int i = 0; i < 3; i++) {
            float y = top + (bottom - top) * i / 2;
            canvas.drawLine(left, y, right, y, paint);
            canvas.drawText(
                    String.format(Locale.getDefault(), "%.1f", max - (max - min) * i / 2),
                    0,
                    y,
                    paint);
        }
        String day =
                Instant.ofEpochMilli(series.times[0])
                        .atOffset(ZoneOffset.ofHours(10))
                        .format(DateTimeFormatter.ofPattern("d MMM"));
        canvas.drawText("Tide · m above LAT · " + day + " AEST", 0, Ui.dp(getContext(), 16), paint);
        long start = series.times[0], end = series.times[series.times.length - 1];
        Path path = new Path();
        for (int i = 0; i < series.times.length; i++) {
            float x = left + (right - left) * (series.times[i] - start) / (float) (end - start);
            float y = bottom - (float) ((series.heights[i] - min) / (max - min)) * (bottom - top);
            if (i == 0) path.moveTo(x, y);
            else path.lineTo(x, y);
        }
        paint.setColor(Ui.SKY);
        paint.setStrokeWidth(Ui.dp(getContext(), 2));
        paint.setStyle(Paint.Style.STROKE);
        canvas.drawPath(path, paint);
        paint.setStyle(Paint.Style.FILL);
        if (caughtAt >= start && caughtAt <= end) {
            float x = left + (right - left) * (caughtAt - start) / (float) (end - start);
            paint.setColor(Ui.WHITE);
            canvas.drawLine(x, top, x, bottom, paint);
            canvas.drawText(
                    marker, Math.min(x + 4, right - 40), top + Ui.dp(getContext(), 12), paint);
        }
        paint.setColor(Ui.MUTED);
        canvas.drawText(clock(start), left, getHeight() - 8, paint);
        canvas.drawText(clock(end), Math.max(left, right - 45), getHeight() - 8, paint);
    }
}
