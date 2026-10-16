package au.com.waterlogged.services;

import android.content.Context;
import android.net.Uri;

import au.com.waterlogged.model.*;

import org.json.*;

import java.io.*;
import java.lang.reflect.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.*;
import java.util.zip.*;

/** Logical backups: never copy an open SQLite/WAL file or trust imported absolute photo paths. */
public final class BackupService {
    private static final int MAX_RECORDS = 20000;
    private static final long PHOTO_LIMIT = 32L * 1024 * 1024, MANIFEST_LIMIT = 16L * 1024 * 1024;

    private BackupService() {}

    public static String exportBackup(
            Context context, WaterDatabase db, File photos, Uri uri, char[] password)
            throws Exception {
        File zip = File.createTempFile("backup-", ".zip", context.getCacheDir());
        File encrypted = File.createTempFile("backup-", ".wlog", context.getCacheDir());
        try {
            List<CatchRecord> catches = new ArrayList<>();
            List<Species> species = new ArrayList<>();
            List<FishingSession> sessions = new ArrayList<>();
            db.runInTransaction(
                    () -> {
                        catches.addAll(db.catches().all());
                        species.addAll(db.species().all());
                        sessions.addAll(db.sessions().all());
                    });
            JSONObject manifest =
                    new JSONObject()
                            .put("format", 1)
                            .put("schema", 4)
                            .put("createdAt", System.currentTimeMillis());
            if (catches.size() > MAX_RECORDS
                    || species.size() > MAX_RECORDS
                    || sessions.size() > MAX_RECORDS)
                throw new IOException(
                        "The pilot supports up to 20,000 records of each type per backup.");
            JSONArray rows = new JSONArray();
            long size = 0;
            try (ZipOutputStream out = new ZipOutputStream(new FileOutputStream(zip))) {
                for (CatchRecord original : catches) {
                    CatchRecord record = original.copy();
                    String entry = null;
                    if (record.photoPath != null) {
                        File photo = new File(record.photoPath);
                        if (!photos.getCanonicalPath().equals(photo.getCanonicalFile().getParent())
                                || !photo.isFile())
                            throw new IOException(
                                    "A catch photo is missing. Restore its file or remove that"
                                            + " photo before backing up.");
                        entry = "photos/" + rows.length() + ".jpg";
                        if (photo.length() > PHOTO_LIMIT)
                            throw new IOException("A photo exceeds the 32 MB limit.");
                        size += photo.length();
                        if (size > BackupCrypto.MAX_BYTES)
                            throw new IOException("Backup exceeds the 256 MB pilot limit.");
                        out.putNextEntry(new ZipEntry(entry));
                        try (InputStream in = new FileInputStream(photo)) {
                            copy(in, out, PHOTO_LIMIT);
                        }
                        out.closeEntry();
                    }
                    record.photoPath = entry;
                    rows.put(encode(record));
                }
                manifest.put("catches", rows)
                        .put("species", encodeList(species))
                        .put("sessions", encodeList(sessions));
                byte[] json = manifest.toString().getBytes(StandardCharsets.UTF_8);
                if (json.length > MANIFEST_LIMIT)
                    throw new IOException("Diary manifest is too large.");
                out.putNextEntry(new ZipEntry("diary.json"));
                out.write(json);
                out.closeEntry();
            }
            try (InputStream in = new FileInputStream(zip);
                    OutputStream out = new FileOutputStream(encrypted)) {
                BackupCrypto.encrypt(in, out, password);
            }
            try (InputStream in = new FileInputStream(encrypted);
                    OutputStream out = context.getContentResolver().openOutputStream(uri, "w")) {
                if (out == null) throw new IOException("Cannot open the selected destination.");
                copy(in, out, BackupCrypto.MAX_BYTES + 1024 * 1024);
            }
            return "Encrypted backup saved: "
                    + catches.size()
                    + " catches, "
                    + sessions.size()
                    + " sessions, "
                    + species.size()
                    + " species. Keep the password; it cannot be recovered.";
        } finally {
            zip.delete();
            encrypted.delete();
        }
    }

