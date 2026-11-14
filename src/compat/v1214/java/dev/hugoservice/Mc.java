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
        return Style.EMPTY.withFont(Identifier.ofVanilla("default"));
    }

    public static net.minecraft.text.Text iconText() {
        return net.minecraft.text.Text.literal("\uE000").setStyle(Style.EMPTY.withFont(Identifier.of("hugoservice", "icon")));
    }

    public static Style uiStyle(boolean pixel, boolean aurora, boolean bold) {
        return Style.EMPTY.withFont(Identifier.ofVanilla("default"));
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
        return SharedConstants.getGameVersion().getResourceVersion(ResourceType.CLIENT_RESOURCES);
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


    public static void setAnchorCutout() {
        net.fabricmc.fabric.api.blockrenderlayer.v1.BlockRenderLayerMap.INSTANCE.putBlock(net.minecraft.block.Blocks.RESPAWN_ANCHOR, net.minecraft.client.render.RenderLayer.getCutout());
    }

    public static void noTextShadow(net.minecraft.client.gui.widget.TextFieldWidget f) {
    }

    public static void attachHudAfterHotbar(Identifier id, java.util.function.BiConsumer<net.minecraft.client.gui.DrawContext, net.minecraft.client.render.RenderTickCounter> hud) {
        net.fabricmc.fabric.api.client.rendering.v1.HudRenderCallback.EVENT.register((ctx, tick) -> hud.accept(ctx, tick));
    }

    public static void attachHudBeforeChat(Identifier id, java.util.function.BiConsumer<net.minecraft.client.gui.DrawContext, net.minecraft.client.render.RenderTickCounter> hud) {
        net.fabricmc.fabric.api.client.rendering.v1.HudRenderCallback.EVENT.register((ctx, tick) -> hud.accept(ctx, tick));
    }

    public static void guiPush(net.minecraft.client.gui.DrawContext ctx) {
        ctx.getMatrices().push();
    }

    public static void guiPop(net.minecraft.client.gui.DrawContext ctx) {
        ctx.getMatrices().pop();
    }

    public static void guiTranslate(net.minecraft.client.gui.DrawContext ctx, float x, float y) {
        ctx.getMatrices().translate(x, y, 0.0f);
    }

    public static void guiScale(net.minecraft.client.gui.DrawContext ctx, float x, float y) {
        ctx.getMatrices().scale(x, y, 1.0f);
    }

    public static void guiRotate(net.minecraft.client.gui.DrawContext ctx, float radians) {
        ctx.getMatrices().multiply(new org.joml.Quaternionf().rotateZ(radians));
    }

    public static void registerCrosshairHud() {
        attachHudAfterHotbar(Identifier.of("hugoservice", "crosshair"), (ctx, tick) -> {
            if (Crosshair.active()) {
                Crosshair.drawCurrent(ctx, ctx.getScaledWindowWidth() / 2.0f, ctx.getScaledWindowHeight() / 2.0f);
            }
        });
    }

    public static void blit(net.minecraft.client.gui.DrawContext ctx, Identifier id, int x, int y, float u, float v, int w, int h, int regionW, int regionH, int texW, int texH) {
        ctx.drawTexture(net.minecraft.client.render.RenderLayer::getGuiTextured, id, x, y, u, v, w, h, regionW, regionH, texW, texH);
    }

    public static void blit(net.minecraft.client.gui.DrawContext ctx, Identifier id, int x, int y, float u, float v, int w, int h, int regionW, int regionH, int texW, int texH, int color) {
        ctx.drawTexture(net.minecraft.client.render.RenderLayer::getGuiTextured, id, x, y, u, v, w, h, regionW, regionH, texW, texH, color);
    }

    public static void guiSprite(net.minecraft.client.gui.DrawContext ctx, Identifier sprite, int x, int y, int w, int h) {
        ctx.drawGuiTexture(net.minecraft.client.render.RenderLayer::getGuiTextured, sprite, x, y, w, h);
    }

    public static void texturedQuad(net.minecraft.client.gui.DrawContext ctx, Identifier id, int x0, int y0, int x1, int y1, float u0, float u1, float v0, float v1) {
    }


    public static net.minecraft.client.texture.NativeImageBackedTexture nativeTex(String name, net.minecraft.client.texture.NativeImage image) {
        return new net.minecraft.client.texture.NativeImageBackedTexture(image);
    }

    public static void playSound(net.minecraft.client.world.ClientWorld world, double x, double y, double z, net.minecraft.sound.SoundEvent sound, net.minecraft.sound.SoundCategory cat, float vol, float pitch) {
        world.playSound(x, y, z, sound, cat, vol, pitch, false);
    }

    public static void addParticle(net.minecraft.client.world.ClientWorld world, net.minecraft.particle.ParticleEffect particle, double x, double y, double z, double vx, double vy, double vz) {
        world.addParticle(particle, x, y, z, vx, vy, vz);
    }


    public static void registerResource(Identifier id) {
        MinecraftClient.getInstance().getTextureManager().registerTexture(id, (net.minecraft.client.texture.ReloadableTexture) new net.minecraft.client.texture.ResourceTexture(id));
    }


    public static void setPixel(net.minecraft.client.texture.NativeImage img, int x, int y, int argb) {
        img.setColorArgb(x, y, argb);
    }

    public static int getPixel(net.minecraft.client.texture.NativeImage img, int x, int y) {
        return img.getColorArgb(x, y);
    }

    public static net.minecraft.entity.LightningEntity lightning(net.minecraft.world.World world) {
        return (net.minecraft.entity.LightningEntity) net.minecraft.entity.EntityType.LIGHTNING_BOLT.create(world, net.minecraft.entity.SpawnReason.EVENT);
    }

    public static boolean coolingDown(net.minecraft.entity.player.ItemCooldownManager manager, net.minecraft.item.ItemStack stack) {
        return manager.isCoolingDown(stack);
    }

    private Mc() {
    }
}
