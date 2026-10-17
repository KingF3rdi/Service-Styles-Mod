package dev.hugoservice;

import com.mojang.blaze3d.buffers.GpuBuffer;
import com.mojang.blaze3d.buffers.GpuBufferSlice;
import com.mojang.blaze3d.systems.RenderPass;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.textures.AddressMode;
import com.mojang.blaze3d.textures.FilterMode;
import com.mojang.blaze3d.vertex.VertexFormat;
import dev.hugoservice.Categories.Group;
import dev.hugoservice.Styles.Style;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gl.Framebuffer;
import net.minecraft.client.gl.RenderPipelines;
import net.minecraft.client.render.BufferBuilder;
import net.minecraft.client.render.BuiltBuffer;
import net.minecraft.client.render.VertexFormats;
import net.minecraft.client.render.state.SkyRenderState;
import net.minecraft.client.texture.NativeImage;
import net.minecraft.client.texture.NativeImageBackedTexture;
import net.minecraft.client.util.BufferAllocator;
import net.minecraft.util.Identifier;
import net.minecraft.world.dimension.DimensionType;
import org.joml.Matrix4f;
import org.joml.Vector3f;
import org.joml.Vector4f;

import java.awt.image.BufferedImage;
import java.util.OptionalDouble;
import java.util.OptionalInt;

/**
 * Custom skybox (like Skyboxify / OptiFine custom sky). Sources are OptiFine/MCPatcher 3x2 sky
 * textures from packs or your own images (3:2 = cube layout, anything else = 360° panorama).
 * Everything is converted to the OptiFine 3x2 layout and drawn as one cube in the sky pass.
 */
public final class Sky {
    public static final String GROUP = "skybox";
    private static final int MAX_FACE = 1024;
    private static final Identifier TEXTURE_ID = Identifier.of("hugoservice", "skybox");

    /**
     * Face transforms in the exact order MCPatcher/OptiFine use (tile = col + 3 * row of the 3x2 grid):
     * 0 bottom, 1 top, 2 south, 3 west, 4 north, 5 east. Each one maps the bottom quad to its side.
     */
    static final Matrix4f[] TILE = new Matrix4f[6];
    /** TILE as seen in the world: old vanilla turned the sky -90° around Y before MCPatcher drew it. */
    static final Matrix4f[] WORLD = new Matrix4f[6];
    static {
        float r = (float) Math.toRadians(90);
        Matrix4f north = new Matrix4f().rotate(r, 1, 0, 0).rotate(-r, 0, 0, 1);
        TILE[4] = north;
        TILE[1] = new Matrix4f(north).rotate(r, 1, 0, 0);
        TILE[0] = new Matrix4f(north).rotate(-r, 1, 0, 0);
        TILE[5] = new Matrix4f(north).rotate(r, 0, 0, 1);
        TILE[2] = new Matrix4f(TILE[5]).rotate(r, 0, 0, 1);
        TILE[3] = new Matrix4f(TILE[2]).rotate(r, 0, 0, 1);
        for (int i = 0; i < 6; i++) WORLD[i] = new Matrix4f().rotate(-r, 0, 1, 0).mul(TILE[i]);
    }

    public static volatile SkyRenderState state;
    private static NativeImageBackedTexture texture;
    private static GpuBuffer cube;
    private static String loadedKey = Styles.VANILLA_ID, loadingKey, failedKey;
    private static volatile NativeImage pending;
    private static volatile String pendingKey;

    // ---------------------------------------------------------------- rendering

    /** True when a custom skybox replaces the vanilla overworld sky this frame. */
    public static boolean active() {
        SkyRenderState s = state;
        return texture != null && !loadedKey.equals(Styles.VANILLA_ID) && s != null && s.skybox == DimensionType.Skybox.OVERWORLD;
    }

