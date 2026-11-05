package dev.hugoservice;

import java.util.UUID;
import net.minecraft.text.Text;

/** Name faker / skin lookup needs 1.21.10+ auth and SkinTextures APIs. */
public final class NameFaker {
    static void tick(net.minecraft.client.MinecraftClient mc) {
    }

    public static Text apply(Text text) {
        return text;
    }

    public static Text apply(Text text, boolean tab) {
        return text;
    }

    public static Object skin(UUID id) {
        return null;
    }

    private NameFaker() {
    }
}
