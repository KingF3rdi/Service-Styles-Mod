// Based on Titus Cosmetics 1.3.22, MIT License, Copyright (c) 2026 Titus. See LICENSE_TitusCosmetics.txt.
package dev.hugoservice.cosmetics;

import java.awt.image.BufferedImage;
import java.util.List;

/**
 * Pet textures. Each box is painted in its own region of the 64x64 texture, found by the texel position of the box in
 * the catalog. Faces get a light top, a mid side, a dark bottom and an outline, and the features (eyes, beaks, scales,
 * feathers) are painted on the front faces.
 *
 * <p>The parrots are drawn in the game with the vanilla parrot textures, see {@link Cosmetic#texture()}. Their texture
 * here is only for the GUI icon, which is built without the game's files. So each part of a parrot is painted in a flat
 * colour of its variant, and the parts keep the regions of the vanilla parrot texture.
 */
final class PetArt {
	private PetArt() {
	}

	static void paint(BufferedImage img, Cosmetic cosmetic) {
		List<CosmeticBox> boxes = cosmetic.boxes();
		if (cosmetic.texture() != null) {
			parrot(img, boxes, paletteOf(cosmetic.id()));
			return;
		}
		switch (cosmetic.id()) {
			case "glow_cube" -> glowCube(img, boxes);
			case "pocket_ghost" -> ghost(img, boxes);
			case "lil_owl" -> owl(img, boxes);
			case "baby_ghast" -> babyGhast(img, boxes);
			case "ghast" -> ghast(img, boxes);
			case "baby_ender_dragon" -> babyDragon(img, boxes);
			case "baby_blaze" -> babyBlaze(img, boxes);
			default -> plain(img, boxes, 0xFF808080);
		}
	}

	/** Flat colours of one parrot variant: body, wings and tail, head and crest, beak, and legs. */
	private record Palette(int body, int wing, int head, int beak, int feet) {
	}

	private static final int BEAK = 0xFF222222;
	private static final int FEET = 0xFF9C7F6E;

	/** The flat colours of a parrot, taken from the vanilla parrot texture of each variant. */
	private static Palette paletteOf(String id) {
		return switch (id) {
			case "parrot_red_blue" -> new Palette(0xFFE01E1E, 0xFF1E56E0, 0xFFE01E1E, BEAK, FEET);
			case "parrot_blue" -> new Palette(0xFF1E3FD6, 0xFF0F2AA8, 0xFF1E3FD6, BEAK, FEET);
			case "parrot_green" -> new Palette(0xFF8CC20A, 0xFF6FA300, 0xFF8CC20A, BEAK, FEET);
			case "parrot_yellow_blue" -> new Palette(0xFFFFD100, 0xFF1FAEDF, 0xFF18BDFF, BEAK, FEET);
			default -> new Palette(0xFF999999, 0xFF7E7E7E, 0xFFFFE533, BEAK, FEET);
		};
	}

	/**
	 * A parrot in flat colours. The parts are recognised by their texture offsets in the vanilla parrot layout: the body at
	 * 2,8, the wings and the tail at 19,8 and 22,1, the head at 2,2 with its top plate at 10,0 and the crest at 2,18, the
	 * beaks at 11,7 and 16,7, and the legs at 14,18.
	 */
	private static void parrot(BufferedImage img, List<CosmeticBox> boxes, Palette p) {
		for (CosmeticBox b : boxes) {
			int colour;
			if (b.u() == 2 && b.v() == 8) {
				colour = p.body();
			} else if (b.u() == 19 || b.u() == 22) {
				colour = p.wing();
			} else if (b.u() == 2 || b.u() == 10) {
				colour = p.head();
			} else if (b.u() == 11 || b.u() == 16) {
				colour = p.beak();
			} else {
				colour = p.feet();
			}
			Paint.solidCube(img, b, colour, colour, colour, colour, colour);
		}
	}

	/** Each distinct region once: a plain cube per box with a simple shading. */
	private static void plain(BufferedImage img, List<CosmeticBox> boxes, int colour) {
		for (CosmeticBox b : boxes) {
			Paint.solidCube(img, b, Paint.shade(colour, 1.2), Paint.shade(colour, 0.5), Paint.shade(colour, 0.8),
					colour, Paint.shade(colour, 0.7));
		}
	}

