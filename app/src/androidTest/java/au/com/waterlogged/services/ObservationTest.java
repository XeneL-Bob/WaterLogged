package au.com.waterlogged.services;

import static org.junit.Assert.*;

import androidx.test.core.app.ApplicationProvider;
import androidx.test.ext.junit.runners.AndroidJUnit4;

import au.com.waterlogged.services.ConditionsProvider.*;

import org.json.JSONObject;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.time.Instant;

@RunWith(AndroidJUnit4.class)
public class ObservationTest {
    @Test
    public void weatherUsesObservationAtOrBeforeCatchAndKeepsGustSeparate() throws Exception {
        JSONObject json =
                new JSONObject(
                        "{\"observations\":{\"data\":[{\"aifstime_utc\":\"20261015123000\",\"wind_spd_kmh\":26},{\"aifstime_utc\":\"20261015120000\",\"wind_spd_kmh\":28,\"gust_kmh\":39,\"air_temp\":20.1,\"wind_dir\":\"ESE\"}]}}");
        long time = Instant.parse("2026-10-15T12:20:00Z").toEpochMilli();
        Snapshot s =
                ConditionParsers.observations(
                        new HttpJsonClient.Response(json, 123, false), time, "Test station");
        assertEquals("28.0", s.readings.get(Field.WIND_SPEED).value);
        assertEquals("39.0", s.readings.get(Field.WIND_GUST).value);
        assertEquals(
                Instant.parse("2026-10-15T12:00:00Z").toEpochMilli(),
                s.readings.get(Field.WIND_SPEED).validAt);
        assertTrue(s.readings.get(Field.WIND_SPEED).source.contains("observed"));
        try {
            ConditionParsers.observations(
                    new HttpJsonClient.Response(json, 123, false),
                    time + 3 * 3600000,
                    "Test station");
            fail("Old observations must be rejected");
        } catch (IllegalArgumentException expected) {
        }
    }

    @Test
    public void invalidStationNumbersStayMissing() throws Exception {
        JSONObject json =
                new JSONObject(
                        "{\"observations\":{\"data\":[{\"aifstime_utc\":\"20261015120000\",\"wind_spd_kmh\":-9999,\"gust_kmh\":null,\"air_temp\":null,\"wind_dir\":\"-\"}]}}");
        Snapshot s =
                ConditionParsers.observations(
                        new HttpJsonClient.Response(json, 123, false),
                        Instant.parse("2026-10-15T12:01:00Z").toEpochMilli(),
                        "Test station");
        assertTrue(s.readings.isEmpty());
    }

    @Test
    public void buoyHsIsTotalWavesRatherThanSwellOrMaximumWave() throws Exception {
        JSONObject json =
                new JSONObject(
                        "{\"result\":{\"records\":[{\"DateTime\":\"2026-10-15T21:00:00\",\"Hs\":\"1.652\",\"Hmax\":\"2.750\"},{\"DateTime\":\"2026-10-15T23:00:00\",\"Hs\":\"9.0\"}]}}");
        long at = Instant.parse("2026-10-15T12:20:00Z").toEpochMilli();
        Snapshot s =
                ConditionParsers.buoy(
                        new HttpJsonClient.Response(json, 123, false), at, "Test buoy");
        assertEquals("1.652", s.readings.get(Field.WAVE_HEIGHT).value);
        assertFalse(s.readings.containsKey(Field.SWELL));
        assertFalse(s.readings.containsKey(Field.TIDE_HEIGHT));
        assertEquals(
                Instant.parse("2026-10-15T11:00:00Z").toEpochMilli(),
                s.readings.get(Field.WAVE_HEIGHT).validAt);
        try {
            ConditionParsers.buoy(
                    new HttpJsonClient.Response(json, 123, false), at + 4 * 3600000, "Test buoy");
            fail("Stale buoy data must be rejected");
        } catch (IllegalArgumentException expected) {
        }
    }

    @Test
    public void changingPointSelectsAnotherSupportedStation() {
        ObservationCatalog weather =
                new ObservationCatalog(
                        ApplicationProvider.getApplicationContext(), "weather_stations.json");
        ObservationCatalog.Station mooloolaba = weather.nearest(-26.6825, 153.1201),
                brisbane = weather.nearest(-27.39, 153.12);
        assertNotNull(mooloolaba);
        assertEquals("Sunshine Coast Airport", mooloolaba.name);
        assertNotNull(brisbane);
        assertNotEquals(mooloolaba.id, brisbane.id);
        assertNull(weather.nearest(-25, 135));
        ObservationCatalog wave =
                new ObservationCatalog(
                        ApplicationProvider.getApplicationContext(), "wave_buoys.json");
        assertEquals("Mooloolaba", wave.nearest(-26.6825, 153.1201).name);
    }
}
