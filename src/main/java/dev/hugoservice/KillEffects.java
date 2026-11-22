package dev.hugoservice;

import it.unimi.dsi.fastutil.ints.IntList;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.rendering.v1.hud.HudElementRegistry;
import net.fabricmc.fabric.api.client.rendering.v1.hud.VanillaHudElements;
import net.minecraft.block.Blocks;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.render.RenderTickCounter;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.component.DataComponentTypes;
import net.minecraft.component.type.FireworkExplosionComponent;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.LightningEntity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.SpawnReason;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemConvertible;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.particle.BlockStateParticleEffect;
import net.minecraft.particle.DustParticleEffect;
import net.minecraft.particle.ParticleEffect;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvent;
import net.minecraft.sound.SoundEvents;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;
import org.joml.Matrix3x2fStack;

public final class KillEffects {
    public static final String[] NAMES = new String[]{"Off", "Lightning", "Shockwave", "Light Pillar", "Kill Popup", "Shatter", "Soul Rise", "Firework", "Blood Burst", "Ender"};
    /** Kill sound, played on your side at full volume; "Effect's own" keeps each effect's sound. */
    public static final String[] SOUNDS = new String[]{"Effect's own", "Off", "Ding", "Pling", "Orb", "Level up", "Chime", "Thunder", "Custom .ogg"};
    public static final int CUSTOM_SOUND = 8;
    /** Your .ogg goes into the built pack as this sound (PackBuilder), so it plays after Apply. */
    public static final Identifier CUSTOM_ID = Identifier.of("hugoservice", "kill_custom");
    private static final Map<Integer, Long> HIT = new HashMap<Integer, Long>();
    private static final List<Running> RUNNING = new ArrayList<Running>();
    private static ItemStack popup;
    private static String popupName = "";
    private static long popupAt;
    private static int nextId = -2000000;

    private KillEffects() {
    }

    public static Path customSoundFile() {
        return Config.DIR.resolve("kill_sound.ogg");
    }

    static void init() {
        ClientTickEvents.END_CLIENT_TICK.register(KillEffects::tick);
        HudElementRegistry.attachElementBefore(VanillaHudElements.CHAT, Identifier.of("hugoservice", "kill_popup"), KillEffects::hud);
    }

    public static void attacked(Entity target) {
        if (Config.INSTANCE.killEffect <= 0 && Config.INSTANCE.killSound <= 0 || !(target instanceof PlayerEntity)) {
            return;
        }
        if (HIT.size() > 64) {
            HIT.clear();
        }
        HIT.put(target.getId(), System.currentTimeMillis());
    }

    public static void died(LivingEntity e) {
        Config cfg = Config.INSTANCE;
        MinecraftClient mc = MinecraftClient.getInstance();
        if (cfg.killEffect <= 0 && cfg.killSound <= 0 || !(e instanceof PlayerEntity) || e == mc.player) {
            return;
        }
        Long hit = HIT.remove(e.getId());
        if (cfg.killOnlyMine && (hit == null || System.currentTimeMillis() - hit > 5000L)) {
            return;
        }
        KillEffects.play(cfg.killEffect, Mc.pos(e), (PlayerEntity)e);
    }

    /** Plays the effect at pos, then the kill sound (or the effect's own sound). */
    public static void play(int type, Vec3d pos, PlayerEntity victim) {
        MinecraftClient mc = MinecraftClient.getInstance();
        ClientWorld world = mc.world;
        if (world == null) {
            return;
        }
        if (type == 1) {
            LightningEntity bolt = EntityType.LIGHTNING_BOLT.create((World)world, SpawnReason.EVENT);
            if (bolt != null) {
                bolt.setId(nextId--);
                bolt.setCosmetic(true);
                bolt.refreshPositionAfterTeleport(pos);
                world.addEntity(bolt);
            }
        }
        if (type == 4) {
            popup = new ItemStack((ItemConvertible)Items.PLAYER_HEAD);
            popup.set(DataComponentTypes.PROFILE, Mc.headProfile(victim.getGameProfile()));
            popupName = victim.getName().getString();
            popupAt = System.currentTimeMillis();
        }
        if (type > 0 && type != 4) {
            RUNNING.add(new Running(type, pos, new int[1]));
        }
        int sound = Config.INSTANCE.killSound;
        if (sound == 0) {
            KillEffects.effectSound(world, type, pos);
        } else {
            KillEffects.killSound(mc, sound);
        }
    }

