package au.com.waterlogged.services;

import au.com.waterlogged.services.ConditionsProvider.Field;
import au.com.waterlogged.services.ConditionsProvider.Reading;

import org.json.JSONObject;

import java.math.BigDecimal;
import java.text.DateFormat;
import java.util.Date;

/** Stored with each catch, independently of the disposable network cache. */
public final class ConditionMetadata {
    private final JSONObject data;

    public ConditionMetadata(String json) {
        JSONObject parsed;
        try {
            parsed = new JSONObject(json == null ? "{}" : json);
        } catch (Exception e) {
            parsed = new JSONObject();
        }
        data = parsed;
    }

    public boolean automatic(Field f) {
        JSONObject o = data.optJSONObject(f.name());
        return o != null && !o.optBoolean("manual", true);
    }

    public boolean manual(Field f) {
        JSONObject o = data.optJSONObject(f.name());
        return o != null && o.optBoolean("manual", true);
    }

    public boolean canApply(Field f, String current) {
        JSONObject o = data.optJSONObject(f.name());
        if (o != null) return !o.optBoolean("manual", true) && same(current, o.optString("value"));
        return current.trim().isEmpty()
                || current.equals("Unknown")
                || current.equals("Not recorded");
    }

    public void remember(Field f, Reading r) {
        try {
            JSONObject o = new JSONObject();
            o.put("value", r.value);
            o.put("source", r.source);
            o.put("validAt", r.validAt);
            o.put("retrievedAt", r.retrievedAt);
            o.put("manual", false);
            data.put(f.name(), o);
        } catch (Exception e) {
            throw new IllegalStateException(e);
        }
    }

    public void edited(Field f, String value) {
        JSONObject o = data.optJSONObject(f.name());
        // Spinners can report programmatic selections after the controller's fill guard ends.
        if (o != null && same(value, o.optString("value"))) return;
        try {
            if (o == null) o = new JSONObject();
            o.put("manual", true);
            o.put("value", value);
            data.put(f.name(), o);
        } catch (Exception e) {
            throw new IllegalStateException(e);
        }
    }

    public void remove(Field f) {
        data.remove(f.name());
    }

    public String json() {
        return data.toString();
    }

    public String label(Field f) {
        JSONObject o = data.optJSONObject(f.name());
        if (o == null) return "Manual / not recorded";
        String source = o.optString("source");
        if (o.optBoolean("manual", true))
            return source.isEmpty() ? "Manual entry" : "Manual correction · originally " + source;
        DateFormat fmt = DateFormat.getDateTimeInstance(DateFormat.SHORT, DateFormat.SHORT);
        return quality(f)
                + " · "
                + age(o.optLong("validAt"), System.currentTimeMillis())
                + "\n"
                + source
                + "\nFor "
                + fmt.format(new Date(o.optLong("validAt")))
                + " · retrieved "
                + fmt.format(new Date(o.optLong("retrievedAt")));
    }

    public String quality(Field f) {
        JSONObject o = data.optJSONObject(f.name());
        if (o == null || o.optBoolean("manual", true)) return "Manual";
        String source = o.optString("source").toLowerCase(java.util.Locale.ROOT);
        String kind =
                source.contains("observed")
                        ? "Observed"
                        : source.contains("prediction")
                                ? "Prediction"
                                : source.contains("calculated")
                                        ? "Calculated"
                                        : f == Field.LOCATION ? "Area" : "Forecast";
        return source.contains("cached") ? "Cached · " + kind : kind;
    }

    public static String age(long valid, long now) {
        long minutes = Math.abs(now - valid) / 60000;
        String amount =
                minutes < 1
                        ? "less than a minute"
                        : minutes < 60
                                ? minutes + " min"
                                : minutes < 1440 ? minutes / 60 + " h" : minutes / 1440 + " days";
        return valid > now ? "for " + amount + " from now" : amount + " ago";
    }

    public String brief(Field f, long now) {
        JSONObject o = data.optJSONObject(f.name());
        if (o == null) return "";
        return quality(f) + "\n" + age(o.optLong("validAt"), now);
    }

    public static boolean same(String a, String b) {
        if (a.trim().equals(b.trim())) return true;
        try {
            return new BigDecimal(a.trim()).compareTo(new BigDecimal(b.trim())) == 0;
        } catch (Exception e) {
            return false;
        }
    }
}
