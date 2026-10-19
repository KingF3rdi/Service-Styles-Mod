package dev.hugoservice;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.texture.NativeImage;
import net.minecraft.client.texture.NativeImageBackedTexture;
import net.minecraft.util.Identifier;

import java.io.InputStream;

/** End crystal core colour: the crystal texture (from your packs) with only the core cube recoloured. */
public final class CrystalCore {
    private static final Identifier ID = Identifier.of("hugoservice", "crystal_core");
    private static final Identifier SOURCE = Identifier.ofVanilla("textures/entity/end_crystal/end_crystal.png");
    private static NativeImageBackedTexture texture;
    private static Integer made;
    private static int builtFor = -1, reloads;

    /** Texture to draw crystals with, or null for the normal one. */
    public static Identifier texture() {
        Integer rgb = Config.INSTANCE.crystalCore;
        if (rgb == null) return null;
        if (texture == null || !rgb.equals(made) || builtFor != reloads) build(rgb);
        return texture == null ? null : ID;
    }

    /** Packs changed: take the crystal texture again next time. */
    static void invalidate() { reloads++; }

    private static void build(int rgb) {
        MinecraftClient mc = MinecraftClient.getInstance();
        made = rgb;
        builtFor = reloads;
        try (InputStream in = mc.getResourceManager().open(SOURCE)) {
            NativeImage img = NativeImage.read(in);
            int k = Math.max(1, img.getWidth() / 64);
            // the core cube's faces: u 32..64, v 0..16 of the 64x32 layout
            for (int y = 0; y < 16 * k && y < img.getHeight(); y++) for (int x = 32 * k; x < 64 * k && x < img.getWidth(); x++) {
                int c = img.getColorArgb(x, y);
                float lum = Math.max(c >> 16 & 255, Math.max(c >> 8 & 255, c & 255)) / 255f, f = 0.25f + 0.75f * lum;
                int r = Math.round((rgb >> 16 & 255) * f), g = Math.round((rgb >> 8 & 255) * f), b = Math.round((rgb & 255) * f);
                img.setColorArgb(x, y, c & 0xFF000000 | r << 16 | g << 8 | b);
            }
            if (texture == null) {
                texture = new NativeImageBackedTexture(() -> "hugoservice crystal core", img);
                mc.getTextureManager().registerTexture(ID, texture);
            } else {
                texture.setImage(img);
            }
            texture.upload();
        } catch (Exception e) {
            HugoService.LOG.warn("Could not recolour the end crystal core", e);
        }
    }

    private CrystalCore() {}
}
