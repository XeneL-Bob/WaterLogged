package au.com.waterlogged.model;

import static org.junit.Assert.*;

import org.junit.Test;

public class SessionDefaultsTest {
    @Test
    public void defaultsDoNotReuseAnOldCatchTimeOrConditions() {
        FishingSession session = new FishingSession();
        session.locationName = "Public test harbour";
        session.bait = "Prawn";
        session.method = "Boat";
        session.latitude = -26.68;
        session.longitude = 153.12;
        session.startedAt = 1000;
        CatchRecord first = new CatchRecord();
        first.caughtAt = 2000;
        session.applyTo(first);
        CatchRecord second = new CatchRecord();
        second.caughtAt = 3000;
        session.applyTo(second);
        assertEquals(2000, first.caughtAt);
        assertEquals(3000, second.caughtAt);
        assertNotEquals(first.id, second.id);
        assertEquals(session.id, second.sessionId);
        assertEquals("Prawn", second.bait);
        assertNull(second.windKmh);
        assertNull(second.tideSeriesJson);
    }
}
