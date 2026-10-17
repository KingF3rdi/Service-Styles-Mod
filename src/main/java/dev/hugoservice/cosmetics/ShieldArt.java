// Based on Titus Cosmetics 1.3.22, MIT License, Copyright (c) 2026 Titus. See LICENSE_TitusCosmetics.txt.
package dev.hugoservice.cosmetics;

import java.awt.image.BufferedImage;

/**
 * Shield textures: translucent glass cells. Each cell has a bright rim, a glowing centre that fades towards the edges,
 * and a diagonal shine. The alpha is part of the design: the cells are drawn with translucent blending and back-face
 * culling, so the body shows through them.
 */
final class ShieldArt {
	private ShieldArt() {
	}

	static void paint(BufferedImage img, Cosmetic cosmetic) {
		for (CosmeticBox b : cosmetic.boxes()) {
			switch (cosmetic.id()) {
				case "aegis_ring" -> glass(img, b, argb(235, 255, 210, 122), argb(95, 77, 225, 255),
						argb(160, 150, 240, 255), argb(170, 255, 255, 255));
				case "frost_barrier" -> crystal(img, b);
				case "ember_shield" -> {
					if (b.u() == 16) {
						glass(img, b, argb(255, 255, 120, 40), argb(255, 255, 196, 90), argb(255, 255, 240, 170),
								argb(255, 255, 240, 170));
					} else {
						glass(img, b, argb(230, 255, 61, 26), argb(120, 255, 122, 46), argb(200, 255, 210, 122),
								argb(150, 255, 170, 80));
					}
				}
				default -> glass(img, b, argb(200, 255, 255, 255), argb(90, 200, 230, 255),
						argb(150, 255, 255, 255), argb(160, 255, 255, 255));
			}
		}
	}

	/** One glass cell: rim on the edge texels, a glowing centre, a diagonal shine. */
	private static void glass(BufferedImage img, CosmeticBox b, int rim, int base, int core, int shine) {
		int bevel = Paint.mix(base, rim, 0.3);
		Paint.Face face = (i, j, w, h) -> {
			if (i == 0 || j == 0) {
				return rim;
			}
			if (i == w - 1 || j == h - 1) {
				return bevel;
			}
			if ((i + j) % 4 == 0) {
				return shine;
			}
			double cx = (w - 1) / 2.0;
			double cy = (h - 1) / 2.0;
			double r = Math.hypot(i - cx, j - cy) / Math.max(1.0, Math.max(cx, cy));
			return Paint.mix(core, base, r);
		};
		Paint.cube(img, b, face, face, face, face, face);
	}

	/** Frost cell: a white rim, pale blue glass, and a crossed facet pattern on the faces. */
	private static void crystal(BufferedImage img, CosmeticBox b) {
		int rim = argb(235, 225, 248, 255);
		int base = argb(130, 90, 185, 240);
		int facet = argb(200, 210, 245, 255);
		int shadow = argb(120, 60, 150, 220);
		Paint.Face face = (i, j, w, h) -> {
			if (i == 0 || j == 0) {
				return rim;
			}
			if (i == j || i + j == w - 1) {
				return facet;
			}
			return (i == w - 1 || j == h - 1) ? shadow : base;
		};
		Paint.cube(img, b, face, face, face, face, face);
	}

	private static int argb(int a, int r, int g, int b) {
		return Paint.argb(a, r, g, b);
	}
}
