package dev.hugoservice;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.PlayerListEntry;
import net.minecraft.text.MutableText;
import net.minecraft.text.Style;
import net.minecraft.text.Text;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.regex.Pattern;

/** Streamer-style name faker: swaps real player names (yours or enemies) in name tags, tab list and chat. */
public final class NameFaker {
    /** Tier tags like "HT3", "LT2", "RHT1" plus a following separator. */
    private static final Pattern TIER = Pattern.compile("\\b(?:R?[HL]T[1-5])\\b\\s*[|•·]?\\s*");
    private static volatile Map<String, String> names = Map.of();
    private static volatile UUID self;

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
            if (cfg.fakeEnemies) m.put(real, "Enemy" + (Math.floorMod(e.getProfile().id().hashCode(), 9000) + 1000));
        }
        if (cfg.fakeOwn) m.put(own, cfg.fakeName);
        names = m;
    }

    /** Returns the text with real names replaced (and tier tags removed if wanted); styles are kept. */
    public static Text apply(Text text) {
        Map<String, String> m = names;
        boolean tiers = Config.INSTANCE.hideTier;
        if (m.isEmpty() && !tiers) return text;
        String plain = text.getString();
        boolean touch = tiers && TIER.matcher(plain).find();
        for (String real : m.keySet()) if (!touch && plain.contains(real)) touch = true;
        if (!touch) return text;
        MutableText out = Text.empty();
        text.visit((style, s) -> {
            out.append(Text.literal(fix(s, m, tiers)).setStyle(style));
            return Optional.empty();
        }, Style.EMPTY);
        return out;
    }

    private static String fix(String s, Map<String, String> m, boolean tiers) {
        for (var e : m.entrySet()) s = s.replace(e.getKey(), e.getValue());
        return tiers ? TIER.matcher(s).replaceAll("") : s;
    }

    /** Whether this player should be shown with a default skin. */
    public static boolean hideSkin(UUID id) {
        Config cfg = Config.INSTANCE;
        if (id == null) return false;
        return id.equals(self) ? cfg.fakeOwn && cfg.fakeOwnSkin : cfg.fakeEnemies && cfg.fakeEnemySkin;
    }

    private NameFaker() {}
}
