package dev.hugoservice;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.texture.NativeImage;
import net.minecraft.client.texture.NativeImageBackedTexture;
import net.minecraft.util.Identifier;
import net.minecraft.util.Util;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketTimeoutException;
import java.net.URI;
import java.net.URLDecoder;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Base64;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Spotify player: what you are listening to (Spotify Web API, your own app's client id, PKCE login without a
 * client secret) plus time-synced lyrics from LRCLIB. Only the login token is stored, in the mod's config.
 */
public final class Spotify {
    static final int PORT = 43219;
    public static final String REDIRECT = "http://127.0.0.1:" + PORT + "/callback";
    public static final Identifier COVER = Identifier.of("hugoservice", "spotify/cover");
    private static final String SCOPES = "user-read-currently-playing user-read-playback-state user-modify-playback-state";
    private static final String UA = "7auction Styles (https://github.com/KingF3rdi/Service-Styles-Mod)";
    private static final HttpClient HTTP = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(6)).build();
    private static final Gson GSON = new Gson();
    private static final Pattern LRC = Pattern.compile("\\[(\\d+):(\\d+(?:\\.\\d+)?)]");

    public record Track(String id, String name, String artists, String album, long durationMs, String coverUrl) {}
    public record Line(long timeMs, String text) {}

    public static volatile Track track;
    public static volatile boolean playing;
    public static volatile List<Line> lyrics = List.of();
    public static volatile String status = "";
    public static volatile boolean coverReady;
    private static volatile long progressMs, progressAt;
    private static volatile String access;
    private static volatile long accessUntil;
    private static volatile boolean busy;
    private static volatile String lyricsFor = "", coverFor = "";
    private static long lastPoll;

    public static boolean connected() { return !Config.INSTANCE.spotifyRefresh.isBlank(); }

    /** Song position now, moving on between polls while it plays. */
    public static long progress() {
        Track t = track;
        long p = playing ? progressMs + (System.currentTimeMillis() - progressAt) : progressMs;
        return t == null ? p : Math.min(p, t.durationMs());
    }

    /** Index of the lyric line being sung (slightly early, so it reads in time), or -1. */
    public static int lineIndex() {
        List<Line> l = lyrics;
        long now = progress() + 250;
        int lo = 0, hi = l.size() - 1, found = -1;
        while (lo <= hi) {
            int mid = (lo + hi) >>> 1;
            if (l.get(mid).timeMs() <= now) { found = mid; lo = mid + 1; } else hi = mid - 1;
        }
        return found;
    }

    // ================================================================ login (PKCE)

    /** Opens the Spotify login in the browser and waits for its redirect on 127.0.0.1. */
    public static void connect() {
        String clientId = Config.INSTANCE.spotifyClientId.trim();
        if (clientId.isEmpty()) { status = "Enter your Client ID first"; return; }
        String verifier = random(64), state = random(16);
        String challenge = Base64.getUrlEncoder().withoutPadding().encodeToString(sha256(verifier));
        String url = "https://accounts.spotify.com/authorize?response_type=code&client_id=" + enc(clientId)
            + "&scope=" + enc(SCOPES) + "&redirect_uri=" + enc(REDIRECT)
            + "&code_challenge_method=S256&code_challenge=" + challenge + "&state=" + state;
        Thread t = new Thread(() -> {
            try (ServerSocket server = new ServerSocket(PORT, 4, InetAddress.getLoopbackAddress())) {
                server.setSoTimeout(180_000);
                status = "Log in in your browser...";
                Util.getOperatingSystem().open(URI.create(url));
                while (true) {
                    try (Socket s = server.accept()) {
                        String first = new BufferedReader(new InputStreamReader(s.getInputStream(), StandardCharsets.UTF_8)).readLine();
                        if (first == null || !first.startsWith("GET /callback")) { reply(s, 404, "Not found"); continue; }
                        Map<String, String> q = query(first.split(" ")[1]);
                        boolean ok = state.equals(q.get("state")) && q.containsKey("code");
                        reply(s, 200, ok ? "Connected to 7auction Styles. You can close this tab."
                            : "Spotify login failed: " + q.getOrDefault("error", "unknown"));
                        if (!ok) { status = "Login cancelled"; return; }
                        token("grant_type=authorization_code&code=" + enc(q.get("code")) + "&redirect_uri=" + enc(REDIRECT)
                            + "&client_id=" + enc(clientId) + "&code_verifier=" + verifier);
                        return;
                    }
                }
            } catch (SocketTimeoutException e) {
                status = "Login timed out";
            } catch (Exception e) {
                status = "Login failed: " + e.getMessage();
                HugoService.LOG.warn("Spotify login failed", e);
            }
        }, "HugoService-Spotify-Login");
        t.setDaemon(true);
        t.start();
    }

    public static void disconnect() {
        Config.INSTANCE.spotifyRefresh = "";
        Config.save();
        access = null;
        track = null;
        lyrics = List.of();
        status = "Disconnected";
    }

    /** Token request (login or refresh). Blocking, call off the render thread. */
    private static boolean token(String form) throws Exception {
        HttpResponse<String> res = HTTP.send(HttpRequest.newBuilder(URI.create("https://accounts.spotify.com/api/token"))
            .timeout(Duration.ofSeconds(8)).header("Content-Type", "application/x-www-form-urlencoded")
            .POST(HttpRequest.BodyPublishers.ofString(form)).build(), HttpResponse.BodyHandlers.ofString());
        JsonObject json = GSON.fromJson(res.body(), JsonObject.class);
        if (res.statusCode() != 200 || json == null || !json.has("access_token")) {
            if (json != null && "invalid_grant".equals(str(json, "error"))) {
                MinecraftClient.getInstance().execute(Spotify::disconnect);
                status = "Please connect again";
            } else {
                status = "Spotify said " + res.statusCode();
            }
            return false;
        }
        access = json.get("access_token").getAsString();
        accessUntil = System.currentTimeMillis() + (json.has("expires_in") ? json.get("expires_in").getAsLong() : 3600) * 1000 - 30_000;
        String refresh = str(json, "refresh_token");
        if (refresh != null) MinecraftClient.getInstance().execute(() -> { Config.INSTANCE.spotifyRefresh = refresh; Config.save(); });
        status = "Connected";
        return true;
    }

    private static boolean ensureAccess() throws Exception {
        if (access != null && System.currentTimeMillis() < accessUntil) return true;
        String refresh = Config.INSTANCE.spotifyRefresh, id = Config.INSTANCE.spotifyClientId.trim();
        if (refresh.isBlank() || id.isEmpty()) return false;
        return token("grant_type=refresh_token&refresh_token=" + enc(refresh) + "&client_id=" + enc(id));
    }

    // ================================================================ polling

    /** Called every client tick; asks Spotify what plays every 2.5 s while the player is shown. */
    /** True when the player has a source: automatic detection, or a connected Spotify account. */
    public static boolean active() { return auto() || connected(); }

    /** Automatic mode: read what plays from Windows (no login). */
    public static boolean auto() { return !"account".equals(Config.INSTANCE.spotifyMode); }

    static void tick() {
        Config cfg = Config.INSTANCE;
        boolean shown = cfg.spotifyWindow || cfg.spotifySky;
        if (auto()) {
            if (shown) MediaWatcher.ensure(cfg.spotifyAnyApp); else { MediaWatcher.stop(); track = null; }
            return;
        }
        MediaWatcher.stop();
        if (!connected() || !shown || busy) return;
        long now = System.currentTimeMillis();
        if (now - lastPoll < 2500) return;
        lastPoll = now;
        busy = true;
        Thread.ofVirtual().start(() -> {
            try {
                poll();
            } catch (Exception e) {
                HugoService.LOG.debug("Spotify poll failed", e);
            } finally {
                busy = false;
            }
        });
    }

    private static void poll() throws Exception {
        if (!ensureAccess()) return;
        HttpResponse<String> res = HTTP.send(HttpRequest.newBuilder(URI.create("https://api.spotify.com/v1/me/player/currently-playing"))
            .timeout(Duration.ofSeconds(8)).header("Authorization", "Bearer " + access).GET().build(), HttpResponse.BodyHandlers.ofString());
        if (res.statusCode() == 401) { access = null; return; }
        if (res.statusCode() == 204 || res.body().isBlank()) { track = null; playing = false; return; }
        if (res.statusCode() != 200) return;
        JsonObject json = GSON.fromJson(res.body(), JsonObject.class);
        JsonObject item = json.has("item") && json.get("item").isJsonObject() ? json.getAsJsonObject("item") : null;
        if (item == null) { track = null; return; }
        StringBuilder artists = new StringBuilder();
        if (item.has("artists")) for (JsonElement a : item.getAsJsonArray("artists")) {
            artists.append(artists.isEmpty() ? "" : ", ").append(str(a.getAsJsonObject(), "name"));
        }
        String album = "", cover = null;
        if (item.has("album") && item.get("album").isJsonObject()) {
            JsonObject al = item.getAsJsonObject("album");
            album = String.valueOf(str(al, "name"));
            JsonArray images = al.has("images") ? al.getAsJsonArray("images") : new JsonArray();
            if (!images.isEmpty()) cover = str(images.get(images.size() - 1).getAsJsonObject(), "url"); // smallest
        }
        Track t = new Track(String.valueOf(str(item, "id")), String.valueOf(str(item, "name")), artists.toString(), album,
            item.has("duration_ms") ? item.get("duration_ms").getAsLong() : 0, cover);
        progressMs = json.has("progress_ms") && !json.get("progress_ms").isJsonNull() ? json.get("progress_ms").getAsLong() : 0;
        progressAt = System.currentTimeMillis();
        playing = json.has("is_playing") && json.get("is_playing").getAsBoolean();
        track = t;
        if (!t.id().equals(lyricsFor)) {
            lyricsFor = t.id();
            lyrics = List.of();
            Thread.ofVirtual().start(() -> {
                List<Line> found = fetchLyrics(t);
                if (t.id().equals(lyricsFor)) lyrics = found;
            });
        }
        if (t.coverUrl() != null && !t.coverUrl().equals(coverFor)) {
            coverFor = t.coverUrl();
            loadCover(t.coverUrl());
        }
    }

    /** Synced lyrics from LRCLIB (free, no account), empty when the song has none; asks again while LRCLIB is busy. */
    static List<Line> fetchLyrics(Track t) {
        for (int attempt = 0; attempt < 4; attempt++) {
            if (attempt > 0) {
                try { Thread.sleep(4000L * attempt); } catch (InterruptedException e) { return List.of(); }
                if (!t.id().equals(lyricsFor) && !lyricsFor.isEmpty()) return List.of(); // song changed meanwhile
            }
            try {
                String synced = findLyrics(t);
                return synced == null ? List.of() : parseLrc(synced);
            } catch (java.io.IOException e) {
                HugoService.LOG.debug("LRCLIB busy or offline for {}, trying again", t.name(), e);
            } catch (Exception e) {
                HugoService.LOG.debug("No lyrics for {}", t.name(), e);
                return List.of();
            }
        }
        return List.of();
    }

    /**
     * Exact match first; then a free search, because Spotify writes "Song - Artist Remix" where LRCLIB has
     * "Song (Artist Remix)"; then the bare title. Searches only take a version of the same length, else lines run off.
     */
    private static String findLyrics(Track t) throws Exception {
        String artist = t.artists().split(",")[0].trim();
        long dur = Math.round(t.durationMs() / 1000.0);
        HttpResponse<String> exact = lrclib("/api/get?track_name=" + enc(t.name()) + "&artist_name=" + enc(artist)
            + "&album_name=" + enc(t.album()) + "&duration=" + dur);
        if (exact.statusCode() == 200) {
            String s = str(GSON.fromJson(exact.body(), JsonObject.class), "syncedLyrics");
            if (s != null) return s;
        }
        String s = closest(lrclib("/api/search?q=" + enc(artist + " " + t.name().replaceAll("[()\\[\\]]|\\s-\\s", " "))), dur);
        if (s != null) return s;
        String bare = t.name().replaceAll("\\s*[(\\[].*?[)\\]]", "").replaceAll("\\s+-\\s.*$", "").trim();
        return closest(lrclib("/api/search?track_name=" + enc(bare) + "&artist_name=" + enc(artist)), dur);
    }

    /** One LRCLIB request; a busy or failing server becomes an IOException so the caller tries again later. */
    private static HttpResponse<String> lrclib(String path) throws Exception {
        HttpResponse<String> res = HTTP.send(HttpRequest.newBuilder(URI.create("https://lrclib.net" + path))
            .timeout(Duration.ofSeconds(8)).header("User-Agent", UA).GET().build(), HttpResponse.BodyHandlers.ofString());
        if (res.statusCode() == 429 || res.statusCode() >= 500) throw new java.io.IOException("LRCLIB " + res.statusCode());
        return res;
    }

    /** Synced lyrics of the search hit whose length is closest to the song (within 8 s), or null. */
    static String closest(HttpResponse<String> res, long dur) {
        if (res.statusCode() != 200) return null;
        String best = null;
        double bestDiff = 8.5;
        for (JsonElement e : GSON.fromJson(res.body(), JsonArray.class)) {
            JsonObject o = e.getAsJsonObject();
            String s = str(o, "syncedLyrics");
            if (s == null || !o.has("duration") || o.get("duration").isJsonNull()) continue;
            double diff = dur <= 0 ? 0 : Math.abs(o.get("duration").getAsDouble() - dur);
            if (diff < bestDiff) { bestDiff = diff; best = s; }
        }
        return best;
    }

    static List<Line> parseLrc(String lrc) {
        List<Line> out = new ArrayList<>();
        for (String raw : lrc.split("\\R")) {
            Matcher m = LRC.matcher(raw);
            int end = 0;
            List<Long> times = new ArrayList<>();
            while (m.find() && m.start() == end) {
                times.add(Long.parseLong(m.group(1)) * 60_000 + Math.round(Double.parseDouble(m.group(2)) * 1000));
                end = m.end();
            }
            String text = raw.substring(end).trim();
            for (long time : times) out.add(new Line(time, text));
        }
        out.sort((a, b) -> Long.compare(a.timeMs(), b.timeMs()));
        return List.copyOf(out);
    }

    /** One update from the Windows media watcher (automatic mode). Called on its reader thread. */
    static void fromSystem(JsonObject j) {
        if (!auto()) return;
        String title = str(j, "title");
        if (title == null || title.isBlank()) { track = null; playing = false; return; }
        String artist = String.valueOf(str(j, "artist")), album = str(j, "album");
        long dur = j.has("dur") ? j.get("dur").getAsLong() : 0;
        long pos = j.has("pos") ? j.get("pos").getAsLong() : 0;
        boolean play = j.has("playing") && j.get("playing").getAsBoolean();
        // the player stamps its position only now and then: move it on to the moment of this sample
        long upd = j.has("upd") ? j.get("upd").getAsLong() : 0, sample = j.has("now") ? j.get("now").getAsLong() : upd;
        long position = pos + (play && upd > 0 ? Math.max(0, sample - upd) : 0);
        String id = artist + "|" + title;
        progressMs = dur > 0 ? Math.min(position, dur) : position;
        progressAt = System.currentTimeMillis();
        playing = play;
        Track t = new Track(id, title, artist, album == null ? "" : album, dur, null);
        boolean changed = track == null || !track.id().equals(id);
        track = t;
        if (!id.equals(lyricsFor)) {
            lyricsFor = id;
            lyrics = List.of();
            if (changed) coverReady = false;
            Thread.ofVirtual().start(() -> {
                List<Line> found = fetchLyrics(t);
                if (id.equals(lyricsFor)) lyrics = found;
            });
        }
        String cover = str(j, "cover");
        if (cover != null && !cover.isEmpty()) {
            try {
                BufferedImage img = ImageIO.read(java.nio.file.Path.of(cover).toFile());
                if (img != null) showCover(Images.encode(img));
            } catch (Exception e) {
                HugoService.LOG.debug("Cover file failed", e);
            }
        }
    }

    /** Registers PNG bytes as the cover texture (render thread). */
    private static void showCover(byte[] png) {
        MinecraftClient mc = MinecraftClient.getInstance();
        mc.execute(() -> {
            try {
                NativeImage image = NativeImage.read(new ByteArrayInputStream(png));
                mc.getTextureManager().registerTexture(COVER, new NativeImageBackedTexture(() -> "spotify cover", image));
                coverReady = true;
            } catch (Exception e) {
                HugoService.LOG.debug("Cover failed", e);
            }
        });
    }

    private static void loadCover(String url) {
        try {
            HttpResponse<byte[]> res = HTTP.send(HttpRequest.newBuilder(URI.create(url)).timeout(Duration.ofSeconds(8)).GET().build(),
                HttpResponse.BodyHandlers.ofByteArray());
            BufferedImage img = res.statusCode() == 200 ? ImageIO.read(new ByteArrayInputStream(res.body())) : null;
            if (img == null) return;
            showCover(Images.encode(img)); // covers are JPEG, NativeImage reads PNG
        } catch (Exception e) {
            HugoService.LOG.debug("Cover download failed", e);
        }
    }

    // ================================================================ controls (Spotify Premium)

    public static void playPause() {
        if (auto()) MediaWatcher.command("toggle"); else control("PUT", playing ? "pause" : "play");
        progressMs = progress();
        progressAt = System.currentTimeMillis();
        playing = !playing;
    }
    public static void next() { if (auto()) MediaWatcher.command("next"); else control("POST", "next"); }
    public static void previous() { if (auto()) MediaWatcher.command("prev"); else control("POST", "previous"); }

    private static void control(String method, String what) {
        if (!connected()) return;
        Thread.ofVirtual().start(() -> {
            try {
                if (!ensureAccess()) return;
                HttpResponse<String> res = HTTP.send(HttpRequest.newBuilder(URI.create("https://api.spotify.com/v1/me/player/" + what))
                    .timeout(Duration.ofSeconds(8)).header("Authorization", "Bearer " + access)
                    .method(method, HttpRequest.BodyPublishers.noBody()).build(), HttpResponse.BodyHandlers.ofString());
                if (res.statusCode() == 403) status = "Controls need Spotify Premium";
                else if (res.statusCode() == 404) status = "Open Spotify on a device first";
                lastPoll = 0; // show the change soon
            } catch (Exception e) {
                HugoService.LOG.debug("Spotify control failed", e);
            }
        });
    }

    // ================================================================ helpers

    private static void reply(Socket s, int code, String message) throws Exception {
        String safe = message.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;");
        String body = "<!doctype html><meta charset=utf-8><title>7auction Styles</title>"
            + "<body style=\"font-family:sans-serif;background:#0b1522;color:#f2f8ff;display:grid;place-items:center;height:100vh;margin:0\">"
            + "<p>" + safe + "</p></body>";
        byte[] b = body.getBytes(StandardCharsets.UTF_8);
        OutputStream out = s.getOutputStream();
        out.write(("HTTP/1.1 " + code + (code == 200 ? " OK" : " Not Found") + "\r\nContent-Type: text/html; charset=utf-8\r\nContent-Length: "
            + b.length + "\r\nConnection: close\r\n\r\n").getBytes(StandardCharsets.US_ASCII));
        out.write(b);
        out.flush();
    }

    private static Map<String, String> query(String target) {
        Map<String, String> out = new HashMap<>();
        int q = target.indexOf('?');
        if (q < 0) return out;
        for (String pair : target.substring(q + 1).split("&")) {
            int eq = pair.indexOf('=');
            if (eq > 0) out.put(URLDecoder.decode(pair.substring(0, eq), StandardCharsets.UTF_8), URLDecoder.decode(pair.substring(eq + 1), StandardCharsets.UTF_8));
        }
        return out;
    }

    private static String str(JsonObject o, String key) {
        return o != null && o.has(key) && !o.get(key).isJsonNull() ? o.get(key).getAsString() : null;
    }

    private static String enc(String s) { return URLEncoder.encode(s, StandardCharsets.UTF_8); }

    private static String random(int length) {
        String chars = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789";
        SecureRandom r = new SecureRandom();
        StringBuilder sb = new StringBuilder(length);
        for (int i = 0; i < length; i++) sb.append(chars.charAt(r.nextInt(chars.length())));
        return sb.toString();
    }

    private static byte[] sha256(String s) {
        try {
            return MessageDigest.getInstance("SHA-256").digest(s.getBytes(StandardCharsets.US_ASCII));
        } catch (Exception e) {
            throw new IllegalStateException(e);
        }
    }

    private Spotify() {}
}
