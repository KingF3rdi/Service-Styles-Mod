package dev.hugoservice;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gl.RenderPipelines;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.PlayerEntity;
import org.joml.Matrix3x2fStack;

import java.util.List;

/**
 * Own crosshair, drawn by the mod in place of the vanilla sprite (no resource pack, no reload). Sizes are in
 * crosshair units: one unit is one screen pixel at GUI scale 2, so it looks the same size at every scale.
 */
public final class Crosshair {
    public static final List<String> STYLES = List.of("cross", "dot", "circle", "t", "x", "square", "pixel");

    /** Everything that decides how a crosshair looks. */
    public record Spec(String style, int length, int gap, int thickness, int outline, int dot, int color, int alpha) {
        static Spec of(Config c) {
            return new Spec(c.chStyle, c.chLength, c.chGap, c.chThickness, c.chOutline, c.chDot, c.chColor, c.chAlpha);
        }
    }

    public record Preset(String name, Spec spec) {}

    public static final List<Preset> PRESETS = List.of(
        new Preset("Classic", new Spec("cross", 8, 4, 2, 1, 0, 0xFFFFFF, 100)),
        new Preset("Small", new Spec("cross", 5, 3, 2, 1, 0, 0xFFFFFF, 100)),
        new Preset("Dot", new Spec("dot", 0, 0, 2, 1, 5, 0xFFFFFF, 100)),
        new Preset("Circle Dot", new Spec("circle", 4, 9, 2, 1, 3, 0xFFFFFF, 100)),
        new Preset("T-Shape", new Spec("t", 8, 4, 2, 1, 0, 0x6CD3FF, 100)),
        new Preset("Green Plus", new Spec("cross", 9, 0, 2, 1, 0, 0x5CF07A, 100)),
        new Preset("X", new Spec("x", 7, 4, 2, 1, 0, 0xFFFFFF, 100)),
        new Preset("Box", new Spec("square", 4, 7, 2, 1, 3, 0xFFFFFF, 100)),
        new Preset("Sniper", new Spec("cross", 14, 8, 1, 0, 3, 0xFF4F4F, 100)),
        new Preset("Pink", new Spec("cross", 6, 5, 2, 1, 3, 0xFF5CC8, 100)),
        new Preset("Ghost", new Spec("cross", 8, 4, 2, 0, 0, 0xFFFFFF, 55)),
        new Preset("Pixel art", new Spec("pixel", 0, 0, 0, 0, 0, 0xFFFFFF, 100))
    );

    public static boolean active() {
        return Config.INSTANCE.chOn;
    }

    /** True while the crosshair is on an entity that the indicator should react to. */
    public static boolean onTarget() {
        Config c = Config.INSTANCE;
        Entity e = MinecraftClient.getInstance().targetedEntity;
        return c.chEntity && e != null && e.isAlive() && (!c.chEntityPlayers || e instanceof PlayerEntity);
    }

    /** Draws the configured crosshair centred on (cx, cy) in GUI coordinates. */
    public static void drawCurrent(DrawContext ctx, float cx, float cy) {
        Config c = Config.INSTANCE;
        boolean target = onTarget();
        draw(ctx, Spec.of(c), cx, cy, target ? c.chEntityColor : c.chColor, target && c.chEntityMarker, c.crosshair, target);
        Shapes.flush(); // the circle style may have added a ring to the atlas
    }

    /**
     * Draws a crosshair centred on (cx, cy) in GUI coordinates. {@code color} is RGB (the indicator passes its own),
     * {@code marker} adds four diagonal ticks around it, {@code pixels} is the 15x15 pixel art for the pixel style.
     */
    public static void draw(DrawContext ctx, Spec s, float cx, float cy, int color, boolean marker, int[] pixels) {
        draw(ctx, s, cx, cy, color, marker, pixels, false);
    }

