// Based on Titus Cosmetics 1.3.22, MIT License, Copyright (c) 2026 Titus. See LICENSE_TitusCosmetics.txt.
package dev.hugoservice.cosmetics;

import java.awt.image.BufferedImage;
import java.util.List;
import java.util.Random;

/**
 * Square icons of cosmetics for the GUI cards. A cosmetic with boxes is rendered in 3D from a three-quarter view with
 * its texture, see {@link BoxRasterizer}. An aura has no boxes, so its icon is its orb texture, scaled up smoothly,
 * with a few sparkles. The icon depends only on the cosmetic and the size, so it is the same on every client.
 */
public final class CosmeticIcons {
	/**
	 * The body of the player at rest, in the skin layout: head, torso, both arms and both legs. An overlay icon is this
	 * body with the overlay texture on it, so the icon of an overlay looks like a player.
	 */
	private static final List<CosmeticBox> PLAYER_BODY = List.of(
			new CosmeticBox(-4, -8, -4, 8, 8, 8, 0, 0),
			new CosmeticBox(-4, 0, -2, 8, 12, 4, 16, 16),
			new CosmeticBox(-8, 0, -2, 4, 12, 4, 40, 16),
			new CosmeticBox(4, 0, -2, 4, 12, 4, 32, 48),
			new CosmeticBox(-4, 12, -2, 4, 12, 4, 0, 16),
			new CosmeticBox(0, 12, -2, 4, 12, 4, 16, 48));
	/** The neutral grey of the body under an overlay. It shows where the overlay is transparent. */
	private static final int MANNEQUIN = 0xFF9399AC;
	/** Brightness of a glint in the icon. Full, so the light of a glint shows at the size of a card. */
	private static final double ICON_LIGHT = 1.0;

	private CosmeticIcons() {
	}

	/** Icon of a cosmetic as ARGB, {@code size} by {@code size} pixels, transparent around the object. */
	public static BufferedImage of(Cosmetic cosmetic, int size) {
		if (size < 1) {
			throw new IllegalArgumentException("icon size must be positive: " + size);
		}
		if (cosmetic.anchor() == Anchor.WHOLE) {
			return BoxRasterizer.render(PLAYER_BODY,
					overMannequin(CosmeticTextures.of(cosmetic), OverlayArt.isCoat(cosmetic.id())), size);
		}
		if (cosmetic.boxes().isEmpty()) {
			return orb(cosmetic, size);
		}
		return BoxRasterizer.render(cosmetic.boxes(), CosmeticTextures.of(cosmetic), size);
	}

	/**
	 * The overlay drawn over the grey body, the way the game draws it. A glint adds its light to the grey, and texels
	 * below the cut-out add nothing. A coat is laid over the grey with its alpha. Either way the grey shows where the
	 * overlay is absent, so the icon stays a light, readable player.
	 */
	private static BufferedImage overMannequin(BufferedImage overlay, boolean coat) {
		int size = overlay.getWidth();
		BufferedImage image = new BufferedImage(size, size, BufferedImage.TYPE_INT_ARGB);
		for (int y = 0; y < size; y++) {
			for (int x = 0; x < size; x++) {
				int top = overlay.getRGB(x, y);
				int a = top >>> 24;
				int r;
				int g;
				int b;
				if (coat) {
					r = mixChannel((top >> 16) & 0xFF, (MANNEQUIN >> 16) & 0xFF, a);
					g = mixChannel((top >> 8) & 0xFF, (MANNEQUIN >> 8) & 0xFF, a);
					b = mixChannel(top & 0xFF, MANNEQUIN & 0xFF, a);
				} else {
					int lit = a < 26 ? 0 : 1; // The game cuts out alpha below 0.1 and adds the rest.
					r = addLight((MANNEQUIN >> 16) & 0xFF, (top >> 16) & 0xFF, lit);
					g = addLight((MANNEQUIN >> 8) & 0xFF, (top >> 8) & 0xFF, lit);
					b = addLight(MANNEQUIN & 0xFF, top & 0xFF, lit);
				}
				image.setRGB(x, y, 0xFF000000 | (r << 16) | (g << 8) | b);
			}
		}
		return image;
	}

