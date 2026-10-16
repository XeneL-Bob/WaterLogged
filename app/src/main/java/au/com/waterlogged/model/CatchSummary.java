package au.com.waterlogged.model;

import java.text.DateFormat;
import java.util.Date;

/** Deliberately excludes coordinates, photos and free-text notes from the share payload. */
public final class CatchSummary {
    private CatchSummary() {}

    public static String text(CatchRecord r) {
        return "Water Logged\n"
                + r.title
                + "\nLocation: "
                + r.locationName
                + "\nCaught: "
                + DateFormat.getDateTimeInstance().format(new Date(r.caughtAt))
                + "\nLength: "
                + (r.lengthCm == null ? "Not recorded" : r.lengthCm + " cm")
                + "\nQuantity: "
                + r.quantity
                + "\nStatus: "
                + r.disposition;
    }
}
