package au.com.waterlogged.model;

import androidx.annotation.NonNull;
import androidx.room.Entity;
import androidx.room.Index;
import androidx.room.PrimaryKey;

import java.io.Serializable;
import java.util.UUID;

/** One timestamped observation. Coordinates and photo paths stay in private app storage. */
@Entity(
        tableName = "catches",
        indices = {@Index("caughtAt"), @Index("lengthCm")})
public class CatchRecord implements Serializable, Cloneable {
    @PrimaryKey @NonNull public String id = UUID.randomUUID().toString();
    @NonNull public String speciesId = "";
    @NonNull public String title = "";
    @NonNull public String locationName = "";
    public long caughtAt = System.currentTimeMillis();
    @NonNull public String environment = "Inshore";
    public Double lengthCm;
    public Double weightKg;
    public int quantity = 1;
    @NonNull public String disposition = "Released";
    @NonNull public String bait = "";
    @NonNull public String method = "";
    @NonNull public String notes = "";
    public Double latitude;
    public Double longitude;
    public Double accuracyMetres;
    public Long locationCapturedAt;
    public String photoPath;
    public Double tideHeightM;
    @NonNull public String tideDirection = "Unknown";
    public Double windKmh;
    @NonNull public String windDirection = "";
    public Double airTemperatureC;
    public Double swellM;

    /** Total significant wave height is separate from the swell component. */
    public Double waveHeightM;

    public Double windGustKmh;
    @NonNull public String moonPhase = "Not recorded";
    @NonNull public String conditionsSource = "Manual / not recorded";

    /** Per-field source, valid time, retrieval time and manual-override status (JSON). */
    public String conditionsMetadata;

    /** Null selects the closest supported Queensland gauge. */
    public String tideStationId;

    public String sessionId;

    /** The source day's tide curve travels with the saved snapshot and encrypted backup. */
    public String tideSeriesJson;

    public CatchRecord copy() {
        try {
            return (CatchRecord) clone();
        } catch (CloneNotSupportedException e) {
            throw new AssertionError(e);
        }
    }
}
