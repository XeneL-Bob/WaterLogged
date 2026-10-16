package au.com.waterlogged.services;

import android.content.Context;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/** Public sensor positions only. Your fishing mark never leaves this lookup. */
public final class ObservationCatalog {
    public static final String WAVE_RESOURCE = "2bbef99e-9974-49b9-a316-57402b00609c";

    public static final class Station {
        public final String id, name, url;
        public final double latitude, longitude;

        Station(JSONObject row) throws Exception {
            id = row.getString("id");
            name = row.getString("name");
            url = row.optString("url");
            latitude = row.getDouble("latitude");
            longitude = row.getDouble("longitude");
        }

        public double distance(double lat, double lon) {
            return GeoMath.distanceKm(lat, lon, latitude, longitude);
        }
    }

    private final List<Station> stations = new ArrayList<>();

    public ObservationCatalog(Context context, String asset) {
        try (InputStream in = context.getAssets().open(asset)) {
            java.io.ByteArrayOutputStream out = new java.io.ByteArrayOutputStream();
            byte[] buffer = new byte[4096];
            int n;
            while ((n = in.read(buffer)) != -1) out.write(buffer, 0, n);
            JSONArray rows =
                    new JSONObject(out.toString(StandardCharsets.UTF_8.name()))
                            .getJSONArray("stations");
            for (int i = 0; i < rows.length(); i++)
                stations.add(new Station(rows.getJSONObject(i)));
        } catch (Exception ignored) {
            // Missing reference data means a labelled model fallback, never a guessed station.
        }
    }

    public Station nearest(double lat, double lon) {
        return stations.stream()
                .filter(s -> s.distance(lat, lon) <= 50)
                .min(Comparator.comparingDouble(s -> s.distance(lat, lon)))
                .orElse(null);
    }
}
