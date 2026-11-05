/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  net.fabricmc.fabric.api.client.rendering.v1.hud.HudElementRegistry
 *  net.fabricmc.fabric.api.client.rendering.v1.hud.VanillaHudElements
 *  net.fabricmc.fabric.api.client.rendering.v1.world.WorldRenderContext
 *  net.fabricmc.fabric.api.client.rendering.v1.world.WorldRenderEvents
 *  net.minecraft.client.gl.RenderPipelines
 *  net.minecraft.text.StyleSpriteSource
 *  net.minecraft.text.StyleSpriteSource$Font
 *  net.minecraft.text.Text
 *  net.minecraft.text.Style
 *  net.minecraft.util.Identifier
 *  net.minecraft.client.MinecraftClient
 *  net.minecraft.client.font.TextRenderer
 *  net.minecraft.client.font.TextRenderer$TextLayerType
 *  net.minecraft.client.gui.DrawContext
 *  net.minecraft.util.math.MathHelper
 *  net.minecraft.client.util.math.MatrixStack
 *  net.minecraft.text.MutableText
 *  net.minecraft.text.StringVisitable
 *  net.minecraft.client.render.RenderTickCounter
 *  org.joml.Matrix3x2fStack
 *  org.joml.Quaternionf
 *  org.joml.Quaternionfc
 *  org.joml.Vector3f
 */
package dev.hugoservice;

import dev.hugoservice.Config;
import dev.hugoservice.Shapes;
import dev.hugoservice.Spotify;
import dev.hugoservice.StyleScreen;
import java.util.List;
import net.fabricmc.fabric.api.client.rendering.v1.hud.HudElementRegistry;
import net.fabricmc.fabric.api.client.rendering.v1.hud.VanillaHudElements;
import net.minecraft.client.gl.RenderPipelines;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.text.Text;
import net.minecraft.text.Style;
import net.minecraft.util.Identifier;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.util.math.MathHelper;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.text.MutableText;
import net.minecraft.text.StringVisitable;
import net.minecraft.client.render.RenderTickCounter;
import org.joml.Matrix3x2fStack;
import org.joml.Quaternionf;
import org.joml.Quaternionfc;
import org.joml.Vector3f;

public final class SpotifyHud {
    private static Style ui() {
        return Mc.uiStyle(false, false, false);
    }

    private static Style bold() {
        return Mc.uiStyle(false, false, true);
    }
    private static final float[] SIDE_YAW = new float[]{180.0f, 270.0f, 0.0f, 90.0f};
    private static int lastLine = -2;
    private static int skyLine = -2;
    private static String peekTrack = "";
    private static long peekAt;
    private static float skyYaw;
    private static long lineAt;

    static void init() {
        HudElementRegistry.attachElementBefore((Identifier)VanillaHudElements.CHAT, (Identifier)Identifier.of((String)"hugoservice", (String)"spotify"), SpotifyHud::render);
        Mc.registerSpotifySky();
    }

    private static void render(DrawContext ctx, RenderTickCounter tick) {
        MinecraftClient mc = MinecraftClient.getInstance();
        Config cfg = Config.INSTANCE;
        Spotify.Track t = Spotify.track;
        if (mc.options.hudHidden || t == null || !Spotify.active()) {
            return;
        }
        if (cfg.spotifyWindow) {
            SpotifyHud.window(ctx, mc, t, cfg.spotifyLyrics ? SpotifyHud.currentLine() : -1);
        }
        Shapes.flush();
    }

    private static void window(DrawContext ctx, MinecraftClient mc, Spotify.Track t, int line) {
        float px;
        Config cfg = Config.INSTANCE;
        if (!t.id().equals(peekTrack)) {
            peekTrack = t.id();
            peekAt = System.currentTimeMillis();
        }
        float slide = 0.0f;
        if (cfg.spotifyPeek) {
            long since = System.currentTimeMillis() - peekAt;
            if (since > 6000L) {
                return;
            }
            slide = since < 300L ? 1.0f - (float)since / 300.0f : (since > 5600L ? (float)(since - 5600L) / 400.0f : 0.0f);
        }
        TextRenderer tr = mc.textRenderer;
        List<Spotify.Line> lyrics = Spotify.lyrics;
        String lyric = line >= 0 && line < lyrics.size() ? lyrics.get(line).text() : null;
        int w = 176;
        int h = lyric != null && !lyric.isEmpty() ? 58 : 46;
        int m = 6;
        float k = (float)Math.max(50, Math.min(200, cfg.spotifyScale)) / 100.0f;
        int sw = ctx.getScaledWindowWidth();
        int sh = ctx.getScaledWindowHeight();
        int corner = cfg.spotifyCorner;
        float f = px = corner % 2 == 0 ? (float)m : (float)sw - (float)w * k - (float)m;
        float py = corner < 2 ? (float)m : (float)sh - (float)h * k - (float)m - (float)(corner == 2 ? 40 : 0);
        int n = corner % 2 == 0 ? -1 : 1;
        Matrix3x2fStack mat = ctx.getMatrices();
        mat.pushMatrix();
        mat.translate(px += (float)n * slide * ((float)w * k + (float)m), py);
        mat.scale(k, k);
        int x = 0;
        int y = 0;
        StyleScreen.glass(ctx, x, y, w, h, 10.0f, -1341450974, -1660944385);
        int c = 34;
        if (Spotify.coverReady) {
            ctx.drawTexture(RenderPipelines.GUI_TEXTURED, Spotify.COVER, x + 6, y + 6, 0.0f, 0.0f, c, c, 64, 64, 64, 64);
        } else {
            StyleScreen.round(ctx, x + 6, y + 6, c, c, 6.0f, -14829228);
        }
        int tx = x + 6 + c + 7;
        int tw = w - (tx - x) - 8;
        ctx.drawText(tr, (Text)Text.literal((String)SpotifyHud.cut(tr, t.name(), tw, bold())).setStyle(bold()), tx, y + 8, -853761, false);
        ctx.drawText(tr, (Text)Text.literal((String)SpotifyHud.cut(tr, t.artists(), tw, ui())).setStyle(ui()), tx, y + 19, -6047537, false);
        long p = Spotify.progress();
        long d = Math.max(1L, t.durationMs());
        String time = SpotifyHud.clock(p) + " / " + SpotifyHud.clock(d);
        int barY = y + 33;
        int timeW = tr.getWidth((StringVisitable)Text.literal((String)time).setStyle(ui()));
        int bw = tw - timeW - 5;
        StyleScreen.round(ctx, tx, barY, bw, 3.0f, 1.5f, 0x40FFFFFF);
        StyleScreen.round(ctx, tx, barY, Math.max(3.0f, (float)bw * (float)p / (float)d), 3.0f, 1.5f, Spotify.playing ? -14829228 : -9600357);
        ctx.drawText(tr, (Text)Text.literal((String)time).setStyle(ui()), tx + bw + 5, barY - 3, -9600357, false);
        if (lyric != null && !lyric.isEmpty()) {
            float fade = Math.min(1.0f, (float)(System.currentTimeMillis() - lineAt) / 250.0f);
            int col = Math.round(255.0f * fade) << 24 | 0x8AD6FF;
            ctx.drawText(tr, (Text)Text.literal((String)SpotifyHud.cut(tr, lyric, w - 12, ui())).setStyle(ui()), x + 6, y + 45, col, false);
        }
        mat.popMatrix();
    }

