// Based on Titus Cosmetics 1.3.22, MIT License, Copyright (c) 2026 Titus. See LICENSE_TitusCosmetics.txt.
package dev.hugoservice.cosmetics;

import java.awt.image.BufferedImage;

/**
 * Accessory textures: metal for the jetpack, black lacquer and a red wrap for the katana, knitted stripes for the scarf,
 * and stitched leather with a brass buckle for the satchel. Every box is painted in its own region, found by the texel
 * position of the box in the catalog.
 */
final class AccessoryArt {
	private AccessoryArt() {
	}

	static void paint(BufferedImage img, Cosmetic cosmetic) {
		switch (cosmetic.id()) {
			case "jetpack" -> jetpack(img, cosmetic);
			case "katana_sheath" -> katana(img, cosmetic);
			case "scarf" -> scarf(img, cosmetic);
			case "satchel" -> satchel(img, cosmetic);
			default -> {
				for (CosmeticBox b : cosmetic.boxes()) {
					Paint.solidCube(img, b, 0xFF8A8A8A, 0xFF4A4A4A, 0xFF6A6A6A, 0xFF7A7A7A, 0xFF5A5A5A);
				}
			}
		}
	}

	/** Dark frame with vents, metal tanks with rivets, grey nozzles and glowing orange flames. */
	private static void jetpack(BufferedImage img, Cosmetic cosmetic) {
		for (CosmeticBox b : cosmetic.boxes()) {
			if (b.u() == 0 && b.v() == 0) {
				Paint.cube(img, b, (i, j, w, h) -> 0xFF4A515B, (i, j, w, h) -> 0xFF1F2328,
						(i, j, w, h) -> 0xFF3A4048,
						(i, j, w, h) -> (j % 3 == 1 && i % 2 == 0) ? 0xFF171A1F : 0xFF2F353D,
						(i, j, w, h) -> 0xFF2A2F36);
			} else if (b.u() == 20) {
				Paint.cube(img, b, (i, j, w, h) -> 0xFFDDE2EA, (i, j, w, h) -> 0xFF3D434C,
						(i, j, w, h) -> 0xFF7D8590,
						(i, j, w, h) -> (i == 1 && (j == 2 || j == h - 3)) ? 0xFF3A4048
								: Paint.mix(0xFFC9CED6, 0xFF6B7480, j / (double) Math.max(1, h)),
						(i, j, w, h) -> (j % 4 == 0) ? 0xFF8A94A0 : 0xFF5C6470);
			} else if (b.u() == 34) {
				Paint.cube(img, b, (i, j, w, h) -> 0xFF8A94A0, (i, j, w, h) -> 0xFF2A2F36,
						(i, j, w, h) -> 0xFF23272D,
						(i, j, w, h) -> (i == 0 || j == 0 || i == w - 1 || j == h - 1) ? 0xFF8A94A0 : 0xFF2A2F36,
						(i, j, w, h) -> 0xFF23272D);
			} else if (b.u() == 44) {
				Paint.cube(img, b, (i, j, w, h) -> 0xFFFFD27A, (i, j, w, h) -> 0xFFFF5A1A,
						(i, j, w, h) -> 0xFFFFB02E,
						(i, j, w, h) -> (i == 0 || i == w - 1) ? 0xFFFF8A2E : 0xFFFFF2B0,
						(i, j, w, h) -> 0xFFFFB02E);
			}
		}
	}

	/** A black lacquer sheath with a gloss stripe, a red wrapped grip and a gold guard. */
	private static void katana(BufferedImage img, Cosmetic cosmetic) {
		for (CosmeticBox b : cosmetic.boxes()) {
			if (b.u() == 0 && b.v() == 0) {
				Paint.cube(img, b, (i, j, w, h) -> 0xFF2A2A30, (i, j, w, h) -> 0xFF0B0B0E,
						(i, j, w, h) -> 0xFF101014,
						(i, j, w, h) -> i == 1 ? 0xFF4A4A57 : 0xFF1B1B20,
						(i, j, w, h) -> 0xFF18181D);
			} else if (b.u() == 16) {
				Paint.cube(img, b, (i, j, w, h) -> 0xFFB3262E, (i, j, w, h) -> 0xFF5A1015,
						(i, j, w, h) -> 0xFF7A1A20,
						(i, j, w, h) -> ((i + j) % 2 == 0) ? 0xFFC83A3A : 0xFF8E1B22,
						(i, j, w, h) -> 0xFF8E1B22);
			} else if (b.u() == 32) {
				Paint.cube(img, b, (i, j, w, h) -> 0xFFF2D27A, (i, j, w, h) -> 0xFF6B4A12,
						(i, j, w, h) -> 0xFFB8862A,
						(i, j, w, h) -> (i == 0 || j == 0 || i == w - 1 || j == h - 1) ? 0xFF6B4A12 : 0xFFE0B040,
						(i, j, w, h) -> 0xFFB8862A);
			}
		}
	}

