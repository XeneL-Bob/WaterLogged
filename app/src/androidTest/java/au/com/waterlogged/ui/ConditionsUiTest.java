package au.com.waterlogged.ui;

import static org.junit.Assert.*;

import android.os.Bundle;

import androidx.test.core.app.ActivityScenario;
import androidx.test.core.app.ApplicationProvider;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.platform.app.InstrumentationRegistry;

import au.com.waterlogged.MainActivity;
import au.com.waterlogged.R;
import au.com.waterlogged.model.*;
import au.com.waterlogged.services.*;
import au.com.waterlogged.services.ConditionsProvider.*;

import org.junit.Test;
import org.junit.runner.RunWith;

import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicReference;

@RunWith(AndroidJUnit4.class)
public class ConditionsUiTest {
    private static final class FakeConditions implements ConditionsProvider {
        int calls;
        long requestedTime;
        Callback callback;

        public void fetch(
                double lat, double lon, long time, boolean coast, String gauge, Callback cb) {
            calls++;
            requestedTime = time;
            callback = cb;
        }

        void complete(String temp) {
            Snapshot s = new Snapshot();
            s.complete = true;
            s.readings.put(
                    Field.TEMPERATURE, new Reading(temp, "Fixture weather", requestedTime, 2000));
            s.readings.put(
                    Field.WIND_SPEED, new Reading(12, "Fixture weather", requestedTime, 2000));
            callback.update(s);
        }
    }

    public static class TestItem extends CatchItemFragment {
        static FakeConditions fake;

        @Override
        protected ConditionsProvider createConditionsProvider() {
            return fake;
        }
    }

    private WaterRepository repo() {
        return WaterRepository.get(ApplicationProvider.getApplicationContext());
    }

    private CatchRecord fixture() throws Exception {
        CatchRecord r = new CatchRecord();
        r.title = "Conditions test fixture";
        r.locationName = "Test harbour";
        r.latitude = -26.68;
        r.longitude = 153.12;
        r.caughtAt = 1000000;
        r.airTemperatureC = 24.0;
        ConditionMetadata m = new ConditionMetadata(null);
        m.remember(Field.TEMPERATURE, new Reading(24, "Original fixture source", r.caughtAt, 1000));
        r.conditionsMetadata = m.json();
        save(r);
        return r;
    }

    private void save(CatchRecord r) throws Exception {
        CountDownLatch l = new CountDownLatch(1);
        AtomicReference<Exception> error = new AtomicReference<>();
        repo().save(
                        r,
                        (v, e) -> {
                            error.set(e);
                            l.countDown();
                        });
        assertTrue(l.await(5, TimeUnit.SECONDS));
        assertNull(error.get());
    }

    private void delete(CatchRecord r) throws Exception {
        CountDownLatch l = new CountDownLatch(1);
        repo().delete(r, (v, e) -> l.countDown());
        assertTrue(l.await(5, TimeUnit.SECONDS));
    }

    private TestItem open(ActivityScenario<MainActivity> scenario, CatchRecord r) throws Exception {
        TestItem item = new TestItem();
        Bundle b = new Bundle();
        b.putString("id", r.id);
        item.setArguments(b);
        scenario.onActivity(
                a ->
                        a.getSupportFragmentManager()
                                .beginTransaction()
                                .replace(R.id.content, item)
                                .commitNow());
        long end = System.currentTimeMillis() + 5000;
        AtomicReference<Boolean> ready = new AtomicReference<>(false);
        while (!ready.get() && System.currentTimeMillis() < end) {
            scenario.onActivity(
                    a ->
                            ready.set(
                                    item.getView() != null
                                            && ((CatchFormView) item.getView()).save.isEnabled()));
            Thread.sleep(20);
        }
        assertTrue(ready.get());
        InstrumentationRegistry.getInstrumentation().waitForIdleSync();
        return item;
    }

    @Test
    public void reopenDoesNotFetchAndRefreshKeepsManualCorrection() throws Exception {
        CatchRecord r = fixture();
        TestItem.fake = new FakeConditions();
        try (ActivityScenario<MainActivity> scenario =
                ActivityScenario.launch(MainActivity.class)) {
            TestItem item = open(scenario, r);
            scenario.onActivity(
                    a -> {
                        CatchFormView f = (CatchFormView) item.getView();
                        assertEquals(0, TestItem.fake.calls);
                        assertEquals("24.0", f.temperature.getText().toString());
                        f.temperature.setText("21");
                        f.refreshConditions.performClick();
                        assertEquals(r.caughtAt, TestItem.fake.requestedTime);
                        TestItem.fake.complete("29");
                        assertEquals("21", f.temperature.getText().toString());
                        assertEquals("12", f.wind.getText().toString());
                    });
        } finally {
            delete(r);
        }
    }

