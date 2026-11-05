package dev.hugoservice;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import java.io.IOException;
import java.io.InputStream;
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
import java.util.Comparator;
import java.util.HexFormat;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Predicate;
import java.util.regex.Pattern;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;
import java.util.zip.ZipInputStream;
import java.util.zip.ZipOutputStream;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.client.MinecraftClient;

public final class StylesCloud {
    public static final String FOLDER_PREFIX = "hugoservice_cloud_";
    private static final String[] MIRRORS = new String[]{
            "https://raw.githubusercontent.com/Bauloo0/bauloo-styles-cloud/main/",
            "https://cdn.jsdelivr.net/gh/Bauloo0/bauloo-styles-cloud@main/",
            "https://bauloo-styles-cloud.bauloo-styles-cloud-worker.workers.dev/raw/"
    };
    private static volatile int mirror;
    private static final long MAX_PACK_BYTES = 0x5F00000L;
    private static final long MAX_UNPACKED_BYTES = 0x19000000L;
    private static final Pattern ID = Pattern.compile("^[a-z][a-z0-9]{1,15}_[a-z0-9_]{1,40}$");
    private static final Pattern SHA = Pattern.compile("^[0-9a-f]{64}$");
    private static final HttpClient HTTP = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(8L)).followRedirects(HttpClient.Redirect.NORMAL).build();
    private static volatile List<Style> styles = List.of();
    private static volatile int version;
    private static volatile long lastFetch;
    private static volatile boolean fetching;
    private static volatile boolean cacheLoaded;
    private static final Map<String, CompletableFuture<Boolean>> DOWNLOADS = new ConcurrentHashMap<>();
    private static final Map<String, CompletableFuture<Boolean>> PREVIEWS = new ConcurrentHashMap<>();

    private StylesCloud() {
    }

    public static int version() {
        return version;
    }

    static Path dir() {
        return FabricLoader.getInstance().getConfigDir().resolve("hugoservice/cloud");
    }

    static Path packFile(Style s) {
        return dir().resolve("packs").resolve(s.id() + ".zip");
    }

    static Path previewFile(Style s) {
        return dir().resolve("previews").resolve(s.id() + ".png");
    }

    public static long stamp(String styleId) {
        Style s = byId(styleId);
        if (s == null) {
            return version;
        }
        long n = version;
        try {
            Path preview = previewFile(s);
            if (Files.exists(preview)) {
                n = Math.max(n, Files.getLastModifiedTime(preview).toMillis());
            }
            if (isReady(s)) {
                Path pack = packFile(s);
                n = Math.max(n, Files.getLastModifiedTime(pack).toMillis());
            }
        } catch (IOException ignored) {
        }
        return n;
    }

    public static void refresh(boolean force) {
        if (!cacheLoaded) {
            StylesCloud.loadCache();
        }
        long now = System.currentTimeMillis();
        if (fetching || !force && now - lastFetch < 60000L) {
            return;
        }
        fetching = true;
        HttpRequest api = HttpRequest.newBuilder(URI.create("https://api.github.com/repos/Bauloo0/bauloo-styles-cloud/contents/index.json?ref=main"))
                .header("Accept", "application/vnd.github.raw+json")
                .header("User-Agent", "7auction-Styles")
                .timeout(Duration.ofSeconds(15L))
                .GET()
                .build();
        HTTP.sendAsync(api, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8))
                .exceptionally(error -> null)
                .thenApplyAsync(response -> response != null && response.statusCode() == 200
                        ? response
                        : StylesCloud.getFile("index.json", HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8), Duration.ofSeconds(15L), r -> r.statusCode() == 200 && StylesCloud.parse(r.body()) != null))
                .thenAccept(response -> {
                    if (response == null || response.statusCode() != 200) {
                        return;
                    }
                    List<Style> parsed = StylesCloud.parse(response.body());
                    if (parsed == null) {
                        return;
                    }
                    try {
                        Files.createDirectories(StylesCloud.dir());
                        Files.writeString(StylesCloud.dir().resolve("index.json"), response.body());
                    } catch (IOException ignored) {
                    }
                    StylesCloud.publish(parsed);
                }).whenComplete((ignored, error) -> {
                    lastFetch = System.currentTimeMillis();
                    fetching = false;
                });
    }

    private static void loadCache() {
        cacheLoaded = true;
        StylesCloud.seedBundled();
        Path cached = StylesCloud.dir().resolve("index.json");
        if (!Files.exists(cached)) {
            return;
        }
        try {
            List<Style> parsed = StylesCloud.parse(Files.readString(cached));
            if (parsed != null) {
                StylesCloud.publish(parsed);
            }
        } catch (IOException ignored) {
        }
    }

    private static void seedBundled() {
        try (InputStream in = StylesCloud.class.getResourceAsStream("/hugoservice/extras/index.json")) {
            if (in == null) {
                return;
            }
            String json = new String(in.readAllBytes(), StandardCharsets.UTF_8);
            List<Style> parsed = StylesCloud.parse(json);
            Files.createDirectories(dir().resolve("packs"));
            Files.createDirectories(dir().resolve("previews"));
            Path index = dir().resolve("index.json");
            if (!Files.exists(index)) {
                Files.writeString(index, json);
            }
            if (parsed == null) {
                return;
            }
            for (Style s : parsed) {
                StylesCloud.copyResource("/hugoservice/extras/packs/" + s.id() + ".zip", packFile(s));
                if (s.hasPreview()) {
                    StylesCloud.copyResource("/hugoservice/extras/previews/" + s.id() + ".png", previewFile(s));
                }
                Path marker = dir().resolve("packs").resolve(s.id() + ".v2.sha");
                if (Files.exists(packFile(s)) && !Files.exists(marker)) {
                    Files.writeString(marker, s.sha256());
                }
            }
            HugoService.LOG.info("[Cloud] seeded {} bundled extra packs", parsed.size());
        } catch (Exception e) {
            HugoService.LOG.warn("[Cloud] bundled extras: {}", e.toString());
        }
    }

    private static void copyResource(String resource, Path dest) throws IOException {
        if (Files.exists(dest) && Files.size(dest) > 8L) {
            return;
        }
        try (InputStream in = StylesCloud.class.getResourceAsStream(resource)) {
            if (in == null) {
                return;
            }
            Files.createDirectories(dest.getParent());
            Files.copy(in, dest, StandardCopyOption.REPLACE_EXISTING);
        }
    }

    private static void publish(List<Style> parsed) {
        CloudBinder.bind();
        if (!parsed.equals(styles)) {
            styles = List.copyOf(parsed);
            ++version;
        }
        MinecraftClient client = MinecraftClient.getInstance();
        if (client != null) {
            Thread t = new Thread(Styles::rescan, "HugoService-Scan");
            t.setDaemon(true);
            t.start();
        }
    }

    static <T> HttpResponse<T> getFile(String path, HttpResponse.BodyHandler<T> handler, Duration timeout, Predicate<HttpResponse<T>> ok) {
        HttpResponse<T> last = null;
        int start = mirror;
        for (int k = 0; k < MIRRORS.length; ++k) {
            int m = (start + k) % MIRRORS.length;
            try {
                HttpRequest request = HttpRequest.newBuilder(URI.create(MIRRORS[m] + path))
                        .header("User-Agent", "7auction-Styles")
                        .timeout(timeout)
                        .GET()
                        .build();
                last = HTTP.send(request, handler);
                if (!ok.test(last)) {
                    continue;
                }
                mirror = m;
                return last;
            } catch (Exception e) {
                HugoService.LOG.info("[Cloud] {} not reachable for {}: {}", MIRRORS[m], path, e.toString());
            }
        }
        return last;
    }

    private static List<Style> parse(String json) {
        try {
            JsonObject root = JsonParser.parseString(json).getAsJsonObject();
            CloudCategories.read(root);
            JsonArray array = root.getAsJsonArray("styles");
            ArrayList<Style> result = new ArrayList<>();
            for (JsonElement element : array) {
                if (!element.isJsonObject()) {
                    continue;
                }
                JsonObject o = element.getAsJsonObject();
                String id = string(o, "id");
                String category = string(o, "category");
                String name = string(o, "name");
                String sha = string(o, "sha256");
                long size = o.has("size") ? o.get("size").getAsLong() : -1L;
                if (id == null || category == null || name == null || sha == null || !ID.matcher(id).matches()
                        || !id.startsWith(category + "_") || CloudCategories.get(category) == null || !SHA.matcher(sha).matches()
                        || size <= 0L || size > MAX_PACK_BYTES || name.isBlank() || name.length() > 40) {
                    continue;
                }
                boolean preview = o.has("preview") && ("previews/" + id + ".png").equals(string(o, "preview"));
                result.add(new Style(id, category, name.trim(), sha, size, preview));
            }
            result.sort(Comparator.comparing(Style::id));
            return result;
        } catch (Exception e) {
            return null;
        }
    }

    private static String string(JsonObject o, String key) {
        return o.has(key) && o.get(key).isJsonPrimitive() ? o.get(key).getAsString() : null;
    }

    public static List<Style> of(String category) {
        if (!cacheLoaded) {
            StylesCloud.loadCache();
        }
        ArrayList<Style> result = new ArrayList<>();
        for (Style s : styles) {
            if (s.category().equals(category)) {
                result.add(s);
            }
        }
        return result;
    }

    public static Style byId(String id) {
        for (Style s : styles) {
            if (s.id().equals(id)) {
                return s;
            }
        }
        return null;
    }

    public static boolean isReady(Style s) {
        try {
            Path marker = StylesCloud.dir().resolve("packs").resolve(s.id() + ".v2.sha");
            return Files.exists(StylesCloud.packFile(s)) && Files.exists(marker) && Files.readString(marker).trim().equals(s.sha256());
        } catch (IOException e) {
            return false;
        }
    }

    public static CompletableFuture<Boolean> download(Style s) {
        if (StylesCloud.isReady(s)) {
            return CompletableFuture.completedFuture(true);
        }
        return DOWNLOADS.computeIfAbsent(s.id(), id -> CompletableFuture.supplyAsync(() -> StylesCloud.fetchPack(s)).whenComplete((ok, error) -> DOWNLOADS.remove(id)));
    }

    public static CompletableFuture<Boolean> downloadByStyleId(String styleId) {
        if (styleId == null || !styleId.startsWith("cloud:")) {
            return CompletableFuture.completedFuture(false);
        }
        Style s = byId(styleId.substring("cloud:".length()));
        return s == null ? CompletableFuture.completedFuture(false) : download(s);
    }

    private static boolean fetchPack(Style s) {
        Path temp = null;
        try {
            Files.createDirectories(StylesCloud.dir().resolve("packs"));
            temp = Files.createTempFile("hugoservice-cloud-", ".zip");
            Path file = temp;
            HttpResponse<Path> response = StylesCloud.getFile("packs/" + s.id() + ".zip", HttpResponse.BodyHandlers.ofFile(temp), Duration.ofMinutes(3L), r -> r.statusCode() == 200);
            if (response == null || response.statusCode() != 200 || Files.size(temp) != s.size() || !sha256(temp).equals(s.sha256())) {
                HugoService.LOG.warn("Cloud style {}: download failed", s.id());
                return false;
            }
            Path target = StylesCloud.packFile(s);
            Files.createDirectories(target.getParent());
            Path cleaned = Files.createTempFile("hugoservice-cloud-clean-", ".zip");
            if (!copyAllowed(temp, cleaned, s.category())) {
                Files.deleteIfExists(cleaned);
                return false;
            }
            Files.move(cleaned, target, StandardCopyOption.REPLACE_EXISTING);
            Files.writeString(StylesCloud.dir().resolve("packs").resolve(s.id() + ".v2.sha"), s.sha256());
            return true;
        } catch (Exception e) {
            HugoService.LOG.warn("Cloud style {}: {}", s.id(), e.toString());
            return false;
        } finally {
            if (temp != null) {
                try {
                    Files.deleteIfExists(temp);
                } catch (IOException ignored) {
                }
            }
        }
    }

    private static boolean copyAllowed(Path source, Path destination, String category) throws IOException {
        long unpacked = 0L;
        int kept = 0;
        byte[] buffer = new byte[65536];
        try (ZipInputStream in = new ZipInputStream(Files.newInputStream(source));
             ZipOutputStream out = new ZipOutputStream(Files.newOutputStream(destination))) {
            ZipEntry entry;
            while ((entry = in.getNextEntry()) != null) {
                String name = entry.getName().replace('\\', '/');
                if (entry.isDirectory() || name.startsWith("/") || name.contains("..")) {
                    continue;
                }
                boolean allowed = name.startsWith("assets/") || name.equals("pack.png");
                if (!allowed) {
                    continue;
                }
                out.putNextEntry(new ZipEntry(name));
                int read;
                while ((read = in.read(buffer)) > 0) {
                    unpacked += read;
                    if (unpacked > MAX_UNPACKED_BYTES) {
                        return false;
                    }
                    out.write(buffer, 0, read);
                }
                out.closeEntry();
                ++kept;
            }
        }
        return kept > 0;
    }

    private static String sha256(Path file) throws Exception {
        MessageDigest digest = MessageDigest.getInstance("SHA-256");
        try (InputStream in = Files.newInputStream(file)) {
            byte[] buffer = new byte[65536];
            int read;
            while ((read = in.read(buffer)) > 0) {
                digest.update(buffer, 0, read);
            }
        }
        return HexFormat.of().formatHex(digest.digest());
    }

    public static void ensurePreview(Style s) {
        if (s == null || !s.hasPreview()) {
            return;
        }
        Path cached = previewFile(s);
        try {
            if (Files.exists(cached) && Files.size(cached) > 8L) {
                return;
            }
        } catch (IOException ignored) {
        }
        PREVIEWS.computeIfAbsent(s.id(), id -> CompletableFuture.supplyAsync(() -> fetchPreview(s)).whenComplete((ok, error) -> PREVIEWS.remove(id)));
    }

    public static byte[] previewPng(Style s) {
        if (s == null) {
            return null;
        }
        Path cached = previewFile(s);
        try {
            if (Files.exists(cached) && Files.size(cached) > 8L) {
                return Files.readAllBytes(cached);
            }
        } catch (IOException ignored) {
        }
        try (InputStream bundled = StylesCloud.class.getResourceAsStream("/hugoservice/extras/previews/" + s.id() + ".png")) {
            if (bundled != null) {
                return bundled.readAllBytes();
            }
        } catch (IOException ignored) {
        }
        if (!s.hasPreview()) {
            return null;
        }
        try {
            PREVIEWS.computeIfAbsent(s.id(), id -> CompletableFuture.supplyAsync(() -> fetchPreview(s)).whenComplete((ok, error) -> PREVIEWS.remove(id))).join();
        } catch (Exception ignored) {
        }
        try {
            if (Files.exists(cached) && Files.size(cached) > 8L) {
                return Files.readAllBytes(cached);
            }
        } catch (IOException ignored) {
        }
        return null;
    }

    private static boolean fetchPreview(Style s) {
        try {
            HttpResponse<byte[]> response = getFile("previews/" + s.id() + ".png", HttpResponse.BodyHandlers.ofByteArray(), Duration.ofSeconds(20L),
                    r -> r.statusCode() == 200 && r.body() != null && r.body().length > 8 && r.body().length <= 0x200000);
            if (response == null || response.statusCode() != 200) {
                return false;
            }
            Path cached = previewFile(s);
            Files.createDirectories(cached.getParent());
            Files.write(cached, response.body());
            return true;
        } catch (Exception e) {
            HugoService.LOG.debug("Cloud style {}: preview {}", s.id(), e.toString());
            return false;
        }
    }

    public static byte[] readNamed(Style s, String entryName) {
        if (!isReady(s) || entryName == null) {
            return null;
        }
        String want = entryName.replace('\\', '/').toLowerCase(Locale.ROOT);
        try (ZipFile z = new ZipFile(packFile(s).toFile())) {
            var en = z.entries();
            while (en.hasMoreElements()) {
                ZipEntry e = en.nextElement();
                if (e.isDirectory()) {
                    continue;
                }
                String name = e.getName().replace('\\', '/');
                if (!name.toLowerCase(Locale.ROOT).equals(want) && !name.toLowerCase(Locale.ROOT).endsWith("/" + want)) {
                    continue;
                }
                try (InputStream in = z.getInputStream(e)) {
                    return in.readAllBytes();
                }
            }
        } catch (Exception e) {
            return null;
        }
        return null;
    }

    public static byte[] read(Style s, String groupPath) {
        if (!isReady(s)) {
            return null;
        }
        String want = groupPath.toLowerCase(Locale.ROOT);
        try (ZipFile z = new ZipFile(packFile(s).toFile())) {
            var en = z.entries();
            while (en.hasMoreElements()) {
                ZipEntry e = en.nextElement();
                if (e.isDirectory()) {
                    continue;
                }
                String name = e.getName().replace('\\', '/');
                String mapped = CloudCategories.target(name.toLowerCase(Locale.ROOT), s.category());
                String path = mapped != null ? CloudCategories.toGroupPath(mapped) : Styles.normalize(name);
                if (path == null) {
                    String stripped = CloudCategories.toGroupPath(name.toLowerCase(Locale.ROOT));
                    if (stripped.equals(want)) {
                        path = stripped;
                    }
                }
                if (path == null || !path.equals(want) && !path.equals(want + ".mcmeta")) {
                    continue;
                }
                try (InputStream in = z.getInputStream(e)) {
                    return in.readAllBytes();
                }
            }
        } catch (Exception e) {
            return null;
        }
        return null;
    }

    public record Style(String id, String category, String name, String sha256, long size, boolean hasPreview) {
    }
}
