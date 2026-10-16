package au.com.waterlogged.services;

import static org.junit.Assert.*;

import androidx.test.core.app.ApplicationProvider;
import androidx.test.ext.junit.runners.AndroidJUnit4;

import au.com.waterlogged.services.ConditionsProvider.*;

import org.json.*;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.time.Instant;

@RunWith(AndroidJUnit4.class)
public class ConditionsTest {
    @Test
    public void manualCorrectionAndManualClearSurviveRefreshAndSerialization() {
        ConditionMetadata m = new ConditionMetadata(null);
        assertTrue(m.canApply(Field.TEMPERATURE, ""));
        assertFalse(m.canApply(Field.LOCATION, "My secret spot"));
        m.remember(Field.TEMPERATURE, new Reading(24, "Test weather", 1000, 2000));
        assertTrue(m.canApply(Field.TEMPERATURE, "24.0"));
        m.edited(Field.TEMPERATURE, "21");
        m = new ConditionMetadata(m.json());
        assertFalse(m.canApply(Field.TEMPERATURE, "21"));
        assertFalse(m.automatic(Field.TEMPERATURE));
        assertTrue(m.label(Field.TEMPERATURE).startsWith("Manual correction"));
        m.edited(Field.TEMPERATURE, "");
        assertFalse(new ConditionMetadata(m.json()).canApply(Field.TEMPERATURE, ""));
    }

    @Test
    public void missingWeatherValuesStayUnknownAndWrongTimeIsRejected() throws Exception {
        JSONObject j =
                new JSONObject(
                        "{\"latitude\":-26.68,\"longitude\":153.12,\"hourly\":{\"time\":[1000],\"temperature_2m\":[null],\"wind_speed_10m\":[12],\"wind_direction_10m\":[134]}}");
        Snapshot s =
                ConditionParsers.weather(
                        new HttpJsonClient.Response(j, 12345, false),
                        -26.68,
                        153.12,
                        1000000,
                        false);
        assertFalse(s.readings.containsKey(Field.TEMPERATURE));
        assertEquals("SE", s.readings.get(Field.WIND_DIRECTION).value);
        assertEquals(12345, s.readings.get(Field.WIND_SPEED).retrievedAt);
        try {
            ConditionParsers.weather(
                    new HttpJsonClient.Response(j, 12345, false), -26.68, 153.12, 10000000, false);
            fail("Must reject unrelated time");
        } catch (IllegalArgumentException expected) {
        }
        try {
            ConditionParsers.weather(
                    new HttpJsonClient.Response(j, 12345, false), 0, 0, 1000000, false);
            fail("Must reject distant grid");
        } catch (IllegalArgumentException expected) {
        }
    }

    @Test
    public void queenslandTidesUseUtcPlusTenAndWaterLevelTrend() throws Exception {
        JSONArray rows =
                new JSONArray(
                        "[{\"Date\":\"14/10/2026\",\"Time\":\"09:50\",\"Reading\":\"1.1\"},{\"Date\":\"14/10/2026\",\"Time\":\"10:00\",\"Reading\":\"1.2\"},{\"Date\":\"14/10/2026\",\"Time\":\"10:10\",\"Reading\":\"1.3\"}]");
        long at = Instant.parse("2026-10-14T00:02:00Z").toEpochMilli();
        Snapshot s = ConditionParsers.tides(rows, at, "Test gauge", 1000);
        assertNotNull(s.tideSeries);
        assertEquals(3, TideSeries.fromJson(s.tideSeries.json()).times.length);
        assertEquals("1.2", s.readings.get(Field.TIDE_HEIGHT).value);
        assertEquals("Rising", s.readings.get(Field.TIDE_TREND).value);
        assertEquals(
                Instant.parse("2026-10-14T00:00:00Z").toEpochMilli(),
                s.readings.get(Field.TIDE_HEIGHT).validAt);
        try {
            ConditionParsers.tides(rows, at + 3600000, "Test gauge", 1000);
            fail("Must reject distant tide sample");
        } catch (IllegalArgumentException expected) {
        }
    }

    @Test
    public void nearestGaugeUsesVerifiedCoordinatesAndRejectsFarInland() {
        TideCatalog catalog = new TideCatalog(ApplicationProvider.getApplicationContext());
        TideCatalog.Station s = catalog.select(-26.68, 153.12, null);
        assertNotNull(s);
        assertTrue(s.name.contains("Mooloolaba"));
        assertTrue(s.distance(-26.68, 153.12) < 3);
        assertNull(catalog.select(-25, 135, null));
        assertNull(catalog.select(-26.68, 153.12, "unknown-station"));
    }

    @Test
    public void qualityLabelsDistinguishObservedCachedManualAndPrediction() {
        assertEquals(
                " · cached",
                HttpJsonClient.cacheLabel(
                        new HttpJsonClient.Response(new JSONObject(), 1000, false, true)));
        assertTrue(
                HttpJsonClient.cacheLabel(new HttpJsonClient.Response(new JSONObject(), 1000, true))
                        .contains("refresh unavailable"));
        ConditionMetadata metadata = new ConditionMetadata(null);
        metadata.remember(Field.WIND_SPEED, new Reading(20, "BOM station · observed", 1000, 2000));
        assertEquals("Observed", metadata.quality(Field.WIND_SPEED));
        metadata.remember(
                Field.WAVE_HEIGHT, new Reading(1.2, "Buoy · observed · cached", 1000, 2000));
        assertEquals("Cached · Observed", metadata.quality(Field.WAVE_HEIGHT));
        metadata.remember(Field.TIDE_HEIGHT, new Reading(1.1, "MSQ · prediction", 1000, 2000));
        assertEquals("Prediction", metadata.quality(Field.TIDE_HEIGHT));
        metadata.edited(Field.WIND_SPEED, "");
        assertEquals("Manual", metadata.quality(Field.WIND_SPEED));
        assertEquals("20 min ago", ConditionMetadata.age(1000, 1201000));
    }
}