	/** A baby ghast: white with grey shading, black eyes and a small mouth on the front, grey tentacles. */
	private static void babyGhast(BufferedImage img, List<CosmeticBox> boxes) {
		for (CosmeticBox b : boxes) {
			if (b.v() == 24) {
				Paint.solidCube(img, b, 0xFFEDEDF5, 0xFFB9B9C8, 0xFFD6D6E6, 0xFFEDEDF5, 0xFFD6D6E6);
			} else {
				Paint.cube(img, b, (i, j, w, h) -> 0xFFF4F4FA, (i, j, w, h) -> 0xFFB9B9C8,
						(i, j, w, h) -> 0xFFDADAE8,
						(i, j, w, h) -> ghastFace(i, j), (i, j, w, h) -> 0xFFDADAE8);
			}
		}
	}

	/** The front of a baby ghast: black eyes on rows 2..3, a dark mouth on row 5 of the 8x8 face. */
	private static int ghastFace(int i, int j) {
		if ((i == 1 || i == 2 || i == 5 || i == 6) && (j == 2 || j == 3)) {
			return 0xFF141418;
		}
		if (j == 5 && (i == 3 || i == 4)) {
			return 0xFF2A2A34;
		}
		return 0xFFF4F4FA;
	}

	/**
	 * A ghast: a white cube with grey streaks on top, grey dots on the sides, and a face on the front with two dark eyes and a dark
	 * mouth. The tentacles are light grey.
	 */
	private static void ghast(BufferedImage img, List<CosmeticBox> boxes) {
		for (CosmeticBox b : boxes) {
			if (b.v() == 24) {
				Paint.solidCube(img, b, 0xFFF2F2F8, 0xFFB0B0BF, 0xFFD3D3DF, 0xFFE4E4EE, 0xFFD3D3DF);
			} else {
				Paint.cube(img, b, (i, j, w, h) -> ghastTop(i, j), (i, j, w, h) -> 0xFFB9B9C8,
						(i, j, w, h) -> ghastSide(i, j), (i, j, w, h) -> ghastFront(i, j, w, h), (i, j, w, h) -> 0xFFD6D6E2);
			}
		}
	}

	/** Streaks of grey and pink across the white top. */
	private static int ghastTop(int i, int j) {
		return (i - j) % 4 == 0 && j % 3 != 0 ? 0xFFBDAFB8 : 0xFFF3F3F8;
	}

	/** Grey dots on the sides. */
	private static int ghastSide(int i, int j) {
		return i % 3 == 1 && j % 3 == 1 ? 0xFF8F8FA0 : 0xFFC4C4D2;
	}

	/** The face on the 8 by 8 front: two dark eyes on rows 2 and 3, and a dark mouth on rows 5 and 6. */
	private static int ghastFront(int i, int j, int w, int h) {
		if ((i == 1 || i == 2 || i == 5 || i == 6) && (j == 2 || j == 3)) {
			return 0xFF2E2E3A;
		}
		if ((j == 5 && i >= 2 && i <= 5) || (j == 6 && (i == 3 || i == 4))) {
			return 0xFF2E2E3A;
		}
		return j == h - 1 ? 0xFFD0D0DC : 0xFFF0F0F6;
	}

	/** A baby ender dragon: purple scales, dark wing membranes, and pink eyes on the front of the head. */
	private static void babyDragon(BufferedImage img, List<CosmeticBox> boxes) {
		for (CosmeticBox b : boxes) {
			if (b.v() == 16) {
				Paint.cube(img, b, (i, j, w, h) -> 0xFF4A1F6E, (i, j, w, h) -> 0xFF1E0A2E,
						(i, j, w, h) -> 0xFF3B1A5A,
						(i, j, w, h) -> dragonEye(i, j) ? 0xFFFF5CE6 : 0xFF3B1A5A, (i, j, w, h) -> 0xFF2E1245);
			} else if (b.u() == 24 || b.u() == 40) {
				Paint.solidCube(img, b, 0xFF5B2A8F, 0xFF2E1245, 0xFF4A1F6E, 0xFF5B2A8F, 0xFF4A1F6E);
			} else {
				Paint.solidCube(img, b, 0xFF3B1A5A, 0xFF1E0A2E, 0xFF2E1245, 0xFF3B1A5A, 0xFF2E1245);
			}
		}
	}

	/** Two pink eyes on row 1 of the 4x4 front of the dragon's head, at its two ends. */
	private static boolean dragonEye(int i, int j) {
		return (i == 0 || i == 3) && j == 1;
	}

