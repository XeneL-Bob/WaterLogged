package au.com.waterlogged.ui;

import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.util.LruCache;
import android.widget.ImageView;

import au.com.waterlogged.R;
import au.com.waterlogged.model.SpeciesCatalogue;

import com.google.android.material.dialog.MaterialAlertDialogBuilder;

import java.io.InputStream;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/** Offline thumbnails, with a view tag to stop recycled rows borrowing another fish's face. */
public final class SpeciesImages {
    private static final ExecutorService IO = Executors.newFixedThreadPool(2);
    private static final LruCache<String, Bitmap> CACHE =
            new LruCache<String, Bitmap>(8 * 1024 * 1024) {
                @Override
                protected int sizeOf(String key, Bitmap bitmap) {
                    return bitmap.getByteCount();
                }
            };

    private SpeciesImages() {}

    public static void show(ImageView image, String id) {
        Context context = image.getContext().getApplicationContext();
        String asset = SpeciesCatalogue.get(context).asset(id), token = "species:" + id;
        image.setTag(token);
        image.setImageResource(R.drawable.ic_fish);
        image.setContentDescription(
                asset.isEmpty() ? "Generic fish icon" : "Species reference image");
        if (asset.isEmpty()) return;
        Bitmap cached = CACHE.get(asset);
        if (cached != null) {
            image.setImageBitmap(cached);
            return;
        }
        IO.execute(
                () -> {
                    Bitmap bitmap = null;
                    try (InputStream in = context.getAssets().open(asset)) {
                        bitmap = BitmapFactory.decodeStream(in);
                        if (bitmap != null) CACHE.put(asset, bitmap);
                    } catch (Exception ignored) {
                    }
                    Bitmap ready = bitmap;
                    image.post(
                            () -> {
                                if (ready != null && token.equals(image.getTag()))
                                    image.setImageBitmap(ready);
                            });
                });
    }

    public static void showCredits(Context context, String id) {
        SpeciesCatalogue catalogue = SpeciesCatalogue.get(context);
        MaterialAlertDialogBuilder dialog =
                new MaterialAlertDialogBuilder(context)
                        .setTitle("Species image & credits")
                        .setMessage(catalogue.credits(id))
                        .setPositiveButton("Close", null);
        String source = catalogue.source(id);
        if (!source.isEmpty())
            dialog.setNeutralButton(
                    "Photo source",
                    (d, which) -> {
                        try {
                            context.startActivity(
                                    new Intent(Intent.ACTION_VIEW, Uri.parse(source)));
                        } catch (android.content.ActivityNotFoundException
                                | SecurityException ignored) {
                        }
                    });
        dialog.show();
    }
}