    public static String restore(
            Context context, WaterDatabase db, File photos, Uri uri, char[] password)
            throws Exception {
        File zip = File.createTempFile("restore-", ".zip", context.getCacheDir());
        File stage = new File(context.getCacheDir(), "restore-" + UUID.randomUUID());
        if (!stage.mkdir()) throw new IOException("Cannot prepare restore.");
        List<File> committedFiles = new ArrayList<>();
        boolean success = false;
        try {
            // Authenticate every chunk and the terminator before parsing or modifying the diary.
            try (InputStream in = context.getContentResolver().openInputStream(uri);
                    OutputStream out = new FileOutputStream(zip)) {
                if (in == null) throw new IOException("Cannot open backup.");
                BackupCrypto.decrypt(in, out, password);
            } catch (java.security.GeneralSecurityException | EOFException error) {
                throw new IOException("Wrong password, incomplete or damaged backup.", error);
            }
            JSONObject manifest = null;
            Map<String, File> images = new HashMap<>();
            Set<String> names = new HashSet<>();
            long total = 0;
            try (ZipInputStream in = new ZipInputStream(new FileInputStream(zip))) {
                ZipEntry entry;
                while ((entry = in.getNextEntry()) != null) {
                    String name = entry.getName();
                    if (!names.add(name) || names.size() > MAX_RECORDS + 1)
                        throw new IOException("Duplicate or excessive backup entries.");
                    if (name.equals("diary.json")) {
                        ByteArrayOutputStream bytes = new ByteArrayOutputStream();
                        total += copy(in, bytes, MANIFEST_LIMIT);
                        manifest = new JSONObject(bytes.toString(StandardCharsets.UTF_8.name()));
                    } else if (name.matches("photos/[0-9]+\\.jpg")) {
                        File file = new File(stage, UUID.randomUUID() + ".jpg");
                        try (OutputStream out = new FileOutputStream(file)) {
                            total += copy(in, out, PHOTO_LIMIT);
                        }
                        images.put(name, file);
                    } else throw new IOException("Unsupported path in backup.");
                    if (total > BackupCrypto.MAX_BYTES)
                        throw new IOException("Expanded backup is too large.");
                }
            }
            if (manifest == null
                    || manifest.getInt("format") != 1
                    || manifest.getInt("schema") != 4)
                throw new IOException("Unsupported diary backup version.");
            long created = manifest.getLong("createdAt");
            if (created < 0) throw new IOException("Invalid backup time.");
            List<CatchRecord> catches =
                    decodeList(manifest.getJSONArray("catches"), CatchRecord.class);
            List<Species> species = decodeList(manifest.getJSONArray("species"), Species.class);
            List<FishingSession> sessions =
                    decodeList(manifest.getJSONArray("sessions"), FishingSession.class);
            for (CatchRecord r : catches) validate(r, images);
            for (Species s : species) {
                id(s.id);
                text(s.commonName, 200, true);
                text(s.scientificName, 200, false);
                text(s.aliases, 1000, false);
            }
            Set<String> sessionIds = new HashSet<>();
            for (FishingSession s : sessions) {
                id(s.id);
                sessionIds.add(s.id);
                text(s.locationName, 500, true);
                text(s.bait, 500, false);
                text(s.method, 500, false);
                coordinate(s.latitude, s.longitude);
                if (s.startedAt < 0 || (s.endedAt != null && s.endedAt < s.startedAt))
                    throw new IOException("Invalid session time.");
                if (s.endedAt == null) s.endedAt = Math.max(s.startedAt, created);
                s.activeSlot = null; // Restoring a backup never starts a second active trip.
            }
            for (CatchRecord r : catches)
                if (r.sessionId != null && !sessionIds.contains(r.sessionId))
                    throw new IOException("Missing session in backup.");
            int[] added = {0, 0, 0};
            photos.mkdirs();
            db.runInTransaction(
                    () -> {
                        try {
                            for (Species s : species)
                                if (db.species().find(s.id) == null) db.species().insert(s);
                                else db.species().setFavourite(s.id, s.favourite);
                            for (FishingSession s : sessions)
                                if (db.sessions().find(s.id) == null) {
                                    db.sessions().insert(s);
                                    added[1]++;
                                }
                            for (CatchRecord r : catches)
                                if (db.catches().find(r.id) == null) {
                                    if (r.photoPath != null) {
                                        File destination =
                                                new File(
                                                        photos,
                                                        "restored-" + UUID.randomUUID() + ".jpg");
                                        committedFiles.add(destination);
                                        Files.copy(
                                                images.get(r.photoPath).toPath(),
                                                destination.toPath());
                                        r.photoPath = destination.getAbsolutePath();
                                    }
                                    db.catches().save(r);
                                    added[0]++;
                                }
                        } catch (Exception error) {
                            throw new IllegalStateException(
                                    "Restore was rolled back; existing records are unchanged.",
                                    error);
                        }
                    });
            success = true;
            return "Restored "
                    + added[0]
                    + " missing catches and "
                    + added[1]
                    + " sessions. Existing catches were kept and favourite choices restored. Trips"
                    + " active in the backup are restored as ended at backup time.";
        } finally {
            if (!success) for (File file : committedFiles) file.delete();
            File[] temporary = stage.listFiles();
            if (temporary != null) for (File file : temporary) file.delete();
            stage.delete();
            zip.delete();
        }
    }

