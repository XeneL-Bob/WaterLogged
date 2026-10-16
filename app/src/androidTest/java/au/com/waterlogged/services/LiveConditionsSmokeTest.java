package au.com.waterlogged.services;

import static org.junit.Assert.*;

import androidx.test.core.app.ApplicationProvider;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.platform.app.InstrumentationRegistry;

import au.com.waterlogged.services.ConditionsProvider.*;

import org.junit.Assume;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.util.*;
import java.util.concurrent.*;

/** Opt-in integration check against public example coordinates; ordinary tests stay offline. */
@RunWith(AndroidJUnit4.class)
public class LiveConditionsSmokeTest {
    @Test
    public void publicMooloolabaExampleReturnsWeatherSwellMoonAndTides() throws Exception {
        Assume.assumeTrue(
                "Pass -e liveConditions true to opt in",
                InstrumentationRegistry.getArguments()
                        .getString("liveConditions", "false")
                        .equals("true"));
        CountDownLatch done = new CountDownLatch(1);
        EnumMap<Field, Reading> values = new EnumMap<>(Field.class);
        List<String> notices = new ArrayList<>();
        LiveConditionsProvider provider =
                new LiveConditionsProvider(ApplicationProvider.getApplicationContext());
        InstrumentationRegistry.getInstrumentation()
                .runOnMainSync(
                        () ->
                                provider.fetch(
                                        -26.6825,
                                        153.1201,
                                        System.currentTimeMillis(),
                                        true,
                                        null,
                                        s -> {
                                            values.putAll(s.readings);
                                            if (s.complete) {
                                                notices.addAll(s.notices);
                                                done.countDown();
                                            }
                                        }));
        assertTrue("Provider did not finish", done.await(40, TimeUnit.SECONDS));
        for (Field field :
                new Field[] {
                    Field.LOCATION,
                    Field.TEMPERATURE,
                    Field.WIND_SPEED,
                    Field.WIND_DIRECTION,
                    Field.WIND_GUST,
                    Field.SWELL,
                    Field.WAVE_HEIGHT,
                    Field.TIDE_HEIGHT,
                    Field.TIDE_TREND,
                    Field.MOON
                }) assertTrue(field + " missing: " + notices, values.containsKey(field));
        assertTrue(values.get(Field.TIDE_HEIGHT).source.contains("Mooloolaba"));
        String weatherSource = values.get(Field.TEMPERATURE).source;
        assertTrue(
                weatherSource,
                weatherSource.contains("model point") || weatherSource.contains("BOM"));
        if (weatherSource.contains("BOM"))
            assertTrue(weatherSource.contains("Sunshine Coast Airport"));
        for (Map.Entry<Field, Reading> e : values.entrySet())
            android.util.Log.i(
                    "WaterLoggedPublicTest",
                    e.getKey()
                            + "="
                            + e.getValue().value
                            + " | "
                            + e.getValue().source
                            + " | validAt="
                            + e.getValue().validAt);
        assertTrue(
                Math.abs(values.get(Field.TEMPERATURE).validAt - System.currentTimeMillis())
                        <= (weatherSource.contains("observed") ? 60 : 30) * 60000L + 60000);
    }
}
