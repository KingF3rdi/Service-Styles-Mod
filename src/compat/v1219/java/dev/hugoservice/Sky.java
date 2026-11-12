/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.mojang.blaze3d.buffers.GpuBuffer
 *  com.mojang.blaze3d.buffers.GpuBufferSlice
 *  com.mojang.blaze3d.systems.RenderPass
 *  com.mojang.blaze3d.systems.RenderSystem
 *  com.mojang.blaze3d.systems.RenderSystem$ShapeIndexBuffer
 *  com.mojang.blaze3d.textures.AddressMode
 *  com.mojang.blaze3d.textures.FilterMode
 *  com.mojang.blaze3d.vertex.VertexFormat$DrawMode
 *  net.minecraft.client.texture.NativeImage
 *  net.minecraft.client.texture.NativeImageBackedTexture
 *  net.minecraft.client.texture.AbstractTexture
 *  net.minecraft.client.gl.RenderPipelines
 *  net.minecraft.client.render.state.SkyRenderState
 *  net.minecraft.client.gl.Framebuffer
 *  net.minecraft.client.render.BufferBuilder
 *  net.minecraft.world.dimension.DimensionType$Skybox
 *  net.minecraft.client.render.VertexFormats
 *  net.minecraft.util.Identifier
 *  net.minecraft.client.MinecraftClient
 *  net.minecraft.client.util.BufferAllocator
 *  net.minecraft.client.render.BuiltBuffer
 *  org.joml.Matrix4f
 *  org.joml.Matrix4fc
 *  org.joml.Vector3f
 *  org.joml.Vector3fc
 *  org.joml.Vector4f
 *  org.joml.Vector4fc
 */
package dev.hugoservice;

import com.mojang.blaze3d.buffers.GpuBuffer;
import com.mojang.blaze3d.buffers.GpuBufferSlice;
import com.mojang.blaze3d.systems.RenderPass;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.textures.AddressMode;
import com.mojang.blaze3d.textures.FilterMode;
import com.mojang.blaze3d.vertex.VertexFormat;
import dev.hugoservice.Categories;
import dev.hugoservice.Config;
import dev.hugoservice.HugoService;
import dev.hugoservice.Images;
import dev.hugoservice.Styles;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.util.OptionalDouble;
import java.util.OptionalInt;
import net.minecraft.client.texture.NativeImage;
import net.minecraft.client.texture.NativeImageBackedTexture;
import net.minecraft.client.texture.AbstractTexture;
import net.minecraft.client.gl.RenderPipelines;
import net.minecraft.client.render.state.SkyRenderState;
import net.minecraft.client.gl.Framebuffer;
import net.minecraft.client.render.BufferBuilder;
import net.minecraft.world.dimension.DimensionType;
import net.minecraft.client.render.VertexFormats;
import net.minecraft.util.Identifier;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.util.BufferAllocator;
import net.minecraft.client.render.BuiltBuffer;
import org.joml.Matrix4f;
import org.joml.Matrix4fc;
import org.joml.Vector3f;
import org.joml.Vector3fc;
import org.joml.Vector4f;
import org.joml.Vector4fc;

public final class Sky {
    public static final String GROUP = "skybox";
    private static final int MAX_FACE = 1024;
    private static final Identifier TEXTURE_ID;
    static final Matrix4f[] TILE;
    static final Matrix4f[] WORLD;
    public static volatile SkyRenderState state;
    private static NativeImageBackedTexture texture;
    private static GpuBuffer cube;
    private static String loadedKey;
    private static String loadingKey;
    private static String failedKey;
    private static volatile NativeImage pending;
    private static volatile String pendingKey;

    public static boolean active() {
        SkyRenderState s = state;
        return texture != null && !loadedKey.equals("vanilla") && Mc.skyOverworld(s);
    }

    public static boolean render() {
        Sky.update();
        if (!Sky.active()) {
            return false;
        }
        Config cfg = Config.INSTANCE;
        SkyRenderState s = state;
        Matrix4f view = new Matrix4f((Matrix4fc)RenderSystem.getModelViewMatrix());
        view.rotate((float)Math.toRadians(-90.0), 0.0f, 1.0f, 0.0f);
        if (cfg.skyRotate) {
            view.rotate(Mc.sunAngle(s), 0.0f, 0.0f, 1.0f);
        }
        float b = 1.0f;
        if (cfg.skyNightFade) {
            b = Math.max(0.18f, 1.0f - s.starBrightness * 1.6f);
        }
        RenderSystem.ShapeIndexBuffer indices = RenderSystem.getSequentialBuffer((VertexFormat.DrawMode)VertexFormat.DrawMode.QUADS);
        GpuBuffer indexBuffer = indices.getIndexBuffer(36);
        Framebuffer fb = MinecraftClient.getInstance().getFramebuffer();
        GpuBufferSlice transforms = Mc.writeSkyTransforms((Matrix4fc)view, (Vector4fc)new Vector4f(b *= 1.0f - s.rainGradient * 0.3f, b, b, 1.0f));
        try (RenderPass pass = RenderSystem.getDevice().createCommandEncoder().createRenderPass(() -> "HugoService skybox", fb.getColorAttachmentView(), OptionalInt.empty(), fb.getDepthAttachmentView(), OptionalDouble.empty());){
            pass.setPipeline(RenderPipelines.POSITION_TEX_COLOR_END_SKY);
            RenderSystem.bindDefaultUniforms((RenderPass)pass);
            pass.setUniform("DynamicTransforms", transforms);
            Mc.bindSkySampler(pass, texture);
            pass.setVertexBuffer(0, cube);
            pass.setIndexBuffer(indexBuffer, indices.getIndexType());
            pass.drawIndexed(0, 0, 36, 1);
        }
        return true;
    }

