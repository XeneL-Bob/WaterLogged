package au.com.waterlogged.model;

import static org.junit.Assert.*;

import org.junit.Test;

public class CatchSummaryTest {
    @Test
    public void shareIncludesRequiredFieldsAndOmitsSensitiveFields() {
        CatchRecord r = new CatchRecord();
        r.title = "Snapper";
        r.locationName = "Sunshine Coast";
        r.lengthCm = 48.5;
        r.latitude = -26.6825;
        r.longitude = 153.1201;
        r.notes = "SECRET MARK";
        r.photoPath = "/private/secret.jpg";
        String summary = CatchSummary.text(r);
        assertTrue(summary.contains("Snapper"));
        assertTrue(summary.contains("Sunshine Coast"));
        assertTrue(summary.contains("48.5 cm"));
        assertTrue(summary.contains("Released"));
        assertFalse(summary.contains("26.6825"));
        assertFalse(summary.contains("153.1201"));
        assertFalse(summary.contains("SECRET MARK"));
        assertFalse(summary.contains("secret.jpg"));
    }

    @Test
    public void unknownLengthIsNotReportedAsZero() {
        assertTrue(CatchSummary.text(new CatchRecord()).contains("Not recorded"));
    }

    @Test
    public void snapshotsKeepIdWithoutMutatingPriorSave() {
        CatchRecord r = new CatchRecord();
        r.title = "Snapper";
        CatchRecord copy = r.copy();
        copy.title = "Bream";
        assertEquals(r.id, copy.id);
        assertEquals("Snapper", r.title);
    }
}
