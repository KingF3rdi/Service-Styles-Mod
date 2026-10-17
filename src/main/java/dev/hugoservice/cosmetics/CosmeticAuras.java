// Based on Titus Cosmetics 1.3.22, MIT License, Copyright (c) 2026 Titus. See LICENSE_TitusCosmetics.txt.
package dev.hugoservice.cosmetics;

import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.AbstractClientPlayerEntity;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.particle.DustParticleEffect;
import net.minecraft.particle.ParticleEffect;
import net.minecraft.particle.ParticleType;
import net.minecraft.particle.SimpleParticleType;
import net.minecraft.registry.Registries;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.random.Random;

/**
 * Spawns the particles of the aura cosmetics. On every client tick, each player wearing an aura gets a few particles on a
 * ring around the body, outside the model, and they rise from there. The particles exist only on the client that spawns them.
 */
public final class CosmeticAuras {
	/** Particles per player on every client tick. One or two are hardly seen, so an aura needs a steady flow. */
	private static final int PER_TICK = 3;
	/** Particles start on a ring this far from the middle of the body, in blocks, so the aura surrounds the model. */
	private static final double RING_MIN = 0.7;
	private static final double RING_SPAN = 0.5;
	/** Players farther away than this many blocks get no particles. */
	private static final double RANGE = 64.0;

	private CosmeticAuras() {
	}

	/** Starts spawning the particles. Called by {@link CosmeticRendering#init()}. */
	static void init() {
		ClientTickEvents.END_CLIENT_TICK.register(CosmeticAuras::tick);
	}

	private static void tick(MinecraftClient client) {
		ClientWorld level = client.world;
		if (level == null || client.player == null || client.isPaused()) {
			return;
		}
		Random random = level.getRandom();
		for (AbstractClientPlayerEntity player : level.getPlayers()) {
			if (player.squaredDistanceTo(client.player) > RANGE * RANGE) {
				continue;
			}
			Loadout loadout = CosmeticRendering.loadoutOf(player);
			trail(level, player, loadout.get(CosmeticCategory.TRAIL), random);
			Cosmetic aura = loadout.get(CosmeticCategory.AURA);
			if (aura == null || aura.particle() == null) {
				continue;
			}
			ParticleType<?> type = Registries.PARTICLE_TYPE
					.getOptionalValue(Identifier.ofVanilla(aura.particle()))
					.orElse(null);
			// Only simple particles have no extra data, so only they can be spawned from a name alone.
			if (type instanceof SimpleParticleType simple) {
				for (int i = 0; i < PER_TICK; i++) {
					spawn(level, player, simple, random);
				}
			}
		}
	}

	/** Trail (HugoService addition): particles where the player was last tick, only while it moves. */
	private static void trail(ClientWorld level, AbstractClientPlayerEntity player, Cosmetic trail, Random random) {
		if (trail == null || trail.particle() == null) {
			return;
		}
		double dx = player.getX() - player.lastX, dy = player.getY() - player.lastY, dz = player.getZ() - player.lastZ;
		if (dx * dx + dy * dy + dz * dz < 0.0004 || player.isInvisible()) {
			return;
		}
		ParticleEffect effect;
		if (CosmeticCatalog.TRAIL_RAINBOW.equals(trail.particle())) {
			float hue = (level.getTime() % 60) / 60f;
			effect = new DustParticleEffect(java.awt.Color.HSBtoRGB(hue, 0.85f, 1f) & 0xFFFFFF, 1.2f);
		} else if (Registries.PARTICLE_TYPE.getOptionalValue(Identifier.ofVanilla(trail.particle())).orElse(null)
				instanceof SimpleParticleType simple) {
			effect = simple;
		} else {
			return;
		}
		for (int i = 0; i < 2; i++) {
			double x = player.lastX + (random.nextDouble() - 0.5) * 0.35;
			double z = player.lastZ + (random.nextDouble() - 0.5) * 0.35;
			level.addParticleClient(effect, x, player.lastY + 0.05 + random.nextDouble() * 0.15, z, 0.0, 0.01, 0.0);
		}
	}

	private static void spawn(ClientWorld level, AbstractClientPlayerEntity player, SimpleParticleType type,
			Random random) {
		double angle = random.nextDouble() * Math.PI * 2.0;
		double radius = RING_MIN + random.nextDouble() * RING_SPAN;
		double x = player.getX() + Math.cos(angle) * radius;
		double y = player.getY() + random.nextDouble() * (player.getHeight() + 0.2);
		double z = player.getZ() + Math.sin(angle) * radius;
		double rise = 0.03 + random.nextDouble() * 0.03;
		level.addParticleClient(type, x, y, z, 0.0, rise, 0.0);
	}
}
