package au.com.waterlogged.services;

import android.content.Context;
import android.location.Address;
import android.location.Geocoder;
import android.os.Handler;
import android.os.Looper;

import org.json.*;

import java.time.*;
import java.util.*;
import java.util.concurrent.*;

/** No account or key required for this non-commercial student pilot. */
public final class LiveConditionsProvider implements ConditionsProvider {
    private static final ExecutorService IO = Executors.newFixedThreadPool(4);
    private final Handler main = new Handler(Looper.getMainLooper());
    private final HttpJsonClient http;
    private final Context context;
    private final TideCatalog catalog;

    public LiveConditionsProvider(Context c) {
        context = c.getApplicationContext();
        http = new HttpJsonClient(context);
        catalog = new TideCatalog(context);
    }

    public static Reading moon(long time) {
        return new Reading(
                MoonPhase.name(time),
                "Calculated on device · SunCalc",
                time,
                System.currentTimeMillis());
    }

    @Override
    public void fetch(
            double latitude,
            double longitude,
            long caughtAt,
            boolean coastal,
            String tideStationId,
            Callback callback) {
        fetch(latitude, longitude, caughtAt, coastal, tideStationId, callback, false);
    }

    @Override
    public void fetchFresh(
            double latitude,
            double longitude,
            long caughtAt,
            boolean coastal,
            String tideStationId,
            Callback callback) {
        fetch(latitude, longitude, caughtAt, coastal, tideStationId, callback, true);
    }

    private void fetch(
            double latitude,
            double longitude,
            long caughtAt,
            boolean coastal,
            String tideStationId,
            Callback callback,
            boolean fresh) {
        double lat = GeoMath.rounded(latitude), lon = GeoMath.rounded(longitude);
        Batch batch = new Batch(callback, coastal ? 4 : 2);
        Snapshot moon = new Snapshot();
        moon.readings.put(Field.MOON, moon(caughtAt));
        batch.deliver(moon, false);
        batch.task("Location name", () -> location(lat, lon, caughtAt));
        batch.task("Weather", () -> observedWeather(latitude, longitude, caughtAt, fresh));
        if (coastal) {
            batch.task("Waves / swell", () -> marine(latitude, longitude, caughtAt, fresh));
            batch.task(
                    "Queensland tide",
                    () -> tides(latitude, longitude, caughtAt, tideStationId, fresh));
        }
    }

    private interface Fetch {
        Snapshot get() throws Exception;
    }

    private final class Batch {
        final Callback callback;
        final List<String> notices = new ArrayList<>();
        int pending;
        boolean ended;
        final Runnable timeout =
                () -> {
                    if (!ended) {
                        Snapshot s = new Snapshot();
                        s.notices.add(
                                "Some sources timed out. Try Refresh conditions or enter values"
                                        + " manually.");
                        deliver(s, true);
                    }
                };

        Batch(Callback cb, int count) {
            callback = cb;
            pending = count;
            main.postDelayed(timeout, 30000);
        }

        void task(String name, Fetch action) {
            IO.execute(
                    () -> {
                        Snapshot result;
                        try {
                            result = action.get();
                        } catch (Exception e) {
                            result = new Snapshot();
                            result.notices.add(
                                    name
                                            + " unavailable for this location/time. You can enter"
                                            + " it manually.");
                        }
                        Snapshot part = result;
                        main.post(
                                () -> {
                                    pending--;
                                    deliver(part, pending == 0);
                                });
                    });
        }

        void deliver(Snapshot s, boolean done) {
            if (ended) return;
            notices.addAll(s.notices);
            s.notices.clear();
            s.notices.addAll(notices);
            s.complete = done;
            if (done) {
                ended = true;
                main.removeCallbacks(timeout);
            }
            callback.update(s);
        }
    }

    private Snapshot weather(double lat, double lon, long time, boolean marine, boolean fresh)
            throws Exception {
        LocalDate date = Instant.ofEpochMilli(time).atOffset(ZoneOffset.UTC).toLocalDate();
        String url =
                (marine
                                ? "https://marine-api.open-meteo.com/v1/marine"
                                : "https://api.open-meteo.com/v1/forecast")
                        + String.format(Locale.US, "?latitude=%.2f&longitude=%.2f", lat, lon)
                        + "&hourly="
                        + (marine
                                ? "swell_wave_height,wave_height"
                                : "temperature_2m,wind_speed_10m,wind_direction_10m,wind_gusts_10m")
                        + "&timezone=GMT&timeformat=unixtime&start_date="
                        + date
                        + "&end_date="
                        + date.plusDays(1)
                        + (marine ? "" : "&wind_speed_unit=kmh&temperature_unit=celsius");
        return ConditionParsers.weather(http.get(url, 15 * 60000L, fresh), lat, lon, time, marine);
    }

    private Snapshot observedWeather(double lat, double lon, long time, boolean fresh)
            throws Exception {
        Snapshot observed = new Snapshot();
        ObservationCatalog.Station station =
                new ObservationCatalog(context, "weather_stations.json").nearest(lat, lon);
        if (station != null) {
            try {
                String source =
                        String.format(
                                Locale.US,
                                "BOM · %s station (%.1f km away)",
                                station.name,
                                station.distance(lat, lon));
                observed =
                        ConditionParsers.observations(
                                http.get(station.url, 5 * 60000L, fresh), time, source);
            } catch (Exception ignored) {
            }
        }
        if (observed.readings.size() < 4) {
            try {
                Snapshot model =
                        weather(GeoMath.rounded(lat), GeoMath.rounded(lon), time, false, fresh);
                for (Map.Entry<Field, Reading> e : model.readings.entrySet())
                    if (!observed.readings.containsKey(e.getKey()))
                        observed.readings.put(e.getKey(), e.getValue());
                observed.notices.add(
                        "Some weather uses a model estimate: no matching nearby station value for"
                                + " this catch time.");
                observed.notices.addAll(model.notices);
            } catch (Exception error) {
                if (observed.readings.isEmpty()) throw error;
                observed.notices.add("Some weather values unavailable; manual entry is available.");
            }
        }
        return observed;
    }

