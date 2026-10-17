package dev.hugoservice;

import net.fabricmc.fabric.api.client.rendering.v1.hud.HudElementRegistry;
import net.fabricmc.fabric.api.client.rendering.v1.hud.VanillaHudElements;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gl.RenderPipelines;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.render.Camera;
import net.minecraft.client.render.RenderTickCounter;
import net.minecraft.text.StyleSpriteSource;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.MathHelper;
import org.joml.Matrix3x2fStack;
import org.joml.Vector3f;

import java.util.List;

/** Spotify on screen: a glass "now playing" window in a corner and, optionally, the lyrics written on the sky. */
public final class SpotifyHud {
    private static final net.minecraft.text.Style UI = net.minecraft.text.Style.EMPTY
        .withFont(new StyleSpriteSource.Font(Identifier.of("hugoservice", "ui")));
    private static final net.minecraft.text.Style BOLD = net.minecraft.text.Style.EMPTY
        .withFont(new StyleSpriteSource.Font(Identifier.of("hugoservice", "bold")));
    private static float skyYaw = Float.NaN;
    private static int lastLine = -2;
    private static long lineAt;

    static void init() {
        HudElementRegistry.attachElementBefore(VanillaHudElements.CHAT, Identifier.of("hugoservice", "spotify"), SpotifyHud::render);
    }

    private static void render(DrawContext ctx, RenderTickCounter tick) {
        MinecraftClient mc = MinecraftClient.getInstance();
        Config cfg = Config.INSTANCE;
        Spotify.Track t = Spotify.track;
        if (mc.options.hudHidden || t == null || !Spotify.connected()) return;
        int line = Spotify.lineIndex();
        if (line != lastLine) { lastLine = line; lineAt = System.currentTimeMillis(); }
        if (cfg.spotifySky && mc.world != null) sky(ctx, mc, line);
        if (cfg.spotifyWindow) window(ctx, mc, t, cfg.spotifyLyrics ? line : -1);
        Shapes.flush();
    }

    // ---- corner window ----

    private static void window(DrawContext ctx, MinecraftClient mc, Spotify.Track t, int line) {
        TextRenderer tr = mc.textRenderer;
        List<Spotify.Line> lyrics = Spotify.lyrics;
        String lyric = line >= 0 && line < lyrics.size() ? lyrics.get(line).text() : null;
        int w = 176, h = lyric != null && !lyric.isEmpty() ? 58 : 46, m = 6;
        int sw = ctx.getScaledWindowWidth(), sh = ctx.getScaledWindowHeight();
        int corner = Config.INSTANCE.spotifyCorner;
        int x = corner % 2 == 0 ? m : sw - w - m;
        int y = corner < 2 ? m : sh - h - m - (corner == 2 ? 40 : 0); // bottom left: above the chat line
        StyleScreen.glass(ctx, x, y, w, h, 10, 0xB00B1522, 0x9CFFFFFF);
        int c = 34;
        if (Spotify.coverReady) {
            ctx.drawTexture(RenderPipelines.GUI_TEXTURED, Spotify.COVER, x + 6, y + 6, 0, 0, c, c, 64, 64, 64, 64);
        } else {
            StyleScreen.round(ctx, x + 6, y + 6, c, c, 6, 0xFF1DB954);
        }
        int tx = x + 6 + c + 7, tw = w - (tx - x) - 8;
        ctx.drawText(tr, Text.literal(cut(tr, t.name(), tw, BOLD)).setStyle(BOLD), tx, y + 8, 0xFFF2F8FF, false);
        ctx.drawText(tr, Text.literal(cut(tr, t.artists(), tw, UI)).setStyle(UI), tx, y + 19, 0xFFA3B8CF, false);
        long p = Spotify.progress(), d = Math.max(1, t.durationMs());
        String time = clock(p) + " / " + clock(d);
        int barY = y + 33, timeW = tr.getWidth(Text.literal(time).setStyle(UI));
        int bw = tw - timeW - 5;
        StyleScreen.round(ctx, tx, barY, bw, 3, 1.5f, 0x40FFFFFF);
        StyleScreen.round(ctx, tx, barY, Math.max(3, bw * (float) p / d), 3, 1.5f, Spotify.playing ? 0xFF1DB954 : 0xFF6D829B);
        ctx.drawText(tr, Text.literal(time).setStyle(UI), tx + bw + 5, barY - 3, 0xFF6D829B, false);
        if (lyric != null && !lyric.isEmpty()) {
            float fade = Math.min(1, (System.currentTimeMillis() - lineAt) / 250f);
            int col = (Math.round(255 * fade) << 24) | 0x8AD6FF;
            ctx.drawText(tr, Text.literal(cut(tr, lyric, w - 12, UI)).setStyle(UI), x + 6, y + 45, col, false);
        }
    }

