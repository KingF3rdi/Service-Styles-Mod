// Based on Titus Cosmetics 1.3.22, MIT License, Copyright (c) 2026 Titus. See LICENSE_TitusCosmetics.txt.
package dev.hugoservice.cosmetics;

import java.util.List;
import java.util.regex.Pattern;

/**
 * A cosmetic that can be worn. Everything the renderer needs is data, so a new cosmetic is an entry in
 * {@link CosmeticCatalog}, not new rendering code.
 *
 * @param id          stable key of 1 to 40 lower case letters, digits or underscores. Players send it to each other.
 * @param name        name shown in the GUI
 * @param category    where it is worn
 * @param rarity      label and accent colour of its card
 * @param description one short line for the details panel
 * @param anchor      part of the player model it is attached to
 * @param motion      how it moves while it is worn
 * @param boxes       geometry, textured with the cosmetic texture; empty for auras
 * @param particle    particle type name for auras, e.g. {@code flame}; {@code null} for everything else
 * @param texture     vanilla texture the boxes are drawn with, e.g. {@code minecraft:textures/entity/parrot/parrot_blue.png},
 *                    in place of the generated texture of {@link CosmeticTextures}; {@code null} for the generated one
 */
public record Cosmetic(String id, String name, CosmeticCategory category, Rarity rarity, String description,
		Anchor anchor, Motion motion, List<CosmeticBox> boxes, String particle, String texture) {

	private static final Pattern ID = Pattern.compile("[a-z0-9_]{1,40}");
	private static final Pattern TEXTURE = Pattern.compile("[a-z0-9_.:/-]{1,120}");

	/** A cosmetic drawn with its generated texture. */
	public Cosmetic(String id, String name, CosmeticCategory category, Rarity rarity, String description,
			Anchor anchor, Motion motion, List<CosmeticBox> boxes, String particle) {
		this(id, name, category, rarity, description, anchor, motion, boxes, particle, null);
	}

	public Cosmetic {
		if (id == null || !ID.matcher(id).matches()) {
			throw new IllegalArgumentException("bad cosmetic id: " + id);
		}
		if (texture != null && !TEXTURE.matcher(texture).matches()) {
			throw new IllegalArgumentException("bad texture of " + id + ": " + texture);
		}
		boxes = List.copyOf(boxes);
	}
}
