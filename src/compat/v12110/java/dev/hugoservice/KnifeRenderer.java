package dev.hugoservice;

import com.mojang.serialization.MapCodec;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.render.command.OrderedRenderCommandQueue;
import net.minecraft.client.render.item.model.special.SimpleSpecialModelRenderer;
import net.minecraft.client.render.item.model.special.SpecialModelRenderer;
import net.minecraft.client.render.item.model.special.SpecialModelTypes;
import net.minecraft.client.texture.NativeImageBackedTexture;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.item.ItemDisplayContext;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.RotationAxis;
import org.joml.Vector3f;

import java.awt.image.BufferedImage;
import java.util.Set;

/**
 * 1.21.10 knife special model: RenderLayer names and collectVertices(Set).
 */
public final class KnifeRenderer implements SimpleSpecialModelRenderer {
    private static final Identifier TYPE = Identifier.of("hugoservice", "knife");
    private static final Identifier TEXTURE = Identifier.of("hugoservice", "knife_finish");
    private static NativeImageBackedTexture texture;
    private static Knives.Finish painted;
    private static long paintedAt;

    public record Unbaked() implements SpecialModelRenderer.Unbaked {
        public static final MapCodec<Unbaked> CODEC = MapCodec.unit(new Unbaked());

        @Override
        public SpecialModelRenderer<?> bake(SpecialModelRenderer.BakeContext context) { return new KnifeRenderer(); }

        @Override
        public MapCodec<Unbaked> getCodec() { return CODEC; }
    }

    public static void register() {
        if (!Brand.KNIVES) {
            return;
        }
        try {
            var field = SpecialModelTypes.class.getDeclaredField("ID_MAPPER");
            field.setAccessible(true);
            Object mapper = field.get(null);
            mapper.getClass().getMethod("put", Object.class, Object.class).invoke(mapper, TYPE, Unbaked.CODEC);
        } catch (Throwable t) {
            HugoService.LOG.warn("Could not register the knife model type", t);
        }
    }

    @Override
    public void render(ItemDisplayContext context, MatrixStack m, OrderedRenderCommandQueue queue, int light, int overlay,
                       boolean glint, int outline) {
        Knives.Model knife = Knives.active();
        if (knife == null) knife = Knives.MODELS.getFirst();
        paint();
        m.push();
        m.scale(1 / 16f, 1 / 16f, 1 / 16f);
        draw(m, queue, knife.mesh(), light, overlay, glint);
        if (knife.safe() != null) {
            float[] h = context == ItemDisplayContext.FIRST_PERSON_RIGHT_HAND ? KnifeAnimations.butterflyHandles() : null;
            swing(m, queue, knife.safe(), Knives.BF_PIVOT_SAFE, h == null ? 0 : h[0], light, overlay, glint);
            swing(m, queue, knife.bite(), Knives.BF_PIVOT_BITE, h == null ? 0 : h[1], light, overlay, glint);
        }
        m.pop();
    }

    private static void swing(MatrixStack m, OrderedRenderCommandQueue queue, KnifeMesh mesh, float[] pivot, float degrees,
                              int light, int overlay, boolean glint) {
        m.push();
        m.translate(pivot[0], pivot[1], 8);
        m.multiply(RotationAxis.POSITIVE_Z.rotationDegrees(degrees));
        m.translate(-pivot[0], -pivot[1], -8);
        draw(m, queue, mesh, light, overlay, glint);
        m.pop();
    }

    private static void draw(MatrixStack m, OrderedRenderCommandQueue queue, KnifeMesh mesh, int light, int overlay, boolean glint) {
        queue.submitCustom(m, Mc.entityCutoutNoCull(TEXTURE), (entry, vc) -> mesh.emit(entry, vc, light, overlay));
        if (glint) queue.submitCustom(m, Mc.entityGlint(), (entry, vc) -> mesh.emit(entry, vc, light, overlay));
    }

    private static void paint() {
        Knives.Finish f = Knives.chosenFinish();
        long now = System.currentTimeMillis();
        if (texture != null && f == painted && (f.frames() <= 1 || now - paintedAt < 50)) return;
        if (texture == null) {
            texture = new NativeImageBackedTexture(() -> "hugoservice knife finish", Knives.TEX, Knives.TEX, false);
            MinecraftClient.getInstance().getTextureManager().registerTexture(TEXTURE, texture);
        }
        long loop = f.frames() * 100L;
        BufferedImage img = Knives.texture(f, f.frames() > 1 ? (now % loop) / (float) loop : 0);
        var out = texture.getImage();
        for (int y = 0; y < Knives.TEX; y++) for (int x = 0; x < Knives.TEX; x++) out.setColorArgb(x, y, img.getRGB(x, y));
        texture.upload();
        painted = f;
        paintedAt = now;
    }

    @Override
    public void collectVertices(Set<Vector3f> out) {
        Knives.Model knife = Knives.active();
        KnifeMesh mesh = (knife == null ? Knives.MODELS.getFirst() : knife).mesh();
        for (int i = 0; i < 8; i++) {
            float x = (i & 1) == 0 ? mesh.min[0] : mesh.max[0], y = (i & 2) == 0 ? mesh.min[1] : mesh.max[1], z = (i & 4) == 0 ? mesh.min[2] : mesh.max[2];
            out.add(new Vector3f(x / 16, y / 16, z / 16));
        }
    }
}
