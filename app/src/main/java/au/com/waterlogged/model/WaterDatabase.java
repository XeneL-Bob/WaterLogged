package au.com.waterlogged.model;

import androidx.annotation.NonNull;
import androidx.room.Database;
import androidx.room.RoomDatabase;
import androidx.room.migration.Migration;
import androidx.sqlite.db.SupportSQLiteDatabase;

@Database(
        entities = {CatchRecord.class, Species.class, FishingSession.class},
        version = 4,
        exportSchema = true)
public abstract class WaterDatabase extends RoomDatabase {
    public static final Migration MIGRATION_1_2 =
            new Migration(1, 2) {
                @Override
                public void migrate(@NonNull SupportSQLiteDatabase db) {
                    db.execSQL("ALTER TABLE catches ADD COLUMN conditionsMetadata TEXT");
                    db.execSQL("ALTER TABLE catches ADD COLUMN tideStationId TEXT");
                }
            };
    public static final Migration MIGRATION_2_3 =
            new Migration(2, 3) {
                @Override
                public void migrate(@NonNull SupportSQLiteDatabase db) {
                    db.execSQL("ALTER TABLE catches ADD COLUMN waveHeightM REAL");
                    db.execSQL("ALTER TABLE catches ADD COLUMN windGustKmh REAL");
                }
            };

    public static final Migration MIGRATION_3_4 =
            new Migration(3, 4) {
                @Override
                public void migrate(@NonNull SupportSQLiteDatabase db) {
                    db.execSQL("ALTER TABLE catches ADD COLUMN sessionId TEXT");
                    db.execSQL("ALTER TABLE catches ADD COLUMN tideSeriesJson TEXT");
                    db.execSQL(
                            "CREATE TABLE IF NOT EXISTS sessions (id TEXT NOT NULL PRIMARY KEY,"
                                + " startedAt INTEGER NOT NULL, endedAt INTEGER, activeSlot"
                                + " INTEGER, locationName TEXT NOT NULL, bait TEXT NOT NULL, method"
                                + " TEXT NOT NULL, environment TEXT NOT NULL, latitude REAL,"
                                + " longitude REAL)");
                    db.execSQL(
                            "CREATE UNIQUE INDEX IF NOT EXISTS index_sessions_activeSlot ON"
                                    + " sessions(activeSlot)");
                }
            };

    public abstract SessionDao sessions();

    public abstract CatchDao catches();

    public abstract SpeciesDao species();
}