	/** One channel of the overlay over the mannequin, with the overlay's alpha. */
	private static int mixChannel(int over, int under, int alpha) {
		return (over * alpha + under * (255 - alpha)) / 255;
	}

	/** One channel of the grey plus the light of a glint, at the icon brightness. */
	private static int addLight(int grey, int light, int lit) {
		return Math.min(255, grey + (int) Math.round(light * ICON_LIGHT * lit));
	}

	/** The orb texture in the middle 80 percent of the icon, with four sparkles placed by the id. */
	private static BufferedImage orb(Cosmetic cosmetic, int size) {
		BufferedImage texture = CosmeticTextures.of(cosmetic);
		BufferedImage image = new BufferedImage(size, size, BufferedImage.TYPE_INT_ARGB);
		double inset = size * 0.1;
		double span = size * 0.8;
		for (int y = 0; y < size; y++) {
			for (int x = 0; x < size; x++) {
				double u = (x + 0.5 - inset) / span * texture.getWidth() - 0.5;
				double v = (y + 0.5 - inset) / span * texture.getHeight() - 0.5;
				if (u < 0 || v < 0 || u > texture.getWidth() - 1 || v > texture.getHeight() - 1) {
					continue; // Outside the texture: transparent, the texture edge must not be stretched.
				}
				image.setRGB(x, y, sample(texture, u, v));
			}
		}
		double centre = size / 2.0;
		double radius = size * 0.4;
		Random random = new Random(cosmetic.id().hashCode());
		int arm = Math.max(2, size / 14);
		for (int i = 0; i < 4; i++) {
			double angle = (i + 0.2 + 0.6 * random.nextDouble()) * Math.PI / 2;
			double distance = radius * (0.7 + 0.2 * random.nextDouble());
			sparkle(image, (int) Math.round(centre + StrictMath.cos(angle) * distance),
					(int) Math.round(centre + StrictMath.sin(angle) * distance), arm);
		}
		return image;
	}

	/**
	 * Bilinear sample at texel coordinates. The four texels are weighted by their alpha, so a fading edge does not turn
	 * dark.
	 */
	private static int sample(BufferedImage image, double u, double v) {
		int w = image.getWidth();
		int h = image.getHeight();
		int x0 = Math.max(0, Math.min(w - 1, (int) Math.floor(u)));
		int y0 = Math.max(0, Math.min(h - 1, (int) Math.floor(v)));
		int x1 = Math.min(w - 1, x0 + 1);
		int y1 = Math.min(h - 1, y0 + 1);
		double fx = Math.max(0, Math.min(1, u - Math.floor(u)));
		double fy = Math.max(0, Math.min(1, v - Math.floor(v)));
		double alpha = 0;
		double red = 0;
		double green = 0;
		double blue = 0;
		for (int i = 0; i < 4; i++) {
			int x = (i & 1) == 0 ? x0 : x1;
			int y = i < 2 ? y0 : y1;
			double weight = ((i & 1) == 0 ? 1 - fx : fx) * (i < 2 ? 1 - fy : fy);
			int c = image.getRGB(x, y);
			double a = weight * (c >>> 24);
			alpha += a;
			red += a * ((c >> 16) & 0xFF);
			green += a * ((c >> 8) & 0xFF);
			blue += a * (c & 0xFF);
		}
		if (alpha < 0.5) {
			return 0;
		}
		return ((int) Math.round(alpha) << 24) | ((int) Math.round(red / alpha) << 16)
				| ((int) Math.round(green / alpha) << 8) | (int) Math.round(blue / alpha);
	}

	/** A four-pointed glint: a bright centre and arms that fade towards the tips. */
	private static void sparkle(BufferedImage image, int cx, int cy, int arm) {
		for (int k = -arm; k <= arm; k++) {
			int alpha = 255 - 255 * Math.abs(k) / (arm + 1);
			set(image, cx + k, cy, alpha);
			set(image, cx, cy + k, alpha);
		}
		set(image, cx, cy, 255);
		set(image, cx + 1, cy, 255);
		set(image, cx, cy + 1, 255);
	}

	private static void set(BufferedImage image, int x, int y, int alpha) {
		if (x >= 0 && y >= 0 && x < image.getWidth() && y < image.getHeight()) {
			image.setRGB(x, y, (alpha << 24) | 0xFFFFFF);
		}
	}
}
