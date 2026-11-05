/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  net.minecraft.client.texture.NativeImage
 *  net.minecraft.client.texture.NativeImageBackedTexture
 *  net.minecraft.client.texture.AbstractTexture
 *  net.minecraft.util.Identifier
 *  net.minecraft.client.MinecraftClient
 */
package dev.hugoservice;

import dev.hugoservice.Config;
import dev.hugoservice.HugoService;
import java.io.InputStream;
import net.minecraft.client.texture.NativeImage;
import net.minecraft.client.texture.NativeImageBackedTexture;
import net.minecraft.client.texture.AbstractTexture;
import net.minecraft.util.Identifier;
import net.minecraft.client.MinecraftClient;

public final class CrystalCore {
    private static final Identifier ID = Identifier.of((String)"hugoservice", (String)"crystal_core");
    private static final Identifier SOURCE = Identifier.ofVanilla((String)"textures/entity/end_crystal/end_crystal.png");
    private static NativeImageBackedTexture texture;
    private static Integer made;
    private static int builtFor;
    private static int reloads;

    public static Identifier texture() {
        Integer rgb = Config.INSTANCE.crystalCore;
        if (rgb == null) {
            return null;
        }
        if (texture == null || !rgb.equals(made) || builtFor != reloads) {
            CrystalCore.build(rgb);
        }
        return texture == null ? null : ID;
    }

    static void invalidate() {
        ++reloads;
    }

    private static void build(int rgb) {
        MinecraftClient mc = MinecraftClient.getInstance();
        made = rgb;
        builtFor = reloads;
        try (InputStream in = mc.getResourceManager().open(SOURCE);){
            NativeImage img = NativeImage.read((InputStream)in);
            int k = Math.max(1, img.getWidth() / 64);
            for (int y = 0; y < 16 * k && y < img.getHeight(); ++y) {
                for (int x = 32 * k; x < 64 * k && x < img.getWidth(); ++x) {
                    int c = img.getColorArgb(x, y);
                    float lum = (float)Math.max(c >> 16 & 0xFF, Math.max(c >> 8 & 0xFF, c & 0xFF)) / 255.0f;
                    float f = 0.25f + 0.75f * lum;
                    int r = Math.round((float)(rgb >> 16 & 0xFF) * f);
                    int g = Math.round((float)(rgb >> 8 & 0xFF) * f);
                    int b = Math.round((float)(rgb & 0xFF) * f);
                    img.setColorArgb(x, y, c & 0xFF000000 | r << 16 | g << 8 | b);
                }
            }
            if (texture == null) {
                texture = new NativeImageBackedTexture(() -> "hugoservice crystal core", img);
                mc.getTextureManager().registerTexture(ID, (AbstractTexture)texture);
            } else {
                texture.setImage(img);
            }
            texture.upload();
        }
        catch (Exception e) {
            HugoService.LOG.warn("Could not recolour the end crystal core", (Throwable)e);
        }
    }

    private CrystalCore() {
    }

    static {
        builtFor = -1;
    }
}

