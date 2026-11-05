/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientLifecycleEvents
 *  net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents
 *  net.fabricmc.fabric.api.client.rendering.v1.world.WorldRenderEvents
 *  net.minecraft.client.texture.NativeImage
 *  net.minecraft.client.texture.NativeImageBackedTexture
 *  net.minecraft.client.render.RenderLayers
 *  net.minecraft.entity.Entity
 *  net.minecraft.item.ItemStack
 *  net.minecraft.item.Items
 *  net.minecraft.item.ItemConvertible
 *  net.minecraft.block.Blocks
 *  net.minecraft.util.math.BlockPos
 *  net.minecraft.util.math.BlockPos$Mutable
 *  net.minecraft.util.hit.HitResult$Type
 *  net.minecraft.util.math.Vec3d
 *  net.minecraft.util.shape.VoxelShapes
 *  net.minecraft.util.shape.VoxelShape
 *  net.minecraft.client.MinecraftClient
 *  net.minecraft.world.RaycastContext
 *  net.minecraft.world.RaycastContext$FluidHandling
 *  net.minecraft.world.RaycastContext$ShapeType
 *  net.minecraft.client.util.math.MatrixStack
 *  net.minecraft.client.render.VertexConsumer
 *  net.minecraft.client.network.AbstractClientPlayerEntity
 *  net.minecraft.client.network.ClientPlayerEntity
 *  net.minecraft.client.render.VertexRendering
 */
package dev.hugoservice;

import dev.hugoservice.Config;
import dev.hugoservice.NameFaker;
import dev.hugoservice.Presence;
import dev.hugoservice.mixin.OverlayTextureAccessor;
import java.awt.Color;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientLifecycleEvents;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.minecraft.client.texture.NativeImage;
import net.minecraft.client.texture.NativeImageBackedTexture;
import net.minecraft.client.render.RenderLayers;
import net.minecraft.entity.Entity;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.item.ItemConvertible;
import net.minecraft.block.Blocks;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.hit.HitResult;
import net.minecraft.util.math.Vec3d;
import net.minecraft.util.shape.VoxelShapes;
import net.minecraft.util.shape.VoxelShape;
import net.minecraft.client.MinecraftClient;
import net.minecraft.world.RaycastContext;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.client.render.VertexConsumer;
import net.minecraft.client.network.AbstractClientPlayerEntity;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.client.render.VertexRendering;

public final class Fx {
    public static final String HIT = "hit";
    public static final String HITBOX = "hitbox";
    public static final String OUTLINE = "outline";
    public static final String COBWEB = "cobweb";
    public static final String GLOW = "glow";
    public static final String SHIELD = "shield";
    public static final String SHIELD_OFF = "shield_off";
    public static final String NAMETAG = "nametag";
    private static final ItemStack SHIELD_STACK = new ItemStack((ItemConvertible)Items.SHIELD);
    private static final Set<Integer> VISIBLE = new HashSet<Integer>();
    static List<BlockPos> cobwebs = List.of();
    private static int ticks;

    static void init() {
        ClientLifecycleEvents.CLIENT_STARTED.register(client -> Fx.applyHitColor());
        ClientTickEvents.END_CLIENT_TICK.register(Fx::tick);
        ClientTickEvents.END_CLIENT_TICK.register(NameFaker::tick);
        ClientTickEvents.END_CLIENT_TICK.register(Presence::tick);
        Mc.registerCobwebOverlay();
    }

    private static void tick(MinecraftClient mc) {
        VISIBLE.clear();
        if (mc.world == null || mc.player == null) {
            cobwebs = List.of();
            return;
        }
        if (Config.INSTANCE.fx(GLOW) != null) {
            Vec3d eye = mc.player.getCameraPosVec(1.0f);
            if (Config.INSTANCE.glowSelf && !mc.options.getPerspective().isFirstPerson()) {
                VISIBLE.add(mc.player.getId());
            }
            for (AbstractClientPlayerEntity p : mc.world.getPlayers()) {
                if (p == mc.player || p.isInvisible() || p.squaredDistanceTo((Entity)mc.player) > 9216.0 || !Fx.clear(mc, eye, p.getEyePos()) && !Fx.clear(mc, eye, p.getBoundingBox().getCenter())) continue;
                VISIBLE.add(p.getId());
            }
        }
        if (Config.INSTANCE.fx(COBWEB) == null) {
            cobwebs = List.of();
            return;
        }
        if (ticks++ % 10 != 0) {
            return;
        }
        ArrayList<BlockPos> found = new ArrayList<BlockPos>();
        BlockPos c = mc.player.getBlockPos();
        BlockPos.Mutable pos = new BlockPos.Mutable();
        int r = 10;
        for (int x = -r; x <= r; ++x) {
            for (int y = -r; y <= r; ++y) {
                for (int z = -r; z <= r; ++z) {
                    pos.set(c.getX() + x, c.getY() + y, c.getZ() + z);
                    if (!mc.world.getBlockState((BlockPos)pos).isOf(Blocks.COBWEB)) continue;
                    found.add(pos.toImmutable());
                }
            }
        }
        cobwebs = found;
    }

    private static boolean clear(MinecraftClient mc, Vec3d from, Vec3d to) {
        return mc.world.raycast(new RaycastContext(from, to, RaycastContext.ShapeType.VISUAL, RaycastContext.FluidHandling.NONE, (Entity)mc.player)).getType() == HitResult.Type.MISS;
    }

    public static Integer shieldTint() {
        Config cfg = Config.INSTANCE;
        ClientPlayerEntity p = MinecraftClient.getInstance().player;
        if (!cfg.fxOn.contains(SHIELD) || p == null) {
            return null;
        }
        boolean disabled = p.getItemCooldownManager().isCoolingDown(SHIELD_STACK);
        return 0xFF000000 | cfg.fxColorOf(disabled ? SHIELD_OFF : SHIELD);
    }

    public static int glowColor() {
        Config cfg = Config.INSTANCE;
        if (!cfg.glowRainbow) {
            return cfg.fxColorOf(GLOW);
        }
        return Color.HSBtoRGB((float)(System.currentTimeMillis() % 3000L) / 3000.0f, 0.75f, 1.0f) & 0xFFFFFF;
    }

    public static boolean glowing(int entityId) {
        return Config.INSTANCE.fx(GLOW) != null && VISIBLE.contains(entityId);
    }

    public static void applyHitColor() {
        MinecraftClient mc = MinecraftClient.getInstance();
        if (mc.gameRenderer == null) {
            return;
        }
        Integer c = Config.INSTANCE.fx(HIT);
        int alpha = Math.round((float)Math.max(5, Math.min(100, Config.INSTANCE.hitStrength)) * 2.55f);
        int argb = alpha << 24 | (c == null ? 0xFF0000 : c);
        NativeImageBackedTexture tex = ((OverlayTextureAccessor)mc.gameRenderer.getOverlayTexture()).hugoservice$texture();
        NativeImage img = tex.getImage();
        if (img == null) {
            return;
        }
        for (int y = 0; y < 8; ++y) {
            for (int x = 0; x < 16; ++x) {
                img.setColorArgb(x, y, argb);
            }
        }
        tex.upload();
    }

    private Fx() {
    }
}

