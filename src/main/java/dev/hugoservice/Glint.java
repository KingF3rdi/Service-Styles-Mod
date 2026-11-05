/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.mojang.blaze3d.systems.RenderSystem
 *  com.mojang.blaze3d.textures.FilterMode
 *  net.minecraft.client.texture.NativeImage
 *  net.minecraft.client.texture.NativeImageBackedTexture
 *  net.minecraft.client.texture.AbstractTexture
 *  net.minecraft.client.texture.ResourceTexture
 *  net.minecraft.client.texture.ReloadableTexture
 *  net.minecraft.util.Identifier
 *  net.minecraft.client.MinecraftClient
 */
package dev.hugoservice;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.textures.FilterMode;
import dev.hugoservice.Config;
import dev.hugoservice.HugoService;
import dev.hugoservice.Images;
import java.awt.Color;
import java.io.InputStream;
import net.minecraft.client.texture.NativeImage;
import net.minecraft.client.texture.NativeImageBackedTexture;
import net.minecraft.client.texture.AbstractTexture;
import net.minecraft.client.texture.ResourceTexture;
import net.minecraft.client.texture.ReloadableTexture;
import net.minecraft.util.Identifier;
import net.minecraft.client.MinecraftClient;

public final class Glint {
    public static final String[] MODES = new String[]{"Vanilla", "Own colour", "Gradient", "Rainbow"};
    private static final Identifier[] TEXTURES = new Identifier[]{Identifier.ofVanilla((String)"textures/misc/enchanted_glint_item.png"), Identifier.ofVanilla((String)"textures/misc/enchanted_glint_armor.png")};
    private static String applied = "";

    static void tick(MinecraftClient mc) {
        String want;
        Config cfg = Config.INSTANCE;
        String string = want = cfg.glintMode <= 0 ? "" : cfg.glintMode + ":" + cfg.glintColor + ":" + cfg.glintColor2;
        if (want.equals(applied) || mc.getResourceManager() == null) {
            return;
        }
        applied = want;
        for (Identifier id : TEXTURES) {
            if (want.isEmpty()) {
                mc.getTextureManager().registerTexture(id, (ReloadableTexture)new ResourceTexture(id));
                continue;
            }
            try (InputStream in = mc.getResourceManager().open(id);){
                NativeImage img = NativeImage.read((InputStream)in);
                Glint.recolour(img, cfg);
                mc.getTextureManager().registerTexture(id, (AbstractTexture)new NativeImageBackedTexture(() -> "hugoservice glint", img){
                    {
                        Mc.applyLinearSampler(this);
                    }
                });
            }
            catch (Exception e) {
                HugoService.LOG.warn("Could not recolour the enchant glint", (Throwable)e);
            }
        }
    }

    private static void recolour(NativeImage img, Config cfg) {
        int w = img.getWidth();
        int h = img.getHeight();
        for (int y = 0; y < h; ++y) {
            for (int x = 0; x < w; ++x) {
                int c = img.getColorArgb(x, y);
                float lum = (float)Math.max(c >> 16 & 0xFF, Math.max(c >> 8 & 0xFF, c & 0xFF)) / 255.0f;
                float f = (float)x / (float)Math.max(1, w - 1);
                int rgb = switch (cfg.glintMode) {
                    case 2 -> Images.mix(0xFF000000 | cfg.glintColor, 0xFF000000 | cfg.glintColor2, f) & 0xFFFFFF;
                    case 3 -> Color.HSBtoRGB(f, 0.7f, 1.0f) & 0xFFFFFF;
                    default -> cfg.glintColor;
                };
                int r = Math.round((float)(rgb >> 16 & 0xFF) * lum);
                int g = Math.round((float)(rgb >> 8 & 0xFF) * lum);
                int b = Math.round((float)(rgb & 0xFF) * lum);
                img.setColorArgb(x, y, c & 0xFF000000 | r << 16 | g << 8 | b);
            }
        }
    }

    private Glint() {
    }
}

