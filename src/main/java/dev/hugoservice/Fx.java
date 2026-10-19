package dev.hugoservice;

import dev.hugoservice.mixin.OverlayTextureAccessor;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientLifecycleEvents;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.rendering.v1.world.WorldRenderEvents;
import net.minecraft.block.Blocks;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.AbstractClientPlayerEntity;
import net.minecraft.client.render.RenderLayers;
import net.minecraft.client.render.VertexConsumer;
import net.minecraft.client.render.VertexRendering;
import net.minecraft.client.texture.NativeImage;
import net.minecraft.client.texture.NativeImageBackedTexture;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.util.hit.HitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;
import net.minecraft.util.shape.VoxelShapes;
import net.minecraft.world.RaycastContext;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/** In-game colour effects: hit colour, cobweb outlines and player glow (only while visible). */
public final class Fx {
    public static final String HIT = "hit", HITBOX = "hitbox", OUTLINE = "outline", COBWEB = "cobweb", GLOW = "glow",
        SHIELD = "shield", SHIELD_OFF = "shield_off", NAMETAG = "nametag";
    private static final net.minecraft.item.ItemStack SHIELD_STACK = new net.minecraft.item.ItemStack(net.minecraft.item.Items.SHIELD);

    /** Players you can currently see; refreshed every tick. */
    private static final Set<Integer> VISIBLE = new HashSet<>();
    private static List<BlockPos> cobwebs = List.of();
    private static int ticks;

    static void init() {
        ClientLifecycleEvents.CLIENT_STARTED.register(client -> applyHitColor());
        ClientTickEvents.END_CLIENT_TICK.register(Fx::tick);
        ClientTickEvents.END_CLIENT_TICK.register(NameFaker::tick);
        ClientTickEvents.END_CLIENT_TICK.register(Presence::tick);
        WorldRenderEvents.AFTER_ENTITIES.register(ctx -> {
            Integer color = Config.INSTANCE.fx(COBWEB);
            if (color == null || cobwebs.isEmpty()) return;
            Vec3d cam = ctx.worldState().cameraRenderState.pos;
            MatrixStack m = ctx.matrices();
            VertexConsumer lines = ctx.consumers().getBuffer(RenderLayers.lines());
            float width = Math.max(1f, MinecraftClient.getInstance().getWindow().getMinimumLineWidth()) * Math.max(1, Config.INSTANCE.cobwebWidth) / 2f;
            for (BlockPos p : cobwebs) {
                VertexRendering.drawOutline(m, lines, VoxelShapes.fullCube(), p.getX() - cam.x, p.getY() - cam.y, p.getZ() - cam.z,
                    0xFF000000 | color, width);
            }
        });
    }

    private static void tick(MinecraftClient mc) {
        VISIBLE.clear();
        if (mc.world == null || mc.player == null) { cobwebs = List.of(); return; }

        if (Config.INSTANCE.fx(GLOW) != null) {
            Vec3d eye = mc.player.getCameraPosVec(1f);
            if (Config.INSTANCE.glowSelf && !mc.options.getPerspective().isFirstPerson()) VISIBLE.add(mc.player.getId()); // yourself in F5
            for (AbstractClientPlayerEntity p : mc.world.getPlayers()) {
                if (p == mc.player || p.isInvisible() || p.squaredDistanceTo(mc.player) > 96 * 96) continue;
                if (clear(mc, eye, p.getEyePos()) || clear(mc, eye, p.getBoundingBox().getCenter())) VISIBLE.add(p.getId());
            }
        }

        if (Config.INSTANCE.fx(COBWEB) == null) { cobwebs = List.of(); return; }
        if (ticks++ % 10 != 0) return; // twice a second is plenty
        List<BlockPos> found = new ArrayList<>();
        BlockPos c = mc.player.getBlockPos();
        BlockPos.Mutable pos = new BlockPos.Mutable();
        int r = 10;
        for (int x = -r; x <= r; x++) for (int y = -r; y <= r; y++) for (int z = -r; z <= r; z++) {
            pos.set(c.getX() + x, c.getY() + y, c.getZ() + z);
            if (mc.world.getBlockState(pos).isOf(Blocks.COBWEB)) found.add(pos.toImmutable());
        }
        cobwebs = found;
    }

    private static boolean clear(MinecraftClient mc, Vec3d from, Vec3d to) {
        return mc.world.raycast(new RaycastContext(from, to, RaycastContext.ShapeType.VISUAL, RaycastContext.FluidHandling.NONE, mc.player))
            .getType() == HitResult.Type.MISS;
    }

    /** Tint (ARGB) for your first-person shield: ready or disabled by an axe. Null when off. */
    public static Integer shieldTint() {
        Config cfg = Config.INSTANCE;
        var p = MinecraftClient.getInstance().player;
        if (!cfg.fxOn.contains(SHIELD) || p == null) return null;
        boolean disabled = p.getItemCooldownManager().isCoolingDown(SHIELD_STACK);
        return 0xFF000000 | cfg.fxColorOf(disabled ? SHIELD_OFF : SHIELD);
    }

    /** Outline colour of glowing players: yours, or running through the rainbow. */
    public static int glowColor() {
        Config cfg = Config.INSTANCE;
        if (!cfg.glowRainbow) return cfg.fxColorOf(GLOW);
        return java.awt.Color.HSBtoRGB(System.currentTimeMillis() % 3000 / 3000f, 0.75f, 1f) & 0xFFFFFF;
    }

    public static boolean glowing(int entityId) {
        return Config.INSTANCE.fx(GLOW) != null && VISIBLE.contains(entityId);
    }

    /** Rewrites the red "hurt" rows of the entity overlay texture. */
    public static void applyHitColor() {
        MinecraftClient mc = MinecraftClient.getInstance();
        if (mc.gameRenderer == null) return;
        Integer c = Config.INSTANCE.fx(HIT);
        int alpha = Math.round(Math.max(5, Math.min(100, Config.INSTANCE.hitStrength)) * 2.55f); // vanilla: 0xB2 = 70 %
        int argb = alpha << 24 | (c == null ? 0xFF0000 : c);
        NativeImageBackedTexture tex = ((OverlayTextureAccessor) mc.gameRenderer.getOverlayTexture()).hugoservice$texture();
        NativeImage img = tex.getImage();
        if (img == null) return;
        for (int y = 0; y < 8; y++) for (int x = 0; x < 16; x++) img.setColorArgb(x, y, argb);
        tex.upload();
    }

    private Fx() {}
}