    private static void effectSound(ClientWorld w, int type, Vec3d p) {
        SoundEvent s;
        float pitch = 1.0f;
        switch (type) {
            case 2 -> s = SoundEvents.ENTITY_GENERIC_EXPLODE.value();
            case 3 -> s = SoundEvents.BLOCK_BEACON_ACTIVATE;
            case 4 -> { s = SoundEvents.ENTITY_PLAYER_LEVELUP; pitch = 1.6f; }
            case 5 -> s = SoundEvents.BLOCK_GLASS_BREAK;
            case 6 -> s = SoundEvents.PARTICLE_SOUL_ESCAPE.value();
            case 8 -> { s = SoundEvents.ENTITY_PLAYER_HURT; pitch = 0.6f; }
            case 9 -> s = SoundEvents.ENTITY_ENDERMAN_TELEPORT;
            default -> s = null; // lightning and firework make their own
        }
        if (s != null) {
            w.playSoundClient(p.x, p.y, p.z, s, SoundCategory.PLAYERS, 0.8f, pitch, false);
        }
    }

    /** Played where you stand, so it is never quieter because the kill was far away. */
    public static void killSound(MinecraftClient mc, int sound) {
        if (mc.world == null || mc.player == null || sound <= 1) {
            return;
        }
        SoundEvent s;
        float pitch;
        switch (sound) {
            case 2 -> { s = SoundEvents.BLOCK_NOTE_BLOCK_BELL.value(); pitch = 1.5f; }
            case 3 -> { s = SoundEvents.BLOCK_NOTE_BLOCK_PLING.value(); pitch = 2.0f; }
            case 4 -> { s = SoundEvents.ENTITY_EXPERIENCE_ORB_PICKUP; pitch = 1.3f; }
            case 5 -> { s = SoundEvents.ENTITY_PLAYER_LEVELUP; pitch = 1.0f; }
            case 6 -> { s = SoundEvents.BLOCK_AMETHYST_BLOCK_CHIME; pitch = 1.2f; }
            case 7 -> { s = SoundEvents.ENTITY_LIGHTNING_BOLT_THUNDER; pitch = 1.2f; }
            default -> { s = SoundEvent.of(CUSTOM_ID); pitch = 1.0f; }
        }
        Vec3d at = Mc.pos(mc.player);
        float vol = Math.max(0, Math.min(100, Config.INSTANCE.killVolume)) / 100.0f;
        mc.world.playSoundClient(at.x, at.y + 1.0, at.z, s, SoundCategory.PLAYERS, vol, pitch, false);
    }

    private static void dust(ClientWorld w, int rgb, float size, double x, double y, double z, double vx, double vy, double vz) {
        w.addParticleClient((ParticleEffect)new DustParticleEffect(rgb, size), x, y, z, vx, vy, vz);
    }

