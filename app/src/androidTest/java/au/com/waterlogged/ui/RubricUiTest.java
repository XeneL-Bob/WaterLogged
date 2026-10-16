package au.com.waterlogged.ui;

import static org.junit.Assert.*;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.view.accessibility.AccessibilityNodeInfo;

import androidx.test.core.app.ActivityScenario;
import androidx.test.core.app.ApplicationProvider;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.platform.app.InstrumentationRegistry;

import au.com.waterlogged.MainActivity;
import au.com.waterlogged.R;
import au.com.waterlogged.model.*;
import au.com.waterlogged.services.*;

import org.junit.Test;
import org.junit.runner.RunWith;

import java.util.List;
import java.util.UUID;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicReference;

/** Exercises marked UI flows with owned fixtures; never sends a message or deletes user entries. */
@RunWith(AndroidJUnit4.class)
public class RubricUiTest {
    public static class Item extends CatchItemFragment {
        static volatile Intent launched;

        @Override
        public void startActivity(Intent intent) {
            launched = intent;
        }

        @Override
        protected ConditionsProvider createConditionsProvider() {
            return (lat, lon, time, coastal, gauge, callback) -> {
                ConditionsProvider.Snapshot snapshot = new ConditionsProvider.Snapshot();
                snapshot.complete = true;
                callback.update(snapshot);
            };
        }

        @Override
        protected void loadSessionDefaults(WaterRepository.Result<FishingSession> callback) {
            callback.complete(null, null);
        }
    }

    private <T> T work(WaterRepository.Task<T> task) throws Exception {
        CountDownLatch done = new CountDownLatch(1);
        AtomicReference<T> value = new AtomicReference<>();
        AtomicReference<Exception> error = new AtomicReference<>();
        WaterRepository.get(ApplicationProvider.getApplicationContext())
                .execute(
                        task,
                        (v, e) -> {
                            value.set(v);
                            error.set(e);
                            done.countDown();
                        });
        assertTrue(done.await(8, TimeUnit.SECONDS));
        if (error.get() != null) throw error.get();
        return value.get();
    }

    private Item open(ActivityScenario<MainActivity> scenario, Bundle arguments) throws Exception {
        Item item = new Item();
        item.setArguments(arguments);
        scenario.onActivity(
                a ->
                        a.getSupportFragmentManager()
                                .beginTransaction()
                                .replace(R.id.content, item)
                                .commitNow());
        for (int i = 0; i < 200; i++) {
            AtomicReference<Boolean> ready = new AtomicReference<>(false);
            scenario.onActivity(
                    a ->
                            ready.set(
                                    item.getView() != null
                                            && ((CatchFormView) item.getView()).save.isEnabled()));
            if (ready.get()) return item;
            Thread.sleep(25);
        }
        throw new AssertionError("Catch form did not become ready");
    }

    private void clickDialog(String text) throws Exception {
        for (int i = 0; i < 100; i++) {
            AccessibilityNodeInfo root =
                    InstrumentationRegistry.getInstrumentation()
                            .getUiAutomation()
                            .getRootInActiveWindow();
            if (root != null) {
                for (AccessibilityNodeInfo n : root.findAccessibilityNodeInfosByText(text)) {
                    if (text.equalsIgnoreCase(String.valueOf(n.getText()))
                            && n.isClickable()
                            && n.performAction(AccessibilityNodeInfo.ACTION_CLICK)) return;
                }
            }
            Thread.sleep(30);
        }
        throw new AssertionError("Dialog action unavailable: " + text);
    }

