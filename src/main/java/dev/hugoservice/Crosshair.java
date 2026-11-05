/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  net.minecraft.client.gl.RenderPipelines
 *  net.minecraft.entity.Entity
 *  net.minecraft.entity.player.PlayerEntity
 *  net.minecraft.client.MinecraftClient
 *  net.minecraft.client.gui.DrawContext
 *  org.joml.Matrix3x2fStack
 */
package dev.hugoservice;

import dev.hugoservice.Config;
import dev.hugoservice.Images;
import dev.hugoservice.Shapes;
import dev.hugoservice.StyleScreen;
import java.util.List;
import net.minecraft.client.gl.RenderPipelines;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import org.joml.Matrix3x2fStack;

public final class Crosshair {
    public static final List<String> STYLES = List.of("cross", "dot", "circle", "t", "x", "square", "pixel");
    public static final List<Preset> PRESETS = List.of(new Preset("Classic", new Spec("cross", 8, 4, 2, 1, 0, 0xFFFFFF, 100)), new Preset("Small", new Spec("cross", 5, 3, 2, 1, 0, 0xFFFFFF, 100)), new Preset("Dot", new Spec("dot", 0, 0, 2, 1, 5, 0xFFFFFF, 100)), new Preset("Circle Dot", new Spec("circle", 4, 9, 2, 1, 3, 0xFFFFFF, 100)), new Preset("T-Shape", new Spec("t", 8, 4, 2, 1, 0, 7132159, 100)), new Preset("Green Plus", new Spec("cross", 9, 0, 2, 1, 0, 6090874, 100)), new Preset("X", new Spec("x", 7, 4, 2, 1, 0, 0xFFFFFF, 100)), new Preset("Box", new Spec("square", 4, 7, 2, 1, 3, 0xFFFFFF, 100)), new Preset("Sniper", new Spec("cross", 14, 8, 1, 0, 3, 0xFF4F4F, 100)), new Preset("Pink", new Spec("cross", 6, 5, 2, 1, 3, 16735432, 100)), new Preset("Ghost", new Spec("cross", 8, 4, 2, 0, 0, 0xFFFFFF, 55)), new Preset("Pixel art", new Spec("pixel", 0, 0, 0, 0, 0, 0xFFFFFF, 100)));
    private static int fade;

    public static boolean active() {
        return Config.INSTANCE.chOn;
    }

    public static boolean onTarget() {
        Config c = Config.INSTANCE;
        Entity e = MinecraftClient.getInstance().targetedEntity;
        return c.chEntity && e != null && e.isAlive() && (!c.chEntityPlayers || e instanceof PlayerEntity);
    }

    public static void drawCurrent(DrawContext ctx, float cx, float cy) {
        Config c = Config.INSTANCE;
        boolean target = Crosshair.onTarget();
        Crosshair.draw(ctx, Spec.of(c), cx, cy, target ? c.chEntityColor : c.chColor, target && c.chEntityMarker, c.crosshair, target);
        Shapes.flush();
    }

    public static void draw(DrawContext ctx, Spec s, float cx, float cy, int color, boolean marker, int[] pixels) {
        Crosshair.draw(ctx, s, cx, cy, color, marker, pixels, false);
    }

    public static void draw(DrawContext ctx, Spec s, float cx, float cy, int color, boolean marker, int[] pixels, boolean tintPixels) {
        int scale = StyleScreen.scale();
        int unit = Math.max(1, scale / 2);
        int a = Math.round((float)(255 * Math.max(0, Math.min(100, s.alpha()))) / 100.0f);
        fade = Config.INSTANCE.chGradient ? a << 24 | Config.INSTANCE.chColor2 & 0xFFFFFF : 0;
        int col = a << 24 | color & 0xFFFFFF;
        int black = Math.round((float)a * 0.85f) << 24;
        Matrix3x2fStack m = ctx.getMatrices();
        m.pushMatrix();
        m.scale(1.0f / (float)scale, 1.0f / (float)scale);
        int x = Math.round(cx * (float)scale);
        int y = Math.round(cy * (float)scale);
        int len = s.length() * unit;
        int gap = s.gap() * unit;
        int t = Math.max(1, s.thickness() * unit);
        int o = s.outline() * unit;
        switch (s.style()) {
            case "pixel": {
                if (pixels == null) break;
                int px = scale;
                int ox = x - 7 * px - px / 2;
                int oy = y - 7 * px - px / 2;
                for (int i = 0; i < 225; ++i) {
                    int p = pixels[i];
                    if (p >>> 24 == 0) continue;
                    int c = Math.round((float)((p >>> 24) * a) / 255.0f) << 24 | (tintPixels ? color : p) & 0xFFFFFF;
                    int qx = ox + i % 15 * px;
                    int qy = oy + i / 15 * px;
                    ctx.fill(qx, qy, qx + px, qy + px, c);
                }
                break;
            }
            case "circle": {
                int r = gap + Math.max(t, len / 2);
                Crosshair.ring(ctx, x, y, r + o, t + 2 * o, black, o > 0);
                Crosshair.ring(ctx, x, y, r, t, col, true);
                break;
            }
            case "square": {
                int h = gap + len;
                if (o > 0) {
                    Crosshair.frame(ctx, x - h - o, y - h - o, 2 * (h + o), t + 2 * o, black);
                }
                Crosshair.frame(ctx, x - h, y - h, 2 * h, t, col);
                break;
            }
            case "x": {
                m.pushMatrix();
                m.translate((float)x, (float)y);
                m.rotate((float)Math.toRadians(45.0));
                Crosshair.bars(ctx, 0, 0, len, gap, t, o, col, black, true);
                m.popMatrix();
                break;
            }
            case "t": {
                Crosshair.bars(ctx, x, y, len, gap, t, o, col, black, false);
                break;
            }
            case "dot": {
                break;
            }
            default: {
                Crosshair.bars(ctx, x, y, len, gap, t, o, col, black, true);
            }
        }
        if (s.dot() > 0 || s.style().equals("dot")) {
            int d = Math.max(1, (s.dot() > 0 ? s.dot() : 2) * unit);
            Crosshair.box(ctx, x - d / 2, y - d / 2, d, d, o, col, black);
        }
        if (marker) {
            m.pushMatrix();
            m.translate((float)x, (float)y);
            m.rotate((float)Math.toRadians(45.0));
            int mg = Math.max(gap + len, 4 * unit) + 2 * unit;
            int ml = 3 * unit;
            int mt = Math.max(1, unit);
            Crosshair.bars(ctx, 0, 0, ml, mg, mt, unit, col, black, true);
            m.popMatrix();
        }
        m.popMatrix();
    }

