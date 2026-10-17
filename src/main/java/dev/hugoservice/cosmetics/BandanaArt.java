// Based on Titus Cosmetics 1.3.22, MIT License, Copyright (c) 2026 Titus. See LICENSE_TitusCosmetics.txt.
package dev.hugoservice.cosmetics;

import java.awt.image.BufferedImage;

/**
 * Bandana textures. Every bandana has the same strips (see {@link CosmeticCatalog}), and each strip is painted over its whole region with
 * the pattern of its bandana: lightning, gold, plasma, flames, a collage, or red with gold diamonds. The patterns are drawn from the texel
 * position, so every client paints them alike.
 */
final class BandanaArt {
	/** The colour of one texel of a pattern, from its position in the texture. */
	private interface Look {
		int at(int x, int y);
	}

	/** The pastel patches of the collage. */
	private static final int[] PASTELS = {0xFFFFB3C6, 0xFFB3E5FF, 0xFFFFF0A6, 0xFFC8F7C5, 0xFFD9B8FF, 0xFFFFD6A5};

	private BandanaArt() {
	}

	static void paint(BufferedImage img, Cosmetic cosmetic) {
		Look look = lookOf(cosmetic.id());
		for (CosmeticBox b : cosmetic.boxes()) {
			// The region of a box in the cube layout: (2w + 2d) by (h + d) texels from its offset.
			int width = 2 * Math.round(b.w()) + 2 * Math.round(b.d());
			int height = Math.round(b.h()) + Math.round(b.d());
			for (int y = b.v(); y < b.v() + height; y++) {
				for (int x = b.u(); x < b.u() + width; x++) {
					Paint.put(img, x, y, look.at(x, y));
				}
			}
		}
	}

	private static Look lookOf(String id) {
		return switch (id) {
			case "heavenly_bandana" -> BandanaArt::gold;
			case "summer_love_bandana" -> BandanaArt::collage;
			case "plasma_bandana" -> BandanaArt::plasma;
			case "inferno_bandana" -> BandanaArt::inferno;
			case "crimson_bandana" -> BandanaArt::crimson;
			case "pink_lightning_bandana" -> lightning(0xFFE85CB8, 0xFF8A1F5C, 0xFFFFFFFF, 1);
			case "black_lightning_bandana" -> lightning(0xFF2A2A30, 0xFF0A0A0E, 0xFFEFEFF5, 2);
			case "white_lightning_bandana" -> lightning(0xFFF0F0F5, 0xFFB8BCC8, 0xFF5AB8FF, 3);
			case "red_lightning_bandana" -> lightning(0xFFD62828, 0xFF6B0F0F, 0xFFFFE066, 4);
			case "green_lightning_bandana" -> lightning(0xFF2EAF4A, 0xFF0E5A22, 0xFFFFF176, 5);
			case "purple_lightning_bandana" -> lightning(0xFF7E3FD0, 0xFF3A1A6B, 0xFFFF7AE0, 6);
			case "turquoise_lightning_bandana" -> lightning(0xFF2FC9C2, 0xFF0F5F5C, 0xFFFFFFFF, 7);
			default -> (x, y) -> 0xFF808080;
		};
	}

	/** Lightning: cloth with dark patches, and thin bright bolts that zigzag across it. */
	private static Look lightning(int cloth, int dark, int bolt, int seed) {
		return (x, y) -> {
			int base = Paint.mix(dark, cloth, Paint.fbm(x, y, seed));
			// Each bolt is a diagonal line whose position wobbles with noise. Its core is bright, and its edge is only lit.
			double s = (x + 1.7 * y + 6 * Paint.fbm(x, y, seed + 9)) / 11.0;
			double f = s - Math.floor(s);
			double line = Math.min(f, 1 - f);
			if (line < 0.035) {
				return bolt;
			}
			return line < 0.09 ? Paint.mix(base, bolt, 0.55) : base;
		};
	}

	/** Gold leaf: warm gold with soft light, and a sheen that runs across the cloth. */
	private static int gold(int x, int y) {
		int base = Paint.mix(0xFF9C6B12, 0xFFFFE27A, Paint.fbm(x, y, 31));
		double s = (x + y) / 6.0 + 2 * Paint.fbm(x, y, 32);
		double f = s - Math.floor(s);
		return f < 0.12 ? Paint.mix(base, 0xFFFFFFFF, 0.7) : base;
	}

	/** Plasma: blue arcs that glow from a bright core, with white sparks. */
	private static int plasma(int x, int y) {
		if (Paint.hash(x, y, 42) < 0.06) {
			return 0xFFFFFFFF;
		}
		return Paint.ramp(new int[] {0xFF0A2A7A, 0xFF1E7BFF, 0xFF7FE9FF}, Paint.fbm(x, y, 41));
	}

	/** Flames: dark red at the edges, orange and yellow where the noise runs high, stretched upwards. */
	private static int inferno(int x, int y) {
		return Paint.ramp(new int[] {0xFF2A0800, 0xFFB71C00, 0xFFFF7A00, 0xFFFFE066}, Paint.fbm(x * 0.8, y * 2.0, 51));
	}

	/** Red cloth with a lattice of gold diamonds, and a dark dot in the middle of each diamond. */
	private static int crimson(int x, int y) {
		if (Math.floorMod(x + y, 8) == 0 || Math.floorMod(x - y, 8) == 0) {
			return 0xFFFFC65C;
		}
		return Math.floorMod(x, 8) == 4 && Math.floorMod(y, 8) == 4 ? 0xFF5A0508 : 0xFF9E101A;
	}

	/** A collage: pastel patches, with thin white strips across them. */
	private static int collage(int x, int y) {
		int patch = (int) (Paint.hash(Math.floorDiv(x, 4), Math.floorDiv(y, 3), 61) * PASTELS.length);
		return Math.floorMod(x + 2 * y, 7) == 0 ? 0xFFFFFFFF : PASTELS[patch];
	}
}