	/** A baby blaze: yellow-orange head with dark eyes and a dark mouth, and bright rods. */
	private static void babyBlaze(BufferedImage img, List<CosmeticBox> boxes) {
		for (CosmeticBox b : boxes) {
			if (b.u() == 24) {
				Paint.solidCube(img, b, 0xFFFFE27A, 0xFFE0A300, 0xFFFFD84A, 0xFFFFD84A, 0xFFE8B422);
			} else {
				Paint.cube(img, b, (i, j, w, h) -> 0xFFFFC233, (i, j, w, h) -> 0xFFB86E00,
						(i, j, w, h) -> 0xFFE8A11E,
						(i, j, w, h) -> blazeFace(i, j), (i, j, w, h) -> 0xFFE8A11E);
			}
		}
	}

	/** The front of a baby blaze: dark eyes on row 2 and a dark mouth on row 4 of the 6x6 face. */
	private static int blazeFace(int i, int j) {
		if ((i == 1 || i == 4) && j == 2) {
			return 0xFF3A1C00;
		}
		if (j == 4 && (i == 2 || i == 3)) {
			return 0xFF3A1C00;
		}
		return 0xFFFFC233;
	}

	/** Floating cube: glowing cyan edges fading to magenta, and a white core with a violet glow. */
	private static void glowCube(BufferedImage img, List<CosmeticBox> boxes) {
		for (CosmeticBox b : boxes) {
			if (b.u() == 0 && b.v() == 0) {
				Paint.solidCube(img, b, 0xFF5CF6FF, 0xFF2A7DB0, 0xFF46E3FF, 0xFF7CF8FF, 0xFF3AB8E8);
				Paint.cube(img, b, (i, j, w, h) -> Paint.mix(0xFF5CF6FF, 0xFFFF5CD6, i / (double) Math.max(1, w)),
						(i, j, w, h) -> 0xFF2A7DB0,
						(i, j, w, h) -> Paint.mix(0xFF46E3FF, 0xFFFF5CD6, j / (double) Math.max(1, h)),
						(i, j, w, h) -> Paint.mix(0xFF7CF8FF, 0xFFFF5CD6, i / (double) Math.max(1, w)),
						(i, j, w, h) -> 0xFF3AB8E8);
			} else if (b.u() == 16) {
				Paint.cube(img, b, (i, j, w, h) -> 0xFFFFFFFF, (i, j, w, h) -> 0xFF7D4FD8,
						(i, j, w, h) -> Paint.mix(0xFFFFFFFF, 0xFF9B6BFF, 0.5),
						(i, j, w, h) -> Paint.mix(0xFFFFFFFF, 0xFF9B6BFF, (i + j) / (double) (w + h)),
						(i, j, w, h) -> 0xFF9B6BFF);
			}
		}
	}

	/** Pale ghost: lavender sheet with a shaded hem and black eyes painted on the front of the body. */
	private static void ghost(BufferedImage img, List<CosmeticBox> boxes) {
		for (CosmeticBox b : boxes) {
			if (b.u() == 0 && b.v() == 0) {
				Paint.cube(img, b, (i, j, w, h) -> 0xFFF6F3FF, (i, j, w, h) -> 0xFFA59CD8,
						(i, j, w, h) -> 0xFFC9C2F0,
						(i, j, w, h) -> eye(i, j) ? 0xFF141418 : (i == 3 && j == 5 ? 0xFF3A2E5A : 0xFFE8E4FF),
						(i, j, w, h) -> 0xFFD8D2F4);
			} else if (b.u() == 32) {
				Paint.solidCube(img, b, 0xFFF6F3FF, 0xFFA59CD8, 0xFFC9C2F0, 0xFFE8E4FF, 0xFFD8D2F4);
			} else if (b.u() == 0 && b.v() == 24) {
				Paint.cube(img, b, (i, j, w, h) -> 0xFFF6F3FF, (i, j, w, h) -> 0xFFA59CD8,
						(i, j, w, h) -> 0xFFC9C2F0,
						(i, j, w, h) -> j == h - 1 ? 0xFFA59CD8 : 0xFFE8E4FF,
						(i, j, w, h) -> 0xFFD8D2F4);
			}
		}
	}

