package au.com.waterlogged.services;

import au.com.waterlogged.services.ConditionsProvider.*;

import org.json.*;

import java.time.*;
import java.time.format.DateTimeFormatter;
import java.util.*;

/** Rejects absent, out-of-range and wrong-time readings instead of silently using today's data. */
public final class ConditionParsers {
    private ConditionParsers() {}

    public static int nearest(JSONArray seconds, long time, long tolerance) throws JSONException {
        int best = -1;
        long delta = Long.MAX_VALUE;
        for (int i = 0; i < seconds.length(); i++) {
            if (seconds.isNull(i)) continue;
            long d = Math.abs(seconds.getLong(i) * 1000L - time);
            if (d < delta) {
                best = i;
                delta = d;
            }
        }
        return delta <= tolerance ? best : -1;
    }

    public static Double number(JSONArray values, int index, double min, double max) {
        if (values == null || index < 0 || index >= values.length() || values.isNull(index))
            return null;
        double n = values.optDouble(index, Double.NaN);
        return Double.isFinite(n) && n >= min && n <= max ? n : null;
    }

    public static Snapshot weather(
            HttpJsonClient.Response response, double lat, double lon, long time, boolean marine)
            throws Exception {
        Snapshot result = new Snapshot();
        JSONObject json = response.json;
        double gridLat = json.getDouble("latitude"),
                gridLon = json.getDouble("longitude"),
                km = GeoMath.distanceKm(lat, lon, gridLat, gridLon);
        if (!Double.isFinite(km)
                || gridLat < -90
                || gridLat > 90
                || gridLon < -180
                || gridLon > 180
                || km > 50)
            throw new IllegalArgumentException("No nearby model point within 50 km");
        JSONObject hourly = json.getJSONObject("hourly");
        JSONArray times = hourly.getJSONArray("time");
        int i = nearest(times, time, 30 * 60000L);
        if (i < 0) throw new IllegalArgumentException("No reading for this catch time");
        String source =
                String.format(
                        Locale.US,
                        "Open-Meteo %s · model point %.2f, %.2f (%.1f km away)%s",
                        marine ? "marine" : "weather",
                        gridLat,
                        gridLon,
                        km,
                        HttpJsonClient.cacheLabel(response));
        long at = times.getLong(i) * 1000;
        if (marine) {
            put(
                    result,
                    Field.SWELL,
                    number(hourly.optJSONArray("swell_wave_height"), i, 0, 40),
                    source + " · swell component",
                    at,
                    response.retrievedAt);
            put(
                    result,
                    Field.WAVE_HEIGHT,
                    number(hourly.optJSONArray("wave_height"), i, 0, 40),
                    source + " · total significant waves",
                    at,
                    response.retrievedAt);
        } else {
            put(
                    result,
                    Field.TEMPERATURE,
                    number(hourly.optJSONArray("temperature_2m"), i, -90, 65),
                    source,
                    at,
                    response.retrievedAt);
            put(
                    result,
                    Field.WIND_SPEED,
                    number(hourly.optJSONArray("wind_speed_10m"), i, 0, 400),
                    source,
                    at,
                    response.retrievedAt);
            put(
                    result,
                    Field.WIND_GUST,
                    number(hourly.optJSONArray("wind_gusts_10m"), i, 0, 500),
                    source + " · gust",
                    at,
                    response.retrievedAt);
            Double dir = number(hourly.optJSONArray("wind_direction_10m"), i, 0, 360);
            put(
                    result,
                    Field.WIND_DIRECTION,
                    dir == null ? null : GeoMath.compass(dir),
                    source + " · direction wind comes from",
                    at,
                    response.retrievedAt);
        }
        int expected = marine ? 2 : 4;
        if (result.readings.size() < expected)
            result.notices.add(
                    (marine ? "Some wave values" : "Some weather values")
                            + " unavailable for this location/time; enter manually.");
        return result;
    }

    private static void put(Snapshot s, Field f, Object v, String source, long at, long retrieved) {
        if (v != null) s.readings.put(f, new Reading(v, source, at, retrieved));
    }

    private static Double bounded(JSONObject row, String key, double min, double max) {
        if (row.isNull(key)) return null;
        double value = row.optDouble(key, Double.NaN);
        return Double.isFinite(value) && value >= min && value <= max ? value : null;
    }

