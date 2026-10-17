package dev.hugoservice;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.PlayerListEntry;
import net.minecraft.text.StyleSpriteSource;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Name icon: tells the presence server (Cloudflare Worker) that you use the mod and asks which
 * players on your current server do too. Opt-in; only Minecraft UUIDs are sent.
 */
public final class Presence {
    private static final Gson GSON = new Gson();
    private static final HttpClient HTTP = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(5)).build();
    private static final Set<UUID> USERS = ConcurrentHashMap.newKeySet();
    private static final Map<UUID, Long> CHECKED = new ConcurrentHashMap<>();
    private static final Text ICON = Text.literal("")
        .setStyle(net.minecraft.text.Style.EMPTY.withFont(new StyleSpriteSource.Font(Identifier.of("hugoservice", "icon"))));
    private static volatile UUID self;
    private static volatile boolean busy;
    private static int ticks;

    static void tick(MinecraftClient mc) {
        Config cfg = Config.INSTANCE;
        if (!cfg.nameIcon || cfg.presenceUrl.isBlank() || mc.player == null || mc.getNetworkHandler() == null) return;
        self = mc.player.getUuid();
        if (ticks++ % 1200 == 0) post("/heartbeat", "{\"uuid\":\"" + self + "\"}", null);   // every minute
        if (ticks % 200 != 0 || busy) return;                                               // look-ups every 10 s
        long now = System.currentTimeMillis();
        List<String> todo = new ArrayList<>();
        for (PlayerListEntry e : mc.getNetworkHandler().getPlayerList()) {
            UUID id = e.getProfile().id();
            if (id.equals(self) || now - CHECKED.getOrDefault(id, 0L) < 5 * 60_000) continue;
            todo.add(id.toString());
            if (todo.size() == 100) break;
        }
        if (todo.isEmpty()) return;
        busy = true;
        JsonObject body = new JsonObject();
        body.add("uuids", GSON.toJsonTree(todo));
        post("/check", body.toString(), json -> {
            for (String s : todo) {
                UUID id = UUID.fromString(s);
                CHECKED.put(id, now);
                USERS.remove(id);
            }
            if (json != null && json.has("users")) json.getAsJsonArray("users").forEach(u -> USERS.add(UUID.fromString(u.getAsString())));
        });
    }

    private static void post(String path, String body, java.util.function.Consumer<JsonObject> done) {
        String base = Config.INSTANCE.presenceUrl.replaceAll("/+$", "");
        HttpRequest req;
        try {
            req = HttpRequest.newBuilder(URI.create(base + path)).timeout(Duration.ofSeconds(8))
                .header("content-type", "application/json").POST(HttpRequest.BodyPublishers.ofString(body)).build();
        } catch (IllegalArgumentException e) {
            busy = false; // bad URL in config
            return;
        }
        HTTP.sendAsync(req, HttpResponse.BodyHandlers.ofString()).whenComplete((res, err) -> {
            JsonObject json = null;
            if (err == null && res.statusCode() == 200) {
                try { json = GSON.fromJson(res.body(), JsonObject.class); } catch (Exception ignored) {}
            }
            if (done != null) done.accept(json);
            busy = false;
        });
    }

    /** Prefixes the icon when this player uses the mod (you always see it on yourself). */
    public static Text decorate(UUID id, Text name) {
        if (!Config.INSTANCE.nameIcon || id == null || !(id.equals(self) || USERS.contains(id))) return name;
        return Text.empty().append(ICON).append(" ").append(name);
    }

    private Presence() {}
}
