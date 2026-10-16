package au.com.waterlogged.services;

import org.json.*;

import java.util.*;

/** A source day's predictions, not a fitted curve or a tidal-current measurement. */
public final class TideSeries {
    public final long[] times;
    public final double[] heights;
    public final String source;
    public final long retrievedAt;

    public TideSeries(TreeMap<Long, Double> points, String source, long retrievedAt) {
        this.source = source;
        this.retrievedAt = retrievedAt;
        times = new long[points.size()];
        heights = new double[points.size()];
        int i = 0;
        for (Map.Entry<Long, Double> p : points.entrySet()) {
            times[i] = p.getKey();
            heights[i++] = p.getValue();
        }
    }

    public String json() {
        try {
            JSONArray points = new JSONArray();
            for (int i = 0; i < times.length; i++)
                points.put(new JSONArray().put(times[i]).put(heights[i]));
            return new JSONObject()
                    .put("points", points)
                    .put("source", source)
                    .put("retrievedAt", retrievedAt)
                    .toString();
        } catch (JSONException error) {
            throw new IllegalStateException(error);
        }
    }

    public static TideSeries fromJson(String text) throws JSONException {
        if (text == null || text.length() > 100000) throw new JSONException("Invalid tide series");
        JSONObject json = new JSONObject(text);
        JSONArray points = json.getJSONArray("points");
        if (points.length() < 2 || points.length() > 200)
            throw new JSONException("Invalid tide samples");
        TreeMap<Long, Double> values = new TreeMap<>();
        for (int i = 0; i < points.length(); i++) {
            JSONArray p = points.getJSONArray(i);
            long t = p.getLong(0);
            double h = p.getDouble(1);
            if (!Double.isFinite(h) || h < -20 || h > 30 || t < 0 || values.containsKey(t))
                throw new JSONException("Invalid tide sample");
            values.put(t, h);
        }
        if (values.lastKey() - values.firstKey() > 26 * 3600000L)
            throw new JSONException("Invalid tide day");
        return new TideSeries(values, json.getString("source"), json.getLong("retrievedAt"));
    }
}