    static void sky(MatrixStack m, VertexConsumerProvider consumers) {
        float yaw;
        MinecraftClient mc = MinecraftClient.getInstance();
        Config cfg = Config.INSTANCE;
        if (!cfg.spotifySky || mc.options.hudHidden || Spotify.track == null || !Spotify.active()) {
            return;
        }
        int line = SpotifyHud.currentLine();
        List<Spotify.Line> lyrics = Spotify.lyrics;
        if (line < 0 || line >= lyrics.size() || lyrics.get(line).text().isEmpty()) {
            return;
        }
        float pitch = -28.0f;
        if (cfg.spotifySkyFollow) {
            float look = mc.gameRenderer.getCamera().getYaw();
            if (line != skyLine) {
                skyLine = line;
                if (Float.isNaN(skyYaw) || Math.abs(MathHelper.wrapDegrees((float)(look - skyYaw))) > 40.0f) {
                    skyYaw = look;
                }
            }
            yaw = skyYaw;
        } else {
            yaw = SIDE_YAW[Math.floorMod(cfg.spotifySkySide, 4)];
        }
        float dist = Math.max(48.0f, Math.min(128.0f, (float)mc.options.getClampedViewDistance() * 8.0f));
        float scale = dist * 0.0105f;
        Vector3f at = SpotifyHud.dir(yaw, pitch).mul(dist);
        m.push();
        m.translate(at.x, at.y, at.z);
        m.multiply((Quaternionfc)new Quaternionf().rotationYXZ((float)Math.PI - (float)Math.toRadians(yaw), (float)Math.toRadians(-pitch), 0.0f));
        m.scale(scale, -scale, scale);
        TextRenderer tr = mc.textRenderer;
        float fade = Math.min(1.0f, (float)(System.currentTimeMillis() - lineAt) / 400.0f);
        MutableText big = Text.literal((String)lyrics.get(line).text()).setStyle(bold());
        tr.draw((Text)big, (float)(-tr.getWidth((StringVisitable)big)) / 2.0f, -4.0f, Math.max(26, Math.round(235.0f * fade)) << 24 | 0xFFFFFF, true, m.peek().getPositionMatrix(), consumers, TextRenderer.TextLayerType.NORMAL, 0, 0xF000F0);
        if (line + 1 < lyrics.size() && !lyrics.get(line + 1).text().isEmpty()) {
            MutableText next = Text.literal((String)lyrics.get(line + 1).text()).setStyle(ui());
            m.scale(0.5f, 0.5f, 0.5f);
            tr.draw((Text)next, (float)(-tr.getWidth((StringVisitable)next)) / 2.0f, 14.0f, Math.max(26, Math.round(150.0f * fade)) << 24 | 0xCFE8FF, true, m.peek().getPositionMatrix(), consumers, TextRenderer.TextLayerType.NORMAL, 0, 0xF000F0);
        }
        m.pop();
    }

    private static int currentLine() {
        int line = Spotify.lineIndex();
        if (line != lastLine) {
            lastLine = line;
            lineAt = System.currentTimeMillis();
        }
        return line;
    }

    private static Vector3f dir(float yaw, float pitch) {
        double y = Math.toRadians(yaw);
        double p = Math.toRadians(pitch);
        return new Vector3f((float)(-Math.sin(y) * Math.cos(p)), (float)(-Math.sin(p)), (float)(Math.cos(y) * Math.cos(p)));
    }

    private static String cut(TextRenderer tr, String s, int max, Style style) {
        if (tr.getWidth((StringVisitable)Text.literal((String)s).setStyle(style)) <= max) {
            return s;
        }
        while (!s.isEmpty() && tr.getWidth((StringVisitable)Text.literal((String)(s + "...")).setStyle(style)) > max) {
            s = s.substring(0, s.length() - 1);
        }
        return s + "...";
    }

    private static String clock(long ms) {
        long s = ms / 1000L;
        return s / 60L + ":" + String.format("%02d", s % 60L);
    }

    private SpotifyHud() {
    }

    static {
        skyYaw = Float.NaN;
    }
}