    private static void update() {
        String want;
        NativeImage img = pending;
        if (img != null) {
            pending = null;
            if (texture != null) {
                MinecraftClient.getInstance().getTextureManager().destroyTexture(TEXTURE_ID);
            }
            texture = new NativeImageBackedTexture(() -> "hugoservice skybox", img);
            MinecraftClient.getInstance().getTextureManager().registerTexture(TEXTURE_ID, (AbstractTexture)texture);
            Sky.buildCube(img.getWidth(), img.getHeight());
            loadedKey = pendingKey;
        }
        if ((want = Config.INSTANCE.styleOf(GROUP)).equals(loadedKey) || want.equals(loadingKey) || want.equals(failedKey)) {
            return;
        }
        if (want.equals("vanilla")) {
            loadedKey = want;
            return;
        }
        loadingKey = want;
        Categories.Group g = Categories.allGroups().stream().filter(x -> x.id().equals(GROUP)).findFirst().orElseThrow();
        Styles.Style style = Styles.find(g, want);
        Thread t = new Thread(() -> {
            try {
                BufferedImage cubeImg = Sky.load(g, style);
                if (cubeImg != null) {
                    NativeImage ni = new NativeImage(cubeImg.getWidth(), cubeImg.getHeight(), false);
                    int w = cubeImg.getWidth();
                    int[] row = new int[w];
                    for (int y = 0; y < cubeImg.getHeight(); ++y) {
                        cubeImg.getRGB(0, y, w, 1, row, 0, w);
                        for (int x = 0; x < w; ++x) {
                            ni.setColorArgb(x, y, row[x] | 0xFF000000);
                        }
                    }
                    pendingKey = want;
                    pending = ni;
                } else {
                    failedKey = want;
                }
            }
            catch (Exception e) {
                failedKey = want;
                HugoService.LOG.warn("Could not load skybox {}", want, e);
            }
            finally {
                loadingKey = null;
            }
        }, "HugoService-Sky");
        t.setDaemon(true);
        t.start();
    }

    private static void buildCube(int w, int h) {
        if (cube != null) {
            cube.close();
        }
        try (BufferAllocator alloc = BufferAllocator.fixedSized((int)(24 * VertexFormats.POSITION_TEXTURE_COLOR.getVertexSize()));){
            BufferBuilder b = new BufferBuilder(alloc, VertexFormat.DrawMode.QUADS, VertexFormats.POSITION_TEXTURE_COLOR);
            float d = 100.0f;
            float eu = 0.5f / (float)w;
            float ev = 0.5f / (float)h;
            for (int tile = 0; tile < 6; ++tile) {
                float u0 = (float)(tile % 3) / 3.0f + eu;
                float u1 = (float)(tile % 3 + 1) / 3.0f - eu;
                float v0 = (float)(tile / 3) / 2.0f + ev;
                float v1 = (float)(tile / 3 + 1) / 2.0f - ev;
                Matrix4f m = TILE[tile];
                Mc.skyVertex(b, m, -d, -d, -d).texture(u0, v0).color(-1);
                Mc.skyVertex(b, m, -d, -d, d).texture(u0, v1).color(-1);
                Mc.skyVertex(b, m, d, -d, d).texture(u1, v1).color(-1);
                Mc.skyVertex(b, m, d, -d, -d).texture(u1, v0).color(-1);
            }
            try (BuiltBuffer built = b.end();){
                cube = RenderSystem.getDevice().createBuffer(() -> "HugoService skybox", 40, built.getBuffer());
            }
        }
    }

    static BufferedImage load(Categories.Group g, Styles.Style style) throws IOException {
        BufferedImage src = Sky.source(g, style);
        if (src == null) {
            return null;
        }
        return Sky.isCube(src) ? Sky.fitCube(src) : Sky.fromPanorama(src);
    }

    static BufferedImage source(Categories.Group g, Styles.Style style) throws IOException {
        for (String f : g.files()) {
            byte[] png = style.read(f);
            if (png == null) continue;
            return Images.decode(png);
        }
        return null;
    }

    static boolean isCube(BufferedImage img) {
        return img.getWidth() * 2 == img.getHeight() * 3;
    }

