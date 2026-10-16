package au.com.waterlogged.model;

import static org.junit.Assert.*;

import android.content.ContentValues;
import android.content.Context;
import android.database.sqlite.SQLiteDatabase;

import androidx.room.Room;
import androidx.test.core.app.ApplicationProvider;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.platform.app.InstrumentationRegistry;

import org.json.*;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;

@RunWith(AndroidJUnit4.class)
public class MigrationTest {
    @Test
    public void upgradePreservesExistingCatchAndAddsNullableMetadata() throws Exception {
        upgradeFrom(1);
    }

    @Test
    public void versionTwoSnapshotAndFavouriteSurviveUpgrade() throws Exception {
        upgradeFrom(2);
    }

    @Test
    public void versionThreeMeasurementsSurviveSessionsUpgrade() throws Exception {
        upgradeFrom(3);
    }

    private void upgradeFrom(int version) throws Exception {
        Context c = ApplicationProvider.getApplicationContext();
        String name = "migration-fixture.db";
        c.deleteDatabase(name);
        try {
            JSONObject schema;
            try (InputStream in =
                    InstrumentationRegistry.getInstrumentation()
                            .getContext()
                            .getAssets()
                            .open("schema-v" + version + ".json")) {
                ByteArrayOutputStream out = new ByteArrayOutputStream();
                byte[] b = new byte[4096];
                int n;
                while ((n = in.read(b)) != -1) out.write(b, 0, n);
                schema =
                        new JSONObject(out.toString(StandardCharsets.UTF_8.name()))
                                .getJSONObject("database");
            }
            SQLiteDatabase old = c.openOrCreateDatabase(name, 0, null);
            JSONArray entities = schema.getJSONArray("entities");
            for (int i = 0; i < entities.length(); i++) {
                JSONObject entity = entities.getJSONObject(i);
                String table = entity.getString("tableName");
                old.execSQL(entity.getString("createSql").replace("${TABLE_NAME}", table));
                JSONArray indices = entity.getJSONArray("indices");
                for (int j = 0; j < indices.length(); j++)
                    old.execSQL(
                            indices.getJSONObject(j)
                                    .getString("createSql")
                                    .replace("${TABLE_NAME}", table));
                if (table.equals("catches")) {
                    ContentValues row = new ContentValues();
                    JSONArray fields = entity.getJSONArray("fields");
                    for (int j = 0; j < fields.length(); j++) {
                        JSONObject f = fields.getJSONObject(j);
                        if (f.getBoolean("notNull")) {
                            String column = f.getString("columnName");
                            if (f.getString("affinity").equals("TEXT")) row.put(column, "");
                            else row.put(column, 1);
                        }
                    }
                    row.put("id", "existing-catch");
                    row.put("title", "Saved snapper");
                    row.put("locationName", "Test harbour");
                    row.put("notes", "Keep these notes");
                    row.put("latitude", -26.6825);
                    row.put("photoPath", "private/test-photo.jpg");
                    row.put("airTemperatureC", 23.0);
                    if (version >= 2) {
                        row.put(
                                "conditionsMetadata",
                                "{\"TEMPERATURE\":{\"manual\":true,\"value\":\"23\"}}");
                        row.put("tideStationId", "saved-gauge");
                    }
                    if (version >= 3) {
                        row.put("waveHeightM", 1.7);
                        row.put("windGustKmh", 32.0);
                    }
                    old.insertOrThrow(table, null, row);
                } else if (table.equals("species")) {
                    ContentValues row = new ContentValues();
                    row.put("id", "snapper");
                    row.put("commonName", "Snapper");
                    row.put("scientificName", "Chrysophrys auratus");
                    row.put("aliases", "Pagrus auratus");
                    row.put("favourite", 0);
                    old.insertOrThrow(table, null, row);
                }
            }
            old.setVersion(version);
            old.close();
            WaterDatabase upgraded =
                    Room.databaseBuilder(c, WaterDatabase.class, name)
                            .addMigrations(
                                    WaterDatabase.MIGRATION_1_2,
                                    WaterDatabase.MIGRATION_2_3,
                                    WaterDatabase.MIGRATION_3_4)
                            .build();
            try {
                CatchRecord r = upgraded.catches().find("existing-catch");
                assertNotNull(r);
                assertEquals("Saved snapper", r.title);
                assertEquals("Keep these notes", r.notes);
                assertEquals(-26.6825, r.latitude, 0);
                assertEquals("private/test-photo.jpg", r.photoPath);
                assertEquals(23.0, r.airTemperatureC, 0);
                if (version < 3) {
                    assertNull(r.waveHeightM);
                    assertNull(r.windGustKmh);
                } else {
                    assertEquals(1.7, r.waveHeightM, 0);
                    assertEquals(32.0, r.windGustKmh, 0);
                }
                assertNull(r.sessionId);
                assertNull(r.tideSeriesJson);
                assertTrue(upgraded.sessions().all().isEmpty());
                assertFalse(upgraded.species().find("snapper").favourite);
                if (version == 1) {
                    assertNull(r.conditionsMetadata);
                    assertNull(r.tideStationId);
                } else {
                    assertTrue(r.conditionsMetadata.contains("TEMPERATURE"));
                    assertEquals("saved-gauge", r.tideStationId);
                }
            } finally {
                upgraded.close();
            }
        } finally {
            c.deleteDatabase(name);
        }
    }
}