    private Snapshot marine(double lat, double lon, long time, boolean fresh) throws Exception {
        Snapshot measured = new Snapshot();
        ObservationCatalog.Station station =
                new ObservationCatalog(context, "wave_buoys.json").nearest(lat, lon);
        if (station != null) {
            try {
                String filter = new JSONObject().put("Site", station.name).toString();
                String url =
                        "https://www.data.qld.gov.au/api/3/action/datastore_search?resource_id="
                                + ObservationCatalog.WAVE_RESOURCE
                                + "&limit=500&sort=Seconds%20desc&filters="
                                + HttpJsonClient.encode(filter);
                String source =
                        String.format(
                                Locale.US,
                                "Queensland Coastal Data System · %s buoy (%.1f km away)",
                                station.name,
                                station.distance(lat, lon));
                measured = ConditionParsers.buoy(http.get(url, 10 * 60000L, fresh), time, source);
            } catch (Exception ignored) {
            }
        }
        try {
            Snapshot model = weather(GeoMath.rounded(lat), GeoMath.rounded(lon), time, true, fresh);
            // Buoys measure the whole sea state. Never relabel their Hs measurement as swell.
            model.readings.putAll(measured.readings);
            if (measured.readings.isEmpty())
                model.notices.add(
                        "Total waves use a model estimate: no matching nearby buoy observation"
                                + " (maximum age 90 minutes).");
            return model;
        } catch (Exception error) {
            if (measured.readings.isEmpty()) throw error;
            measured.notices.add("Swell model unavailable; total wave observation is still shown.");
            return measured;
        }
    }

    @SuppressWarnings("deprecation")
    private Snapshot location(double lat, double lon, long time) {
        String name = String.format(Locale.US, "Near %.2f, %.2f", lat, lon),
                source = "Approximate coordinate area";
        try {
            if (Geocoder.isPresent()) {
                List<Address> matches =
                        new Geocoder(context, Locale.getDefault()).getFromLocation(lat, lon, 1);
                if (matches != null && !matches.isEmpty()) {
                    Address a = matches.get(0);
                    String locality =
                            a.getLocality() != null ? a.getLocality() : a.getSubAdminArea();
                    String region = a.getAdminArea();
                    if (locality != null) {
                        name = locality + (region == null ? "" : ", " + region);
                        source = "Android location name · rounded GPS area";
                    }
                }
            }
        } catch (Exception ignored) {
        }
        Snapshot s = new Snapshot();
        s.readings.put(Field.LOCATION, new Reading(name, source, time, System.currentTimeMillis()));
        return s;
    }

    private Snapshot tides(double lat, double lon, long time, String selected, boolean fresh)
            throws Exception {
        TideCatalog.Station station = catalog.select(lat, lon, selected);
        if (station == null)
            throw new IllegalArgumentException("No supported Queensland gauge within 50 km");
        int year = Instant.ofEpochMilli(time).atOffset(ZoneOffset.ofHours(10)).getYear();
        JSONObject pkg =
                http.get(
                                "https://www.data.qld.gov.au/api/3/action/package_show?id="
                                        + HttpJsonClient.encode(station.id),
                                86400000L)
                        .json;
        if (!pkg.optBoolean("success"))
            throw new IllegalArgumentException("Tide catalogue unavailable");
        JSONArray resources = pkg.getJSONObject("result").getJSONArray("resources");
        String resource = null;
        for (int i = 0; i < resources.length(); i++) {
            JSONObject r = resources.getJSONObject(i);
            if (r.optString("name").matches(".*\\b" + year + "\\b.*")
                    && "CSV".equalsIgnoreCase(r.optString("format"))) {
                resource = r.getString("id");
                break;
            }
        }
        if (resource == null) throw new IllegalArgumentException("No tide resource for this year");
        String day =
                Instant.ofEpochMilli(time)
                        .atOffset(ZoneOffset.ofHours(10))
                        .format(java.time.format.DateTimeFormatter.ofPattern("dd/MM/uuuu"));
        String filter = new JSONObject().put("Date", day).toString();
        HttpJsonClient.Response response =
                http.get(
                        "https://www.data.qld.gov.au/api/3/action/datastore_search?resource_id="
                                + HttpJsonClient.encode(resource)
                                + "&limit=200&filters="
                                + HttpJsonClient.encode(filter),
                        30L * 86400000,
                        fresh);
        if (!response.json.optBoolean("success"))
            throw new IllegalArgumentException("Tide data unavailable");
        String source =
                String.format(
                        Locale.US,
                        "MSQ / BOM · %s gauge (%.1f km away) · prediction%s",
                        station.name,
                        station.distance(lat, lon),
                        HttpJsonClient.cacheLabel(response));
        Snapshot s =
                ConditionParsers.tides(
                        response.json.getJSONObject("result").getJSONArray("records"),
                        time,
                        source,
                        response.retrievedAt);
        s.notices.add(
                "Tide uses "
                        + station.name
                        + ". Check the gauge matches your waterway; nearest distance alone cannot"
                        + " confirm this.");
        return s;
    }
}
