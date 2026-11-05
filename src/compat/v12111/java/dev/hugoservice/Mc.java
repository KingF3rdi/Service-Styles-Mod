package dev.hugoservice;

import com.mojang.blaze3d.buffers.GpuBufferSlice;
import com.mojang.blaze3d.systems.RenderPass;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.textures.AddressMode;
import com.mojang.blaze3d.textures.FilterMode;
import java.lang.reflect.Field;
import net.minecraft.SharedConstants;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.input.KeyInput;
import net.minecraft.client.render.BufferBuilder;
import net.minecraft.client.render.RenderLayer;
import net.minecraft.client.render.RenderLayers;
import net.minecraft.client.render.VertexConsumer;
import net.minecraft.client.render.VertexRendering;
import net.minecraft.client.render.state.SkyRenderState;
import net.minecraft.client.texture.AbstractTexture;
import net.minecraft.client.util.math.MatrixStack;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.fabricmc.fabric.api.client.rendering.v1.world.WorldRenderEvents;
import net.minecraft.client.option.KeyBinding;
import net.minecraft.client.util.InputUtil;
import net.minecraft.entity.Entity;
import net.minecraft.item.ItemStack;
import net.minecraft.registry.tag.ItemTags;
import net.minecraft.text.Style;
import net.minecraft.text.StyleSpriteSource;
import net.minecraft.resource.ResourceType;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;
import net.minecraft.util.shape.VoxelShape;
import net.minecraft.util.shape.VoxelShapes;
import net.minecraft.world.dimension.DimensionType;
import org.joml.Matrix4f;
import org.joml.Matrix4fc;
import org.joml.Vector3f;
import org.joml.Vector4f;
import org.joml.Vector4fc;

public final class Mc {
    public static boolean hasCtrl(KeyInput input) {
        return input.hasCtrlOrCmd();
    }

    public static RenderLayer lines() {
        return RenderLayers.lines();
    }

    public static RenderLayer entityTranslucent(Identifier id) {
        return RenderLayers.entityTranslucent(id);
    }

    public static RenderLayer entityCutoutNoCull(Identifier id) {
        return RenderLayers.entityCutoutNoCull(id);
    }

    public static float minLineWidth() {
        return MinecraftClient.getInstance().getWindow().getMinimumLineWidth();
    }

    public static void drawOutline(MatrixStack m, VertexConsumer lines, VoxelShape shape, double x, double y, double z, int color, float width) {
        VertexRendering.drawOutline(m, lines, shape, x, y, z, color, width);
    }

    public static void resizeScreen(Screen screen, MinecraftClient mc, int w, int h) {
        screen.resize(w, h);
    }

    public static boolean isSpear(ItemStack stack) {
        return stack.isIn(ItemTags.SPEARS);
    }

    public static boolean skyOverworld(SkyRenderState s) {
        return s != null && s.skybox == DimensionType.Skybox.OVERWORLD;
    }

    public static float sunAngle(SkyRenderState s) {
        return s.sunAngle;
    }

    public static GpuBufferSlice writeSkyTransforms(Matrix4fc view, Vector4fc color) {
        return RenderSystem.getDynamicUniforms().write(view, color, new Vector3f(), new Matrix4f());
    }

    public static void bindSkySampler(RenderPass pass, AbstractTexture texture) {
        pass.bindTexture("Sampler0", texture.getGlTextureView(), RenderSystem.getSamplerCache().get(AddressMode.CLAMP_TO_EDGE, AddressMode.CLAMP_TO_EDGE, FilterMode.LINEAR, FilterMode.LINEAR, false));
    }

    public static BufferBuilder skyVertex(BufferBuilder b, Matrix4f m, float x, float y, float z) {
        return (BufferBuilder) b.vertex((Matrix4fc) m, x, y, z);
    }

    public static void applyLinearSampler(AbstractTexture t) {
        Mc.setSampler(t, RenderSystem.getSamplerCache().getRepeated(FilterMode.LINEAR));
    }

    public static void applyNearestSampler(AbstractTexture t) {
        Mc.setSampler(t, RenderSystem.getSamplerCache().getRepeated(FilterMode.NEAREST));
    }

