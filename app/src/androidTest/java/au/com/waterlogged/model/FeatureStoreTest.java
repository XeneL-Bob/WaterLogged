package au.com.waterlogged.model;

import static org.junit.Assert.*;

import android.content.Context;
import android.net.Uri;

import androidx.lifecycle.*;
import androidx.room.Room;
import androidx.test.core.app.ApplicationProvider;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.platform.app.InstrumentationRegistry;

import au.com.waterlogged.services.*;

import org.junit.*;
import org.junit.runner.RunWith;

import java.io.*;
import java.nio.file.Files;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicReference;
import java.util.zip.*;

@RunWith(AndroidJUnit4.class)
public class FeatureStoreTest {
    private Context context;
    private WaterDatabase db;
    private File directory, photos;

    @Before
    public void setup() {
        context = ApplicationProvider.getApplicationContext();
        db = database();
        directory = new File(context.getCacheDir(), "feature-test-" + UUID.randomUUID());
        directory.mkdir();
        photos = new File(directory, "photos");
        photos.mkdir();
    }

    private WaterDatabase database() {
        return Room.inMemoryDatabaseBuilder(
                        ApplicationProvider.getApplicationContext(), WaterDatabase.class)
                .setQueryExecutor(Runnable::run)
                .setTransactionExecutor(Runnable::run)
                .allowMainThreadQueries()
                .build();
    }

    @After
    public void close() {
        InstrumentationRegistry.getInstrumentation().waitForIdleSync();
        db.close();
        remove(directory);
    }

    private void remove(File f) {
        File[] children = f.listFiles();
        if (children != null) for (File child : children) remove(child);
        f.delete();
    }

    private <T> T value(LiveData<T> data) throws Exception {
        CountDownLatch latch = new CountDownLatch(1);
        AtomicReference<T> result = new AtomicReference<>();
        androidx.lifecycle.Observer<T> observer =
                x -> {
                    result.set(x);
                    latch.countDown();
                };
        InstrumentationRegistry.getInstrumentation()
                .runOnMainSync(() -> data.observeForever(observer));
        assertTrue(latch.await(5, TimeUnit.SECONDS));
        InstrumentationRegistry.getInstrumentation()
                .runOnMainSync(() -> data.removeObserver(observer));
        return result.get();
    }

    private CatchRecord catchRecord(String id, long time, String status) {
        CatchRecord r = new CatchRecord();
        r.id = id;
        r.title = "Snapper";
        r.speciesId = "snapper";
        r.locationName = "Test harbour";
        r.caughtAt = time;
        r.disposition = status;
        return r;
    }

    @Test
    public void searchCombinesAliasesDatesStatusMethodAndSqlSort() throws Exception {
        db.species()
                .insert(
                        new Species(
                                "snapper",
                                "Snapper",
                                "Chrysophrys auratus",
                                "Pagrus pinkie",
                                false));
        CatchRecord one = catchRecord("one", 1000, "Kept");
        one.lengthCm = 40.;
        one.method = "Boat jigging";
        one.notes = "Reef edge";
        db.catches().save(one);
        CatchRecord two = catchRecord("two", 2000, "Released");
        two.lengthCm = 30.;
        two.method = "Shore bait";
        db.catches().save(two);
        CatchRecord three = catchRecord("three", 3000, "Kept");
        three.method = "Boat";
        db.catches().save(three);
        assertEquals(
                1,
                value(db.catches().searchNewest(1000, 3000, "PAGRUS", "Kept", "jig", "")).size());
        assertEquals("one", value(db.catches().searchNewest(0, 0, "REEF", "", "", "")).get(0).id);
        assertEquals(3, value(db.catches().searchNewest(0, 0, "harbour", "", "", "")).size());
        assertEquals(2, value(db.catches().searchNewest(2000, 0, "", "", "", "")).size());
        assertEquals(1, value(db.catches().searchNewest(0, 2000, "", "", "", "")).size());
        List<CatchRecord> sorted = value(db.catches().searchSmallest(0, 0, "", "", "", ""));
        assertEquals("two", sorted.get(0).id);
        assertEquals("three", sorted.get(2).id);
        assertTrue(value(db.catches().searchNewest(0, 0, "missing", "", "", "")).isEmpty());
        // Renaming the catch must not hide it when searching its catalogue's common name.
        one.title = "Reef fish";
        two.title = "Shore fish";
        three.title = "Boat fish";
        db.catches().save(one);
        db.catches().save(two);
        db.catches().save(three);
        assertEquals(3, value(db.catches().searchNewest(0, 0, "SNAPP", "", "", "")).size());
        assertEquals(
                "two", value(db.catches().searchSmallest(0, 0, "snapp", "", "", "")).get(0).id);
    }

    @Test
    public void zeroCatchSessionSurvivesAndCountsTrackDiary() throws Exception {
        FishingSession session = new FishingSession();
        session.locationName = "Test session";
        db.sessions().insert(session);
        assertEquals(0, value(db.sessions().summaries()).get(0).totalFish);
        try {
            db.sessions().insert(new FishingSession());
            fail("Two active sessions accepted");
        } catch (android.database.sqlite.SQLiteConstraintException expected) {
        }
        CatchRecord r = catchRecord("session-catch", 1000, "Kept");
        r.sessionId = session.id;
        r.quantity = 3;
        db.catches().save(r);
        assertEquals(3, value(db.sessions().summaries()).get(0).kept);
        assertEquals(1, value(db.catches().searchNewest(0, 0, "", "", "", session.id)).size());
        db.catches().delete(r.id);
        assertEquals(0, value(db.sessions().summaries()).get(0).entries);
        db.sessions().end(session.id, session.startedAt + 60000);
        assertNull(db.sessions().active());
        assertEquals(1, db.sessions().all().size());
    }

