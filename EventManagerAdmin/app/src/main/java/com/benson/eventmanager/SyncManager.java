package com.benson.eventmanager;

import android.content.Context;
import android.content.SharedPreferences;
import android.net.ConnectivityManager;
import android.net.Network;
import android.net.NetworkCapabilities;
import android.os.Handler;
import android.os.Looper;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.UUID;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public final class SyncManager {
    public interface Callback { void done(String message, boolean success); }
    private static final ExecutorService WORKER = Executors.newSingleThreadExecutor();
    private static final Handler MAIN = new Handler(Looper.getMainLooper());
    private static final String PREFS = "cloud_sync";
    private SyncManager() {}

    public static String cloudUrl(Context context) {
        return context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
                .getString("cloud_url", context.getString(R.string.cloud_base_url));
    }

    public static boolean isConfigured(Context context) {
        return !context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
                .getString("cloud_token", "").isEmpty();
    }

    public static void configure(Context context, String url, String token) {
        SharedPreferences.Editor editor = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
                .edit().putString("cloud_url", url.trim().replaceAll("/+$", ""));
        if (!token.isEmpty()) editor.putString("cloud_token", token);
        editor.remove("uploaded_revision").apply();
    }

    public static void markDirty(Context context) {
        SharedPreferences p = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE);
        p.edit().putLong("revision", p.getLong("revision", 0) + 1).apply();
    }

    public static void request(Context context, Callback callback) {
        Context app = context.getApplicationContext();
        WORKER.execute(() -> {
            String result;
            boolean success = false;
            try {
                SharedPreferences p = app.getSharedPreferences(PREFS, Context.MODE_PRIVATE);
                long revision = p.getLong("revision", 0);
                if (callback == null && (!isConfigured(app) || revision == p.getLong("uploaded_revision", -1))) return;
                if (!hasNetwork(app)) throw new IllegalStateException("No network connection. Data remains on this device; retry when online.");
                String base = cloudUrl(app).trim().replaceAll("/+$", "");
                String token = p.getString("cloud_token", "");
                if (!base.startsWith("https://") || token.isEmpty()) throw new IllegalStateException("Open Cloud settings and enter a HTTPS URL and upload token.");
                String deviceId = p.getString("device_id", null);
                if (deviceId == null) {
                    deviceId = UUID.randomUUID().toString();
                    p.edit().putString("device_id", deviceId).commit();
                }
                JSONObject payload = snapshot(app, deviceId);
                HttpURLConnection conn = (HttpURLConnection) new URL(base + "/api/snapshot").openConnection();
                try {
                    conn.setRequestMethod("PUT");
                    conn.setConnectTimeout(8000); conn.setReadTimeout(15000);
                    conn.setDoOutput(true);
                    conn.setRequestProperty("Content-Type", "application/json; charset=utf-8");
                    conn.setRequestProperty("Authorization", "Bearer " + token);
                    byte[] bytes = payload.toString().getBytes(StandardCharsets.UTF_8);
                    conn.setFixedLengthStreamingMode(bytes.length);
                    try (OutputStream out = conn.getOutputStream()) { out.write(bytes); }
                    int status = conn.getResponseCode();
                    if (status < 200 || status >= 300) throw new IllegalStateException("Upload failed (HTTP " + status + "): " + read(conn.getErrorStream()));
                    if (p.getLong("revision", 0) == revision) p.edit().putLong("uploaded_revision", revision).apply();
                    success = true;
                    result = "Uploaded " + payload.getJSONArray("events").length() + " events and " + payload.getJSONArray("sessions").length() + " sessions.";
                } finally { conn.disconnect(); }
            } catch (Exception ex) {
                result = ex.getMessage() == null ? "Upload failed. Please retry." : ex.getMessage();
            }
            if (callback != null) {
                String message = result; boolean ok = success;
                MAIN.post(() -> callback.done(message, ok));
            }
        });
    }

    private static boolean hasNetwork(Context c) {
        ConnectivityManager cm = (ConnectivityManager) c.getSystemService(Context.CONNECTIVITY_SERVICE);
        if (cm == null) return false;
        Network network = cm.getActiveNetwork();
        NetworkCapabilities caps = cm.getNetworkCapabilities(network);
        return caps != null && caps.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET);
    }

    private static String read(InputStream in) throws Exception {
        if (in == null) return "No server details";
        StringBuilder s = new StringBuilder();
        try (BufferedReader r = new BufferedReader(new InputStreamReader(in, StandardCharsets.UTF_8))) {
            String line; while ((line = r.readLine()) != null && s.length() < 300) s.append(line);
        }
        return s.toString();
    }

    private static JSONObject snapshot(Context c, String deviceId) throws Exception {
        DBManager db = new DBManager(c);
        JSONArray events = new JSONArray(), sessions = new JSONArray();
        for (Event e : db.listEvents()) {
            JSONObject o = new JSONObject();
            o.put("id", e.getId()); o.put("name", e.getName()); o.put("date", e.getDate());
            o.put("location", e.getLocation()); o.put("organiser", e.getOrganiser());
            o.put("type", e.getType()); o.put("status", e.getStatus());
            o.put("description", e.getDescription()); o.put("contactEmail", e.getContactEmail());
            o.put("contactPhone", e.getContactPhone()); events.put(o);
        }
        for (Session s : db.listAllSessions()) {
            JSONObject o = new JSONObject();
            o.put("id", s.getId()); o.put("eventId", s.getEventId()); o.put("name", s.getName());
            o.put("time", s.getTime()); o.put("speaker", s.getSpeaker());
            o.put("room", s.getRoom()); o.put("type", s.getType()); o.put("description", s.getDescription());
            sessions.put(o);
        }
        return new JSONObject().put("deviceId", deviceId).put("events", events).put("sessions", sessions);
    }
}
