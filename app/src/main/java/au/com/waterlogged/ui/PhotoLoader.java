package au.com.waterlogged.ui;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Matrix;
import android.media.ExifInterface;
import android.widget.ImageView;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/** Bounded, background decoding with EXIF orientation; never decode a full camera image on UI. */
public final class PhotoLoader {
    private static final ExecutorService io = Executors.newFixedThreadPool(2);

    private PhotoLoader() {}

    public static void load(ImageView view, String path) {
        view.setTag(path);
        io.execute(
                () -> {
                    Bitmap bitmap = decode(path);
                    view.post(
                            () -> {
                                if (path.equals(view.getTag()) && bitmap != null)
                                    view.setImageBitmap(bitmap);
                            });
                });
    }

    private static Bitmap decode(String path) {
        try {
            BitmapFactory.Options o = new BitmapFactory.Options();
            o.inJustDecodeBounds = true;
            BitmapFactory.decodeFile(path, o);
            o.inSampleSize = 1;
            while (o.outWidth / o.inSampleSize > 1600 || o.outHeight / o.inSampleSize > 1600)
                o.inSampleSize *= 2;
            o.inJustDecodeBounds = false;
            Bitmap b = BitmapFactory.decodeFile(path, o);
            if (b == null) return null;
            int orientation =
                    new ExifInterface(path).getAttributeInt(ExifInterface.TAG_ORIENTATION, 1);
            Matrix m = new Matrix();
            switch (orientation) {
                case 2:
                    m.setScale(-1, 1);
                    break;
                case 3:
                    m.setRotate(180);
                    break;
                case 4:
                    m.setScale(1, -1);
                    break;
                case 5:
                    m.setRotate(90);
                    m.postScale(-1, 1);
                    break;
                case 6:
                    m.setRotate(90);
                    break;
                case 7:
                    m.setRotate(270);
                    m.postScale(-1, 1);
                    break;
                case 8:
                    m.setRotate(270);
                    break;
            }
            return orientation == 1
                    ? b
                    : Bitmap.createBitmap(b, 0, 0, b.getWidth(), b.getHeight(), m, true);
        } catch (Exception e) {
            return null;
        }
    }
}
