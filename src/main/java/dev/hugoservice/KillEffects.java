/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.mojang.authlib.GameProfile
 *  net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents
 *  net.fabricmc.fabric.api.client.rendering.v1.hud.HudElementRegistry
 *  net.fabricmc.fabric.api.client.rendering.v1.hud.VanillaHudElements
 *  net.minecraft.entity.Entity
 *  net.minecraft.entity.EntityType
 *  net.minecraft.entity.LivingEntity
 *  net.minecraft.entity.LightningEntity
 *  net.minecraft.entity.player.PlayerEntity
 *  net.minecraft.item.ItemStack
 *  net.minecraft.item.Items
 *  net.minecraft.item.ItemConvertible
 *  net.minecraft.world.World
 *  net.minecraft.block.Blocks
 *  net.minecraft.particle.BlockStateParticleEffect
 *  net.minecraft.particle.ParticleEffect
 *  net.minecraft.particle.ParticleTypes
 *  net.minecraft.util.math.Vec3d
 *  net.minecraft.util.Identifier
 *  net.minecraft.client.MinecraftClient
 *  net.minecraft.client.gui.DrawContext
 *  net.minecraft.sound.SoundEvent
 *  net.minecraft.sound.SoundEvents
 *  net.minecraft.sound.SoundCategory
 *  net.minecraft.entity.SpawnReason
 *  net.minecraft.client.world.ClientWorld
 *  net.minecraft.component.type.ProfileComponent
 *  net.minecraft.component.DataComponentTypes
 *  net.minecraft.client.render.RenderTickCounter
 *  org.joml.Matrix3x2fStack
 */
package dev.hugoservice;

import com.mojang.authlib.GameProfile;
import dev.hugoservice.Config;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.rendering.v1.hud.HudElementRegistry;
import net.fabricmc.fabric.api.client.rendering.v1.hud.VanillaHudElements;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.LightningEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.item.ItemConvertible;
import net.minecraft.world.World;
import net.minecraft.block.Blocks;
import net.minecraft.particle.BlockStateParticleEffect;
import net.minecraft.particle.ParticleEffect;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.util.math.Vec3d;
import net.minecraft.util.Identifier;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.sound.SoundEvent;
import net.minecraft.sound.SoundEvents;
import net.minecraft.sound.SoundCategory;
import net.minecraft.entity.SpawnReason;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.component.type.ProfileComponent;
import net.minecraft.component.DataComponentTypes;
import net.minecraft.client.render.RenderTickCounter;
import org.joml.Matrix3x2fStack;

public final class KillEffects {
    public static final String[] NAMES = new String[]{"Off", "Lightning", "Shockwave", "Light Pillar", "Kill Popup", "Shatter", "Soul Rise"};
    private static final Map<Integer, Long> HIT = new HashMap<Integer, Long>();
    private static final List<Running> RUNNING = new ArrayList<Running>();
    private static ItemStack popup;
    private static long popupAt;
    private static int nextId;

    static void init() {
        ClientTickEvents.END_CLIENT_TICK.register(KillEffects::tick);
        HudElementRegistry.attachElementBefore((Identifier)VanillaHudElements.CHAT, (Identifier)Identifier.of((String)"hugoservice", (String)"kill_popup"), KillEffects::hud);
    }

    public static void attacked(Entity target) {
        HIT.put(target.getId(), System.currentTimeMillis());
    }

    public static void died(LivingEntity e) {
        Config cfg = Config.INSTANCE;
        MinecraftClient mc = MinecraftClient.getInstance();
        if (cfg.killEffect <= 0 || !(e instanceof PlayerEntity) || e == mc.player) {
            return;
        }
        Long hit = HIT.remove(e.getId());
        if (cfg.killOnlyMine && (hit == null || System.currentTimeMillis() - hit > 5000L)) {
            return;
        }
        KillEffects.play(cfg.killEffect, Mc.pos((net.minecraft.entity.Entity)e), (PlayerEntity)e);
    }

