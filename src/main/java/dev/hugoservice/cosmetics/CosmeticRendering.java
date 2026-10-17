// Based on Titus Cosmetics 1.3.22, MIT License, Copyright (c) 2026 Titus. See LICENSE_TitusCosmetics.txt.
package dev.hugoservice.cosmetics;

import net.fabricmc.fabric.api.client.rendering.v1.LivingEntityFeatureRendererRegistrationCallback;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.render.entity.PlayerEntityRenderer;
import net.minecraft.entity.Entity;

/**
 * Shows the cosmetics of every player. Adds {@link CosmeticLayer} to each player renderer, the default and the slim
 * one, and starts the aura particles. Call {@link #init()} once from the client entrypoint.
 */
public final class CosmeticRendering {
	private CosmeticRendering() {
	}

	/** Adds the cosmetic layer to the player renderers and starts the aura particles. */
	public static void init() {
		LivingEntityFeatureRendererRegistrationCallback.EVENT.register(
				(entityType, entityRenderer, registrationHelper, context) -> {
					if (entityRenderer instanceof PlayerEntityRenderer<?> avatarRenderer) {
						registrationHelper.register(new CosmeticLayer(avatarRenderer));
					}
				});
		CosmeticAuras.init();
	}

	/**
	 * What an entity wears. The local player always wears the loadout the player edits, so it is read with
	 * {@link CosmeticSync#local()}. Every other player is read by UUID.
	 */
	static Loadout loadoutOf(Entity entity) {
		if (entity == MinecraftClient.getInstance().player) {
			return CosmeticSync.local();
		}
		return CosmeticSync.ofPlayer(entity.getUuid());
	}
}
