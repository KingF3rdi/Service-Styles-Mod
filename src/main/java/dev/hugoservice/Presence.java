/*
 * Decompiled with CFR 0.152.
 *
 * Could not load the following classes:
 *  com.google.gson.Gson
 *  com.google.gson.JsonObject
 *  net.minecraft.text.Text
 *  net.minecraft.text.MutableText
 *  net.minecraft.client.MinecraftClient
 *  net.minecraft.client.network.PlayerListEntry
 *  net.minecraft.entity.LivingEntity
 */
package dev.hugoservice;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Consumer;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.PlayerListEntry;
import net.minecraft.entity.LivingEntity;
import net.minecraft.text.MutableText;
import net.minecraft.text.Text;

public final class Presence {
    private static final Gson GSON = new Gson();
    private static final HttpClient HTTP = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(5L)).build();
    private static final Set<UUID> USERS = ConcurrentHashMap.newKeySet();
    private static final Map<UUID, Long> CHECKED = new ConcurrentHashMap<UUID, Long>();
    private static final Text ICON = Mc.iconText();
    private static volatile UUID self;
    private static volatile boolean busy;
    private static int ticks;
    private static int lastBeat;
    private static boolean pendingSend;

    static void tick(MinecraftClient mc) {
        if (mc.player == null) {
            return;
        }
        self = mc.player.getUuid();
        Config cfg = Config.INSTANCE;
        if (!cfg.nameIcon || cfg.presenceUrl.isBlank() || mc.getNetworkHandler() == null) {
            return;
        }
        boolean changed = pendingSend;
        if (ticks++ % 1200 == 0 || changed && ticks - lastBeat >= 100) {
            JsonObject beat = new JsonObject();
            beat.addProperty("uuid", self.toString());
            Presence.post("/heartbeat", beat.toString(), null);
            lastBeat = ticks;
            pendingSend = false;
        } else if (changed) {
            pendingSend = true;
        }
        if (ticks % 200 != 0 || busy) {
            return;
        }
        long now = System.currentTimeMillis();
        ArrayList<String> todo = new ArrayList<String>();
        for (PlayerListEntry e : mc.getNetworkHandler().getPlayerList()) {
            UUID id = Mc.profileId(e.getProfile());
            if (id.equals(self) || now - CHECKED.getOrDefault(id, 0L) < 60000L) continue;
            todo.add(id.toString());
            if (todo.size() != 100) continue;
            break;
        }
        if (todo.isEmpty()) {
            return;
        }
        busy = true;
        JsonObject body = new JsonObject();
        body.add("uuids", GSON.toJsonTree(todo));
        Presence.post("/check", body.toString(), json -> {
            for (String s : todo) {
                UUID id = UUID.fromString(s);
                CHECKED.put(id, now);
                USERS.remove(id);
            }
            if (json != null && json.has("users")) {
                json.getAsJsonArray("users").forEach(u -> USERS.add(UUID.fromString(u.getAsString())));
            }
        });
    }

    private static void post(String path, String body, Consumer<JsonObject> done) {
        HttpRequest req;
        String base = Config.INSTANCE.presenceUrl.replaceAll("/+$", "");
        try {
            req = HttpRequest.newBuilder(URI.create(base + path)).timeout(Duration.ofSeconds(8L)).header("content-type", "application/json").POST(HttpRequest.BodyPublishers.ofString(body)).build();
        }
        catch (IllegalArgumentException e) {
            busy = false;
            return;
        }
        HTTP.sendAsync(req, HttpResponse.BodyHandlers.ofString()).whenComplete((res, err) -> {
            JsonObject json = null;
            if (err == null && res.statusCode() == 200) {
                try {
                    json = (JsonObject)GSON.fromJson((String)res.body(), JsonObject.class);
                }
                catch (Exception exception) {
                    // empty catch block
                }
            }
            if (done != null) {
                done.accept(json);
            }
            busy = false;
        });
    }

    public static boolean shows(UUID id) {
        if (id == null) {
            return false;
        }
        MinecraftClient mc = MinecraftClient.getInstance();
        if (mc.player != null && id.equals(mc.player.getUuid())) {
            return true;
        }
        if (self != null && id.equals(self)) {
            return true;
        }
        return Config.INSTANCE.nameIcon && USERS.contains(id);
    }

    public static boolean showOwnLabel(LivingEntity entity) {
        MinecraftClient mc = MinecraftClient.getInstance();
        if (mc.player == null || entity != mc.player) {
            return false;
        }
        if (!MinecraftClient.isHudEnabled()) {
            return false;
        }
        return !mc.options.getPerspective().isFirstPerson();
    }

    public static Text decorate(UUID id, Text name) {
        if (!Presence.shows(id) || name == null) {
            return name;
        }
        return Text.empty().append(ICON).append(" ").append(name);
    }

    public static Text recolor(Text text, Integer color) {
        if (color == null || text == null) {
            return text;
        }
        MutableText out = MutableText.of(text.getContent()).setStyle(text.getStyle().withColor(color.intValue()));
        for (Text sib : text.getSiblings()) {
            out.append(Presence.recolor(sib, color));
        }
        return out;
    }

    private Presence() {
    }

    static {
        lastBeat = -1000;
    }
}
