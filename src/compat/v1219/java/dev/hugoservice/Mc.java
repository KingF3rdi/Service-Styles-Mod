package dev.hugoservice;

import com.mojang.blaze3d.buffers.GpuBufferSlice;
import com.mojang.blaze3d.systems.RenderPass;
import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.SharedConstants;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.input.KeyInput;
import net.minecraft.client.render.BufferBuilder;
import net.minecraft.client.render.DimensionEffects;
import net.minecraft.client.render.RenderLayer;
import net.minecraft.client.render.VertexConsumer;
import net.minecraft.client.render.VertexRendering;
import net.minecraft.client.render.state.SkyRenderState;
import net.minecraft.client.texture.AbstractTexture;
import net.minecraft.client.util.math.MatrixStack;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.minecraft.client.option.KeyBinding;
import net.minecraft.client.util.InputUtil;
import net.minecraft.entity.Entity;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.registry.Registries;
import net.minecraft.text.Style;
import net.minecraft.text.StyleSpriteSource;
import net.minecraft.resource.ResourceType;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;
import net.minecraft.util.shape.VoxelShape;
import net.minecraft.util.shape.VoxelShapes;
import org.joml.Matrix4f;
import org.joml.Matrix4fc;
import org.joml.Vector3f;
import org.joml.Vector4fc;

public final class Mc {
    public static boolean hasCtrl(KeyInput input) {
        return input.hasCtrl();
    }

    public static RenderLayer lines() {
        return RenderLayer.getLines();
    }

    public static RenderLayer entityTranslucent(Identifier id) {
        return RenderLayer.getEntityTranslucent(id);
    }

    public static RenderLayer entityCutoutNoCull(Identifier id) {
        return RenderLayer.getEntityCutoutNoCull(id);
    }

    public static RenderLayer entityGlint() {
        return RenderLayer.getEntityGlint();
    }

    public static float minLineWidth() {
        return 1.0f;
    }

    public static void drawOutline(MatrixStack m, VertexConsumer lines, VoxelShape shape, double x, double y, double z, int color, float width) {
        VertexRendering.drawOutline(m, lines, shape, x, y, z, color);
    }

    public static void resizeScreen(Screen screen, MinecraftClient mc, int w, int h) {
        screen.resize(mc, w, h);
    }

    public static boolean isSpear(ItemStack stack) {
        Item item = stack.getItem();
        Identifier id = Registries.ITEM.getId(item);
        return id.getPath().contains("spear");
    }

    public static boolean skyOverworld(SkyRenderState s) {
        return s != null && s.skyType == DimensionEffects.SkyType.NORMAL;
    }

    public static float sunAngle(SkyRenderState s) {
        return s.solarAngle;
    }

    public static GpuBufferSlice writeSkyTransforms(Matrix4fc view, Vector4fc color) {
        return RenderSystem.getDynamicUniforms().write(view, color, new Vector3f(), new Matrix4f(), 0.0f);
    }

    public static void bindSkySampler(RenderPass pass, AbstractTexture texture) {
        pass.bindSampler("Sampler0", texture.getGlTextureView());
    }

    public static BufferBuilder skyVertex(BufferBuilder b, Matrix4f m, float x, float y, float z) {
        return (BufferBuilder) b.vertex(m, x, y, z);
    }

    public static void applyLinearSampler(AbstractTexture t) {
        t.setFilter(true, false);
    }

    public static void applyNearestSampler(AbstractTexture t) {
        t.setFilter(false, false);
    }

    public static void applyClampLinearSampler(AbstractTexture t) {
        t.setFilter(true, false);
        t.setClamp(true);
    }

    public static Style iconStyle() {
        return Style.EMPTY.withFont(VANILLA);
    }

    public static Style uiStyle(boolean pixel, boolean aurora, boolean bold) {
        return Style.EMPTY.withFont(VANILLA);
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
        if (Brand.KNIVES) {
            HugoService.INSPECT = KeyBindingHelper.registerKeyBinding(new KeyBinding("key.hugoservice.inspect", InputUtil.Type.KEYSYM, 71, category));
        }
    }

    public static int selectedSlot(net.minecraft.entity.player.PlayerEntity player) {
        return player.getInventory().getSelectedSlot();
    }

    public static float eyeHeight(net.minecraft.entity.Entity entity) {
        return entity.getStandingEyeHeight();
    }

    public static Vec3d cameraPos(MinecraftClient mc) {
        return mc.gameRenderer.getCamera().getCameraPos();
    }

    public static void registerSpotifySky() {
        // Fabric API 0.134.x for 1.21.9 has no WorldRenderEvents; lyrics overlay is 1.21.8 / 1.21.10+.
    }

    public static void registerCobwebOverlay() {
    }

    private static final StyleSpriteSource VANILLA = new StyleSpriteSource.Font(Identifier.ofVanilla("default"));

    private Mc() {
    }
}
