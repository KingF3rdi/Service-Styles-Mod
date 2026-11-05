/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.google.gson.JsonElement
 *  com.google.gson.JsonObject
 *  com.google.gson.JsonParser
 *  com.mojang.authlib.GameProfile
 *  com.mojang.authlib.yggdrasil.ProfileResult
 *  com.mojang.authlib.yggdrasil.response.NameAndId
 *  net.minecraft.client.util.DefaultSkinHelper
 *  net.minecraft.text.Text
 *  net.minecraft.text.Style
 *  net.minecraft.client.MinecraftClient
 *  net.minecraft.text.MutableText
 *  net.minecraft.client.network.PlayerListEntry
 *  net.minecraft.entity.player.SkinTextures
 */
package dev.hugoservice;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.mojang.authlib.GameProfile;
import com.mojang.authlib.yggdrasil.ProfileResult;
import com.mojang.authlib.yggdrasil.response.NameAndId;
import dev.hugoservice.Config;
import dev.hugoservice.HugoService;
import dev.hugoservice.Images;
import dev.hugoservice.StyleScreen;
import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.regex.Pattern;
import net.minecraft.client.util.DefaultSkinHelper;
import net.minecraft.text.Text;
import net.minecraft.text.Style;
import net.minecraft.client.MinecraftClient;
import net.minecraft.text.MutableText;
import net.minecraft.client.network.PlayerListEntry;
import net.minecraft.entity.player.SkinTextures;