    public static String csv(Context context, WaterDatabase db, Uri uri, boolean coordinates)
            throws Exception {
        List<CatchRecord> rows = db.catches().all();
        try (OutputStream raw = context.getContentResolver().openOutputStream(uri, "w")) {
            if (raw == null) throw new IOException("Cannot open CSV destination.");
            Writer out = new OutputStreamWriter(raw, StandardCharsets.UTF_8);
            out.write(
                    "title,location,caught_at_utc,length_cm,weight_kg,quantity,status,bait,method,notes,tide_m,wind_kmh,gust_kmh,temperature_c,swell_m,total_waves_m,moon,session_id"
                            + (coordinates ? ",latitude,longitude" : "")
                            + "\r\n");
            for (CatchRecord r : rows) {
                Object[] values = {
                    r.title,
                    r.locationName,
                    java.time.Instant.ofEpochMilli(r.caughtAt).toString(),
                    r.lengthCm,
                    r.weightKg,
                    r.quantity,
                    r.disposition,
                    r.bait,
                    r.method,
                    r.notes,
                    r.tideHeightM,
                    r.windKmh,
                    r.windGustKmh,
                    r.airTemperatureC,
                    r.swellM,
                    r.waveHeightM,
                    r.moonPhase,
                    r.sessionId
                };
                for (int i = 0; i < values.length; i++) {
                    if (i > 0) out.write(',');
                    out.write(csvCell(values[i]));
                }
                if (coordinates) out.write("," + csvCell(r.latitude) + "," + csvCell(r.longitude));
                out.write("\r\n");
            }
            out.flush();
        }
        return "Exported "
                + rows.size()
                + " catches. CSV is unencrypted and "
                + (coordinates
                        ? "includes exact coordinates."
                        : "excludes coordinates; location names and notes are still included.");
    }

    public static String csvCell(Object value) {
        if (value == null) return "\"\"";
        String s = String.valueOf(value);
        if (value instanceof String
                && (s.trim().matches("(?s)^[=+@\\-].*")
                        || s.startsWith("\t")
                        || s.startsWith("\r"))) s = "'" + s;
        return "\"" + s.replace("\"", "\"\"") + "\"";
    }

    private static JSONArray encodeList(List<?> list) throws Exception {
        JSONArray a = new JSONArray();
        for (Object item : list) a.put(encode(item));
        return a;
    }

    private static JSONObject encode(Object value) throws Exception {
        JSONObject object = new JSONObject();
        for (java.lang.reflect.Field f : value.getClass().getFields())
            if (!Modifier.isStatic(f.getModifiers()))
                object.put(f.getName(), f.get(value) == null ? JSONObject.NULL : f.get(value));
        return object;
    }

