package au.com.waterlogged.model;

import android.content.Context;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/** Public reference data travels with the app. Favourite choices live separately in Room. */
public final class SpeciesCatalogue {
    private static SpeciesCatalogue instance;
    private final Map<String, JSONObject> entries = new LinkedHashMap<>();

    private SpeciesCatalogue(Context context) {
        try (InputStream in = context.getAssets().open("species_catalogue.json")) {
            java.io.ByteArrayOutputStream bytes = new java.io.ByteArrayOutputStream();
            byte[] buffer = new byte[4096];
            int n;
            while ((n = in.read(buffer)) != -1) bytes.write(buffer, 0, n);
            JSONArray rows =
                    new JSONObject(bytes.toString(StandardCharsets.UTF_8.name()))
                            .getJSONArray("species");
            for (int i = 0; i < rows.length(); i++) {
                JSONObject row = rows.getJSONObject(i);
                entries.put(row.getString("id"), row);
            }
        } catch (Exception ignored) {
            // The repository retains the original seed if an asset is missing or damaged.
        }
    }

    public static synchronized SpeciesCatalogue get(Context context) {
        if (instance == null) instance = new SpeciesCatalogue(context.getApplicationContext());
        return instance;
    }

    public List<Species> species() {
        List<Species> result = new ArrayList<>();
        for (JSONObject row : entries.values())
            result.add(
                    new Species(
                            row.optString("id"),
                            row.optString("commonName"),
                            row.optString("scientificName"),
                            row.optString("aliases"),
                            row.optBoolean("favourite")));
        return result;
    }

    public String asset(String id) {
        JSONObject row = entries.get(id);
        return row == null ? "" : row.optString("photoAsset");
    }

    public String source(String id) {
        JSONObject row = entries.get(id);
        return row == null ? "" : row.optString("photoSource");
    }

    public String credits(String id) {
        JSONObject row = entries.get(id);
        if (row == null) return "Custom species. No reference photograph is bundled.";
        String text = row.optString("commonName") + "\n" + row.optString("scientificName");
        if (row.has("photoAsset"))
            text +=
                    "\n\nReference image: "
                            + row.optString("photoAuthor")
                            + "\n"
                            + row.optString("photoLicence")
                            + "\n"
                            + row.optString("photoLicenceUrl")
                            + "\n"
                            + row.optString("photoChanges")
                            + "\n\n"
                            + row.optString("photoSource");
        else
            text +=
                    "\n\n"
                        + "Generic icon: a verified freely licensed photograph has not been bundled"
                        + " for this species.";
        return text
                + "\n\nReference pictures support browsing; confirm identification independently.";
    }
}
