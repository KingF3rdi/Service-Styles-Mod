package dev.hugoservice;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.mojang.authlib.GameProfile;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.PlayerListEntry;
import net.minecraft.client.util.DefaultSkinHelper;
import net.minecraft.entity.player.SkinTextures;
import net.minecraft.text.MutableText;
import net.minecraft.text.Style;
import net.minecraft.text.Text;

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

/**
 * Streamer-style name faker: swaps real player names (yours or enemies) in name tags, tab list and chat. When the fake
 * name is a real account, its skin and its MCTiers tier come along.
 */
public final class NameFaker {
    /** A tier tag like "HT3", "LT2", "RHT1". */
    private static final Pattern TAG = Pattern.compile("\\bR?[HL]T[1-5]\\b");
    /** The same plus a following separator, for removing it. */
    private static final Pattern TIER = Pattern.compile("\\b(?:R?[HL]T[1-5])\\b\\s*[|•·]?\\s*");
    private static final HttpClient HTTP = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(5)).build();
    private static volatile Map<String, String> names = Map.of();
    private static volatile UUID self;
    /** Per fake name (lower case): its skin and its tier tag ("" = no tier). Missing = not known (yet). */
    private static final Map<String, SkinTextures> SKINS = new ConcurrentHashMap<>();
    private static final Map<String, String> TIERS = new ConcurrentHashMap<>();
    private static final Set<String> ASKED = ConcurrentHashMap.newKeySet();

    static void tick(MinecraftClient mc) {
        Config cfg = Config.INSTANCE;
        if ((!cfg.fakeOwn && !cfg.fakeEnemies) || mc.getNetworkHandler() == null || mc.player == null) {
            names = Map.of();
            return;
        }
        self = mc.player.getUuid();
        Map<String, String> m = new HashMap<>();
        String own = mc.player.getGameProfile().name();
        for (PlayerListEntry e : mc.getNetworkHandler().getPlayerList()) {
            String real = e.getProfile().name();
            if (real == null || real.isEmpty() || real.equals(own)) continue;
            if (cfg.fakeEnemies) m.put(real, cfg.fakeEnemyName.isBlank()
                ? "Enemy" + (Math.floorMod(e.getProfile().id().hashCode(), 9000) + 1000) : cfg.fakeEnemyName);
        }
        if (cfg.fakeOwn) m.put(own, cfg.fakeName);
        names = m;
        // look names up once typing is done (not on every key in the menu)
        if (!(mc.currentScreen instanceof StyleScreen)) {
            if (cfg.fakeOwn) lookup(cfg.fakeName);
            if (cfg.fakeEnemies) lookup(cfg.fakeEnemyName);
        }
    }

    /** Fetches skin and tier of a fake name once, in the background; a name that is no account gets neither. */
    private static void lookup(String name) {
        String key = name.trim().toLowerCase(Locale.ROOT);
        if (key.isEmpty() || !ASKED.add(key)) return;
        Thread.ofVirtual().start(() -> {
            MinecraftClient mc = MinecraftClient.getInstance();
            try {
                var id = mc.getApiServices().profileRepository().findProfileByName(name.trim());
                var result = id.isEmpty() ? null : mc.getApiServices().sessionService().fetchProfile(id.get().id(), false);
                GameProfile profile = result == null ? null : result.profile();
                if (profile != null) mc.getSkinProvider().fetchSkinTextures(profile).thenAccept(s -> s.ifPresent(t -> SKINS.put(key, t)));
            } catch (Exception e) {
                HugoService.LOG.debug("No skin for fake name {}", name, e);
            }
            TIERS.put(key, fetchTier(name.trim()));
        });
    }

    /** Best tier of a name on MCTiers ("HT1", "RLT3"...), or "" when it has none. */
    private static String fetchTier(String name) {
        try {
            HttpRequest req = HttpRequest.newBuilder(URI.create("https://mctiers.gg/api/profile/by-name/"
                + URLEncoder.encode(name, StandardCharsets.UTF_8))).timeout(Duration.ofSeconds(8)).GET().build();
            HttpResponse<String> res = HTTP.send(req, HttpResponse.BodyHandlers.ofString());
            return res.statusCode() == 200 ? bestTier(JsonParser.parseString(res.body()).getAsJsonObject()) : "";
        } catch (Exception e) {
            HugoService.LOG.debug("No tier for fake name {}", name, e);
            return "";
        }
    }

    /** Highest ranking over all gamemodes: lower tier first, high before low, active before retired. */
    static String bestTier(JsonObject profile) {
        String best = "";
        int bestScore = Integer.MAX_VALUE;
        JsonElement rankings = profile.get("rankings");
        if (rankings == null || !rankings.isJsonObject()) return "";
        for (var e : rankings.getAsJsonObject().entrySet()) {
            JsonObject r = e.getValue().getAsJsonObject();
            int tier = r.get("tier").getAsInt(), pos = r.get("pos").getAsInt();
            boolean retired = r.has("retired") && r.get("retired").getAsBoolean();
            int score = tier * 4 + pos * 2 + (retired ? 1 : 0);
            if (score < bestScore) {
                bestScore = score;
                best = (retired ? "R" : "") + (pos == 0 ? "H" : "L") + "T" + tier;
            }
        }
        return best;
    }

    /** Tier tag of a fake name; "" when it has none or is not known yet (the real tier never shows). */
    private static String tierOf(String fake) {
        return TIERS.getOrDefault(fake.trim().toLowerCase(Locale.ROOT), "");
    }

    /** Chat: real names replaced; tier tags next to them become the fake name's tier. */
    public static Text apply(Text text) {
        return apply(text, false);
    }

    /**
     * Returns the text with real names replaced; styles are kept. Tier tags next to a faked name become that name's
     * tier (or go when it has none); {@code addTier} puts the tier in front when the text had no tag (name tags, tab).
     */
    public static Text apply(Text text, boolean addTier) {
        Map<String, String> m = names;
        Config cfg = Config.INSTANCE;
        if (m.isEmpty() && !cfg.hideTier) return text;
        String plain = text.getString();
        String fake = null;
        for (var e : m.entrySet()) if (plain.contains(e.getKey())) { fake = e.getValue(); break; }
        boolean hasTag = TAG.matcher(plain).find();
        if (fake == null && !(cfg.hideTier && hasTag)) return text;
        String tier = cfg.hideTier ? "" : fake != null && cfg.fakeTier ? tierOf(fake) : null; // null = leave tags
        MutableText out = Text.empty();
        if (!hasTag && addTier && tier != null && !tier.isEmpty()) out.append(tierTag(tier)).append(Text.literal(" | ").withColor(0x8A8A8A));
        String t = tier;
        text.visit((style, s) -> {
            for (var e : m.entrySet()) s = s.replace(e.getKey(), e.getValue());
            if (t != null) s = t.isEmpty() ? TIER.matcher(s).replaceAll("") : TAG.matcher(s).replaceAll(t);
            out.append(Text.literal(s).setStyle(style));
            return Optional.empty();
        }, Style.EMPTY);
        return out;
    }

    /** A tier tag coloured like tier lists do: gold, silver, bronze, then grey; retired ones paler. */
    private static Text tierTag(String tier) {
        int n = tier.charAt(tier.length() - 1) - '0';
        int color = switch (n) { case 1 -> 0xFFC93C; case 2 -> 0xC9D3DF; case 3 -> 0xE0904A; default -> 0x9FB3C8; };
        return Text.literal(tier).withColor(tier.startsWith("R") ? Knives.mix(0xFF000000 | color, 0xFF808080, 0.5f) & 0xFFFFFF : color);
    }

    /** Skin to show instead of this player's own (the fake name's skin, else a default one), or null for the real one. */
    public static SkinTextures skin(UUID id) {
        Config cfg = Config.INSTANCE;
        if (id == null) return null;
        boolean me = id.equals(self);
        if (me ? !(cfg.fakeOwn && cfg.fakeOwnSkin) : !(cfg.fakeEnemies && cfg.fakeEnemySkin)) return null;
        String fake = me ? cfg.fakeName : cfg.fakeEnemyName;
        SkinTextures s = fake.isBlank() ? null : SKINS.get(fake.trim().toLowerCase(Locale.ROOT));
        return s != null ? s : DefaultSkinHelper.getSkinTextures(id);
    }

    private NameFaker() {}
}