	/** The two eyes of the ghost on the 8x8 front of its body: 1..2 and 5..6, rows 2..3. */
	private static boolean eye(int i, int j) {
		return (i == 1 || i == 2 || i == 5 || i == 6) && (j == 2 || j == 3);
	}

	/** Round owl: brown feathers, a cream belly, a cream face disc, gold eyes and a yellow beak. */
	private static void owl(BufferedImage img, List<CosmeticBox> boxes) {
		for (CosmeticBox b : boxes) {
			if (b.u() == 0 && b.v() == 0) {
				Paint.cube(img, b, (i, j, w, h) -> 0xFF5A3D22, (i, j, w, h) -> 0xFF3A2614,
						(i, j, w, h) -> (i + j) % 3 == 0 ? 0xFF7A5230 : 0xFF6B4A2B,
						(i, j, w, h) -> (i == 0 || i == w - 1) ? 0xFF6B4A2B : ((j + i / 2) % 4 == 0 ? 0xFFC9AE7A
								: 0xFFE8D5A8),
						(i, j, w, h) -> 0xFF4A321E);
			} else if (b.u() == 24 && b.v() == 0) {
				Paint.cube(img, b, (i, j, w, h) -> 0xFF7A5230, (i, j, w, h) -> 0xFF3A2614,
						(i, j, w, h) -> (i + j) % 4 == 0 ? 0xFF8A5E36 : 0xFF6B4A2B,
						(i, j, w, h) -> discFace(i, j, w, h), (i, j, w, h) -> 0xFF4A321E);
			} else if (b.u() == 0 && b.v() == 12) {
				Paint.solidCube(img, b, 0xFFFFD040, 0xFFC98A10, 0xFFE8B02A, 0xFFFFD040, 0xFFC98A10);
				Paint.cube(img, b, (i, j, w, h) -> 0xFFFFD040, (i, j, w, h) -> 0xFFC98A10,
						(i, j, w, h) -> 0xFFE8B02A,
						(i, j, w, h) -> eyeColour(i, j, w, h), (i, j, w, h) -> 0xFFC98A10);
			} else if (b.u() == 8) {
				Paint.solidCube(img, b, 0xFFFFC248, 0xFFB06A10, 0xFFFFA726, 0xFFFFB02E, 0xFFB06A10);
			} else if (b.u() == 16 && b.v() == 12) {
				Paint.solidCube(img, b, 0xFF6B4A2B, 0xFF3A2614, 0xFF5A3D22, 0xFF6B4A2B, 0xFF3A2614);
			} else if (b.u() == 0 && b.v() == 16) {
				Paint.cube(img, b, (i, j, w, h) -> 0xFF4A321E, (i, j, w, h) -> 0xFF2A1A0E,
						(i, j, w, h) -> (j % 3 == 0) ? 0xFF8A5E36 : 0xFF5A3D22,
						(i, j, w, h) -> (j % 3 == 0) ? 0xFF8A5E36 : 0xFF5A3D22,
						(i, j, w, h) -> 0xFF3A2614);
			} else if (b.u() == 16 && b.v() == 20) {
				Paint.solidCube(img, b, 0xFFF0A040, 0xFF8A4E12, 0xFFE08A1E, 0xFFE08A1E, 0xFF8A4E12);
			} else if (b.u() == 24 && b.v() == 12) {
				Paint.solidCube(img, b, 0xFF6B4A2B, 0xFF3A2614, 0xFF5A3D22, 0xFF6B4A2B, 0xFF3A2614);
			}
		}
	}

	/** Cream disc on the owl's face, with a brown rim. */
	private static int discFace(int i, int j, int w, int h) {
		double cx = (w - 1) / 2.0;
		double cy = (h - 1) / 2.0;
		double dx = (i - cx) / Math.max(1.0, w / 2.0);
		double dy = (j - cy) / Math.max(1.0, h / 2.0);
		double r = dx * dx + dy * dy;
		return r < 0.55 ? 0xFFE8D5A8 : (r < 0.9 ? 0xFFC9AE7A : 0xFF7A5230);
	}

	/** Gold eye with a dark pupil in the middle and a darker rim. */
	private static int eyeColour(int i, int j, int w, int h) {
		double cx = (w - 1) / 2.0;
		double cy = (h - 1) / 2.0;
		double r = Math.hypot(i - cx, j - cy);
		if (r < 0.8) {
			return 0xFF141414;
		}
		return r < 1.7 ? 0xFFFFD040 : 0xFFC98A10;
	}
}
