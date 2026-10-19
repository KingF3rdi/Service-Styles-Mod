package dev.hugoservice;

import net.fabricmc.fabric.api.client.rendering.v1.hud.HudElementRegistry;
import net.fabricmc.fabric.api.client.rendering.v1.hud.VanillaHudElements;
import net.fabricmc.fabric.api.client.rendering.v1.world.WorldRenderContext;
import net.fabricmc.fabric.api.client.rendering.v1.world.WorldRenderEvents;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gl.RenderPipelines;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.render.LightmapTextureManager;
import net.minecraft.client.render.RenderTickCounter;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.text.StyleSpriteSource;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.MathHelper;
import org.joml.Matrix3x2fStack;
import org.joml.Quaternionf;
import org.joml.Vector3f;

import java.util.List;

/** Spotify on screen: a glass "now playing" window in a corner and, optionally, the lyrics as 3D text in the sky. */
public final class SpotifyHud {
    private static final net.minecraft.text.Style UI = net.minecraft.text.Style.EMPTY
        .withFont(new StyleSpriteSource.Font(Identifier.of("hugoservice", "ui")));
    private static final net.minecraft.text.Style BOLD = net.minecraft.text.Style.EMPTY
        .withFont(new StyleSpriteSource.Font(Identifier.of("hugoservice", "bold")));
    /** Minecraft yaw of north, east, south, west (yaw 0 looks south). */
    private static final float[] SIDE_YAW = {180, 270, 0, 90};
    private static int lastLine = -2, skyLine = -2;
    private static String peekTrack = "";
    private static long peekAt;
    private static float skyYaw = Float.NaN;
    private static long lineAt;

    static void init() {
        HudElementRegistry.attachElementBefore(VanillaHudElements.CHAT, Identifier.of("hugoservice", "spotify"), SpotifyHud::render);
        WorldRenderEvents.AFTER_ENTITIES.register(SpotifyHud::sky);
    }

    private static void render(DrawContext ctx, RenderTickCounter tick) {
        MinecraftClient mc = MinecraftClient.getInstance();
        Config cfg = Config.INSTANCE;
        Spotify.Track t = Spotify.track;
        if (mc.options.hudHidden || t == null || !Spotify.active()) return;
        if (cfg.spotifyWindow) window(ctx, mc, t, cfg.spotifyLyrics ? currentLine() : -1);
        Shapes.flush();
    }

    // ---- corner window ----

    private static void window(DrawContext ctx, MinecraftClient mc, Spotify.Track t, int line) {
        Config cfg = Config.INSTANCE;
        if (!t.id().equals(peekTrack)) { peekTrack = t.id(); peekAt = System.currentTimeMillis(); }
        float slide = 0;
        if (cfg.spotifyPeek) { // only for a moment when a song starts: slides in, stays, slides out
            long since = System.currentTimeMillis() - peekAt;
            if (since > 6000) return;
            slide = since < 300 ? 1 - since / 300f : since > 5600 ? (since - 5600) / 400f : 0;
        }
        TextRenderer tr = mc.textRenderer;
        List<Spotify.Line> lyrics = Spotify.lyrics;
        String lyric = line >= 0 && line < lyrics.size() ? lyrics.get(line).text() : null;
        int w = 176, h = lyric != null && !lyric.isEmpty() ? 58 : 46, m = 6;
        float k = Math.max(50, Math.min(200, cfg.spotifyScale)) / 100f;
        int sw = ctx.getScaledWindowWidth(), sh = ctx.getScaledWindowHeight();
        int corner = cfg.spotifyCorner;
        float px = corner % 2 == 0 ? m : sw - w * k - m;
        float py = corner < 2 ? m : sh - h * k - m - (corner == 2 ? 40 : 0); // bottom left: above the chat line
        px += (corner % 2 == 0 ? -1 : 1) * slide * (w * k + m);
        Matrix3x2fStack mat = ctx.getMatrices();
        mat.pushMatrix();
        mat.translate(px, py);
        mat.scale(k, k);
        int x = 0, y = 0;
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
        mat.popMatrix();
    }

    // ---- lyrics on the sky ----

    /**
     * The current line hangs 28° above the horizon, as real 3D text in the world: over a fixed side, or (follow view)
     * moving to where you look once a line is over. It stays put while you look around, is seen in perspective and
     * blocks in front cover it.
     */
    private static void sky(WorldRenderContext ctx) {
        MinecraftClient mc = MinecraftClient.getInstance();
        Config cfg = Config.INSTANCE;
        if (!cfg.spotifySky || mc.options.hudHidden || Spotify.track == null || !Spotify.active()) return;
        int line = currentLine();
        List<Spotify.Line> lyrics = Spotify.lyrics;
        if (line < 0 || line >= lyrics.size() || lyrics.get(line).text().isEmpty()) return;
        float yaw, pitch = -28;
        if (cfg.spotifySkyFollow) {
            // moves to where you look, but only once the line is over, and only when you look elsewhere
            float look = mc.gameRenderer.getCamera().getYaw();
            if (line != skyLine) {
                skyLine = line;
                if (Float.isNaN(skyYaw) || Math.abs(MathHelper.wrapDegrees(look - skyYaw)) > 40) skyYaw = look;
            }
            yaw = skyYaw;
        } else {
            yaw = SIDE_YAW[Math.floorMod(cfg.spotifySkySide, 4)];
        }
        // far enough to look like sky, near enough to stay inside the view distance and its fog
        float dist = Math.max(48, Math.min(128, mc.options.getClampedViewDistance() * 8f)), scale = dist * 0.0105f;
        Vector3f at = dir(yaw, pitch).mul(dist);
        MatrixStack m = ctx.matrices();
        m.push();
        m.translate(at.x, at.y, at.z);
        m.multiply(new Quaternionf().rotationYXZ((float) Math.PI - (float) Math.toRadians(yaw), (float) Math.toRadians(-pitch), 0)); // faces you, like a name tag would from there
        m.scale(scale, -scale, scale);
        TextRenderer tr = mc.textRenderer;
        float fade = Math.min(1, (System.currentTimeMillis() - lineAt) / 400f);
        Text big = Text.literal(lyrics.get(line).text()).setStyle(BOLD);
        tr.draw(big, -tr.getWidth(big) / 2f, -4, Math.max(26, Math.round(235 * fade)) << 24 | 0xFFFFFF, true,
            m.peek().getPositionMatrix(), ctx.consumers(), TextRenderer.TextLayerType.NORMAL, 0, LightmapTextureManager.MAX_LIGHT_COORDINATE);
        if (line + 1 < lyrics.size() && !lyrics.get(line + 1).text().isEmpty()) {
            Text next = Text.literal(lyrics.get(line + 1).text()).setStyle(UI);
            m.scale(0.5f, 0.5f, 0.5f);
            tr.draw(next, -tr.getWidth(next) / 2f, 14, Math.max(26, Math.round(150 * fade)) << 24 | 0xCFE8FF, true,
                m.peek().getPositionMatrix(), ctx.consumers(), TextRenderer.TextLayerType.NORMAL, 0, LightmapTextureManager.MAX_LIGHT_COORDINATE);
        }
        m.pop();
    }

    /** Line playing now; notes when it changed, for the fade in. */
    private static int currentLine() {
        int line = Spotify.lineIndex();
        if (line != lastLine) { lastLine = line; lineAt = System.currentTimeMillis(); }
        return line;
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