    /** Called instead of the vanilla top sky; returns false to let vanilla draw. */
    public static boolean render() {
        update();
        if (!active()) return false;
        Config cfg = Config.INSTANCE;
        SkyRenderState s = state;
        Matrix4f view = new Matrix4f(RenderSystem.getModelViewMatrix());
        view.rotate((float) Math.toRadians(-90), 0, 1, 0); // same frame as WORLD
        if (cfg.skyRotate) view.rotate(s.sunAngle, 0, 0, 1); // OptiFine default: rotate around the south axis
        float b = 1f;
        if (cfg.skyNightFade) b = Math.max(0.18f, 1f - s.starBrightness * 1.6f);
        b *= 1f - s.rainGradient * 0.3f;

        RenderSystem.ShapeIndexBuffer indices = RenderSystem.getSequentialBuffer(VertexFormat.DrawMode.QUADS);
        GpuBuffer indexBuffer = indices.getIndexBuffer(36);
        Framebuffer fb = MinecraftClient.getInstance().getFramebuffer();
        GpuBufferSlice transforms = RenderSystem.getDynamicUniforms().write(view, new Vector4f(b, b, b, 1f), new Vector3f(), new Matrix4f());
        try (RenderPass pass = RenderSystem.getDevice().createCommandEncoder().createRenderPass(() -> "HugoService skybox",
                fb.getColorAttachmentView(), OptionalInt.empty(), fb.getDepthAttachmentView(), OptionalDouble.empty())) {
            pass.setPipeline(RenderPipelines.POSITION_TEX_COLOR_END_SKY);
            RenderSystem.bindDefaultUniforms(pass);
            pass.setUniform("DynamicTransforms", transforms);
            pass.bindTexture("Sampler0", texture.getGlTextureView(),
                RenderSystem.getSamplerCache().get(AddressMode.CLAMP_TO_EDGE, AddressMode.CLAMP_TO_EDGE, FilterMode.LINEAR, FilterMode.LINEAR, false));
            pass.setVertexBuffer(0, cube);
            pass.setIndexBuffer(indexBuffer, indices.getIndexType());
            pass.drawIndexed(0, 0, 36, 1);
        }
        return true;
    }

    /** Swaps in a finished image and starts loading when the selection changed. Render thread only. */
    private static void update() {
        NativeImage img = pending;
        if (img != null) {
            pending = null;
            if (texture != null) MinecraftClient.getInstance().getTextureManager().destroyTexture(TEXTURE_ID);
            texture = new NativeImageBackedTexture(() -> "hugoservice skybox", img);
            MinecraftClient.getInstance().getTextureManager().registerTexture(TEXTURE_ID, texture);
            buildCube(img.getWidth(), img.getHeight());
            loadedKey = pendingKey;
        }
        String want = Config.INSTANCE.styleOf(GROUP);
        if (want.equals(loadedKey) || want.equals(loadingKey) || want.equals(failedKey)) return;
        if (want.equals(Styles.VANILLA_ID)) { loadedKey = want; return; }
        loadingKey = want;
        Group g = Categories.allGroups().stream().filter(x -> x.id().equals(GROUP)).findFirst().orElseThrow();
        Style style = Styles.find(g, want);
        Thread t = new Thread(() -> {
            try {
                BufferedImage cubeImg = load(g, style);
                if (cubeImg != null) {
                    NativeImage ni = new NativeImage(cubeImg.getWidth(), cubeImg.getHeight(), false);
                    int w = cubeImg.getWidth();
                    int[] row = new int[w];
                    for (int y = 0; y < cubeImg.getHeight(); y++) {
                        cubeImg.getRGB(0, y, w, 1, row, 0, w);
                        for (int x = 0; x < w; x++) ni.setColorArgb(x, y, row[x] | 0xFF000000);
                    }
                    pendingKey = want;
                    pending = ni;
                } else {
                    failedKey = want; // no sky image in this style: do not retry every frame
                }
            } catch (Exception e) {
                failedKey = want;
                HugoService.LOG.warn("Could not load skybox {}", want, e);
            } finally {
                loadingKey = null;
            }
        }, "HugoService-Sky");
        t.setDaemon(true);
        t.start();
    }

    /** 6 quads, UVs inset by half a texel so faces do not bleed into each other. */
    private static void buildCube(int w, int h) {
        if (cube != null) cube.close();
        try (BufferAllocator alloc = BufferAllocator.fixedSized(24 * VertexFormats.POSITION_TEXTURE_COLOR.getVertexSize())) {
            BufferBuilder b = new BufferBuilder(alloc, VertexFormat.DrawMode.QUADS, VertexFormats.POSITION_TEXTURE_COLOR);
            float d = 100f, eu = 0.5f / w, ev = 0.5f / h;
            for (int tile = 0; tile < 6; tile++) {
                float u0 = (tile % 3) / 3f + eu, u1 = (tile % 3 + 1) / 3f - eu;
                float v0 = (tile / 3) / 2f + ev, v1 = (tile / 3 + 1) / 2f - ev;
                Matrix4f m = TILE[tile];
                b.vertex(m, -d, -d, -d).texture(u0, v0).color(-1);
                b.vertex(m, -d, -d, d).texture(u0, v1).color(-1);
                b.vertex(m, d, -d, d).texture(u1, v1).color(-1);
                b.vertex(m, d, -d, -d).texture(u1, v0).color(-1);
            }
            try (BuiltBuffer built = b.end()) {
                cube = RenderSystem.getDevice().createBuffer(() -> "HugoService skybox", GpuBuffer.USAGE_VERTEX | GpuBuffer.USAGE_COPY_DST, built.getBuffer());
            }
        }
    }

    // ---------------------------------------------------------------- images

