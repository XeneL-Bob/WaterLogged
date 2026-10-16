package au.com.waterlogged.services;

import static org.junit.Assert.*;

import org.junit.Test;

import java.time.Instant;

public class GeoMoonTest {
    @Test
    public void externalCoordinatesAreRoundedWithoutChangingOriginal() {
        double exact = -26.682543;
        assertEquals(-26.68, GeoMath.rounded(exact), 0.000001);
        assertEquals(-26.682543, exact, 0);
        assertEquals(153.12, GeoMath.rounded(153.120123), 0.000001);
    }

    @Test
    public void distanceAndWindDirectionsAreConsistent() {
        assertEquals(0, GeoMath.distanceKm(-26, 153, -26, 153), 0);
        assertEquals(111.195, GeoMath.distanceKm(0, 0, 1, 0), 0.01);
        assertEquals("N", GeoMath.compass(360));
        assertEquals("N", GeoMath.compass(-1));
        assertEquals("SE", GeoMath.compass(134));
        assertEquals("W", GeoMath.compass(270));
    }

    @Test
    public void knownNewAndFullMoonDates() {
        assertEquals(
                "New moon", MoonPhase.name(Instant.parse("2024-04-08T18:21:00Z").toEpochMilli()));
        assertEquals(
                "Full moon", MoonPhase.name(Instant.parse("2024-03-25T07:00:00Z").toEpochMilli()));
    }

    @Test
    public void waxingAndWaningAreDistinguished() {
        assertEquals(
                "First quarter",
                MoonPhase.name(Instant.parse("2024-04-15T19:13:00Z").toEpochMilli()));
        assertEquals(
                "Last quarter",
                MoonPhase.name(Instant.parse("2024-04-02T03:15:00Z").toEpochMilli()));
    }
}
