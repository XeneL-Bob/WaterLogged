package au.com.waterlogged.model;

import androidx.annotation.NonNull;
import androidx.room.Entity;
import androidx.room.Index;
import androidx.room.PrimaryKey;

import java.util.UUID;

/** Trips are saved even when no fish are caught. A unique active slot prevents duplicate starts. */
@Entity(
        tableName = "sessions",
        indices = {@Index(value = "activeSlot", unique = true)})
public class FishingSession {
    @PrimaryKey @NonNull public String id = UUID.randomUUID().toString();
    public long startedAt = System.currentTimeMillis();
    public Long endedAt;
    public Integer activeSlot = 1;
    @NonNull public String locationName = "";
    @NonNull public String bait = "";
    @NonNull public String method = "";
    @NonNull public String environment = "Inshore";
    public Double latitude;
    public Double longitude;

    public void applyTo(CatchRecord catchRecord) {
        catchRecord.sessionId = id;
        catchRecord.locationName = locationName;
        catchRecord.bait = bait;
        catchRecord.method = method;
        catchRecord.environment = environment;
        catchRecord.latitude = latitude;
        catchRecord.longitude = longitude;
        // The session supplies defaults, never an old catch's environmental measurements.
        if (latitude != null) catchRecord.locationCapturedAt = startedAt;
    }
}
