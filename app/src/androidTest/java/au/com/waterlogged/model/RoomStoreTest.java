package au.com.waterlogged.model;

import static org.junit.Assert.*;

import android.os.Handler;
import android.os.Looper;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.Observer;
import androidx.room.Room;
import androidx.test.core.app.ApplicationProvider;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.platform.app.InstrumentationRegistry;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.util.Arrays;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

@RunWith(AndroidJUnit4.class)
public class RoomStoreTest {
    private WaterDatabase db;
    private ExecutorService queries;

    @Before
    public void open() {
        queries = Executors.newSingleThreadExecutor();
        db =
                Room.inMemoryDatabaseBuilder(
                                ApplicationProvider.getApplicationContext(), WaterDatabase.class)
                        .setQueryExecutor(queries)
                        .setTransactionExecutor(queries)
                        .build();
    }

    @After
    public void close() throws Exception {
        // Observer removal and outstanding observable reads must finish before closing the fixture.
        InstrumentationRegistry.getInstrumentation().waitForIdleSync();
        queries.submit(() -> db.close()).get(5, TimeUnit.SECONDS);
        queries.shutdown();
    }

    private CatchRecord catchAt(String name, long time, Double length) {
        CatchRecord r = new CatchRecord();
        r.title = name;
        r.locationName = "Test location";
        r.caughtAt = time;
        r.lengthCm = length;
        db.catches().save(r);
        return r;
    }

    @Test
    public void daoProducesBothSortOrdersAndDayFilter() throws Exception {
        CatchRecord old = catchAt("Old", 1000, 48.0),
                recent = catchAt("Recent", 2000, 32.0),
                unknown = catchAt("Unknown", 3000, null);
        List<CatchRecord> newest = await(db.catches().newest(0, 0));
        assertEquals(unknown.id, newest.get(0).id);
        List<CatchRecord> smallest = await(db.catches().smallest(0, 0));
        assertEquals(recent.id, smallest.get(0).id);
        assertEquals(old.id, smallest.get(1).id);
        assertEquals(unknown.id, smallest.get(2).id);
        List<CatchRecord> day = await(db.catches().newest(2000, 3000));
        assertEquals(1, day.size());
        assertEquals(recent.id, day.get(0).id);
    }

    @Test
    public void repeatedSaveUpdatesSameRowAndDeleteRemovesIt() throws Exception {
        CatchRecord r = catchAt("Snapper", 1000, 48.0);
        r.quantity = 2;
        db.catches().save(r);
        assertEquals(1, await(db.catches().newest(0, 0)).size());
        assertEquals(2, db.catches().find(r.id).quantity);
        db.catches().delete(r.id);
        assertNull(db.catches().find(r.id));
        assertTrue(await(db.catches().newest(0, 0)).isEmpty());
    }

    @Test
    public void searchUsesScientificNamesAliasesAndFavourites() throws Exception {
        db.species()
                .seed(
                        Arrays.asList(
                                new Species(
                                        "s",
                                        "Snapper",
                                        "Chrysophrys auratus",
                                        "Pagrus auratus",
                                        false),
                                new Species(
                                        "b", "Bream", "Acanthopagrus australis", "bream", true)));
        assertEquals("s", await(db.species().search("PAGRUS AUR")).get(0).id);
        assertEquals("b", await(db.species().search("")).get(0).id);
        db.species().toggleFavourite("b");
        assertFalse(await(db.species().search("BREAM")).get(0).favourite);
    }

    @Test
    public void expandedCataloguePreservesExistingFavouriteChoice() throws Exception {
        db.species()
                .seed(
                        Arrays.asList(
                                new Species(
                                        "snapper",
                                        "Snapper",
                                        "Chrysophrys auratus",
                                        "Pagrus auratus",
                                        false)));
        db.species()
                .seedCatalogue(
                        SpeciesCatalogue.get(ApplicationProvider.getApplicationContext())
                                .species());
        assertEquals(32, db.species().count());
        assertFalse(db.species().find("snapper").favourite);
        assertNotNull(db.species().find("mangrove_jack"));
        assertNotNull(db.species().find("australian_bass"));
        assertEquals(
                "Percalates novemaculeata", db.species().find("australian_bass").scientificName);
    }

    private <T> T await(LiveData<T> source) throws Exception {
        CountDownLatch latch = new CountDownLatch(1);
        AtomicReference<T> result = new AtomicReference<>();
        Handler main = new Handler(Looper.getMainLooper());
        Observer<T> observer =
                new Observer<T>() {
                    @Override
                    public void onChanged(T value) {
                        result.set(value);
                        source.removeObserver(this);
                        latch.countDown();
                    }
                };
        main.post(() -> source.observeForever(observer));
        try {
            assertTrue("Room observable timed out", latch.await(5, TimeUnit.SECONDS));
            return result.get();
        } finally {
            InstrumentationRegistry.getInstrumentation()
                    .runOnMainSync(() -> source.removeObserver(observer));
        }
    }
}
