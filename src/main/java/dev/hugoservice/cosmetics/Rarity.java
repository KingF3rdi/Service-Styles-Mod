// Based on Titus Cosmetics 1.3.22, MIT License, Copyright (c) 2026 Titus. See LICENSE_TitusCosmetics.txt.
package dev.hugoservice.cosmetics;

/** How rare a cosmetic is. It only sets the label and the accent colour of its card. */
public enum Rarity {
	COMMON("Common", 0xFF9AA5B1),
	RARE("Rare", 0xFF4FC3FF),
	EPIC("Epic", 0xFFB56BFF),
	LEGENDARY("Legendary", 0xFFFFC65C);

	private final String label;
	private final int color;

	Rarity(String label, int color) {
		this.label = label;
		this.color = color;
	}

	public String label() {
		return label;
	}

	/** ARGB accent colour. */
	public int color() {
		return color;
	}
}
