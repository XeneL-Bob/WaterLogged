package au.com.waterlogged.model;

import androidx.room.Embedded;

public class SessionSummary {
    @Embedded public FishingSession session;
    public int entries;
    public long totalFish, kept, released;

    public String description(long now) {
        long minutes =
                Math.max(
                        0,
                        ((session.endedAt == null ? now : session.endedAt) - session.startedAt)
                                / 60000);
        return entries
                + " entries · "
                + totalFish
                + " fish · "
                + kept
                + " kept · "
                + released
                + " released\nDuration: "
                + minutes / 60
                + " h "
                + minutes % 60
                + " min";
    }
}