    private static void tick(MinecraftClient mc) {
        if (mc.world == null) {
            RUNNING.clear();
            HIT.clear();
            return;
        }
        ClientWorld w = mc.world;
        RUNNING.removeIf(r -> {
            int age = r.age()[0]++;
            Vec3d p = r.pos();
            switch (r.type()) {
                case 1: { // lightning: sparks off the body and a blue ring on the ground
                    for (int i = 0; i < 18; ++i) {
                        w.addParticleClient(ParticleTypes.ELECTRIC_SPARK, p.x, p.y + w.random.nextDouble() * 1.8, p.z, (w.random.nextDouble() - 0.5) * 1.2, w.random.nextDouble() * 0.6, (w.random.nextDouble() - 0.5) * 1.2);
                    }
                    double rad = 0.5 + age * 0.5;
                    for (int i = 0; i < 32; ++i) {
                        double a = i / 32.0 * Math.PI * 2.0;
                        dust(w, 0x9FD8FF, 1.1f, p.x + Math.cos(a) * rad, p.y + 0.1, p.z + Math.sin(a) * rad, 0, 0, 0);
                    }
                    return age >= 6;
                }
                case 2: { // shockwave: an eased double ring with a boom in the middle
                    if (age == 0) {
                        w.addParticleClient(ParticleTypes.SONIC_BOOM, p.x, p.y + 1.0, p.z, 0, 0, 0);
                        w.addParticleClient(ParticleTypes.EXPLOSION, p.x, p.y + 1.0, p.z, 0, 0, 0);
                    }
                    double t = (age + 1) / 12.0;
                    double rad = 0.4 + (1 - Math.pow(1 - t, 3)) * 5.0;
                    int color = age < 6 ? 0xFFFFFF : 0x8FE3FF;
                    for (int i = 0; i < 48; ++i) {
                        double a = i / 48.0 * Math.PI * 2.0;
                        double cx = Math.cos(a), cz = Math.sin(a);
                        dust(w, color, 1.4f, p.x + cx * rad, p.y + 0.15, p.z + cz * rad, 0, 0, 0);
                        if (i % 3 == 0) {
                            w.addParticleClient(ParticleTypes.END_ROD, p.x + cx * rad * 0.55, p.y + 0.1, p.z + cz * rad * 0.55, cx * 0.04, 0.02, cz * 0.04);
                        }
                    }
                    return age >= 11;
                }
                case 3: { // light pillar: a beam that shoots up, a gold helix around it, sparks at the top
                    int top = Math.min(26, (age + 1) * 4);
                    for (int i = 0; i < 14; ++i) {
                        w.addParticleClient(ParticleTypes.END_ROD, p.x + (w.random.nextDouble() - 0.5) * 0.25, p.y + w.random.nextDouble() * top, p.z + (w.random.nextDouble() - 0.5) * 0.25, 0, 0.03, 0);
                    }
                    for (int s = 0; s < 2; ++s) {
                        double a = age * 0.55 + s * Math.PI;
                        dust(w, 0xFFD86B, 1.3f, p.x + Math.cos(a) * 0.8, p.y + age * 0.75, p.z + Math.sin(a) * 0.8, 0, 0, 0);
                    }
                    if (age == 0 || age == 28) {
                        double y = age == 0 ? p.y + 0.2 : p.y + top;
                        for (int i = 0; i < 36; ++i) {
                            double a = i / 36.0 * Math.PI * 2.0;
                            w.addParticleClient(ParticleTypes.FIREWORK, p.x, y, p.z, Math.cos(a) * 0.35, (w.random.nextDouble() - 0.3) * 0.25, Math.sin(a) * 0.35);
                        }
                    }
                    return age >= 30;
                }
                case 5: { // shatter: the body breaks into glass, a white puff, then crits
                    if (age == 0) {
                        BlockStateParticleEffect glass = new BlockStateParticleEffect(ParticleTypes.BLOCK, Blocks.GLASS.getDefaultState());
                        for (int i = 0; i < 90; ++i) {
                            double dx = (w.random.nextDouble() - 0.5) * 0.6, dz = (w.random.nextDouble() - 0.5) * 0.6;
                            w.addParticleClient(glass, p.x + dx, p.y + w.random.nextDouble() * 1.9, p.z + dz, dx * 1.6, w.random.nextDouble() * 0.5, dz * 1.6);
                        }
                        for (int i = 0; i < 25; ++i) {
                            dust(w, 0xFFFFFF, 1.6f, p.x + (w.random.nextDouble() - 0.5) * 0.8, p.y + w.random.nextDouble() * 1.9, p.z + (w.random.nextDouble() - 0.5) * 0.8, 0, 0, 0);
                        }
                    }
                    for (int i = 0; i < 6; ++i) {
                        w.addParticleClient(ParticleTypes.CRIT, p.x, p.y + 1.0, p.z, (w.random.nextDouble() - 0.5) * 0.9, w.random.nextDouble() * 0.6, (w.random.nextDouble() - 0.5) * 0.9);
                    }
                    return age >= 3;
                }
                case 7: { // firework: a real firework burst, then a smaller star above it
                    if (age == 0) {
                        w.addFireworkParticle(p.x, p.y + 1.6, p.z, 0, 0, 0, List.of(new FireworkExplosionComponent(FireworkExplosionComponent.Type.LARGE_BALL, IntList.of(0xFF3B3B, 0xFFD23F, 0x3BC9FF), IntList.of(0xFFFFFF), true, true)));
                    }
                    if (age == 7) {
                        w.addFireworkParticle(p.x, p.y + 3.4, p.z, 0, 0, 0, List.of(new FireworkExplosionComponent(FireworkExplosionComponent.Type.STAR, IntList.of(0xC14BFF, 0xFF6EC7), IntList.of(), false, true)));
                    }
                    return age >= 7;
                }
                case 8: { // blood burst: red block bits fly off, then drops fall
                    if (age == 0) {
                        BlockStateParticleEffect red = new BlockStateParticleEffect(ParticleTypes.BLOCK, Blocks.REDSTONE_BLOCK.getDefaultState());
                        for (int i = 0; i < 70; ++i) {
                            double dx = (w.random.nextDouble() - 0.5) * 0.6, dz = (w.random.nextDouble() - 0.5) * 0.6;
                            w.addParticleClient(red, p.x + dx, p.y + 0.4 + w.random.nextDouble() * 1.4, p.z + dz, dx * 1.8, w.random.nextDouble() * 0.6, dz * 1.8);
                        }
                        for (int i = 0; i < 40; ++i) {
                            dust(w, 0x8B0000, 1.7f, p.x + (w.random.nextDouble() - 0.5) * 0.9, p.y + 0.3 + w.random.nextDouble() * 1.5, p.z + (w.random.nextDouble() - 0.5) * 0.9, 0, 0, 0);
                        }
                    }
                    for (int i = 0; i < 6; ++i) {
                        dust(w, 0xB00000, 1.0f, p.x + (w.random.nextDouble() - 0.5) * 1.2, p.y + 0.2 + w.random.nextDouble() * 1.6, p.z + (w.random.nextDouble() - 0.5) * 1.2, 0, -0.05, 0);
                    }
                    return age >= 10;
                }
                case 9: { // ender: purple pulled in, then a burst out
                    if (age < 12) {
                        for (int i = 0; i < 30; ++i) {
                            w.addParticleClient(ParticleTypes.PORTAL, p.x, p.y + 1.0, p.z, (w.random.nextDouble() - 0.5) * 3.0, (w.random.nextDouble() - 0.5) * 3.0, (w.random.nextDouble() - 0.5) * 3.0);
                        }
                    } else if (age == 12) {
                        for (int i = 0; i < 60; ++i) {
                            w.addParticleClient(ParticleTypes.REVERSE_PORTAL, p.x, p.y + 1.0, p.z, (w.random.nextDouble() - 0.5) * 0.8, (w.random.nextDouble() - 0.5) * 0.8, (w.random.nextDouble() - 0.5) * 0.8);
                        }
                        for (int i = 0; i < 30; ++i) {
                            dust(w, 0x9B30FF, 1.6f, p.x + (w.random.nextDouble() - 0.5) * 1.2, p.y + w.random.nextDouble() * 2.0, p.z + (w.random.nextDouble() - 0.5) * 1.2, 0, 0, 0);
                        }
                    }
                    return age >= 12;
                }
                default: { // soul rise: three strands of souls spiral up
                    if (age == 0) {
                        for (int i = 0; i < 12; ++i) {
                            w.addParticleClient(ParticleTypes.SCULK_SOUL, p.x + (w.random.nextDouble() - 0.5) * 0.6, p.y + w.random.nextDouble() * 1.8, p.z + (w.random.nextDouble() - 0.5) * 0.6, 0, 0.05, 0);
                        }
                    }
                    for (int s = 0; s < 3; ++s) {
                        double a = age * 0.45 + s * Math.PI * 2.0 / 3.0;
                        double r0 = 0.9 - age * 0.02;
                        w.addParticleClient(s % 2 == 0 ? ParticleTypes.SOUL : ParticleTypes.SOUL_FIRE_FLAME, p.x + Math.cos(a) * r0, p.y + age * 0.09, p.z + Math.sin(a) * r0, 0, 0.02, 0);
                    }
                    if (age % 6 == 0) {
                        for (int i = 0; i < 20; ++i) {
                            double a = i / 20.0 * Math.PI * 2.0;
                            dust(w, 0x5FE6FF, 1.0f, p.x + Math.cos(a) * 0.9, p.y + 0.1, p.z + Math.sin(a) * 0.9, 0, 0, 0);
                        }
                    }
                    return age >= 32;
                }
            }
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
        float cx = ctx.getScaledWindowWidth() / 2.0f;
        float cy = ctx.getScaledWindowHeight() / 2.0f - 38.0f;
        Matrix3x2fStack m = ctx.getMatrices();
        m.pushMatrix();
        m.translate(cx, cy);
        m.scale(2.2f * s, 2.2f * s);
        ctx.drawItem(popup, -8, -8);
        m.popMatrix();
        int alpha = Math.max(8, Math.min(255, (int)(255.0f * Math.min(1.0f, s))));
        ctx.drawCenteredTextWithShadow(MinecraftClient.getInstance().textRenderer, popupName, (int)cx, (int)(cy + 22.0f * s), alpha << 24 | 0xFF5555);
    }

    private record Running(int type, Vec3d pos, int[] age) {
    }
}