    public static void applyClampLinearSampler(AbstractTexture t) {
        Mc.setSampler(t, RenderSystem.getSamplerCache().get(AddressMode.CLAMP_TO_EDGE, AddressMode.CLAMP_TO_EDGE, FilterMode.LINEAR, FilterMode.LINEAR, false));
    }

    public static Style iconStyle() {
        return Style.EMPTY.withFont(new StyleSpriteSource.Font(Identifier.of("hugoservice", "icon")));
    }

    public static Style uiStyle(boolean pixel, boolean aurora, boolean bold) {
        StyleSpriteSource font = pixel ? PIXEL : (aurora ? VANILLA : (bold ? SMOOTH_BOLD : SMOOTH));
        return Style.EMPTY.withFont(font);
    }

    public static boolean matchesOpen(int key, int scancode) {
        return HugoService.OPEN.matchesKey(new KeyInput(key, scancode, 0));
    }

    public static void bindKey(KeyBinding bind, int key, int scancode) {
        bind.setBoundKey(InputUtil.fromKeyCode(new KeyInput(key, scancode, 0)));
    }

    public static Vec3d pos(Entity e) {
        return e.getEntityPos();
    }

    public static boolean clientSide(Entity e) {
        return e.getEntityWorld() != null && e.getEntityWorld().isClient();
    }

    public static net.minecraft.component.type.ProfileComponent headProfile(com.mojang.authlib.GameProfile profile) {
        return net.minecraft.component.type.ProfileComponent.ofStatic(profile);
    }

    public static int packFormat() {
        return SharedConstants.getGameVersion().packVersion(ResourceType.CLIENT_RESOURCES).major();
    }

    public static java.util.UUID profileId(com.mojang.authlib.GameProfile profile) {
        return profile.id();
    }

    public static void fieldFormatter(net.minecraft.client.gui.widget.TextFieldWidget f, Style style) {
        f.addFormatter((s, first) -> net.minecraft.text.OrderedText.styledForwardsVisitedString(s, style));
    }

    public static void registerKeys() {
        KeyBinding.Category category = KeyBinding.Category.create(Identifier.of("hugoservice", "main"));
        HugoService.OPEN = KeyBindingHelper.registerKeyBinding(new KeyBinding("key.hugoservice.open", InputUtil.Type.KEYSYM, 344, category));
        HugoService.RESET_POPS = KeyBindingHelper.registerKeyBinding(new KeyBinding("key.hugoservice.reset_pops", InputUtil.Type.KEYSYM, -1, category));
    }

    public static void registerSpotifySky() {
        WorldRenderEvents.AFTER_ENTITIES.register(ctx -> SpotifyHud.sky(ctx.matrices(), ctx.consumers()));
    }

    public static void registerCobwebOverlay() {
        WorldRenderEvents.AFTER_ENTITIES.register(ctx -> {
            Integer color = Config.INSTANCE.fx(Fx.COBWEB);
            if (color == null || Fx.cobwebs.isEmpty()) {
                return;
            }
            Vec3d cam = ctx.worldState().cameraRenderState.pos;
            MatrixStack m = ctx.matrices();
            VertexConsumer lines = ctx.consumers().getBuffer(Mc.lines());
            float width = Math.max(1.0f, Mc.minLineWidth()) * Math.max(1, Config.INSTANCE.cobwebWidth) / 2.0f;
            for (BlockPos p : Fx.cobwebs) {
                Mc.drawOutline(m, lines, VoxelShapes.fullCube(), p.getX() - cam.x, p.getY() - cam.y, p.getZ() - cam.z, 0xFF000000 | color, width);
            }
        });
    }

    private static final StyleSpriteSource SMOOTH = new StyleSpriteSource.Font(Identifier.of("hugoservice", "ui"));
    private static final StyleSpriteSource SMOOTH_BOLD = new StyleSpriteSource.Font(Identifier.of("hugoservice", "bold"));
    private static final StyleSpriteSource PIXEL = new StyleSpriteSource.Font(Identifier.ofVanilla("uniform"));
    private static final StyleSpriteSource VANILLA = new StyleSpriteSource.Font(Identifier.ofVanilla("default"));

    private static void setSampler(AbstractTexture t, Object sampler) {
        try {
            Field f = AbstractTexture.class.getDeclaredField("sampler");
            f.setAccessible(true);
            f.set(t, sampler);
        } catch (ReflectiveOperationException ignored) {
        }
    }

    private Mc() {
    }
}
