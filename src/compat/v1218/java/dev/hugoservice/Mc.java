package dev.hugoservice;

import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.fabricmc.fabric.api.client.rendering.v1.WorldRenderEvents;
import net.minecraft.SharedConstants;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.option.KeyBinding;
import net.minecraft.client.render.RenderLayer;
import net.minecraft.client.render.VertexConsumer;
import net.minecraft.client.render.VertexRendering;
import net.minecraft.client.texture.AbstractTexture;
import net.minecraft.client.util.InputUtil;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.entity.Entity;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.registry.Registries;
import net.minecraft.text.Style;
import net.minecraft.resource.ResourceType;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;
import net.minecraft.util.shape.VoxelShape;
import net.minecraft.util.shape.VoxelShapes;

public final class Mc {
    public static RenderLayer lines() {
        return RenderLayer.getLines();
    }

    public static RenderLayer entityTranslucent(Identifier id) {
        return RenderLayer.getEntityTranslucent(id);
    }

    public static RenderLayer entityCutoutNoCull(Identifier id) {
        return RenderLayer.getEntityCutoutNoCull(id);
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
        return Style.EMPTY.withFont(Identifier.of("hugoservice", "icon"));
    }

    public static Style uiStyle(boolean pixel, boolean aurora, boolean bold) {
        Identifier font = pixel ? Identifier.ofVanilla("uniform") : (aurora ? Identifier.ofVanilla("default") : Identifier.of("hugoservice", bold ? "bold" : "ui"));
        return Style.EMPTY.withFont(font);
    }

    public static boolean matchesOpen(int key, int scancode) {
        return HugoService.OPEN.matchesKey(key, scancode);
    }

    public static void bindKey(KeyBinding bind, int key, int scancode) {
        bind.setBoundKey(InputUtil.fromKeyCode(key, scancode));
    }

    public static Vec3d pos(Entity e) {
        return e.getPos();
    }

    public static boolean clientSide(Entity e) {
        return e.getWorld() != null && e.getWorld().isClient();
    }

    public static net.minecraft.component.type.ProfileComponent headProfile(com.mojang.authlib.GameProfile profile) {
        return new net.minecraft.component.type.ProfileComponent(profile);
    }

    public static int packFormat() {
        return SharedConstants.getGameVersion().packVersion(ResourceType.CLIENT_RESOURCES);
    }

    public static java.util.UUID profileId(com.mojang.authlib.GameProfile profile) {
        return profile.getId();
    }

    public static void fieldFormatter(net.minecraft.client.gui.widget.TextFieldWidget f, Style style) {
    }

    public static void registerKeys() {
        HugoService.OPEN = KeyBindingHelper.registerKeyBinding(new KeyBinding("key.hugoservice.open", InputUtil.Type.KEYSYM, 344, "key.categories.hugoservice"));
        HugoService.RESET_POPS = KeyBindingHelper.registerKeyBinding(new KeyBinding("key.hugoservice.reset_pops", InputUtil.Type.KEYSYM, -1, "key.categories.hugoservice"));
    }

    public static void registerSpotifySky() {
        WorldRenderEvents.AFTER_ENTITIES.register(ctx -> SpotifyHud.sky(ctx.matrixStack(), ctx.consumers()));
    }

    public static void registerCobwebOverlay() {
        WorldRenderEvents.AFTER_ENTITIES.register(ctx -> {
            Integer color = Config.INSTANCE.fx(Fx.COBWEB);
            if (color == null || Fx.cobwebs.isEmpty()) {
                return;
            }
            Vec3d cam = ctx.camera().getPos();
            MatrixStack m = ctx.matrixStack();
            VertexConsumer lines = ctx.consumers().getBuffer(Mc.lines());
            float width = Math.max(1.0f, Mc.minLineWidth()) * Math.max(1, Config.INSTANCE.cobwebWidth) / 2.0f;
            for (BlockPos p : Fx.cobwebs) {
                Mc.drawOutline(m, lines, VoxelShapes.fullCube(), p.getX() - cam.x, p.getY() - cam.y, p.getZ() - cam.z, 0xFF000000 | color, width);
            }
        });
    }

    private Mc() {
    }
}