    public static void play(int type, Vec3d pos, PlayerEntity victim) {
        MinecraftClient mc = MinecraftClient.getInstance();
        ClientWorld world = mc.world;
        if (world == null) {
            return;
        }
        switch (type) {
            case 1: {
                LightningEntity bolt = (LightningEntity)EntityType.LIGHTNING_BOLT.create((World)world, SpawnReason.EVENT);
                if (bolt == null) {
                    return;
                }
                bolt.setId(nextId--);
                bolt.setCosmetic(true);
                bolt.refreshPositionAfterTeleport(pos);
                world.addEntity((Entity)bolt);
                break;
            }
            case 4: {
                popup = new ItemStack((ItemConvertible)Items.PLAYER_HEAD);
                popup.set(DataComponentTypes.PROFILE, Mc.headProfile(victim.getGameProfile()));
                popupAt = System.currentTimeMillis();
                world.playSoundClient(pos.x, pos.y, pos.z, SoundEvents.ENTITY_PLAYER_LEVELUP, SoundCategory.PLAYERS, 0.6f, 1.6f, false);
                break;
            }
            default: {
                RUNNING.add(new Running(type, pos, new int[1]));
                SoundEvent sound = type == 2 ? (SoundEvent)SoundEvents.ENTITY_GENERIC_EXPLODE.value() : (type == 3 ? SoundEvents.BLOCK_BEACON_ACTIVATE : (type == 5 ? SoundEvents.BLOCK_GLASS_BREAK : (SoundEvent)SoundEvents.PARTICLE_SOUL_ESCAPE.value()));
                world.playSoundClient(pos.x, pos.y, pos.z, sound, SoundCategory.PLAYERS, 0.8f, 1.0f, false);
            }
        }
    }

    private static void tick(MinecraftClient mc) {
        if (mc.world == null) {
            RUNNING.clear();
            HIT.clear();
            return;
        }
        ClientWorld w = mc.world;
        RUNNING.removeIf(r -> {
            int[] nArray = r.age();
            int n = nArray[0];
            nArray[0] = n + 1;
            int age = n;
            Vec3d p = r.pos();
            switch (r.type()) {
                case 2: {
                    if (age == 0) {
                        w.addParticleClient((ParticleEffect)ParticleTypes.SONIC_BOOM, p.x, p.y + 1.0, p.z, 0.0, 0.0, 0.0);
                    }
                    double rad = 0.6 + (double)age * 0.45;
                    for (int i = 0; i < 28; ++i) {
                        double a = (double)i / 28.0 * Math.PI * 2.0;
                        w.addParticleClient((ParticleEffect)ParticleTypes.END_ROD, p.x + Math.cos(a) * rad, p.y + 0.1, p.z + Math.sin(a) * rad, 0.0, 0.01, 0.0);
                    }
                    return age >= 9;
                }
                case 3: {
                    for (int i = 0; i < 12; ++i) {
                        double y = p.y + w.random.nextDouble() * 24.0;
                        w.addParticleClient((ParticleEffect)ParticleTypes.END_ROD, p.x + (w.random.nextDouble() - 0.5) * 0.3, y, p.z + (w.random.nextDouble() - 0.5) * 0.3, 0.0, 0.05, 0.0);
                    }
                    return age >= 30;
                }
                case 5: {
                    BlockStateParticleEffect glass = new BlockStateParticleEffect(ParticleTypes.BLOCK, Blocks.GLASS.getDefaultState());
                    for (int i = 0; i < 50; ++i) {
                        w.addParticleClient((ParticleEffect)glass, p.x, p.y + 1.0, p.z, (w.random.nextDouble() - 0.5) * 0.8, w.random.nextDouble() * 0.6, (w.random.nextDouble() - 0.5) * 0.8);
                    }
                    return true;
                }
            }
            for (int i = 0; i < 4; ++i) {
                w.addParticleClient((ParticleEffect)(i % 2 == 0 ? ParticleTypes.SOUL : ParticleTypes.SOUL_FIRE_FLAME), p.x + (w.random.nextDouble() - 0.5) * 0.8, p.y + w.random.nextDouble() * 1.8, p.z + (w.random.nextDouble() - 0.5) * 0.8, 0.0, 0.08, 0.0);
            }
            return age >= 30;
        });
    }

    private static void hud(DrawContext ctx, RenderTickCounter tick) {
        float s;
        if (popup == null) {
            return;
        }
        long t = System.currentTimeMillis() - popupAt;
        if (t > 1600L) {
            popup = null;
            return;
        }
        if (t < 160L) {
            s = (float)t / 160.0f * 1.2f;
        } else if (t < 300L) {
            s = 1.2f - (float)(t - 160L) / 140.0f * 0.2f;
        } else if (t > 1200L) {
            s = 1.0f - (float)(t - 1200L) / 400.0f;
        } else {
            s = 1.0f;
        }
        if (s <= 0.02f) {
            return;
        }
        Matrix3x2fStack m = ctx.getMatrices();
        m.pushMatrix();
        m.translate((float)ctx.getScaledWindowWidth() / 2.0f, (float)ctx.getScaledWindowHeight() / 2.0f - 38.0f);
        m.scale(2.2f * s, 2.2f * s);
        ctx.drawItem(popup, -8, -8);
        m.popMatrix();
    }

    private KillEffects() {
    }

    static {
        nextId = -2000000;
    }

    private record Running(int type, Vec3d pos, int[] age) {
    }
}