    @Test
    public void encryptedRestorePreservesPhotosSnapshotsPreferencesAndExistingCatches()
            throws Exception {
        db.species()
                .insert(new Species("snapper", "Snapper", "Chrysophrys auratus", "Pagrus", false));
        db.species()
                .insert(new Species("custom:fixture", "Local fish", "", "regional nickname", true));
        FishingSession session = new FishingSession();
        session.locationName = "Test trip";
        db.sessions().insert(session);
        CatchRecord original = catchRecord("restore-new", 2000, "Released");
        session.applyTo(original);
        original.airTemperatureC = 24.;
        original.conditionsMetadata = "{\"TEMPERATURE\":{\"manual\":true,\"value\":\"24\"}}";
        TreeMap<Long, Double> points = new TreeMap<>();
        points.put(0L, .7);
        points.put(600000L, 1.2);
        original.tideSeriesJson = new TideSeries(points, "Fixture tide", 3000).json();
        File photo = new File(photos, "catch.jpg");
        byte[] photoBytes = {1, 2, 3, 4, 5};
        Files.write(photo.toPath(), photoBytes);
        original.photoPath = photo.getAbsolutePath();
        db.catches().save(original);
        CatchRecord conflict = catchRecord("conflict", 1000, "Kept");
        conflict.notes = "Older backup";
        db.catches().save(conflict);
        File backup = new File(directory, "diary.wlog");
        BackupService.exportBackup(
                context, db, photos, Uri.fromFile(backup), "test-backup-password".toCharArray());
        WaterDatabase target = database();
        try {
            target.species()
                    .insert(
                            new Species(
                                    "snapper", "Snapper", "Chrysophrys auratus", "Pagrus", true));
            conflict.notes = "Newer local edit";
            target.catches().save(conflict);
            File restoredPhotos = new File(directory, "restored");
            BackupService.restore(
                    context,
                    target,
                    restoredPhotos,
                    Uri.fromFile(backup),
                    "test-backup-password".toCharArray());
            CatchRecord result = target.catches().find(original.id);
            assertNotNull(result);
            assertEquals(original.conditionsMetadata, result.conditionsMetadata);
            assertEquals(original.tideSeriesJson, result.tideSeriesJson);
            assertEquals(original.latitude, result.latitude);
            assertEquals(original.caughtAt, result.caughtAt);
            assertArrayEquals(photoBytes, Files.readAllBytes(new File(result.photoPath).toPath()));
            assertTrue(
                    new File(result.photoPath)
                            .getCanonicalPath()
                            .startsWith(restoredPhotos.getCanonicalPath()));
            assertEquals("Newer local edit", target.catches().find("conflict").notes);
            assertFalse(target.species().find("snapper").favourite);
            assertNotNull(target.species().find("custom:fixture"));
            assertNotNull(target.sessions().find(session.id).endedAt);
            assertNull(target.sessions().active());
            BackupService.restore(
                    context,
                    target,
                    restoredPhotos,
                    Uri.fromFile(backup),
                    "test-backup-password".toCharArray());
            assertEquals(2, target.catches().all().size());
            assertEquals(1, restoredPhotos.listFiles().length);
            File csv = new File(directory, "catches.csv");
            BackupService.csv(context, target, Uri.fromFile(csv), false);
            String csvText =
                    new String(
                            Files.readAllBytes(csv.toPath()),
                            java.nio.charset.StandardCharsets.UTF_8);
            assertFalse(csvText.split("\r\n")[0].contains("latitude"));
            assertFalse(csvText.contains("photoPath"));
        } finally {
            target.close();
        }
    }

    @Test
    public void wrongPasswordAndTraversalCannotModifyDiary() throws Exception {
        CatchRecord existing = catchRecord("keep", 1000, "Kept");
        db.catches().save(existing);
        File backup = new File(directory, "safe.wlog");
        BackupService.exportBackup(
                context, db, photos, Uri.fromFile(backup), "correct-test-password".toCharArray());
        try {
            BackupService.restore(
                    context, db, photos, Uri.fromFile(backup), "wrong-password".toCharArray());
            fail("Wrong password accepted");
        } catch (Exception expected) {
        }
        ByteArrayOutputStream raw = new ByteArrayOutputStream();
        try (ZipOutputStream zip = new ZipOutputStream(raw)) {
            zip.putNextEntry(new ZipEntry("../outside.txt"));
            zip.write(1);
            zip.closeEntry();
        }
        File bad = new File(directory, "bad.wlog");
        try (OutputStream out = new FileOutputStream(bad)) {
            BackupCrypto.encrypt(
                    new ByteArrayInputStream(raw.toByteArray()),
                    out,
                    "correct-test-password".toCharArray());
        }
        try {
            BackupService.restore(
                    context, db, photos, Uri.fromFile(bad), "correct-test-password".toCharArray());
            fail("Traversal accepted");
        } catch (IOException expected) {
        }
        assertEquals(1, db.catches().all().size());
        assertEquals("keep", db.catches().all().get(0).id);
        assertFalse(new File(context.getCacheDir(), "outside.txt").exists());
    }

    @Test
    public void csvProtectsSpreadsheetFormulasAndQuotes() {
        assertEquals("\"'=2+2\"", BackupService.csvCell("=2+2"));
        assertEquals("\"'  @SUM(A1)\"", BackupService.csvCell("  @SUM(A1)"));
        assertEquals("\"hello,\"\"fish\"\"\"", BackupService.csvCell("hello,\"fish\""));
        assertEquals("\"-26.68\"", BackupService.csvCell(-26.68));
    }
}
