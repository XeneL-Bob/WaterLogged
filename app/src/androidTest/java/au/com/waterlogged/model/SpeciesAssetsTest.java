package au.com.waterlogged.model;

import static org.junit.Assert.*;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;

import androidx.test.core.app.ApplicationProvider;
import androidx.test.ext.junit.runners.AndroidJUnit4;

import org.junit.Test;
import org.junit.runner.RunWith;

import java.io.InputStream;

@RunWith(AndroidJUnit4.class)
public class SpeciesAssetsTest {
    @Test
    public void bundledImagesDecodeOfflineAndKeepTheirCredits() throws Exception {
        Context context = ApplicationProvider.getApplicationContext();
        SpeciesCatalogue catalogue = SpeciesCatalogue.get(context);
        assertEquals(32, catalogue.species().size());
        int images = 0;
        for (Species species : catalogue.species()) {
            String asset = catalogue.asset(species.id);
            if (asset.isEmpty()) continue;
            try (InputStream in = context.getAssets().open(asset)) {
                Bitmap bitmap = BitmapFactory.decodeStream(in);
                assertNotNull(species.id, bitmap);
                assertTrue(bitmap.getWidth() <= 2000);
                assertTrue(bitmap.getHeight() <= 2000);
                bitmap.recycle();
            }
            assertTrue(catalogue.source(species.id).startsWith("https://commons.wikimedia.org/"));
            assertTrue(catalogue.credits(species.id).contains("Reference image:"));
            images++;
        }
        assertEquals(30, images);
    }
}
