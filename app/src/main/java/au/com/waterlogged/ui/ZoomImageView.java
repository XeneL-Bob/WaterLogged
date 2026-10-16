package au.com.waterlogged.ui;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Matrix;
import android.view.GestureDetector;
import android.view.MotionEvent;
import android.view.ScaleGestureDetector;

/** A small photo viewer with bounded pinch scale, panning and reset. */
public class ZoomImageView extends androidx.appcompat.widget.AppCompatImageView {
    private final Matrix transform = new Matrix();
    private float scale = 1, lastX, lastY;
    private final ScaleGestureDetector pinch;
    private final GestureDetector gestures;

    public ZoomImageView(Context c) {
        super(c);
        setScaleType(ScaleType.MATRIX);
        pinch =
                new ScaleGestureDetector(
                        c,
                        new ScaleGestureDetector.SimpleOnScaleGestureListener() {
                            @Override
                            public boolean onScale(ScaleGestureDetector d) {
                                float next = Math.max(1, Math.min(5, scale * d.getScaleFactor()));
                                float factor = next / scale;
                                scale = next;
                                transform.postScale(factor, factor, d.getFocusX(), d.getFocusY());
                                setImageMatrix(transform);
                                return true;
                            }
                        });
        gestures =
                new GestureDetector(
                        c,
                        new GestureDetector.SimpleOnGestureListener() {
                            @Override
                            public boolean onDown(MotionEvent e) {
                                return true;
                            }

                            @Override
                            public boolean onDoubleTap(MotionEvent e) {
                                fit();
                                return true;
                            }
                        });
    }

    @Override
    public void setImageBitmap(Bitmap b) {
        super.setImageBitmap(b);
        post(this::fit);
    }

    @Override
    protected void onSizeChanged(int w, int h, int oldw, int oldh) {
        super.onSizeChanged(w, h, oldw, oldh);
        fit();
    }

    private void fit() {
        if (getDrawable() == null || getWidth() == 0) return;
        scale = 1;
        float f =
                Math.min(
                        (float) getWidth() / getDrawable().getIntrinsicWidth(),
                        (float) getHeight() / getDrawable().getIntrinsicHeight());
        transform.setScale(f, f);
        transform.postTranslate(
                (getWidth() - getDrawable().getIntrinsicWidth() * f) / 2,
                (getHeight() - getDrawable().getIntrinsicHeight() * f) / 2);
        setImageMatrix(transform);
    }

    @Override
    public boolean onTouchEvent(MotionEvent e) {
        pinch.onTouchEvent(e);
        gestures.onTouchEvent(e);
        if (e.getActionMasked() == MotionEvent.ACTION_MOVE && !pinch.isInProgress() && scale > 1) {
            transform.postTranslate(e.getX() - lastX, e.getY() - lastY);
            setImageMatrix(transform);
        }
        lastX = e.getX();
        lastY = e.getY();
        if (e.getActionMasked() == MotionEvent.ACTION_UP) performClick();
        return true;
    }

    @Override
    public boolean performClick() {
        super.performClick();
        return true;
    }
}
