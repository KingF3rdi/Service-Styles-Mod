package dev.hugoservice;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.security.MessageDigest;
import java.time.Duration;
import java.util.ArrayList;
import java.util.HexFormat;
import java.util.List;
import java.util.regex.Pattern;
import net.fabricmc.loader.api.FabricLoader;

/**
 * Shared mixes from marketplace/ in the mod's GitHub repo: index.json lists them, each is &lt;id&gt;.zip plus an
 * optional &lt;id&gt;.png preview. Installing drops the zip into the packs folder as market_&lt;id&gt;.zip, where it shows
 * up in Mixes like a bundled mix. Players send exported mixes to the owner, who adds them (see marketplace/README.md).
 */
public final class Marketplace {
    public record Preset(String id, String name, String author, String desc, String sha256, long size) {
    }

    public static final String PREFIX = "market_";
    private static final String[] MIRRORS = {
            "https://raw.githubusercontent.com/KingF3rdi/Service-Styles-Mod/main/marketplace/",
            "https://cdn.jsdelivr.net/gh/KingF3rdi/Service-Styles-Mod@main/marketplace/"
    };
    private static final long MAX_BYTES = 0x5F00000L; // same 95 MB cap as cloud packs
    private static final Pattern ID = Pattern.compile("^[a-z0-9_]{2,40}$");
    private static final Pattern SHA = Pattern.compile("^[0-9a-f]{64}$");
    private static final HttpClient HTTP = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(8L)).followRedirects(HttpClient.Redirect.NORMAL).build();
    private static volatile List<Preset> presets = List.of();
    private static volatile String state = "";
    private static volatile boolean fetching;

    private Marketplace() {
    }

    public static List<Preset> presets() {
        return presets;
    }

    /** "" when loaded, else what to show instead of the list. */
    public static String state() {
        return state;
    }

    static Path cache() {
        return FabricLoader.getInstance().getConfigDir().resolve("hugoservice/marketplace");
    }

    public static Path packFile(Preset p) {
        return Config.UPLOADS.resolve(PREFIX + p.id() + ".zip");
    }

    public static Path previewFile(Preset p) {
        return cache().resolve(p.id() + ".png");
    }

    public static boolean installed(Preset p) {
        return Files.exists(packFile(p));
    }

    public static boolean isMarket(String fileName) {
        return fileName != null && fileName.startsWith(PREFIX) && fileName.endsWith(".zip");
    }

    /** Loads the cached list at once, then fetches the current one in the background. */
    public static void refresh() {
        if (fetching) {
            return;
        }
        fetching = true;
        if (presets.isEmpty()) {
            loadCached();
            state = presets.isEmpty() ? "Loading the marketplace..." : "";
        }
        Thread t = new Thread(() -> {
            try {
                HttpResponse<String> r = get("index.json", HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
                List<Preset> parsed = r == null || r.statusCode() != 200 ? null : parse(r.body());
                if (parsed != null) {
                    Files.createDirectories(cache());
                    Files.writeString(cache().resolve("index.json"), r.body());
                    presets = parsed;
                    state = "";
                    for (Preset p : parsed) {
                        fetchPreview(p);
                    }
                } else if (presets.isEmpty()) {
                    state = "Marketplace not reachable - try again later";
                }
            } catch (Exception e) {
                HugoService.LOG.info("[Market] {}", e.toString());
                if (presets.isEmpty()) {
                    state = "Marketplace not reachable - try again later";
                }
            } finally {
                fetching = false;
            }
        }, "HugoService-Market");
        t.setDaemon(true);
        t.start();
    }

    private static void loadCached() {
        try {
            List<Preset> cached = parse(Files.readString(cache().resolve("index.json")));
            if (cached != null) {
                presets = cached;
            }
        } catch (IOException ignored) {
        }
    }

    /** Pack list name: the marketplace name for an installed market_<id>.zip, else the file-based one. */
    static String nameFor(String fileName, String fallback) {
        if (!isMarket(fileName)) {
            return fallback;
        }
        if (presets.isEmpty()) {
            loadCached();
        }
        String id = fileName.substring(PREFIX.length(), fileName.length() - ".zip".length());
        for (Preset p : presets) {
            if (p.id().equals(id)) {
                return p.name();
            }
        }
        return id.replace('_', ' ');
    }

    /** Downloads, checks size and SHA-256, then moves the pack into place; call off the render thread. */
    public static void install(Preset p) throws Exception {
        HttpResponse<byte[]> r = get(p.id() + ".zip", HttpResponse.BodyHandlers.ofByteArray());
        if (r == null || r.statusCode() != 200) {
            throw new IOException("download failed");
        }
        byte[] zip = r.body();
        String sha = HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(zip));
        if (zip.length != p.size() || !sha.equals(p.sha256())) {
            throw new IOException("file does not match the marketplace list");
        }
        Files.createDirectories(Config.UPLOADS);
        Path tmp = Files.createTempFile(Config.UPLOADS, PREFIX, ".part");
        Files.write(tmp, zip);
        Files.move(tmp, packFile(p), StandardCopyOption.REPLACE_EXISTING);
    }

    private static void fetchPreview(Preset p) {
        Path file = previewFile(p);
        if (Files.exists(file)) {
            return;
        }
        try {
            HttpResponse<byte[]> r = get(p.id() + ".png", HttpResponse.BodyHandlers.ofByteArray());
            if (r != null && r.statusCode() == 200 && r.body().length < 2_000_000) {
                Files.write(file, r.body());
            }
        } catch (Exception ignored) {
        }
    }

    private static <T> HttpResponse<T> get(String path, HttpResponse.BodyHandler<T> handler) {
        String local = System.getProperty("hugoservice.market"); // try a catalog before pushing it, see marketplace/README.md
        for (String base : local != null ? new String[]{local} : MIRRORS) {
            try {
                HttpResponse<T> r = HTTP.send(HttpRequest.newBuilder(URI.create(base + path)).header("User-Agent", Brand.UA)
                        .timeout(Duration.ofSeconds(30L)).GET().build(), handler);
                if (r.statusCode() == 200) {
                    return r;
                }
            } catch (Exception e) {
                HugoService.LOG.info("[Market] {} not reachable for {}: {}", base, path, e.toString());
            }
        }
        return null;
    }

    static List<Preset> parse(String json) {
        try {
            ArrayList<Preset> out = new ArrayList<>();
            for (JsonElement el : JsonParser.parseString(json).getAsJsonObject().getAsJsonArray("presets")) {
                JsonObject o = el.getAsJsonObject();
                String id = str(o, "id");
                String name = str(o, "name");
                String sha = str(o, "sha256");
                long size = o.has("size") ? o.get("size").getAsLong() : -1L;
                if (id == null || name == null || sha == null || !ID.matcher(id).matches() || !SHA.matcher(sha).matches()
                        || size <= 0L || size > MAX_BYTES || name.isBlank()) {
                    continue; // a bad entry is skipped, the rest still show
                }
                String author = str(o, "author");
                String desc = str(o, "description");
                out.add(new Preset(id, name.trim(), author == null ? "" : author.trim(), desc == null ? "" : desc.trim(), sha, size));
            }
            return out;
        } catch (Exception e) {
            return null;
        }
    }

    private static String str(JsonObject o, String key) {
        return o.has(key) && o.get(key).isJsonPrimitive() ? o.get(key).getAsString() : null;
    }
}
