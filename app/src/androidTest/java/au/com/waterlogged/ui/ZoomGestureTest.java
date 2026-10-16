package au.com.waterlogged.ui;

import static org.junit.Assert.*;

import android.content.Context;
import android.graphics.*;
import android.os.Build;
import android.os.SystemClock;
import android.view.*;

import androidx.test.core.app.ApplicationProvider;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.platform.app.InstrumentationRegistry;

import org.junit.Test;
import org.junit.runner.RunWith;

@RunWith(AndroidJUnit4.class)
public class ZoomGestureTest {
    @Test
    public void pinchPanAndDoubleTapResetChangeTheDisplayedImage() {
        InstrumentationRegistry.getInstrumentation()
                .runOnMainSync(
                        () -> {
                            Context context = ApplicationProvider.getApplicationContext();
                            int minimum =
                                    (int)
                                            (200
                                                    * context.getResources()
                                                            .getDisplayMetrics()
                                                            .density);
                            if (Build.VERSION.SDK_INT >= 29)
                                minimum =
                                        ViewConfiguration.get(context)
                                                .getScaledMinimumScalingSpan();
                            float span = Math.max(400, minimum + 100), center = span * 3;
                            android.util.Log.i(
                                    "WaterLoggedGestureTest",
                                    "Framework minimum span="
                                            + minimum
                                            + "; simulated initial span="
                                            + span);
                            ZoomImageView image = new ZoomImageView(context);
                            image.setImageBitmap(
                                    Bitmap.createBitmap(300, 200, Bitmap.Config.ARGB_8888));
                            image.layout(0, 0, (int) (center * 2), (int) (center * 2));
                            float[] initial = matrix(image);
                            long t = SystemClock.uptimeMillis();
                            touch(
                                    image,
                                    t,
                                    t,
                                    MotionEvent.ACTION_DOWN,
                                    new float[] {center - span / 2},
                                    new float[] {center});
                            touch(
                                    image,
                                    t,
                                    t + 20,
                                    MotionEvent.ACTION_POINTER_DOWN
                                            | (1 << MotionEvent.ACTION_POINTER_INDEX_SHIFT),
                                    new float[] {center - span / 2, center + span / 2},
                                    new float[] {center, center});
                            for (int i = 0; i < 3; i++) {
                                float half = span * new float[] {1.2f, 1.7f, 2.3f}[i] / 2;
                                touch(
                                        image,
                                        t,
                                        t + 40 + i * 20,
                                        MotionEvent.ACTION_MOVE,
                                        new float[] {center - half, center + half},
                                        new float[] {center, center});
                            }
                            assertTrue(
                                    "A pinch beyond the framework threshold should enlarge the"
                                        + " image",
                                    matrix(image)[Matrix.MSCALE_X]
                                            > initial[Matrix.MSCALE_X] * 1.2f);
                            float left = center - span * 2.3f / 2, right = center + span * 2.3f / 2;
                            touch(
                                    image,
                                    t,
                                    t + 100,
                                    MotionEvent.ACTION_POINTER_UP
                                            | (1 << MotionEvent.ACTION_POINTER_INDEX_SHIFT),
                                    new float[] {left, right},
                                    new float[] {center, center});
                            float before = matrix(image)[Matrix.MTRANS_X];
                            touch(
                                    image,
                                    t,
                                    t + 120,
                                    MotionEvent.ACTION_MOVE,
                                    new float[] {left + 50},
                                    new float[] {center + 20});
                            assertNotEquals(before, matrix(image)[Matrix.MTRANS_X], .01f);
                            touch(
                                    image,
                                    t,
                                    t + 140,
                                    MotionEvent.ACTION_UP,
                                    new float[] {left + 50},
                                    new float[] {center + 20});
                            touch(
                                    image,
                                    t + 1000,
                                    t + 1000,
                                    MotionEvent.ACTION_DOWN,
                                    new float[] {center},
                                    new float[] {center});
                            touch(
                                    image,
                                    t + 1000,
                                    t + 1020,
                                    MotionEvent.ACTION_UP,
                                    new float[] {center},
                                    new float[] {center});
                            touch(
                                    image,
                                    t + 1100,
                                    t + 1100,
                                    MotionEvent.ACTION_DOWN,
                                    new float[] {center},
                                    new float[] {center});
                            touch(
                                    image,
                                    t + 1100,
                                    t + 1120,
                                    MotionEvent.ACTION_UP,
                                    new float[] {center},
                                    new float[] {center});
                            assertEquals(
                                    initial[Matrix.MSCALE_X],
                                    matrix(image)[Matrix.MSCALE_X],
                                    .001f);
                            assertEquals(
                                    initial[Matrix.MTRANS_Y],
                                    matrix(image)[Matrix.MTRANS_Y],
                                    .001f);
                        });
    }

    private static float[] matrix(ZoomImageView image) {
        float[] values = new float[9];
        image.getImageMatrix().getValues(values);
        return values;
    }

    private static void touch(View view, long down, long at, int action, float[] x, float[] y) {
        MotionEvent.PointerProperties[] properties = new MotionEvent.PointerProperties[x.length];
        MotionEvent.PointerCoords[] coords = new MotionEvent.PointerCoords[x.length];
        for (int i = 0; i < x.length; i++) {
            properties[i] = new MotionEvent.PointerProperties();
            properties[i].id = i;
            properties[i].toolType = MotionEvent.TOOL_TYPE_FINGER;
            coords[i] = new MotionEvent.PointerCoords();
            coords[i].x = x[i];
            coords[i].y = y[i];
            coords[i].pressure = 1;
            coords[i].size = 1;
        }
        MotionEvent event =
                MotionEvent.obtain(
                        down,
                        at,
                        action,
                        x.length,
                        properties,
                        coords,
                        0,
                        0,
                        1,
                        1,
                        0,
                        0,
                        InputDevice.SOURCE_TOUCHSCREEN,
                        0);
        try {
            view.onTouchEvent(event);
        } finally {
            event.recycle();
        }
    }
}