    private static BufferedImage fitCube(BufferedImage img) {
        int face = img.getWidth() / 3;
        if (face <= 1024) {
            return img;
        }
        BufferedImage out = new BufferedImage(3072, 2048, 2);
        Graphics2D g = out.createGraphics();
        g.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);
        g.drawImage(img, 0, 0, out.getWidth(), out.getHeight(), null);
        g.dispose();
        return out;
    }

    static BufferedImage fromPanorama(BufferedImage pano) {
        int face = Math.min(1024, Math.max(64, pano.getHeight() / 2));
        BufferedImage out = new BufferedImage(face * 3, face * 2, 2);
        Vector3f dir = new Vector3f();
        for (int tile = 0; tile < 6; ++tile) {
            int ox = tile % 3 * face;
            int oy = tile / 3 * face;
            for (int y = 0; y < face; ++y) {
                for (int x = 0; x < face; ++x) {
                    float s = ((float)x + 0.5f) / (float)face;
                    float t = ((float)y + 0.5f) / (float)face;
                    WORLD[tile].transformDirection(dir.set(2.0f * s - 1.0f, -1.0f, 2.0f * t - 1.0f));
                    out.setRGB(ox + x, oy + y, Sky.samplePanorama(pano, dir));
                }
            }
        }
        return out;
    }

    private static int samplePanorama(BufferedImage pano, Vector3f d) {
        double len = Math.sqrt(d.x * d.x + d.y * d.y + d.z * d.z);
        double lon = Math.atan2(d.x, -d.z);
        double lat = Math.asin((double)d.y / len);
        int px = (int)((0.5 + lon / (Math.PI * 2)) * (double)pano.getWidth());
        int py = (int)((0.5 - lat / Math.PI) * (double)pano.getHeight());
        return pano.getRGB(Math.floorMod(px, pano.getWidth()), Math.min(pano.getHeight() - 1, Math.max(0, py)));
    }

    private static int sampleCube(BufferedImage cubeImg, Vector3f d) {
        int face = cubeImg.getWidth() / 3;
        Vector3f q = new Vector3f();
        for (int tile = 0; tile < 6; ++tile) {
            new Matrix4f((Matrix4fc)WORLD[tile]).invert().transformDirection(q.set((Vector3fc)d));
            if (q.y >= 0.0f || (double)(-q.y) < (double)Math.abs(q.x) - 1.0E-6 || (double)(-q.y) < (double)Math.abs(q.z) - 1.0E-6) continue;
            float s = (q.x / -q.y + 1.0f) / 2.0f;
            float t = (q.z / -q.y + 1.0f) / 2.0f;
            int x = Math.min(face - 1, (int)(s * (float)face));
            int y = Math.min(face - 1, (int)(t * (float)face));
            return cubeImg.getRGB(tile % 3 * face + x, tile / 3 * face + y);
        }
        return -16777216;
    }

    static BufferedImage preview(Categories.Group g, Styles.Style style) throws IOException {
        BufferedImage src = Sky.source(g, style);
        if (src == null) {
            return null;
        }
        boolean cubeLayout = Sky.isCube(src);
        int w = 192;
        int h = 96;
        BufferedImage out = new BufferedImage(w, h, 2);
        Vector3f d = new Vector3f();
        for (int y = 0; y < h; ++y) {
            for (int x = 0; x < w; ++x) {
                double lon = ((double)x + 0.5) / (double)w * 2.0 * Math.PI - Math.PI;
                double lat = Math.toRadians(55.0 - ((double)y + 0.5) / (double)h * 85.0);
                d.set((float)(Math.cos(lat) * Math.sin(lon)), (float)Math.sin(lat), (float)(-Math.cos(lat) * Math.cos(lon)));
                out.setRGB(x, y, (cubeLayout ? Sky.sampleCube(src, d) : Sky.samplePanorama(src, d)) | 0xFF000000);
            }
        }
        return out;
    }

    private Sky() {
    }

    static {
        Matrix4f north;
        TEXTURE_ID = Identifier.of((String)"hugoservice", (String)GROUP);
        TILE = new Matrix4f[6];
        WORLD = new Matrix4f[6];
        float r = (float)Math.toRadians(90.0);
        Sky.TILE[4] = north = new Matrix4f().rotate(r, 1.0f, 0.0f, 0.0f).rotate(-r, 0.0f, 0.0f, 1.0f);
        Sky.TILE[1] = new Matrix4f((Matrix4fc)north).rotate(r, 1.0f, 0.0f, 0.0f);
        Sky.TILE[0] = new Matrix4f((Matrix4fc)north).rotate(-r, 1.0f, 0.0f, 0.0f);
        Sky.TILE[5] = new Matrix4f((Matrix4fc)north).rotate(r, 0.0f, 0.0f, 1.0f);
        Sky.TILE[2] = new Matrix4f((Matrix4fc)TILE[5]).rotate(r, 0.0f, 0.0f, 1.0f);
        Sky.TILE[3] = new Matrix4f((Matrix4fc)TILE[2]).rotate(r, 0.0f, 0.0f, 1.0f);
        for (int i = 0; i < 6; ++i) {
            Sky.WORLD[i] = new Matrix4f().rotate(-r, 0.0f, 1.0f, 0.0f).mul((Matrix4fc)TILE[i]);
        }
        loadedKey = "vanilla";
    }
}

