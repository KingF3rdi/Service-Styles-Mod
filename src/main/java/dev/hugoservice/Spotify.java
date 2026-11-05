/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.google.gson.Gson
 *  com.google.gson.JsonArray
 *  com.google.gson.JsonElement
 *  com.google.gson.JsonObject
 *  net.minecraft.client.texture.NativeImage
 *  net.minecraft.client.texture.NativeImageBackedTexture
 *  net.minecraft.client.texture.AbstractTexture
 *  net.minecraft.util.Util
 *  net.minecraft.util.Identifier
 *  net.minecraft.client.MinecraftClient
 */
package dev.hugoservice;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import dev.hugoservice.Config;
import dev.hugoservice.HugoService;
import dev.hugoservice.Images;
import dev.hugoservice.MediaWatcher;
import java.awt.image.BufferedImage;
import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
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
import java.nio.file.Path;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Base64;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import javax.imageio.ImageIO;
import net.minecraft.client.texture.NativeImage;
import net.minecraft.client.texture.NativeImageBackedTexture;
import net.minecraft.client.texture.AbstractTexture;
import net.minecraft.util.Util;
import net.minecraft.util.Identifier;
import net.minecraft.client.MinecraftClient;

public final class Spotify {
    static final int PORT = 43219;
    public static final String REDIRECT = "http://127.0.0.1:43219/callback";
    public static final Identifier COVER = Identifier.of((String)"hugoservice", (String)"spotify/cover");
    private static final String SCOPES = "user-read-currently-playing user-read-playback-state user-modify-playback-state";
    private static final String UA = "7auction Styles";
    private static final HttpClient HTTP = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(6L)).build();
    private static final Gson GSON = new Gson();
    private static final Pattern LRC = Pattern.compile("\\[(\\d+):(\\d+(?:\\.\\d+)?)]");
    public static volatile Track track;
    public static volatile boolean playing;
    public static volatile List<Line> lyrics;
    public static volatile String status;
    public static volatile boolean coverReady;
    private static volatile long progressMs;
    private static volatile long progressAt;
    private static volatile String access;
    private static volatile long accessUntil;
    private static volatile boolean busy;
    private static volatile String lyricsFor;
    private static volatile String coverFor;
    private static long lastPoll;

    public static boolean connected() {
        return !Config.INSTANCE.spotifyRefresh.isBlank();
    }

    public static long progress() {
        Track t = track;
        long p = playing ? progressMs + (System.currentTimeMillis() - progressAt) : progressMs;
        return t == null ? p : Math.min(p, t.durationMs());
    }

    public static int lineIndex() {
        List<Line> l = lyrics;
        long now = Spotify.progress() + 250L;
        int lo = 0;
        int hi = l.size() - 1;
        int found = -1;
        while (lo <= hi) {
            int mid = lo + hi >>> 1;
            if (l.get(mid).timeMs() <= now) {
                found = mid;
                lo = mid + 1;
                continue;
            }
            hi = mid - 1;
        }
        return found;
    }

    public static void connect() {
        String clientId = Config.INSTANCE.spotifyClientId.trim();
        if (clientId.isEmpty()) {
            status = "Enter your Client ID first";
            return;
        }
        String verifier = Spotify.random(64);
        String state = Spotify.random(16);
        String challenge = Base64.getUrlEncoder().withoutPadding().encodeToString(Spotify.sha256(verifier));
        String url = "https://accounts.spotify.com/authorize?response_type=code&client_id=" + Spotify.enc(clientId) + "&scope=" + Spotify.enc(SCOPES) + "&redirect_uri=" + Spotify.enc(REDIRECT) + "&code_challenge_method=S256&code_challenge=" + challenge + "&state=" + state;
        Thread t = new Thread(() -> {
            try (ServerSocket server = new ServerSocket(43219, 4, InetAddress.getLoopbackAddress());){
                block20: {
                    Map<String, String> q;
                    Socket s;
                    block18: {
                        block19: {
                            String first;
                            server.setSoTimeout(180000);
                            status = "Log in in your browser...";
                            Util.getOperatingSystem().open(URI.create(url));
                            while (true) {
                                s = server.accept();
                                first = new BufferedReader(new InputStreamReader(s.getInputStream(), StandardCharsets.UTF_8)).readLine();
                                if (first != null && first.startsWith("GET /callback")) break;
                                Spotify.reply(s, 404, "Not found");
                                if (s == null) continue;
                                s.close();
                            }
                            try {
                                q = Spotify.query(first.split(" ")[1]);
                                boolean ok = state.equals(q.get("state")) && q.containsKey("code");
                                Spotify.reply(s, 200, (String)(ok ? "Connected to 7auction Styles. You can close this tab." : "Spotify login failed: " + q.getOrDefault("error", "unknown")));
                                if (ok) break block18;
                                status = "Login cancelled";
                                if (s == null) break block19;
                            }
                            catch (Throwable throwable) {
                                if (s != null) {
                                    try {
                                        s.close();
                                    }
                                    catch (Throwable throwable2) {
                                        throwable.addSuppressed(throwable2);
                                    }
                                }
                                throw throwable;
                            }
                            s.close();
                        }
                        return;
                    }
                    Spotify.token("grant_type=authorization_code&code=" + Spotify.enc(q.get("code")) + "&redirect_uri=" + Spotify.enc(REDIRECT) + "&client_id=" + Spotify.enc(clientId) + "&code_verifier=" + verifier);
                    if (s == null) break block20;
                    s.close();
                }
                return;
            }
            catch (SocketTimeoutException e) {
                status = "Login timed out";
            }
            catch (Exception e) {
                status = "Login failed: " + e.getMessage();
                HugoService.LOG.warn("Spotify login failed", (Throwable)e);
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

    private static boolean token(String form) throws Exception {
        HttpResponse<String> res = HTTP.send(HttpRequest.newBuilder(URI.create("https://accounts.spotify.com/api/token")).timeout(Duration.ofSeconds(8L)).header("Content-Type", "application/x-www-form-urlencoded").POST(HttpRequest.BodyPublishers.ofString(form)).build(), HttpResponse.BodyHandlers.ofString());
        JsonObject json = (JsonObject)GSON.fromJson(res.body(), JsonObject.class);
        if (res.statusCode() != 200 || json == null || !json.has("access_token")) {
            if (json != null && "invalid_grant".equals(Spotify.str(json, "error"))) {
                MinecraftClient.getInstance().execute(Spotify::disconnect);
                status = "Please connect again";
            } else {
                status = "Spotify said " + res.statusCode();
            }
            return false;
        }
        access = json.get("access_token").getAsString();
        accessUntil = System.currentTimeMillis() + (json.has("expires_in") ? json.get("expires_in").getAsLong() : 3600L) * 1000L - 30000L;
        String refresh = Spotify.str(json, "refresh_token");
        if (refresh != null) {
            MinecraftClient.getInstance().execute(() -> {
                Config.INSTANCE.spotifyRefresh = refresh;
                Config.save();
            });
        }
        status = "Connected";
        return true;
    }

    private static boolean ensureAccess() throws Exception {
        if (access != null && System.currentTimeMillis() < accessUntil) {
            return true;
        }
        String refresh = Config.INSTANCE.spotifyRefresh;
        String id = Config.INSTANCE.spotifyClientId.trim();
        if (refresh.isBlank() || id.isEmpty()) {
            return false;
        }
        return Spotify.token("grant_type=refresh_token&refresh_token=" + Spotify.enc(refresh) + "&client_id=" + Spotify.enc(id));
    }

    public static boolean active() {
        return Spotify.auto() || Spotify.connected();
    }

    public static boolean auto() {
        return !"account".equals(Config.INSTANCE.spotifyMode);
    }

    static void tick() {
        boolean shown;
        Config cfg = Config.INSTANCE;
        boolean bl = shown = cfg.spotifyWindow || cfg.spotifySky;
        if (Spotify.auto()) {
            if (shown) {
                MediaWatcher.ensure(cfg.spotifyAnyApp);
            } else {
                MediaWatcher.stop();
                track = null;
            }
            return;
        }
        MediaWatcher.stop();
        if (!Spotify.connected() || !shown || busy) {
            return;
        }
        long now = System.currentTimeMillis();
        if (now - lastPoll < 2500L) {
            return;
        }
        lastPoll = now;
        busy = true;
        Thread.ofVirtual().start(() -> {
            try {
                Spotify.poll();
            }
            catch (Exception e) {
                HugoService.LOG.debug("Spotify poll failed", (Throwable)e);
            }
            finally {
                busy = false;
            }
        });
    }

    private static void poll() throws Exception {
        JsonObject item;
        if (!Spotify.ensureAccess()) {
            return;
        }
        HttpResponse<String> res = HTTP.send(HttpRequest.newBuilder(URI.create("https://api.spotify.com/v1/me/player/currently-playing")).timeout(Duration.ofSeconds(8L)).header("Authorization", "Bearer " + access).GET().build(), HttpResponse.BodyHandlers.ofString());
        if (res.statusCode() == 401) {
            access = null;
            return;
        }
        if (res.statusCode() == 204 || res.body().isBlank()) {
            track = null;
            playing = false;
            return;
        }
        if (res.statusCode() != 200) {
            return;
        }
        JsonObject json = (JsonObject)GSON.fromJson(res.body(), JsonObject.class);
        JsonObject jsonObject = item = json.has("item") && json.get("item").isJsonObject() ? json.getAsJsonObject("item") : null;
        if (item == null) {
            track = null;
            return;
        }
        StringBuilder artists = new StringBuilder();
        if (item.has("artists")) {
            for (JsonElement a : item.getAsJsonArray("artists")) {
                artists.append(artists.isEmpty() ? "" : ", ").append(Spotify.str(a.getAsJsonObject(), "name"));
            }
        }
        String album = "";
        String cover = null;
        if (item.has("album") && item.get("album").isJsonObject()) {
            JsonArray images;
            JsonObject al = item.getAsJsonObject("album");
            album = String.valueOf(Spotify.str(al, "name"));
            JsonArray jsonArray = images = al.has("images") ? al.getAsJsonArray("images") : new JsonArray();
            if (!images.isEmpty()) {
                cover = Spotify.str(images.get(images.size() - 1).getAsJsonObject(), "url");
            }
        }
        Track t = new Track(String.valueOf(Spotify.str(item, "id")), String.valueOf(Spotify.str(item, "name")), artists.toString(), album, item.has("duration_ms") ? item.get("duration_ms").getAsLong() : 0L, cover);
        progressMs = json.has("progress_ms") && !json.get("progress_ms").isJsonNull() ? json.get("progress_ms").getAsLong() : 0L;
        progressAt = System.currentTimeMillis();
        playing = json.has("is_playing") && json.get("is_playing").getAsBoolean();
        track = t;
        if (!t.id().equals(lyricsFor)) {
            lyricsFor = t.id();
            lyrics = List.of();
            Thread.ofVirtual().start(() -> {
                List<Line> found = Spotify.fetchLyrics(t);
                if (t.id().equals(lyricsFor)) {
                    lyrics = found;
                }
            });
        }
        if (t.coverUrl() != null && !t.coverUrl().equals(coverFor)) {
            coverFor = t.coverUrl();
            Spotify.loadCover(t.coverUrl());
        }
    }

    static List<Line> fetchLyrics(Track t) {
        for (int attempt = 0; attempt < 4; ++attempt) {
            if (attempt > 0) {
                try {
                    Thread.sleep(4000L * (long)attempt);
                }
                catch (InterruptedException e) {
                    return List.of();
                }
                if (!t.id().equals(lyricsFor) && !lyricsFor.isEmpty()) {
                    return List.of();
                }
            }
            try {
                String synced = Spotify.findLyrics(t);
                return synced == null ? List.of() : Spotify.parseLrc(synced);
            }
            catch (IOException e) {
                HugoService.LOG.debug("LRCLIB busy or offline for {}, trying again", t.name(), e);
                continue;
            }
            catch (Exception e) {
                HugoService.LOG.debug("No lyrics for {}", t.name(), e);
                return List.of();
            }
        }
        return List.of();
    }

    private static String findLyrics(Track t) throws Exception {
        String s;
        String artist = t.artists().split(",")[0].trim();
        long dur = Math.round((double)t.durationMs() / 1000.0);
        HttpResponse<String> exact = Spotify.lrclib("/api/get?track_name=" + Spotify.enc(t.name()) + "&artist_name=" + Spotify.enc(artist) + "&album_name=" + Spotify.enc(t.album()) + "&duration=" + dur);
        if (exact.statusCode() == 200 && (s = Spotify.str((JsonObject)GSON.fromJson(exact.body(), JsonObject.class), "syncedLyrics")) != null) {
            return s;
        }
        s = Spotify.closest(Spotify.lrclib("/api/search?q=" + Spotify.enc(artist + " " + t.name().replaceAll("[()\\[\\]]|\\s-\\s", " "))), dur);
        if (s != null) {
            return s;
        }
        String bare = t.name().replaceAll("\\s*[(\\[].*?[)\\]]", "").replaceAll("\\s+-\\s.*$", "").trim();
        return Spotify.closest(Spotify.lrclib("/api/search?track_name=" + Spotify.enc(bare) + "&artist_name=" + Spotify.enc(artist)), dur);
    }

    private static HttpResponse<String> lrclib(String path) throws Exception {
        HttpResponse<String> res = HTTP.send(HttpRequest.newBuilder(URI.create("https://lrclib.net" + path)).timeout(Duration.ofSeconds(8L)).header("User-Agent", UA).GET().build(), HttpResponse.BodyHandlers.ofString());
        if (res.statusCode() == 429 || res.statusCode() >= 500) {
            throw new IOException("LRCLIB " + res.statusCode());
        }
        return res;
    }

    static String closest(HttpResponse<String> res, long dur) {
        if (res.statusCode() != 200) {
            return null;
        }
        String best = null;
        double bestDiff = 8.5;
        for (JsonElement e : (JsonArray)GSON.fromJson(res.body(), JsonArray.class)) {
            JsonObject o = e.getAsJsonObject();
            String s = Spotify.str(o, "syncedLyrics");
            if (s == null || !o.has("duration") || o.get("duration").isJsonNull()) continue;
            double d = dur <= 0L ? 0.0 : Math.abs(o.get("duration").getAsDouble() - (double)dur);
            double diff = d;
            if (!(diff < bestDiff)) continue;
            bestDiff = diff;
            best = s;
        }
        return best;
    }

    static List<Line> parseLrc(String lrc) {
        ArrayList<Line> out = new ArrayList<Line>();
        for (String raw : lrc.split("\\R")) {
            Matcher m = LRC.matcher(raw);
            int end = 0;
            ArrayList<Long> times = new ArrayList<Long>();
            while (m.find() && m.start() == end) {
                times.add(Long.parseLong(m.group(1)) * 60000L + Math.round(Double.parseDouble(m.group(2)) * 1000.0));
                end = m.end();
            }
            String text = raw.substring(end).trim();
            Iterator iterator = times.iterator();
            while (iterator.hasNext()) {
                long time = (Long)iterator.next();
                out.add(new Line(time, text));
            }
        }
        out.sort((a, b) -> Long.compare(a.timeMs(), b.timeMs()));
        return List.copyOf(out);
    }

    static void fromSystem(JsonObject j) {
        String cover;
        if (!Spotify.auto()) {
            return;
        }
        String title = Spotify.str(j, "title");
        if (title == null || title.isBlank()) {
            track = null;
            playing = false;
            return;
        }
        String artist = String.valueOf(Spotify.str(j, "artist"));
        String album = Spotify.str(j, "album");
        long dur = j.has("dur") ? j.get("dur").getAsLong() : 0L;
        long pos = j.has("pos") ? j.get("pos").getAsLong() : 0L;
        boolean play = j.has("playing") && j.get("playing").getAsBoolean();
        long upd = j.has("upd") ? j.get("upd").getAsLong() : 0L;
        long sample = j.has("now") ? j.get("now").getAsLong() : upd;
        long position = pos + (play && upd > 0L ? Math.max(0L, sample - upd) : 0L);
        String id = artist + "|" + title;
        progressMs = dur > 0L ? Math.min(position, dur) : position;
        progressAt = System.currentTimeMillis();
        playing = play;
        Track t = new Track(id, title, artist, album == null ? "" : album, dur, null);
        boolean changed = track == null || !track.id().equals(id);
        track = t;
        if (!id.equals(lyricsFor)) {
            lyricsFor = id;
            lyrics = List.of();
            if (changed) {
                coverReady = false;
            }
            Thread.ofVirtual().start(() -> {
                List<Line> found = Spotify.fetchLyrics(t);
                if (id.equals(lyricsFor)) {
                    lyrics = found;
                }
            });
        }
        if ((cover = Spotify.str(j, "cover")) != null && !cover.isEmpty()) {
            try {
                BufferedImage img = ImageIO.read(Path.of(cover, new String[0]).toFile());
                if (img != null) {
                    Spotify.showCover(Images.encode(img));
                }
            }
            catch (Exception e) {
                HugoService.LOG.debug("Cover file failed", (Throwable)e);
            }
        }
    }

    private static void showCover(byte[] png) {
        MinecraftClient mc = MinecraftClient.getInstance();
        mc.execute(() -> {
            try {
                NativeImage image = NativeImage.read((InputStream)new ByteArrayInputStream(png));
                mc.getTextureManager().registerTexture(COVER, (AbstractTexture)new NativeImageBackedTexture(() -> "spotify cover", image));
                coverReady = true;
            }
            catch (Exception e) {
                HugoService.LOG.debug("Cover failed", (Throwable)e);
            }
        });
    }

    private static void loadCover(String url) {
        try {
            BufferedImage img;
            HttpResponse<byte[]> res = HTTP.send(HttpRequest.newBuilder(URI.create(url)).timeout(Duration.ofSeconds(8L)).GET().build(), HttpResponse.BodyHandlers.ofByteArray());
            BufferedImage bufferedImage = img = res.statusCode() == 200 ? ImageIO.read(new ByteArrayInputStream(res.body())) : null;
            if (img == null) {
                return;
            }
            Spotify.showCover(Images.encode(img));
        }
        catch (Exception e) {
            HugoService.LOG.debug("Cover download failed", (Throwable)e);
        }
    }

    public static void playPause() {
        if (Spotify.auto()) {
            MediaWatcher.command("toggle");
        } else {
            Spotify.control("PUT", playing ? "pause" : "play");
        }
        progressMs = Spotify.progress();
        progressAt = System.currentTimeMillis();
        playing = !playing;
    }

    public static void next() {
        if (Spotify.auto()) {
            MediaWatcher.command("next");
        } else {
            Spotify.control("POST", "next");
        }
    }

    public static void previous() {
        if (Spotify.auto()) {
            MediaWatcher.command("prev");
        } else {
            Spotify.control("POST", "previous");
        }
    }

    private static void control(String method, String what) {
        if (!Spotify.connected()) {
            return;
        }
        Thread.ofVirtual().start(() -> {
            try {
                if (!Spotify.ensureAccess()) {
                    return;
                }
                HttpResponse<String> res = HTTP.send(HttpRequest.newBuilder(URI.create("https://api.spotify.com/v1/me/player/" + what)).timeout(Duration.ofSeconds(8L)).header("Authorization", "Bearer " + access).method(method, HttpRequest.BodyPublishers.noBody()).build(), HttpResponse.BodyHandlers.ofString());
                if (res.statusCode() == 403) {
                    status = "Controls need Spotify Premium";
                } else if (res.statusCode() == 404) {
                    status = "Open Spotify on a device first";
                }
                lastPoll = 0L;
            }
            catch (Exception e) {
                HugoService.LOG.debug("Spotify control failed", (Throwable)e);
            }
        });
    }

    private static void reply(Socket s, int code, String message) throws Exception {
        String safe = message.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;");
        String body = "<!doctype html><meta charset=utf-8><title>7auction Styles</title><body style=\"font-family:sans-serif;background:#0b1522;color:#f2f8ff;display:grid;place-items:center;height:100vh;margin:0\"><p>" + safe + "</p></body>";
        byte[] b = body.getBytes(StandardCharsets.UTF_8);
        OutputStream out = s.getOutputStream();
        out.write(("HTTP/1.1 " + code + (code == 200 ? " OK" : " Not Found") + "\r\nContent-Type: text/html; charset=utf-8\r\nContent-Length: " + b.length + "\r\nConnection: close\r\n\r\n").getBytes(StandardCharsets.US_ASCII));
        out.write(b);
        out.flush();
    }

    private static Map<String, String> query(String target) {
        HashMap<String, String> out = new HashMap<String, String>();
        int q = target.indexOf(63);
        if (q < 0) {
            return out;
        }
        for (String pair : target.substring(q + 1).split("&")) {
            int eq = pair.indexOf(61);
            if (eq <= 0) continue;
            out.put(URLDecoder.decode(pair.substring(0, eq), StandardCharsets.UTF_8), URLDecoder.decode(pair.substring(eq + 1), StandardCharsets.UTF_8));
        }
        return out;
    }

    private static String str(JsonObject o, String key) {
        return o != null && o.has(key) && !o.get(key).isJsonNull() ? o.get(key).getAsString() : null;
    }

    private static String enc(String s) {
        return URLEncoder.encode(s, StandardCharsets.UTF_8);
    }

    private static String random(int length) {
        String chars = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789";
        SecureRandom r = new SecureRandom();
        StringBuilder sb = new StringBuilder(length);
        for (int i = 0; i < length; ++i) {
            sb.append(chars.charAt(r.nextInt(chars.length())));
        }
        return sb.toString();
    }

    private static byte[] sha256(String s) {
        try {
            return MessageDigest.getInstance("SHA-256").digest(s.getBytes(StandardCharsets.US_ASCII));
        }
        catch (Exception e) {
            throw new IllegalStateException(e);
        }
    }

    private Spotify() {
    }

    static {
        lyrics = List.of();
        status = "";
        lyricsFor = "";
        coverFor = "";
    }

    public record Track(String id, String name, String artists, String album, long durationMs, String coverUrl) {
    }

    public record Line(long timeMs, String text) {
    }
}