public final class NameFaker {
    private static final Pattern TAG = Pattern.compile("\\bR?[HL]T[1-5]\\b");
    private static final Pattern TIER = Pattern.compile("\\b(?:R?[HL]T[1-5])\\b\\s*[|\u2022\u00b7]?\\s*");
    private static final HttpClient HTTP = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(5L)).build();
    private static volatile Map<String, String> names = Map.of();
    private static volatile UUID self;
    private static final Map<String, SkinTextures> SKINS;
    private static final Map<String, String> TIERS;
    private static final Set<String> ASKED;

    static void tick(MinecraftClient mc) {
        Config cfg = Config.INSTANCE;
        if (!cfg.fakeOwn && !cfg.fakeEnemies || mc.getNetworkHandler() == null || mc.player == null) {
            names = Map.of();
            return;
        }
        self = mc.player.getUuid();
        HashMap<String, String> m = new HashMap<String, String>();
        String own = mc.player.getGameProfile().name();
        for (PlayerListEntry e : mc.getNetworkHandler().getPlayerList()) {
            String real = e.getProfile().name();
            if (real == null || real.isEmpty() || real.equals(own) || !cfg.fakeEnemies) continue;
            m.put(real, (String)(cfg.fakeEnemyName.isBlank() ? "Enemy" + (Math.floorMod(e.getProfile().id().hashCode(), 9000) + 1000) : cfg.fakeEnemyName));
        }
        if (cfg.fakeOwn) {
            m.put(own, cfg.fakeName);
        }
        names = m;
        if (!(mc.currentScreen instanceof StyleScreen)) {
            if (cfg.fakeOwn) {
                NameFaker.lookup(cfg.fakeName);
            }
            if (cfg.fakeEnemies) {
                NameFaker.lookup(cfg.fakeEnemyName);
            }
        }
    }

    private static void lookup(String name) {
        String key = name.trim().toLowerCase(Locale.ROOT);
        if (key.isEmpty() || !ASKED.add(key)) {
            return;
        }
        Thread.ofVirtual().start(() -> {
            MinecraftClient mc = MinecraftClient.getInstance();
            try {
                GameProfile profile;
                Optional id = mc.getApiServices().profileRepository().findProfileByName(name.trim());
                ProfileResult result = id.isEmpty() ? null : mc.getApiServices().sessionService().fetchProfile(((NameAndId)id.get()).id(), false);
                GameProfile gameProfile = profile = result == null ? null : result.profile();
                if (profile != null) {
                    mc.getSkinProvider().fetchSkinTextures(profile).thenAccept(s -> s.ifPresent(t -> SKINS.put(key, (SkinTextures)t)));
                }
            }
            catch (Exception e) {
                HugoService.LOG.debug("No skin for fake name {}", name, e);
            }
            TIERS.put(key, NameFaker.fetchTier(name.trim()));
        });
    }

    private static String fetchTier(String name) {
        try {
            HttpRequest req = HttpRequest.newBuilder(URI.create("https://mctiers.gg/api/profile/by-name/" + URLEncoder.encode(name, StandardCharsets.UTF_8))).timeout(Duration.ofSeconds(8L)).GET().build();
            HttpResponse<String> res = HTTP.send(req, HttpResponse.BodyHandlers.ofString());
            return res.statusCode() == 200 ? NameFaker.bestTier(JsonParser.parseString((String)res.body()).getAsJsonObject()) : "";
        }
        catch (Exception e) {
            HugoService.LOG.debug("No tier for fake name {}", name, e);
            return "";
        }
    }

    static String bestTier(JsonObject profile) {
        String best = "";
        int bestScore = Integer.MAX_VALUE;
        JsonElement rankings = profile.get("rankings");
        if (rankings == null || !rankings.isJsonObject()) {
            return "";
        }
        for (Map.Entry e : rankings.getAsJsonObject().entrySet()) {
            boolean retired;
            int pos;
            JsonObject r = ((JsonElement)e.getValue()).getAsJsonObject();
            int tier = r.get("tier").getAsInt();
            int score = tier * 4 + (pos = r.get("pos").getAsInt()) * 2 + ((retired = r.has("retired") && r.get("retired").getAsBoolean()) ? 1 : 0);
            if (score >= bestScore) continue;
            bestScore = score;
            best = (retired ? "R" : "") + (pos == 0 ? "H" : "L") + "T" + tier;
        }
        return best;
    }

    private static String tierOf(String fake) {
        return TIERS.getOrDefault(fake.trim().toLowerCase(Locale.ROOT), "");
    }

    public static Text apply(Text text) {
        return NameFaker.apply(text, false);
    }

    public static Text apply(Text text, boolean addTier) {
        Map<String, String> m = names;
        Config cfg = Config.INSTANCE;
        if (m.isEmpty() && !cfg.hideTier) {
            return text;
        }
        String plain = text.getString();
        String fake = null;
        for (Map.Entry<String, String> e : m.entrySet()) {
            if (!plain.contains(e.getKey())) continue;
            fake = e.getValue();
            break;
        }
        boolean hasTag = TAG.matcher(plain).find();
        if (!(fake != null || cfg.hideTier && hasTag)) {
            return text;
        }
        String tier = cfg.hideTier ? "" : (fake != null && cfg.fakeTier ? NameFaker.tierOf(fake) : null);
        MutableText out = Text.empty();
        if (!hasTag && addTier && tier != null && !tier.isEmpty()) {
            out.append(NameFaker.tierTag(tier)).append((Text)Text.literal((String)" | ").withColor(0x8A8A8A));
        }
        String t = tier;
        text.visit((style, s) -> {
            for (Map.Entry e : m.entrySet()) {
                s = s.replace((CharSequence)e.getKey(), (CharSequence)e.getValue());
            }
            if (t != null) {
                s = t.isEmpty() ? TIER.matcher(s).replaceAll("") : TAG.matcher(s).replaceAll(t);
            }
            out.append((Text)Text.literal((String)s).setStyle(style));
            return Optional.empty();
        }, Style.EMPTY);
        return out;
    }

    private static Text tierTag(String tier) {
        int n = tier.charAt(tier.length() - 1) - 48;
        int color = switch (n) {
            case 1 -> 16763196;
            case 2 -> 13226975;
            case 3 -> 14717002;
            default -> 10466248;
        };
        return Text.literal((String)tier).withColor(tier.startsWith("R") ? Images.mix(0xFF000000 | color, -8355712, 0.5f) & 0xFFFFFF : color);
    }

    public static SkinTextures skin(UUID id) {
        Config cfg = Config.INSTANCE;
        if (id == null) {
            return null;
        }
        boolean me = id.equals(self);
        if (me ? !cfg.fakeOwn || !cfg.fakeOwnSkin : !cfg.fakeEnemies || !cfg.fakeEnemySkin) {
            return null;
        }
        String fake = me ? cfg.fakeName : cfg.fakeEnemyName;
        SkinTextures s = fake.isBlank() ? null : SKINS.get(fake.trim().toLowerCase(Locale.ROOT));
        return s != null ? s : DefaultSkinHelper.getSkinTextures((UUID)id);
    }

    private NameFaker() {
    }

    static {
        SKINS = new ConcurrentHashMap<String, SkinTextures>();
        TIERS = new ConcurrentHashMap<String, String>();
        ASKED = ConcurrentHashMap.newKeySet();
    }
}

