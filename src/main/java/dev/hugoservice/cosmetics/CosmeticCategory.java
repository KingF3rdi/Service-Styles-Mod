// Based on Titus Cosmetics 1.3.22, MIT License, Copyright (c) 2026 Titus. See LICENSE_TitusCosmetics.txt.
package dev.hugoservice.cosmetics;

/** Where a cosmetic is worn. The order here is the order of the sidebar. */
public enum CosmeticCategory {
	CAPE("Capes"),
	HAT("Hats"),
	BANDANA("Bandanas"),
	HALO("Halos"),
	BACK("Back"),
	FACE("Face"),
	AURA("Auras"),
	OVERLAY("Overlays"),
	PET("Pets"),
	SHIELD("Shields"),
	ACCESSORY("Accessories");

	private final String label;

	CosmeticCategory(String label) {
		this.label = label;
	}

	public String label() {
		return label;
	}
}