    /** Reads the style's sky image and returns it in the 3x2 cube layout. */
    static BufferedImage load(Group g, Style style) throws java.io.IOException {
        BufferedImage src = source(g, style);
        if (src == null) return null;
        return isCube(src) ? fitCube(src) : fromPanorama(src);
    }

    static BufferedImage source(Group g, Style style) throws java.io.IOException {
        for (String f : g.files()) {
            byte[] png = style.read(f);
            if (png != null) return Images.decode(png);
        }
        return null;
    }

    static boolean isCube(BufferedImage img) {
        return img.getWidth() * 2 == img.getHeight() * 3;
    }

    /** Caps very large cube textures. */
    private static BufferedImage fitCube(BufferedImage img) {
        int face = img.getWidth() / 3;
        if (face <= MAX_FACE) return img;
        BufferedImage out = new BufferedImage(MAX_FACE * 3, MAX_FACE * 2, BufferedImage.TYPE_INT_ARGB);
        var g = out.createGraphics();
        g.setRenderingHint(java.awt.RenderingHints.KEY_INTERPOLATION, java.awt.RenderingHints.VALUE_INTERPOLATION_BILINEAR);
        g.drawImage(img, 0, 0, out.getWidth(), out.getHeight(), null);
        g.dispose();
        return out;
    }

    /** 360° panorama (equirectangular) to the 3x2 cube layout. */
    static BufferedImage fromPanorama(BufferedImage pano) {
        int face = Math.min(MAX_FACE, Math.max(64, pano.getHeight() / 2));
        BufferedImage out = new BufferedImage(face * 3, face * 2, BufferedImage.TYPE_INT_ARGB);
        Vector3f dir = new Vector3f();
        for (int tile = 0; tile < 6; tile++) {
            int ox = (tile % 3) * face, oy = (tile / 3) * face;
            for (int y = 0; y < face; y++) for (int x = 0; x < face; x++) {
                float s = (x + 0.5f) / face, t = (y + 0.5f) / face;
                WORLD[tile].transformDirection(dir.set(2 * s - 1, -1, 2 * t - 1));
                out.setRGB(ox + x, oy + y, samplePanorama(pano, dir));
            }
        }
        return out;
    }

    /** Panorama centre looks north, +x is east. */
    private static int samplePanorama(BufferedImage pano, Vector3f d) {
        double len = Math.sqrt(d.x * d.x + d.y * d.y + d.z * d.z);
        double lon = Math.atan2(d.x, -d.z), lat = Math.asin(d.y / len);
        int px = (int) ((0.5 + lon / (2 * Math.PI)) * pano.getWidth());
        int py = (int) ((0.5 - lat / Math.PI) * pano.getHeight());
        return pano.getRGB(Math.floorMod(px, pano.getWidth()), Math.min(pano.getHeight() - 1, Math.max(0, py)));
    }

    /** Colour of the cube image in a direction (used for menu previews). */
    private static int sampleCube(BufferedImage cubeImg, Vector3f d) {
        int face = cubeImg.getWidth() / 3;
        Vector3f q = new Vector3f();
        for (int tile = 0; tile < 6; tile++) {
            new Matrix4f(WORLD[tile]).invert().transformDirection(q.set(d));
            if (q.y >= 0 || -q.y < Math.abs(q.x) - 1e-6 || -q.y < Math.abs(q.z) - 1e-6) continue;
            float s = (q.x / -q.y + 1) / 2, t = (q.z / -q.y + 1) / 2;
            int x = Math.min(face - 1, (int) (s * face)), y = Math.min(face - 1, (int) (t * face));
            return cubeImg.getRGB((tile % 3) * face + x, (tile / 3) * face + y);
        }
        return 0xFF000000;
    }

    /** Wide preview looking around the horizon (north in the middle). */
    static BufferedImage preview(Group g, Style style) throws java.io.IOException {
        BufferedImage src = source(g, style);
        if (src == null) return null;
        boolean cubeLayout = isCube(src);
        int w = 192, h = 96;
        BufferedImage out = new BufferedImage(w, h, BufferedImage.TYPE_INT_ARGB);
        Vector3f d = new Vector3f();
        for (int y = 0; y < h; y++) for (int x = 0; x < w; x++) {
            double lon = (x + 0.5) / w * 2 * Math.PI - Math.PI;
            double lat = Math.toRadians(55 - (y + 0.5) / h * 85);   // +55° .. -30°
            d.set((float) (Math.cos(lat) * Math.sin(lon)), (float) Math.sin(lat), (float) (-Math.cos(lat) * Math.cos(lon)));
            out.setRGB(x, y, (cubeLayout ? sampleCube(src, d) : samplePanorama(src, d)) | 0xFF000000);
        }
        return out;
    }

    private Sky() {}
}