    /** Same, {@code tintPixels} paints pixel art in {@code color} (the indicator) instead of its own colours. */
    public static void draw(DrawContext ctx, Spec s, float cx, float cy, int color, boolean marker, int[] pixels, boolean tintPixels) {
        int scale = StyleScreen.scale();
        int unit = Math.max(1, scale / 2);
        int a = Math.round(255 * Math.max(0, Math.min(100, s.alpha())) / 100f);
        fade = Config.INSTANCE.chGradient ? a << 24 | Config.INSTANCE.chColor2 & 0xFFFFFF : 0;
        int col = a << 24 | color & 0xFFFFFF, black = Math.round(a * 0.85f) << 24;
        Matrix3x2fStack m = ctx.getMatrices();
        m.pushMatrix();
        m.scale(1f / scale, 1f / scale);
        int x = Math.round(cx * scale), y = Math.round(cy * scale);
        int len = s.length() * unit, gap = s.gap() * unit, t = Math.max(1, s.thickness() * unit), o = s.outline() * unit;
        switch (s.style()) {
            case "pixel" -> {
                if (pixels != null) {
                    int px = scale, ox = x - 7 * px - px / 2, oy = y - 7 * px - px / 2;
                    for (int i = 0; i < 225; i++) {
                        int p = pixels[i];
                        if (p >>> 24 == 0) continue;
                        int c = Math.round((p >>> 24) * a / 255f) << 24 | (tintPixels ? color : p) & 0xFFFFFF;
                        int qx = ox + i % 15 * px, qy = oy + i / 15 * px;
                        ctx.fill(qx, qy, qx + px, qy + px, c);
                    }
                }
            }
            case "circle" -> {
                int r = gap + Math.max(t, len / 2);
                ring(ctx, x, y, r + o, t + 2 * o, black, o > 0);
                ring(ctx, x, y, r, t, col, true);
            }
            case "square" -> {
                int h = gap + len;
                if (o > 0) frame(ctx, x - h - o, y - h - o, 2 * (h + o), t + 2 * o, black);
                frame(ctx, x - h, y - h, 2 * h, t, col);
            }
            case "x" -> {
                m.pushMatrix();
                m.translate(x, y);
                m.rotate((float) Math.toRadians(45));
                bars(ctx, 0, 0, len, gap, t, o, col, black, true);
                m.popMatrix();
            }
            case "t" -> bars(ctx, x, y, len, gap, t, o, col, black, false);
            case "dot" -> {}
            default -> bars(ctx, x, y, len, gap, t, o, col, black, true);
        }
        if (s.dot() > 0 || s.style().equals("dot")) {
            int d = Math.max(1, (s.dot() > 0 ? s.dot() : 2) * unit);
            box(ctx, x - d / 2, y - d / 2, d, d, o, col, black);
        }
        if (marker) {
            m.pushMatrix();
            m.translate(x, y);
            m.rotate((float) Math.toRadians(45));
            int mg = Math.max(gap + len, 4 * unit) + 2 * unit, ml = 3 * unit, mt = Math.max(1, unit);
            bars(ctx, 0, 0, ml, mg, mt, unit, col, black, true);
            m.popMatrix();
        }
        m.popMatrix();
    }

    /** Four bars around (x, y); without the top one for the T style. */
    private static void bars(DrawContext ctx, int x, int y, int len, int gap, int t, int o, int col, int black, boolean top) {
        if (len <= 0) return;
        int h = t / 2;
        if (top) arm(ctx, x - h, y - gap - len, t, len, o, col, black, 0);
        arm(ctx, x - h, y + gap + (t % 2), t, len, o, col, black, 1);
        arm(ctx, x - gap - len, y - h, len, t, o, col, black, 2);
        arm(ctx, x + gap + (t % 2), y - h, len, t, o, col, black, 3);
    }

    /** Second colour of the gradient for the crosshair being drawn (0 = none). */
    private static int fade;

    /** One arm; with a gradient it fades from the centre colour to the far one (dir: 0 up, 1 down, 2 left, 3 right). */
    private static void arm(DrawContext ctx, int x, int y, int w, int h, int o, int col, int black, int dir) {
        if (fade == 0) { box(ctx, x, y, w, h, o, col, black); return; }
        if (o > 0) ctx.fill(x - o, y - o, x + w + o, y + h + o, black);
        switch (dir) {
            case 0 -> ctx.fillGradient(x, y, x + w, y + h, fade, col);
            case 1 -> ctx.fillGradient(x, y, x + w, y + h, col, fade);
            default -> {
                for (int i = 0; i < w; i++) {
                    float f = (dir == 2 ? w - 1 - i : i) / (float) Math.max(1, w - 1);
                    ctx.fill(x + i, y, x + i + 1, y + h, Knives.mix(col, fade, f));
                }
            }
        }
    }

    private static void box(DrawContext ctx, int x, int y, int w, int h, int o, int col, int black) {
        if (o > 0) ctx.fill(x - o, y - o, x + w + o, y + h + o, black);
        ctx.fill(x, y, x + w, y + h, col);
    }

    private static void frame(DrawContext ctx, int x, int y, int size, int t, int col) {
        ctx.fill(x, y, x + size, y + t, col);
        ctx.fill(x, y + size - t, x + size, y + size, col);
        ctx.fill(x, y + t, x + t, y + size - t, col);
        ctx.fill(x + size - t, y + t, x + size, y + size - t, col);
    }

    /** Anti-aliased ring of outer radius r and thickness t from the shape atlas. */
    private static void ring(DrawContext ctx, int x, int y, int r, int t, int col, boolean draw) {
        if (!draw || r <= 0) return;
        Shapes.Sprite sp = Shapes.get(2 * r, 2 * r, r, Math.min(t, r), 15);
        if (sp == null) return;
        ctx.drawTexture(RenderPipelines.GUI_TEXTURED, Shapes.ID, x - r, y - r, sp.u(), sp.v(), 2 * r, 2 * r, 2 * r, 2 * r, Shapes.SIZE, Shapes.SIZE, col);
    }

    /** Copies a preset into the config (pixel art keeps your painting). */
    public static void apply(Spec s) {
        Config c = Config.INSTANCE;
        c.chOn = true;
        c.chStyle = s.style();
        if (!s.style().equals("pixel")) {
            c.chLength = s.length(); c.chGap = s.gap(); c.chThickness = s.thickness();
            c.chOutline = s.outline(); c.chDot = s.dot(); c.chColor = s.color(); c.chAlpha = s.alpha();
        }
        Config.save();
    }

    private Crosshair() {}
}