    public static Snapshot observations(HttpJsonClient.Response response, long time, String source)
            throws Exception {
        JSONArray rows = response.json.getJSONObject("observations").getJSONArray("data");
        JSONObject selected = null;
        long selectedAt = Long.MIN_VALUE;
        DateTimeFormatter fmt = DateTimeFormatter.ofPattern("uuuuMMddHHmmss");
        for (int i = 0; i < rows.length(); i++) {
            try {
                JSONObject row = rows.getJSONObject(i);
                long at =
                        LocalDateTime.parse(row.getString("aifstime_utc"), fmt)
                                .toInstant(ZoneOffset.UTC)
                                .toEpochMilli();
                // An observation from after the catch would describe a different moment.
                if (at <= time && time - at <= 60 * 60000L && at > selectedAt) {
                    selected = row;
                    selectedAt = at;
                }
            } catch (Exception ignored) {
            }
        }
        if (selected == null)
            throw new IllegalArgumentException("No observation in the previous hour");
        Snapshot s = new Snapshot();
        String label = source + " · observed" + HttpJsonClient.cacheLabel(response);
        put(
                s,
                Field.TEMPERATURE,
                bounded(selected, "air_temp", -90, 65),
                label,
                selectedAt,
                response.retrievedAt);
        put(
                s,
                Field.WIND_SPEED,
                bounded(selected, "wind_spd_kmh", 0, 400),
                label + " · sustained wind",
                selectedAt,
                response.retrievedAt);
        put(
                s,
                Field.WIND_GUST,
                bounded(selected, "gust_kmh", 0, 500),
                label + " · gust",
                selectedAt,
                response.retrievedAt);
        String direction = selected.optString("wind_dir", "");
        if (direction.matches("N|NNE|NE|ENE|E|ESE|SE|SSE|S|SSW|SW|WSW|W|WNW|NW|NNW|CALM"))
            put(s, Field.WIND_DIRECTION, direction, label, selectedAt, response.retrievedAt);
        return s;
    }

    public static Snapshot buoy(HttpJsonClient.Response response, long time, String source)
            throws Exception {
        JSONArray rows = response.json.getJSONObject("result").getJSONArray("records");
        JSONObject selected = null;
        long selectedAt = Long.MIN_VALUE;
        for (int i = 0; i < rows.length(); i++) {
            try {
                JSONObject row = rows.getJSONObject(i);
                long at =
                        LocalDateTime.parse(row.getString("DateTime"))
                                .toInstant(ZoneOffset.ofHours(10))
                                .toEpochMilli();
                if (at <= time
                        && time - at <= 90 * 60000L
                        && at > selectedAt
                        && bounded(row, "Hs", 0, 40) != null) {
                    selected = row;
                    selectedAt = at;
                }
            } catch (Exception ignored) {
            }
        }
        if (selected == null)
            throw new IllegalArgumentException("No buoy observation in the previous 90 minutes");
        Snapshot s = new Snapshot();
        put(
                s,
                Field.WAVE_HEIGHT,
                bounded(selected, "Hs", 0, 40),
                source
                        + " · observed Hs; total waves, not swell"
                        + HttpJsonClient.cacheLabel(response),
                selectedAt,
                response.retrievedAt);
        return s;
    }

    public static Snapshot tides(JSONArray rows, long time, String source, long retrieved)
            throws Exception {
        TreeMap<Long, Double> readings = new TreeMap<>();
        DateTimeFormatter fmt = DateTimeFormatter.ofPattern("dd/MM/uuuu HH:mm");
        for (int i = 0; i < rows.length(); i++) {
            try {
                JSONObject r = rows.getJSONObject(i);
                long t =
                        LocalDateTime.parse(r.getString("Date") + " " + r.getString("Time"), fmt)
                                .toInstant(ZoneOffset.ofHours(10))
                                .toEpochMilli();
                double height = Double.parseDouble(r.getString("Reading").trim());
                if (Double.isFinite(height) && height >= -20 && height <= 30)
                    readings.put(t, height);
            } catch (Exception ignored) {
            }
        }
        Map.Entry<Long, Double> before = readings.floorEntry(time),
                after = readings.ceilingEntry(time);
        Map.Entry<Long, Double> closest =
                before == null
                        ? after
                        : after == null
                                ? before
                                : time - before.getKey() <= after.getKey() - time ? before : after;
        if (closest == null || Math.abs(closest.getKey() - time) > 5 * 60000L)
            throw new IllegalArgumentException("No tide prediction near this catch time");
        Snapshot result = new Snapshot();
        put(
                result,
                Field.TIDE_HEIGHT,
                closest.getValue(),
                source + " · metres above LAT",
                closest.getKey(),
                retrieved);
        if (readings.size() > 1) result.tideSeries = new TideSeries(readings, source, retrieved);
        Map.Entry<Long, Double> left = readings.lowerEntry(closest.getKey()),
                right = readings.higherEntry(closest.getKey());
        if (left != null && right != null && right.getKey() - left.getKey() <= 20 * 60000L) {
            double change = right.getValue() - left.getValue();
            put(
                    result,
                    Field.TIDE_TREND,
                    Math.abs(change) < 0.005 ? "Turning" : change > 0 ? "Rising" : "Falling",
                    source + " · water-level trend, not current",
                    closest.getKey(),
                    retrieved);
        } else result.notices.add("Tide trend unavailable at the edge of this day's predictions.");
        return result;
    }
}