    @Test
    public void abandonedRequestCannotWriteToReplacementView() throws Exception {
        CatchRecord r = fixture();
        TestItem.fake = new FakeConditions();
        try (ActivityScenario<MainActivity> scenario =
                ActivityScenario.launch(MainActivity.class)) {
            TestItem old = open(scenario, r);
            scenario.onActivity(
                    a -> ((CatchFormView) old.getView()).refreshConditions.performClick());
            Callback delayed = TestItem.fake.callback;
            TestItem replacement = open(scenario, r);
            scenario.onActivity(
                    a -> {
                        Snapshot s = new Snapshot();
                        s.complete = true;
                        s.readings.put(
                                Field.TEMPERATURE,
                                new Reading(55, "Abandoned request", r.caughtAt, 9999));
                        delayed.update(s);
                        assertEquals(
                                "24.0",
                                ((CatchFormView) replacement.getView())
                                        .temperature
                                        .getText()
                                        .toString());
                    });
        } finally {
            delete(r);
        }
    }

    @Test
    public void automaticResetRestoresProviderOwnershipWithoutChangingOtherManualFields()
            throws Exception {
        CatchRecord r = fixture();
        TestItem.fake = new FakeConditions();
        try (ActivityScenario<MainActivity> scenario =
                ActivityScenario.launch(MainActivity.class)) {
            TestItem item = open(scenario, r);
            scenario.onActivity(
                    a -> {
                        CatchFormView f = (CatchFormView) item.getView();
                        f.temperature.setText("21");
                        f.wind.setText("33");
                        assertEquals(
                                android.view.View.VISIBLE,
                                f.automaticButtons.get(Field.TEMPERATURE).getVisibility());
                        f.automaticButtons.get(Field.TEMPERATURE).performClick();
                        TestItem.fake.complete("29");
                        assertEquals("29", f.temperature.getText().toString());
                        assertEquals("33", f.wind.getText().toString());
                        assertEquals(
                                android.view.View.GONE,
                                f.automaticButtons.get(Field.TEMPERATURE).getVisibility());
                    });
        } finally {
            delete(r);
        }
    }

    @Test
    public void savedTideCurveReopensWithoutNetwork() throws Exception {
        CatchRecord r = fixture();
        java.util.TreeMap<Long, Double> points = new java.util.TreeMap<>();
        points.put(0L, .7);
        points.put(2000000L, 1.2);
        r.tideSeriesJson = new TideSeries(points, "Fixture MSQ prediction", 1000).json();
        save(r);
        TestItem.fake = new FakeConditions();
        try (ActivityScenario<MainActivity> scenario =
                ActivityScenario.launch(MainActivity.class)) {
            TestItem item = open(scenario, r);
            scenario.onActivity(
                    a -> {
                        CatchFormView f = (CatchFormView) item.getView();
                        assertEquals(0, TestItem.fake.calls);
                        assertEquals(android.view.View.VISIBLE, f.tideChart.getVisibility());
                        assertTrue(f.tideDescription.getText().toString().contains("LAT"));
                    });
            scenario.recreate();
            scenario.onActivity(
                    a -> {
                        CatchFormView f =
                                (CatchFormView)
                                        a.getSupportFragmentManager()
                                                .findFragmentById(R.id.content)
                                                .getView();
                        assertEquals(android.view.View.VISIBLE, f.tideChart.getVisibility());
                        assertEquals(0, TestItem.fake.calls);
                    });
        } finally {
            delete(r);
        }
    }

    @Test
    public void rotationPreservesManualDraftAndOriginalTimestamp() throws Exception {
        CatchRecord r = fixture();
        try (ActivityScenario<MainActivity> scenario =
                ActivityScenario.launch(MainActivity.class)) {
            scenario.onActivity(
                    a ->
                            a.getSupportFragmentManager()
                                    .beginTransaction()
                                    .replace(R.id.content, CatchItemFragment.existing(r.id))
                                    .commitNow());
            waitForForm(scenario);
            scenario.onActivity(
                    a -> {
                        CatchFormView f =
                                (CatchFormView)
                                        a.getSupportFragmentManager()
                                                .findFragmentById(R.id.content)
                                                .getView();
                        f.temperature.setText("21");
                        f.wind.setText("9");
                        f.wind.setText("");
                    });
            scenario.recreate();
            waitForForm(scenario);
            scenario.onActivity(
                    a -> {
                        CatchFormView f =
                                (CatchFormView)
                                        a.getSupportFragmentManager()
                                                .findFragmentById(R.id.content)
                                                .getView();
                        assertEquals("21", f.temperature.getText().toString());
                        assertEquals("", f.wind.getText().toString());
                        assertEquals("Test harbour", f.location.getText().toString());
                        assertTrue(
                                f.conditionsStatus
                                        .getText()
                                        .toString()
                                        .startsWith("Saved snapshot"));
                    });
        } finally {
            delete(r);
        }
    }

    private void waitForForm(ActivityScenario<MainActivity> scenario) throws Exception {
        long end = System.currentTimeMillis() + 5000;
        AtomicReference<Boolean> ready = new AtomicReference<>(false);
        while (!ready.get() && System.currentTimeMillis() < end) {
            scenario.onActivity(
                    a -> {
                        android.view.View v =
                                a.getSupportFragmentManager()
                                        .findFragmentById(R.id.content)
                                        .getView();
                        ready.set(
                                v instanceof CatchFormView && ((CatchFormView) v).save.isEnabled());
                    });
            Thread.sleep(20);
        }
        assertTrue(ready.get());
        InstrumentationRegistry.getInstrumentation().waitForIdleSync();
    }
}
