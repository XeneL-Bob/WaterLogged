package au.com.waterlogged.services;

import android.content.Context;

import org.json.*;

import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.*;

/** Gauge coordinates verified against the published prediction headers; selection stays local. */
public final class TideCatalog {
    public static class Station {
        public final String id, name;
        public final double latitude, longitude;

        Station(JSONObject j) throws JSONException {
            id = j.getString("id");
            name = j.getString("name");
            latitude = j.getDouble("latitude");
            longitude = j.getDouble("longitude");
        }

        public double distance(double lat, double lon) {
            return GeoMath.distanceKm(lat, lon, latitude, longitude);
        }
    }

    private final List<Station> stations = new ArrayList<>();

    public TideCatalog(Context context) {
        try (InputStream in = context.getAssets().open("qld_tide_stations.json")) {
            java.io.ByteArrayOutputStream out = new java.io.ByteArrayOutputStream();
            byte[] b = new byte[4096];
            int n;
            while ((n = in.read(b)) != -1) out.write(b, 0, n);
            JSONArray array =
                    new JSONObject(out.toString(StandardCharsets.UTF_8.name()))
                            .getJSONArray("stations");
            for (int i = 0; i < array.length(); i++) {
                JSONObject j = array.getJSONObject(i);
                String licence = j.optString("licence");
                if ("cc-by".equalsIgnoreCase(licence) || "CC-BY-4.0".equalsIgnoreCase(licence))
                    stations.add(new Station(j));
            }
        } catch (Exception ignored) {
            /* Unsupported/missing catalogue leaves manual tides available. */
        }
    }

    public List<Station> nearby(double lat, double lon) {
        List<Station> result = new ArrayList<>();
        for (Station s : stations) if (s.distance(lat, lon) <= 50) result.add(s);
        result.sort(Comparator.comparingDouble(s -> s.distance(lat, lon)));
        return result;
    }

    public Station select(double lat, double lon, String id) {
        List<Station> nearby = nearby(lat, lon);
        if (id == null) return nearby.isEmpty() ? null : nearby.get(0);
        for (Station s : nearby) if (s.id.equals(id)) return s;
        return null;
    }

    public String name(String id) {
        for (Station s : stations) if (s.id.equals(id)) return s.name;
        return "Selected gauge";
    }
}