	/** Knitted scarf: red and cream stripes on the wrap, a zigzag on the front tail, plain darker red on the back tail. */
	private static void scarf(BufferedImage img, Cosmetic cosmetic) {
		for (CosmeticBox b : cosmetic.boxes()) {
			if (b.u() == 0 && b.v() == 0) {
				Paint.cube(img, b, (i, j, w, h) -> 0xFFE05A60, (i, j, w, h) -> 0xFF8E1B22,
						(i, j, w, h) -> 0xFFA52832,
						(i, j, w, h) -> ((i / 2) % 2 == 0) ? 0xFFC8323C : 0xFFF2E6D0,
						(i, j, w, h) -> ((i / 2) % 2 == 0) ? 0xFFC8323C : 0xFFF2E6D0);
			} else if (b.u() == 32 && b.v() == 0) {
				Paint.cube(img, b, (i, j, w, h) -> 0xFFE05A60, (i, j, w, h) -> 0xFF8E1B22,
						(i, j, w, h) -> 0xFFA52832,
						(i, j, w, h) -> ((i + j / 2) % 4 < 2) ? 0xFFC8323C : 0xFFF2E6D0,
						(i, j, w, h) -> 0xFF8E1B22);
			} else if (b.u() == 32 && b.v() == 12) {
				Paint.cube(img, b, (i, j, w, h) -> 0xFF8E1B22, (i, j, w, h) -> 0xFF5A1015,
						(i, j, w, h) -> 0xFF6B1219,
						(i, j, w, h) -> (j % 3 == 0) ? 0xFF6B1219 : 0xFF8E1B22,
						(i, j, w, h) -> 0xFF6B1219);
			}
		}
	}

	/** Brown leather with stitching, a flap, a strap and a brass buckle. */
	private static void satchel(BufferedImage img, Cosmetic cosmetic) {
		for (CosmeticBox b : cosmetic.boxes()) {
			if (b.u() == 0 && b.v() == 0) {
				Paint.cube(img, b, (i, j, w, h) -> 0xFF5C3A20, (i, j, w, h) -> 0xFF3A2414,
						(i, j, w, h) -> 0xFF5A381F,
						(i, j, w, h) -> stitched(i, j, w, h, 0xFF7A4B2A),
						(i, j, w, h) -> 0xFF6B4226);
			} else if (b.u() == 16) {
				Paint.cube(img, b, (i, j, w, h) -> 0xFFA46E40, (i, j, w, h) -> 0xFF4A2E18,
						(i, j, w, h) -> 0xFF6B4226,
						(i, j, w, h) -> stitched(i, j, w, h, 0xFF8A5A34),
						(i, j, w, h) -> 0xFF6B4226);
			} else if (b.u() == 32) {
				Paint.solidCube(img, b, 0xFF5C3A20, 0xFF3A2414, 0xFF3A2414, 0xFF4A2E18, 0xFF3A2414);
			} else if (b.u() == 40) {
				Paint.cube(img, b, (i, j, w, h) -> 0xFFF2D27A, (i, j, w, h) -> 0xFF8A6A20,
						(i, j, w, h) -> 0xFFA4822E,
						(i, j, w, h) -> (i == 0 && j == 0) ? 0xFFFFF0B0 : 0xFFE8C15A,
						(i, j, w, h) -> 0xFFA4822E);
			}
		}
	}

	/** A leather face with a darker border and a row of stitches two texels apart. */
	private static int stitched(int i, int j, int w, int h, int leather) {
		if (i == 0 || j == 0 || i == w - 1 || j == h - 1) {
			return Paint.shade(leather, 0.6);
		}
		if ((j == 1 || j == h - 2) && i % 2 == 0) {
			return 0xFFC9A36A;
		}
		return leather;
	}
}