    private static <T> List<T> decodeList(JSONArray array, Class<T> type) throws Exception {
        if (array.length() > MAX_RECORDS) throw new IOException("Too many records.");
        List<T> list = new ArrayList<>();
        Set<String> ids = new HashSet<>();
        for (int i = 0; i < array.length(); i++) {
            JSONObject json = array.getJSONObject(i);
            id(json.getString("id"));
            T item;
            if (type == Species.class) item = type.cast(new Species("", "", "", "", false));
            else item = type.getDeclaredConstructor().newInstance();
            for (java.lang.reflect.Field f : type.getFields())
                if (!Modifier.isStatic(f.getModifiers()) && json.has(f.getName())) {
                    Object value = json.opt(f.getName());
                    if (value == JSONObject.NULL) {
                        if (!f.getType().isPrimitive()) f.set(item, null);
                    } else if (f.getType() == String.class)
                        f.set(item, json.getString(f.getName()));
                    else if (f.getType() == Double.class) {
                        double d = json.getDouble(f.getName());
                        if (!Double.isFinite(d)) throw new IOException("Invalid measurement.");
                        f.set(item, d);
                    } else if (f.getType() == Long.class || f.getType() == long.class)
                        f.set(item, json.getLong(f.getName()));
                    else if (f.getType() == Integer.class || f.getType() == int.class)
                        f.set(item, json.getInt(f.getName()));
                    else if (f.getType() == boolean.class)
                        f.set(item, json.getBoolean(f.getName()));
                }
            String key = (String) type.getField("id").get(item);
            id(key);
            if (!ids.add(key)) throw new IOException("Duplicate record ID.");
            list.add(item);
        }
        return list;
    }

    private static void validate(CatchRecord r, Map<String, File> images) throws Exception {
        id(r.id);
        text(r.title, 500, true);
        text(r.locationName, 500, true);
        text(r.speciesId, 200, false);
        text(r.notes, 10000, false);
        text(r.bait, 500, false);
        text(r.method, 500, false);
        text(r.windDirection, 200, false);
        text(r.moonPhase, 200, false);
        text(r.conditionsSource, 1000, false);
        text(r.tideDirection, 200, false);
        if (!Arrays.asList("Inshore", "Offshore", "Estuary", "Freshwater").contains(r.environment)
                || !Arrays.asList("Kept", "Released", "Bait", "Other").contains(r.disposition)
                || r.quantity < 1
                || r.quantity > 999
                || r.caughtAt < 0) throw new IOException("Invalid catch details.");
        coordinate(r.latitude, r.longitude);
        bound(r.lengthCm, 0, 1000);
        bound(r.weightKg, 0, 10000);
        bound(r.windKmh, 0, 400);
        bound(r.windGustKmh, 0, 500);
        bound(r.swellM, 0, 40);
        bound(r.waveHeightM, 0, 40);
        bound(r.airTemperatureC, -90, 65);
        bound(r.tideHeightM, -20, 30);
        if (r.conditionsMetadata != null) {
            text(r.conditionsMetadata, 100000, false);
            new JSONObject(r.conditionsMetadata);
        }
        if (r.tideSeriesJson != null) TideSeries.fromJson(r.tideSeriesJson);
        if (r.photoPath != null && !images.containsKey(r.photoPath))
            throw new IOException("Missing or invalid photo in backup.");
    }

    private static void coordinate(Double lat, Double lon) throws IOException {
        if ((lat == null) != (lon == null)) throw new IOException("Incomplete coordinates.");
        bound(lat, -90, 90);
        bound(lon, -180, 180);
    }

    private static void bound(Double value, double min, double max) throws IOException {
        if (value != null && (!Double.isFinite(value) || value < min || value > max))
            throw new IOException("Invalid measurement range.");
    }

    private static void id(String s) throws IOException {
        if (s == null || !s.matches("[A-Za-z0-9:_-]{1,100}"))
            throw new IOException("Invalid record ID.");
    }

    private static void text(String s, int max, boolean required) throws IOException {
        if (s == null || s.length() > max || (required && s.trim().isEmpty()))
            throw new IOException("Invalid text field.");
    }

    private static long copy(InputStream in, OutputStream out, long limit) throws IOException {
        byte[] b = new byte[8192];
        long total = 0;
        int n;
        while ((n = in.read(b)) != -1) {
            total += n;
            if (total > limit) throw new IOException("File exceeds pilot size limit.");
            out.write(b, 0, n);
        }
        return total;
    }
}