    private static void bars(DrawContext ctx, int x, int y, int len, int gap, int t, int o, int col, int black, boolean top) {
        if (len <= 0) {
            return;
        }
        int h = t / 2;
        if (top) {
            Crosshair.arm(ctx, x - h, y - gap - len, t, len, o, col, black, 0);
        }
        Crosshair.arm(ctx, x - h, y + gap + t % 2, t, len, o, col, black, 1);
        Crosshair.arm(ctx, x - gap - len, y - h, len, t, o, col, black, 2);
        Crosshair.arm(ctx, x + gap + t % 2, y - h, len, t, o, col, black, 3);
    }

    private static void arm(DrawContext ctx, int x, int y, int w, int h, int o, int col, int black, int dir) {
        if (fade == 0) {
            Crosshair.box(ctx, x, y, w, h, o, col, black);
            return;
        }
        if (o > 0) {
            ctx.fill(x - o, y - o, x + w + o, y + h + o, black);
        }
        switch (dir) {
            case 0: {
                ctx.fillGradient(x, y, x + w, y + h, fade, col);
                break;
            }
            case 1: {
                ctx.fillGradient(x, y, x + w, y + h, col, fade);
                break;
            }
            default: {
                for (int i = 0; i < w; ++i) {
                    float f = (float)(dir == 2 ? w - 1 - i : i) / (float)Math.max(1, w - 1);
                    ctx.fill(x + i, y, x + i + 1, y + h, Images.mix(col, fade, f));
                }
            }
        }
    }

    private static void box(DrawContext ctx, int x, int y, int w, int h, int o, int col, int black) {
        if (o > 0) {
            ctx.fill(x - o, y - o, x + w + o, y + h + o, black);
        }
        ctx.fill(x, y, x + w, y + h, col);
    }

    private static void frame(DrawContext ctx, int x, int y, int size, int t, int col) {
        ctx.fill(x, y, x + size, y + t, col);
        ctx.fill(x, y + size - t, x + size, y + size, col);
        ctx.fill(x, y + t, x + t, y + size - t, col);
        ctx.fill(x + size - t, y + t, x + size, y + size - t, col);
    }

    private static void ring(DrawContext ctx, int x, int y, int r, int t, int col, boolean draw) {
        if (!draw || r <= 0) {
            return;
        }
        Shapes.Sprite sp = Shapes.get(2 * r, 2 * r, r, Math.min(t, r), 15);
        if (sp == null) {
            return;
        }
        ctx.drawTexture(RenderPipelines.GUI_TEXTURED, Shapes.ID, x - r, y - r, (float)sp.u(), (float)sp.v(), 2 * r, 2 * r, 2 * r, 2 * r, 2048, 2048, col);
    }

    public static void apply(Spec s) {
        Config c = Config.INSTANCE;
        c.chOn = true;
        c.chStyle = s.style();
        if (!s.style().equals("pixel")) {
            c.chLength = s.length();
            c.chGap = s.gap();
            c.chThickness = s.thickness();
            c.chOutline = s.outline();
            c.chDot = s.dot();
            c.chColor = s.color();
            c.chAlpha = s.alpha();
        }
        Config.save();
    }

    private Crosshair() {
    }

    public record Spec(String style, int length, int gap, int thickness, int outline, int dot, int color, int alpha) {
        static Spec of(Config c) {
            return new Spec(c.chStyle, c.chLength, c.chGap, c.chThickness, c.chOutline, c.chDot, c.chColor, c.chAlpha);
        }
    }

    public record Preset(String name, Spec spec) {
    }
}

