package au.com.waterlogged.model;

import static org.junit.Assert.*;

import android.graphics.BitmapFactory;

import androidx.test.core.app.ApplicationProvider;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.platform.app.InstrumentationRegistry;

import org.junit.*;
import org.junit.runner.RunWith;

import java.io.File;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicReference;

/**
 * Opt-in verification/cleanup of the two labelled records created by the manual emulator journey.
 */
@RunWith(AndroidJUnit4.class)
public class EmulatorJourneyTest {
    @Test
    public void cameraAndOfflineSessionCatchesPersistAndOnlyOwnedFixturesAreRemoved()
            throws Exception {
        Assume.assumeTrue(
                "true".equals(InstrumentationRegistry.getArguments().getString("fieldJourney")));
        CountDownLatch latch = new CountDownLatch(1);
        AtomicReference<Exception> error = new AtomicReference<>();
        WaterRepository.get(ApplicationProvider.getApplicationContext())
                .execute(
                        (db, photos) -> {
                            try {
                                List<FishingSession> trips = new ArrayList<>();
                                for (FishingSession session : db.sessions().all())
                                    if (session.locationName.equals("WaterLogged_emulator_trip"))
                                        trips.add(session);
                                assertEquals(1, trips.size());
                                FishingSession trip = trips.get(0);
                                assertNotNull(trip.endedAt);
                                List<CatchRecord> owned = new ArrayList<>();
                                for (CatchRecord r : db.catches().all())
                                    if (trip.id.equals(r.sessionId)) owned.add(r);
                                assertEquals(2, owned.size());
                                CatchRecord camera =
                                        owned.stream()
                                                .filter(
                                                        r ->
                                                                r.title.equals(
                                                                        "WaterLogged_camera_test"))
                                                .findFirst()
                                                .get();
                                CatchRecord offline =
                                        owned.stream()
                                                .filter(
                                                        r ->
                                                                r.title.equals(
                                                                        "WaterLogged_offline_test"))
                                                .findFirst()
                                                .get();
                                assertNotEquals(camera.id, offline.id);
                                assertNotEquals(camera.caughtAt, offline.caughtAt);
                                for (CatchRecord r : owned) {
                                    assertEquals("Prawn", r.bait);
                                    assertEquals("Boat", r.method);
                                    assertEquals(-26.6825, r.latitude, 0);
                                    assertEquals(153.1201, r.longitude, 0);
                                    assertNotNull(r.conditionsMetadata);
                                }
                                assertNotNull(camera.photoPath);
                                BitmapFactory.Options bounds = new BitmapFactory.Options();
                                bounds.inJustDecodeBounds = true;
                                BitmapFactory.decodeFile(camera.photoPath, bounds);
                                assertTrue(bounds.outWidth > 0);
                                assertTrue(bounds.outHeight > 0);
                                List<Species> custom = new ArrayList<>();
                                for (Species s : db.species().all())
                                    if (s.commonName.equals("WaterLogged_custom_test"))
                                        custom.add(s);
                                assertEquals(1, custom.size());
                                assertTrue(custom.get(0).id.startsWith("custom:"));
                                assertTrue(custom.get(0).aliases.contains("emulator_alias"));
                                assertTrue(custom.get(0).favourite);
                                for (CatchRecord r : db.catches().all())
                                    if (custom.get(0).id.equals(r.speciesId)
                                            && r.title.equals("WaterLogged_custom_test"))
                                        owned.add(r);
                                Set<String> ids = new HashSet<>();
                                for (CatchRecord r : owned) ids.add(r.id);
                                Set<String> untouched = new HashSet<>();
                                for (CatchRecord r : db.catches().all())
                                    if (!ids.contains(r.id)) untouched.add(r.id);
                                db.runInTransaction(
                                        () -> {
                                            for (CatchRecord r : owned) db.catches().delete(r.id);
                                            db.getOpenHelper()
                                                    .getWritableDatabase()
                                                    .execSQL(
                                                            "DELETE FROM sessions WHERE id = ?",
                                                            new Object[] {trip.id});
                                            db.getOpenHelper()
                                                    .getWritableDatabase()
                                                    .execSQL(
                                                            "DELETE FROM species WHERE id = ?",
                                                            new Object[] {custom.get(0).id});
                                        });
                                for (CatchRecord r : owned)
                                    if (r.photoPath != null) {
                                        File f = new File(r.photoPath);
                                        if (photos.getCanonicalPath()
                                                .equals(f.getCanonicalFile().getParent()))
                                            assertTrue(f.delete());
                                    }
                                assertEquals(untouched.size(), db.catches().all().size());
                                for (String id : untouched) assertNotNull(db.catches().find(id));
                                return null;
                            } catch (AssertionError failure) {
                                throw new Exception(failure);
                            }
                        },
                        (result, failure) -> {
                            error.set(failure);
                            latch.countDown();
                        });
        assertTrue(latch.await(10, TimeUnit.SECONDS));
        assertNull(error.get());
    }
}
