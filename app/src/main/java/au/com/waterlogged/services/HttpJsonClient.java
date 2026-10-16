package au.com.waterlogged.services;

import android.content.Context;

import org.json.JSONObject;

import java.io.*;
import java.net.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.security.MessageDigest;
import java.util.Arrays;
import java.util.Comparator;

/** Bounded HTTPS requests and a small private cache; cache ages remain visible to callers. */
public final class HttpJsonClient {
    public static class Response {
        public final JSONObject json;
        public final long retrievedAt;
        public final boolean stale;
        public final boolean cached;

        Response(JSONObject j, long t, boolean s) {
            this(j, t, s, s);
        }

        Response(JSONObject j, long t, boolean s, boolean cached) {
            json = j;
            retrievedAt = t;
            stale = s;
            this.cached = cached;
        }
    }

    private final File directory;

    public HttpJsonClient(Context c) {
        directory = new File(c.getCacheDir(), "conditions");
        directory.mkdirs();
    }

    public Response get(String url, long ttl) throws Exception {
        return get(url, ttl, false);
    }

    public Response get(String url, long ttl, boolean forceNetwork) throws Exception {
        if (!url.startsWith("https://")) throw new IOException("HTTPS required");
        File file = new File(directory, key(url) + ".json");
        Response cached = null;
        long now = System.currentTimeMillis();
        try {
            JSONObject wrapper =
                    new JSONObject(
                            new String(Files.readAllBytes(file.toPath()), StandardCharsets.UTF_8));
            cached =
                    new Response(
                            wrapper.getJSONObject("data"), wrapper.getLong("retrievedAt"), false);
        } catch (Exception ignored) {
        }
        if (!forceNetwork
                && cached != null
                && now - cached.retrievedAt >= 0
                && now - cached.retrievedAt < ttl)
            return new Response(cached.json, cached.retrievedAt, false, true);
        HttpURLConnection connection = null;
        try {
            connection = (HttpURLConnection) new URL(url).openConnection();
            connection.setConnectTimeout(8000);
            connection.setReadTimeout(8000);
            connection.setRequestProperty("Accept", "application/json");
            connection.setRequestProperty("User-Agent", "WaterLogged-CSC202/0.3");
            if (connection.getResponseCode() != 200)
                throw new IOException("Provider returned HTTP " + connection.getResponseCode());
            ByteArrayOutputStream bytes = new ByteArrayOutputStream();
            try (InputStream in = connection.getInputStream()) {
                byte[] buffer = new byte[8192];
                int n;
                while ((n = in.read(buffer)) != -1) {
                    if (bytes.size() + n > 2_000_000) throw new IOException("Response too large");
                    bytes.write(buffer, 0, n);
                }
            }
            JSONObject json = new JSONObject(bytes.toString(StandardCharsets.UTF_8.name()));
            long retrieved = System.currentTimeMillis();
            try {
                JSONObject wrapper =
                        new JSONObject().put("retrievedAt", retrieved).put("data", json);
                File temp = File.createTempFile("response_", ".tmp", directory);
                try {
                    Files.write(temp.toPath(), wrapper.toString().getBytes(StandardCharsets.UTF_8));
                    Files.move(temp.toPath(), file.toPath(), StandardCopyOption.REPLACE_EXISTING);
                } finally {
                    temp.delete();
                }
                trim();
            } catch (IOException ignored) {
                /* A cache write failure must not discard a valid response. */
            }
            return new Response(json, retrieved, false);
        } catch (Exception e) {
            if (cached != null
                    && now - cached.retrievedAt >= 0
                    && now - cached.retrievedAt < Math.max(ttl, 86400000L))
                return new Response(cached.json, cached.retrievedAt, true);
            throw e;
        } finally {
            if (connection != null) connection.disconnect();
        }
    }

    private static String key(String value) throws Exception {
        byte[] digest =
                MessageDigest.getInstance("SHA-256").digest(value.getBytes(StandardCharsets.UTF_8));
        StringBuilder b = new StringBuilder();
        for (byte part : digest) b.append(String.format(java.util.Locale.US, "%02x", part & 255));
        return b.toString();
    }

    private void trim() {
        File[] files = directory.listFiles((d, n) -> n.endsWith(".json"));
        if (files == null || files.length <= 64) return;
        Arrays.sort(files, Comparator.comparingLong(File::lastModified));
        for (int i = 0; i < files.length - 64; i++) files[i].delete();
    }

    public static String encode(String s) {
        try {
            return URLEncoder.encode(s, "UTF-8");
        } catch (UnsupportedEncodingException e) {
            throw new AssertionError(e);
        }
    }

    public static String cacheLabel(Response response) {
        return response.cached ? " · cached" + (response.stale ? "; refresh unavailable" : "") : "";
    }
}