    // ---- lyrics on the sky ----

    /**
     * The current line hangs in the sky ahead of you, 28° above the horizon. Its direction follows your view slowly,
     * so it stays put while you look around and drifts back in front of you.
     */
    private static void sky(DrawContext ctx, MinecraftClient mc, int line) {
        List<Spotify.Line> lyrics = Spotify.lyrics;
        if (line < 0 || line >= lyrics.size()) return;
        String text = lyrics.get(line).text();
        if (text.isEmpty()) return;
        Camera cam = mc.gameRenderer.getCamera();
        float camYaw = cam.getYaw(), camPitch = cam.getPitch();
        if (Float.isNaN(skyYaw)) skyYaw = camYaw;
        skyYaw += MathHelper.wrapDegrees(camYaw - skyYaw) * 0.02f;
        float[] screen = project(ctx, mc, skyYaw, -28, camYaw, camPitch);
        if (screen == null) return;
        TextRenderer tr = mc.textRenderer;
        float fade = Math.min(1, (System.currentTimeMillis() - lineAt) / 400f);
        Text big = Text.literal(text).setStyle(BOLD);
        Matrix3x2fStack mat = ctx.getMatrices();
        mat.pushMatrix();
        mat.translate(screen[0], screen[1]);
        mat.scale(2.4f, 2.4f);
        int a = Math.round(235 * fade);
        ctx.drawText(tr, big, -tr.getWidth(big) / 2, -4, a << 24 | 0xFFFFFF, true);
        if (line + 1 < lyrics.size() && !lyrics.get(line + 1).text().isEmpty()) {
            Text next = Text.literal(lyrics.get(line + 1).text()).setStyle(UI);
            mat.scale(0.5f, 0.5f);
            ctx.drawText(tr, next, -tr.getWidth(next) / 2, 14, Math.round(150 * fade) << 24 | 0xCFE8FF, true);
        }
        mat.popMatrix();
    }

    /** Screen position of a direction (yaw, pitch in Minecraft degrees) for the current camera, or null behind it. */
    private static float[] project(DrawContext ctx, MinecraftClient mc, float yaw, float pitch, float camYaw, float camPitch) {
        Vector3f d = dir(yaw, pitch), f = dir(camYaw, camPitch);
        double cy = Math.toRadians(camYaw);
        Vector3f r = new Vector3f((float) -Math.cos(cy), 0, (float) -Math.sin(cy));
        Vector3f u = new Vector3f(r).cross(f);
        float z = d.dot(f);
        if (z < 0.15f) return null;
        float tanV = (float) Math.tan(Math.toRadians(mc.options.getFov().getValue()) / 2);
        float w = ctx.getScaledWindowWidth(), h = ctx.getScaledWindowHeight();
        float tanH = tanV * w / h;
        float nx = d.dot(r) / z / tanH, ny = d.dot(u) / z / tanV;
        return new float[] {w / 2 * (1 + nx), h / 2 * (1 - ny)};
    }

    private static Vector3f dir(float yaw, float pitch) {
        double y = Math.toRadians(yaw), p = Math.toRadians(pitch);
        return new Vector3f((float) (-Math.sin(y) * Math.cos(p)), (float) -Math.sin(p), (float) (Math.cos(y) * Math.cos(p)));
    }

    private static String cut(TextRenderer tr, String s, int max, net.minecraft.text.Style style) {
        if (tr.getWidth(Text.literal(s).setStyle(style)) <= max) return s;
        while (!s.isEmpty() && tr.getWidth(Text.literal(s + "...").setStyle(style)) > max) s = s.substring(0, s.length() - 1);
        return s + "...";
    }

    private static String clock(long ms) {
        long s = ms / 1000;
        return s / 60 + ":" + String.format("%02d", s % 60);
    }

    private SpotifyHud() {}
}
