package dev.hugoservice;

import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.rendering.v1.hud.HudElementRegistry;
import net.fabricmc.fabric.api.client.rendering.v1.hud.VanillaHudElements;
import net.minecraft.block.Blocks;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.render.RenderTickCounter;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.LightningEntity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.SpawnReason;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.particle.BlockStateParticleEffect;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.minecraft.component.DataComponentTypes;
import net.minecraft.component.type.ProfileComponent;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.Vec3d;
import org.joml.Matrix3x2fStack;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Kill effects, on your screen only: where a player dies there is lightning, a shockwave, a pillar of light, their
 * head popping up, shards or rising souls. "Only my kills" needs your hit on them in the last 5 seconds.
 */
public final class KillEffects {
    public static final String[] NAMES = {"Off", "Lightning", "Shockwave", "Light Pillar", "Kill Popup", "Shatter", "Soul Rise"};
    private static final Map<Integer, Long> HIT = new HashMap<>();
    private record Running(int type, Vec3d pos, int[] age) {}
    private static final List<Running> RUNNING = new ArrayList<>();
    private static ItemStack popup;
    private static long popupAt;
    private static int nextId = -2_000_000; // own ids for the client-only lightning, far from the server's

    static void init() {
        ClientTickEvents.END_CLIENT_TICK.register(KillEffects::tick);
        HudElementRegistry.attachElementBefore(VanillaHudElements.CHAT, Identifier.of("hugoservice", "kill_popup"), KillEffects::hud);
    }

    /** You hit something. */
    public static void attacked(Entity target) {
        HIT.put(target.getId(), System.currentTimeMillis());
    }

    /** A living entity died (entity status 3). */
    public static void died(LivingEntity e) {
        Config cfg = Config.INSTANCE;
        MinecraftClient mc = MinecraftClient.getInstance();
        if (cfg.killEffect <= 0 || !(e instanceof PlayerEntity) || e == mc.player) return;
        Long hit = HIT.remove(e.getId());
        if (cfg.killOnlyMine && (hit == null || System.currentTimeMillis() - hit > 5000)) return;
        play(cfg.killEffect, e.getEntityPos(), (PlayerEntity) e);
    }

    /** Runs an effect at a position (also the menu's "Try it", in front of you). */
    public static void play(int type, Vec3d pos, PlayerEntity victim) {
        MinecraftClient mc = MinecraftClient.getInstance();
        ClientWorld world = mc.world;
        if (world == null) return;
        switch (type) {
            case 1 -> {
                LightningEntity bolt = EntityType.LIGHTNING_BOLT.create(world, SpawnReason.EVENT);
                if (bolt == null) return;
                bolt.setId(nextId--);
                bolt.setCosmetic(true);
                bolt.refreshPositionAfterTeleport(pos);
                world.addEntity(bolt);
            }
            case 4 -> {
                popup = new ItemStack(Items.PLAYER_HEAD); // their own head, with their skin
                popup.set(DataComponentTypes.PROFILE, ProfileComponent.ofStatic(victim.getGameProfile()));
                popupAt = System.currentTimeMillis();
                world.playSoundClient(pos.x, pos.y, pos.z, SoundEvents.ENTITY_PLAYER_LEVELUP, SoundCategory.PLAYERS, 0.6f, 1.6f, false);
            }
            default -> {
                RUNNING.add(new Running(type, pos, new int[1]));
                var sound = type == 2 ? SoundEvents.ENTITY_GENERIC_EXPLODE.value() : type == 3 ? SoundEvents.BLOCK_BEACON_ACTIVATE
                    : type == 5 ? SoundEvents.BLOCK_GLASS_BREAK : SoundEvents.PARTICLE_SOUL_ESCAPE.value();
                world.playSoundClient(pos.x, pos.y, pos.z, sound, SoundCategory.PLAYERS, 0.8f, 1f, false);
            }
        }
    }

    private static void tick(MinecraftClient mc) {
        if (mc.world == null) { RUNNING.clear(); HIT.clear(); return; }
        ClientWorld w = mc.world;
        RUNNING.removeIf(r -> {
            int age = r.age()[0]++;
            Vec3d p = r.pos();
            switch (r.type()) {
                case 2 -> { // shockwave: a ring of light running out over the ground
                    if (age == 0) w.addParticleClient(ParticleTypes.SONIC_BOOM, p.x, p.y + 1, p.z, 0, 0, 0);
                    double rad = 0.6 + age * 0.45;
                    for (int i = 0; i < 28; i++) {
                        double a = i / 28.0 * Math.PI * 2;
                        w.addParticleClient(ParticleTypes.END_ROD, p.x + Math.cos(a) * rad, p.y + 0.1, p.z + Math.sin(a) * rad, 0, 0.01, 0);
                    }
                    return age >= 9;
                }
                case 3 -> { // pillar of light up into the sky
                    for (int i = 0; i < 12; i++) {
                        double y = p.y + w.random.nextDouble() * 24;
                        w.addParticleClient(ParticleTypes.END_ROD, p.x + (w.random.nextDouble() - 0.5) * 0.3, y, p.z + (w.random.nextDouble() - 0.5) * 0.3, 0, 0.05, 0);
                    }
                    return age >= 30;
                }
                case 5 -> { // shards burst out
                    var glass = new BlockStateParticleEffect(ParticleTypes.BLOCK, Blocks.GLASS.getDefaultState());
                    for (int i = 0; i < 50; i++) {
                        w.addParticleClient(glass, p.x, p.y + 1, p.z, (w.random.nextDouble() - 0.5) * 0.8, w.random.nextDouble() * 0.6, (w.random.nextDouble() - 0.5) * 0.8);
                    }
                    return true;
                }
                default -> { // souls rise
                    for (int i = 0; i < 4; i++) {
                        w.addParticleClient(i % 2 == 0 ? ParticleTypes.SOUL : ParticleTypes.SOUL_FIRE_FLAME,
                            p.x + (w.random.nextDouble() - 0.5) * 0.8, p.y + w.random.nextDouble() * 1.8, p.z + (w.random.nextDouble() - 0.5) * 0.8, 0, 0.08, 0);
                    }
                    return age >= 30;
                }
            }
        });
    }

    /** Kill popup: their head pops up above the crosshair, bounces, then shrinks away. No text. */
    private static void hud(DrawContext ctx, RenderTickCounter tick) {
        if (popup == null) return;
        long t = System.currentTimeMillis() - popupAt;
        if (t > 1600) { popup = null; return; }
        float s = t < 160 ? t / 160f * 1.2f : t < 300 ? 1.2f - (t - 160) / 140f * 0.2f : t > 1200 ? 1 - (t - 1200) / 400f : 1;
        if (s <= 0.02f) return;
        Matrix3x2fStack m = ctx.getMatrices();
        m.pushMatrix();
        m.translate(ctx.getScaledWindowWidth() / 2f, ctx.getScaledWindowHeight() / 2f - 38);
        m.scale(2.2f * s, 2.2f * s);
        ctx.drawItem(popup, -8, -8);
        m.popMatrix();
    }

    private KillEffects() {}
}
