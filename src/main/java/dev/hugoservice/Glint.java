package dev.hugoservice;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.textures.FilterMode;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.texture.NativeImage;
import net.minecraft.client.texture.NativeImageBackedTexture;
import net.minecraft.util.Identifier;

import java.io.InputStream;

/**
 * Enchant glint in your colour, a two-colour gradient or a rainbow: the glint textures (from your packs) are
 * recoloured and put in place of the originals; "vanilla" loads the originals again. Changes show at once.
 */
public final class Glint {
    public static final String[] MODES = {"Vanilla", "Own colour", "Gradient", "Rainbow"};
    private static final Identifier[] TEXTURES = {
        Identifier.ofVanilla("textures/misc/enchanted_glint_item.png"), Identifier.ofVanilla("textures/misc/enchanted_glint_armor.png")};
    private static String applied = "";

    /** Called every client tick; only does work when the choice changed. */
    static void tick(MinecraftClient mc) {
        Config cfg = Config.INSTANCE;
        String want = cfg.glintMode <= 0 ? "" : cfg.glintMode + ":" + cfg.glintColor + ":" + cfg.glintColor2;
        if (want.equals(applied) || mc.getResourceManager() == null) return;
        applied = want;
        for (Identifier id : TEXTURES) {
            if (want.isEmpty()) { // back to the pack's own glint
                mc.getTextureManager().registerTexture(id, new net.minecraft.client.texture.ResourceTexture(id)); // loads it right away
                continue;
            }
            try (InputStream in = mc.getResourceManager().open(id)) {
                NativeImage img = NativeImage.read(in);
                recolour(img, cfg);
                mc.getTextureManager().registerTexture(id, new NativeImageBackedTexture(() -> "hugoservice glint", img) {
                    { sampler = RenderSystem.getSamplerCache().getRepeated(FilterMode.LINEAR); } // the glint scrolls, so it must repeat
                });
            } catch (Exception e) {
                HugoService.LOG.warn("Could not recolour the enchant glint", e);
            }
        }
    }

    /** Keeps the glint's brightness pattern, the colour comes from the choice (across the texture for gradient and rainbow). */
    private static void recolour(NativeImage img, Config cfg) {
        int w = img.getWidth(), h = img.getHeight();
        for (int y = 0; y < h; y++) for (int x = 0; x < w; x++) {
            int c = img.getColorArgb(x, y);
            float lum = Math.max(c >> 16 & 255, Math.max(c >> 8 & 255, c & 255)) / 255f, f = x / (float) Math.max(1, w - 1);
            int rgb = switch (cfg.glintMode) {
                case 2 -> Knives.mix(0xFF000000 | cfg.glintColor, 0xFF000000 | cfg.glintColor2, f) & 0xFFFFFF;
                case 3 -> java.awt.Color.HSBtoRGB(f, 0.7f, 1f) & 0xFFFFFF;
                default -> cfg.glintColor;
            };
            int r = Math.round((rgb >> 16 & 255) * lum), g = Math.round((rgb >> 8 & 255) * lum), b = Math.round((rgb & 255) * lum);
            img.setColorArgb(x, y, c & 0xFF000000 | r << 16 | g << 8 | b);
        }
    }

    private Glint() {}
}