    @Test
    public void newViewAutoSaveValidationAndConfirmedDelete() throws Exception {
        String title = "Rubric fixture " + UUID.randomUUID();
        List<String> before =
                work(
                        (db, p) -> {
                            java.util.ArrayList<String> ids = new java.util.ArrayList<>();
                            for (CatchRecord r : db.catches().all()) ids.add(r.id);
                            return ids;
                        });
        String[] owned = {null};
        try (ActivityScenario<MainActivity> scenario =
                ActivityScenario.launch(MainActivity.class)) {
            Bundle args = new Bundle();
            args.putString("name", title);
            Item item = open(scenario, args);
            scenario.onActivity(
                    a -> {
                        CatchFormView f = (CatchFormView) item.getView();
                        assertEquals(View.GONE, f.delete.getVisibility());
                        // A fresh catch may already receive the emulator's automatic GPS fix.
                        // Check the empty-mark state independently of that asynchronous device
                        // result.
                        CatchFormView withoutMark = new CatchFormView(a, false);
                        withoutMark.bind(new CatchRecord());
                        assertFalse(withoutMark.map.isEnabled());
                        f.location.setText("Public test harbour");
                        f.length.setText("38");
                        f.quantity.setText("2");
                        f.notes.setText("First line\nSecond line");
                    });
            // Check the debounce actually writes while the item remains open.
            for (int i = 0; i < 120; i++) {
                CatchRecord saved =
                        work(
                                (db, p) -> {
                                    for (CatchRecord r : db.catches().all())
                                        if (title.equals(r.title)) return r;
                                    return null;
                                });
                if (saved != null) {
                    owned[0] = saved.id;
                    break;
                }
                Thread.sleep(25);
            }
            assertNotNull("Auto-save must create a row", owned[0]);
            scenario.onActivity(
                    a -> {
                        CatchFormView f = (CatchFormView) item.getView();
                        f.quantity.setInputType(android.text.InputType.TYPE_CLASS_TEXT);
                        for (String bad :
                                new String[] {"", "abc", "-1", "1.2", "999999999999999"}) {
                            f.quantity.setText(bad);
                            try {
                                f.read(new CatchRecord());
                                fail("Invalid quantity accepted");
                            } catch (IllegalArgumentException expected) {
                                assertNotNull(f.quantity.getError());
                            }
                        }
                        f.quantity.setText("2");
                        f.save.performClick();
                    });
            for (int i = 0; i < 100; i++) {
                AtomicReference<Boolean> list = new AtomicReference<>(false);
                scenario.onActivity(
                        a ->
                                list.set(
                                        a.getSupportFragmentManager().findFragmentById(R.id.content)
                                                instanceof CatchListFragment));
                if (list.get()) break;
                Thread.sleep(30);
            }
            assertEquals(
                    1,
                    (int)
                            work(
                                    (db, p) -> {
                                        int n = 0;
                                        for (CatchRecord r : db.catches().all())
                                            if (title.equals(r.title)) n++;
                                        return n;
                                    }));
            args = new Bundle();
            args.putString("id", owned[0]);
            Item reopened = open(scenario, args);
            scenario.onActivity(
                    a -> {
                        CatchFormView f = (CatchFormView) reopened.getView();
                        assertEquals(title, f.title.getText().toString());
                        assertEquals("38.0", f.length.getText().toString());
                        assertEquals("2", f.quantity.getText().toString());
                        assertEquals("First line\nSecond line", f.notes.getText().toString());
                        assertEquals(View.VISIBLE, f.delete.getVisibility());
                        f.date.performClick();
                    });
            clickDialog("Cancel");
            scenario.onActivity(a -> ((CatchFormView) reopened.getView()).delete.performClick());
            clickDialog("Cancel");
            assertNotNull(work((db, p) -> db.catches().find(owned[0])));
            scenario.onActivity(a -> ((CatchFormView) reopened.getView()).delete.performClick());
            clickDialog("Delete");
            for (int i = 0; i < 100 && work((db, p) -> db.catches().find(owned[0])) != null; i++)
                Thread.sleep(30);
            assertNull(work((db, p) -> db.catches().find(owned[0])));
            Thread.sleep(1100);
            assertNull(
                    "A pending save must not recreate a deleted row",
                    work((db, p) -> db.catches().find(owned[0])));
            scenario.onActivity(
                    a ->
                            assertTrue(
                                    a.getSupportFragmentManager().findFragmentById(R.id.content)
                                            instanceof CatchListFragment));
        } finally {
            work(
                    (db, p) -> {
                        for (CatchRecord r : db.catches().all())
                            if (title.equals(r.title)) db.catches().delete(r.id);
                        return null;
                    });
        }
        List<String> after =
                work(
                        (db, p) -> {
                            java.util.ArrayList<String> ids = new java.util.ArrayList<>();
                            for (CatchRecord r : db.catches().all()) ids.add(r.id);
                            return ids;
                        });
        assertEquals(before, after);
    }

    @Test
    public void mapAndShareIntentsUseSavedMarkAndPrivateSummary() throws Exception {
        CatchRecord record = new CatchRecord();
        record.title = "Rubric intent fixture";
        record.locationName = "Mooloolaba, QLD";
        record.latitude = -26.6825;
        record.longitude = 153.1201;
        record.lengthCm = 45.0;
        record.notes = "Private fishing note";
        work(
                (db, p) -> {
                    db.catches().save(record);
                    return null;
                });
        try (ActivityScenario<MainActivity> scenario =
                ActivityScenario.launch(MainActivity.class)) {
            Bundle args = new Bundle();
            args.putString("id", record.id);
            Item item = open(scenario, args);
            scenario.onActivity(a -> ((CatchFormView) item.getView()).map.performClick());
            assertEquals(Intent.ACTION_VIEW, Item.launched.getAction());
            assertEquals("com.google.android.apps.maps", Item.launched.getPackage());
            assertTrue(Item.launched.getDataString().contains("-26.682500,153.120100"));
            scenario.onActivity(a -> ((CatchFormView) item.getView()).share.performClick());
            clickDialog("Share");
            for (int i = 0;
                    i < 100 && !Intent.ACTION_CHOOSER.equals(Item.launched.getAction());
                    i++) Thread.sleep(30);
            assertEquals(Intent.ACTION_CHOOSER, Item.launched.getAction());
            Intent send = Item.launched.getParcelableExtra(Intent.EXTRA_INTENT);
            assertEquals("text/plain", send.getType());
            String text = send.getStringExtra(Intent.EXTRA_TEXT);
            assertTrue(text.contains("Mooloolaba"));
            assertTrue(text.contains("45"));
            assertFalse(text.contains("-26.6825"));
            assertFalse(text.contains("153.1201"));
            assertFalse(text.contains("Private fishing note"));
        } finally {
            work(
                    (db, p) -> {
                        db.catches().delete(record.id);
                        return null;
                    });
        }
    }
}
