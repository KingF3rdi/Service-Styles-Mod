// Based on Titus Cosmetics 1.3.22, MIT License, Copyright (c) 2026 Titus. See LICENSE_TitusCosmetics.txt.
package dev.hugoservice.cosmetics;

import java.awt.image.BufferedImage;
import java.util.Arrays;

/**
 * Overlay textures, in the player skin layout, so each pattern lands on the matching part of the body.
 *
 * <p>The game draws a glint with an additive pipeline: it adds the texel colour to the picture and reads the texel
 * alpha only as a cut-out, nothing is drawn below 0.1. A glint therefore stores its light in its colour: black adds nothing,
 * and an alpha of 0 removes the texel. The two coats, venom and void, are translucent and use the alpha as opacity.
 * Most patterns repeat every 64 texels, so the scroll wraps without a seam. The flames and the bolts are drawn per body
 * part, so they do not continue across the edge of the texture.
 */
final class OverlayArt {
	private static final int SIZE = CosmeticTextures.SIZE;
	private static final String VENOM = "venom_coat";
	private static final String VOID = "void_glint";

	private static final int GOLD = 0xFFC45A;
	private static final int GOLD_HOT = 0xFFF1C8;
	private static final int ICE = 0xFF5FC4FF;
	private static final int ICE_EDGE = 0xFFE6F8FF;
	private static final int FLAME_HOT = 0xFFFFF2B8;
	private static final int FLAME_MID = 0xFFFF8A24;
	private static final int FLAME_RED = 0xFFC8300F;
	private static final int VIOLET = 0xFF8E4DFF;
	private static final int VIOLET_HOT = 0xFFE4D2FF;
	private static final int LIME = 0xFF9AF53A;
	private static final int LIME_HOT = 0xFFEBFFB8;
	private static final int ROSE = 0xFFFF5F9E;
	private static final int ROSE_HOT = 0xFFFFE3EF;
	private static final int VOID_DEEP = 0xFF12052A;
	private static final int VOID_VIOLET = 0xFF3E1C84;
	private static final int BOLT = 0xFFE6F7FF;
	private static final int BOLT_GLOW = 0xFF3C9DFF;
	private static final int SLIME = 0xFF63F59A;
	private static final int SLIME_LIGHT = 0xFFD2FFE2;
	private static final int WHITE = 0xFFFFFFFF;

	/** Body parts of the skin as {u, v, w, h, d}: head, hat, body, jacket, both arms and sleeves, both legs and pants. */
	private static final int[][] CUBES = {
			{0, 0, 8, 8, 8}, {32, 0, 8, 8, 8},
			{16, 16, 8, 12, 4}, {16, 32, 8, 12, 4},
			{40, 16, 4, 12, 4}, {40, 32, 4, 12, 4}, {32, 48, 4, 12, 4}, {48, 48, 4, 12, 4},
			{0, 16, 4, 12, 4}, {0, 32, 4, 12, 4}, {16, 48, 4, 12, 4}, {0, 48, 4, 12, 4}};
	private static final int FACE_COUNT = CUBES.length * 4;
	/** Each vertical face of a part: its texel origin, size, and the face it belongs to. */
	private static final int[] FACE_X = new int[FACE_COUNT];
	private static final int[] FACE_Y = new int[FACE_COUNT];
	private static final int[] FACE_W = new int[FACE_COUNT];
	private static final int[] FACE_H = new int[FACE_COUNT];
	/** The vertical face each texel belongs to, or -1 on the top and bottom faces of the parts. */
	private static final int[] FACE_AT = new int[SIZE * SIZE];

	static {
		Arrays.fill(FACE_AT, -1);
		int face = 0;
		for (int[] cube : CUBES) {
			int u = cube[0];
			int v = cube[1];
			int w = cube[2];
			int h = cube[3];
			int d = cube[4];
			// Right side, front, left side and back, in the order of the cube layout.
			int[] xs = {u, u + d, u + d + w, u + 2 * d + w};
			int[] ws = {d, w, d, w};
			for (int k = 0; k < 4; k++) {
				FACE_X[face] = xs[k];
				FACE_Y[face] = v + d;
				FACE_W[face] = ws[k];
				FACE_H[face] = h;
				for (int y = 0; y < h; y++) {
					for (int x = 0; x < ws[k]; x++) {
						FACE_AT[(v + d + y) * SIZE + xs[k] + x] = face;
					}
				}
				face++;
			}
		}
	}

	private OverlayArt() {
	}

	/** True for the two coats: translucent, with the alpha as opacity. Every other overlay is light, added to the skin. */
	static boolean isCoat(String id) {
		return VENOM.equals(id) || VOID.equals(id);
	}

	/** True for the overlays whose pattern flows down the body rather than across it. */
	static boolean flowsDown(String id) {
		return VENOM.equals(id);
	}

	/** Paints the overlay texture of a cosmetic. The pattern and the colours come from its id. */
	static void paint(BufferedImage img, Cosmetic cosmetic) {
		String id = cosmetic.id();
		int seed = id.hashCode();
		Canvas canvas = new Canvas();
		switch (id) {
			case "glacier_glint" -> facets(canvas, seed);
			case "prism_glint" -> prism(canvas, seed);
			case "blaze_glint" -> flames(canvas, seed);
			case "violet_swirl" -> swirl(canvas, seed);
			case "verdant_glint" -> circuit(canvas, seed);
			case "rose_glint" -> petals(canvas, seed);
			case "void_glint" -> nebula(canvas, seed);
			case "storm_crackle" -> lightning(canvas, seed);
			case "venom_coat" -> coat(canvas, seed);
			default -> sheen(canvas, seed);
		}
		if (isCoat(id)) {
			canvas.writeCoat(img, VENOM.equals(id) ? SLIME : VOID_DEEP);
		} else {
			canvas.writeLight(img);
		}
	}

	/** Gold sheen: thin diagonal streaks that roll over the skin, a warm wash between them and a few sparkles. */
	private static void sheen(Canvas c, int seed) {
		for (int y = 0; y < SIZE; y++) {
			for (int x = 0; x < SIZE; x++) {
				double s = (x + 0.5 * y + 2.0 * (Paint.value(x, y, 4, seed) - 0.5)) / 16.0;
				double f = s - Math.floor(s);
				double d = Math.min(f, 1 - f) * 16.0;
				double streak = Math.exp(-d * d / 1.4) * (0.55 + 0.45 * Paint.value(x, y, 8, seed + 1));
				double wash = 0.06 * Math.max(0, Math.cos(2 * Math.PI * (x + 0.5 * y) / SIZE));
				c.light(x, y, Paint.mix(GOLD, GOLD_HOT, streak), 0.42 * streak + wash);
			}
		}
		for (int k = 0; k < 6; k++) {
			sparkle(c, (int) (SIZE * Paint.hash(k, 1, seed)), (int) (SIZE * Paint.hash(k, 2, seed)), 3, GOLD_HOT,
					GOLD_HOT, 0.9);
		}
	}

	/** Ice facets: a Voronoi pattern with a faint blue tint in each cell, bright thin edges, and sparks at some seeds. */
	private static void facets(Canvas c, int seed) {
		double[] px = new double[16];
		double[] py = new double[16];
		for (int cy = 0; cy < 4; cy++) {
			for (int cx = 0; cx < 4; cx++) {
				int k = cy * 4 + cx;
				px[k] = cx * 16 + 16 * Paint.hash(cx, cy, seed);
				py[k] = cy * 16 + 16 * Paint.hash(cx, cy, seed + 7);
			}
		}
		for (int y = 0; y < SIZE; y++) {
			for (int x = 0; x < SIZE; x++) {
				double best = Double.MAX_VALUE;
				double second = Double.MAX_VALUE;
				int nearest = 0;
				for (int k = 0; k < 16; k++) {
					double dx = Paint.wrapDelta(x, px[k]);
					double dy = Paint.wrapDelta(y, py[k]);
					double d = Math.sqrt(dx * dx + dy * dy);
					if (d < best) {
						second = best;
						best = d;
						nearest = k;
					} else if (d < second) {
						second = d;
					}
				}
				double edge = clamp01(1.0 - (second - best) / 1.1);
				// Only some facets are tinted, so the texture stays transparent between the edges.
				double tint = Paint.hash(nearest, 5, seed + 3);
				c.light(x, y, ICE, tint > 0.4 ? 0.03 + 0.1 * tint : 0);
				c.light(x, y, ICE_EDGE, 0.45 * edge * edge);
			}
		}
		for (int k = 0; k < 16; k++) {
			if (Paint.hash(k, 6, seed) > 0.55) {
				sparkle(c, (int) px[k], (int) py[k], 2, ICE_EDGE, ICE_EDGE, 0.8);
			}
		}
	}

	/** Prism: fine spectral streaks along the diagonal, and rainbow sparks that are white at the centre. */
	private static void prism(Canvas c, int seed) {
		for (int y = 0; y < SIZE; y++) {
			for (int x = 0; x < SIZE; x++) {
				double t = (x + 0.5 * y) / 8.0;
				double f = t - Math.floor(t);
				double d = Math.min(f, 1 - f) * 8.0;
				int streak = Math.floorMod((int) Math.floor(t), 8);
				double line = Math.exp(-d * d / 0.8) * (0.5 + 0.5 * Paint.value(x, y, 8, seed + 2));
				c.light(x, y, Paint.hsv(streak / 8.0 + 0.25 * y / SIZE, 0.45, 1.0), 0.22 * line);
			}
		}
		for (int k = 0; k < 14; k++) {
			int ray = Paint.hsv(Paint.hash(k, 8, seed), 0.55, 1.0);
			sparkle(c, (int) (SIZE * Paint.hash(k, 9, seed)), (int) (SIZE * Paint.hash(k, 10, seed)),
					2 + (int) (2 * Paint.hash(k, 11, seed)), WHITE, ray, 0.85);
		}
	}

	/**
	 * Fire: a tongue of flame rises from the bottom edge of every part of the body. It is bright at the root, red at the
	 * ragged tip, and each column of a part has a tongue of its own height.
	 */
	private static void flames(Canvas c, int seed) {
		for (int y = 0; y < SIZE; y++) {
			for (int x = 0; x < SIZE; x++) {
				int face = FACE_AT[y * SIZE + x];
				if (face < 0) {
					continue;
				}
				int col = x - FACE_X[face];
				int h = FACE_H[face];
				int fromBottom = FACE_Y[face] + h - 1 - y;
				// Most tongues are short and a few reach the top, so the flames have gaps between them.
				double tall = Paint.hash(face * 16 + col, 3, seed);
				double reach = (0.15 + 0.85 * tall * tall) * (0.8 + 0.4 * Paint.value(x, y, 8, seed + 5));
				double up = fromBottom / (double) Math.max(1, h - 1);
				if (up > reach) {
					continue;
				}
				double s = up / reach;
				// Each column has its own brightness, so the roots of the tongues do not line up into a bar.
				double column = 0.35 + 0.65 * Paint.hash(face * 16 + col, 4, seed);
				double heat = 0.62 * Math.pow(1 - s, 1.2) * column * (0.6 + 0.6 * Paint.value(x, y, 8, seed + 4));
				int colour = s < 0.45 ? Paint.mix(FLAME_HOT, FLAME_MID, s / 0.45)
						: Paint.mix(FLAME_MID, FLAME_RED, (s - 0.45) / 0.55);
				c.light(x, y, colour, heat);
			}
		}
	}

	/** Violet swirl: spiral arms that curl out from four centres, one in each 32 texel cell, and fade with distance. */
	private static void swirl(Canvas c, int seed) {
		for (int y = 0; y < SIZE; y++) {
			for (int x = 0; x < SIZE; x++) {
				double arm = 0;
				for (int k = 0; k < 4; k++) {
					double dx = signedWrap(x - (16 + 32 * (k & 1)));
					double dy = signedWrap(y - (16 + 32 * (k >> 1)));
					double r = Math.sqrt(dx * dx + dy * dy);
					double theta = StrictMath.atan2(dy, dx);
					double s = r / 5.0 - theta / (2 * Math.PI) * 2 + 1.2 * (Paint.value(x, y, 8, seed) - 0.5);
					double f = s - Math.floor(s);
					double d = Math.min(f, 1 - f) * 5.0;
					arm = Math.max(arm, Math.exp(-d * d / 0.7) * clamp01(1 - r / 19.0));
				}
				if (arm > 0) {
					c.light(x, y, Paint.mix(VIOLET, VIOLET_HOT, arm), 0.5 * Math.pow(arm, 1.3));
				}
			}
		}
	}

	/** Verdant circuit: lime traces on an eight texel grid, a few brighter pulses, and a node at every crossing. */
	private static void circuit(Canvas c, int seed) {
		for (int j = 0; j < 8; j++) {
			for (int i = 0; i < 8; i++) {
				int hx = 8 * i;
				int hy = 8 * j;
				if (Paint.hash(i, j, seed) > 0.3) {
					for (int k = 1; k < 8; k++) {
						c.light(hx + k, hy, LIME, 0.32);
					}
				}
				if (Paint.hash(i, j, seed + 1) > 0.3) {
					for (int k = 1; k < 8; k++) {
						c.light(hx, hy + k, LIME, 0.32);
					}
				}
				if (Paint.hash(i, j, seed + 3) > 0.8) {
					for (int k = 2; k < 6; k++) {
						c.light(hx + k, hy, LIME_HOT, 0.45);
					}
				}
				c.light(hx, hy, LIME_HOT, 0.4 + 0.3 * Paint.hash(i, j, seed + 2));
			}
		}
	}

	/**
	 * Rose: four five-petalled flowers, one in each 32 texel cell, each turned by its own angle. Each petal has a thin
	 * outline and a faint fill, and every flower has a bright centre.
	 */
	private static void petals(Canvas c, int seed) {
		for (int y = 0; y < SIZE; y++) {
			for (int x = 0; x < SIZE; x++) {
				double dx = (x % 32) - 16.0;
				double dy = (y % 32) - 16.0;
				double r = Math.sqrt(dx * dx + dy * dy);
				double turn = 2 * Math.PI * Paint.hash(x / 32, y / 32, seed);
				double edge = 12.0 * Math.abs(StrictMath.cos(2.5 * (StrictMath.atan2(dy, dx) - turn)));
				double outline = Math.exp(-(r - edge) * (r - edge) / 0.5) * 0.4;
				double fill = r < edge ? 0.1 * (1 - r / edge) : 0;
				double core = Math.exp(-r * r / 3.0) * 0.8;
				c.light(x, y, ROSE, outline + fill);
				c.light(x, y, ROSE_HOT, core);
			}
		}
	}

	/** Void: a dark violet haze in clouds, with white stars and a few four-pointed ones, all translucent. */
	private static void nebula(Canvas c, int seed) {
		for (int y = 0; y < SIZE; y++) {
			for (int x = 0; x < SIZE; x++) {
				double cloud = clamp01((Paint.fbm(x, y, seed) - 0.46) * 2.4);
				if (cloud > 0) {
					c.over(x, y, Paint.mix(VOID_DEEP, VOID_VIOLET, cloud), 0.36 * cloud);
				}
			}
		}
		for (int k = 0; k < 64; k++) {
			c.over((int) (SIZE * Paint.hash(k, 4, seed)), (int) (SIZE * Paint.hash(k, 5, seed)), WHITE,
					0.5 + 0.5 * Paint.hash(k, 6, seed));
		}
		for (int k = 0; k < 5; k++) {
			int cx = (int) (SIZE * Paint.hash(k, 7, seed));
			int cy = (int) (SIZE * Paint.hash(k, 8, seed));
			c.over(cx, cy, WHITE, 1.0);
			for (int arm = 1; arm <= 2; arm++) {
				double alpha = arm == 1 ? 0.6 : 0.25;
				c.over(cx + arm, cy, WHITE, alpha);
				c.over(cx - arm, cy, WHITE, alpha);
				c.over(cx, cy + arm, WHITE, alpha);
				c.over(cx, cy - arm, WHITE, alpha);
			}
		}
	}

	/** Storm: one bolt runs down each part of the body, with a glow on both sides and now and then a fork. */
	private static void lightning(Canvas c, int seed) {
		for (int face = 0; face < FACE_COUNT; face++) {
			int w = FACE_W[face];
			int h = FACE_H[face];
			double x = (w - 1) * Paint.hash(face, 0, seed);
			for (int row = 0; row < h; row++) {
				// The bolt runs straight for a few texels, then jumps sideways, which reads as lightning rather than noise.
				if (row % 3 == 0) {
					x = Math.max(0.5, Math.min(w - 1.5, x + (Paint.hash(row, face, seed + 1) - 0.5) * 3.0));
				}
				bolt(c, face, (int) Math.floor(x), row);
				if (Paint.hash(row, face + 9, seed) > 0.93 && row + 3 < h) {
					double bx = x;
					for (int s = 1; s <= 3; s++) {
						bx += Paint.hash(s, face + row, seed) > 0.5 ? 1 : -1;
						bolt(c, face, (int) Math.floor(bx), row + s);
					}
				}
			}
		}
	}

	/** One texel of a bolt, with its glow, inside a face. Texels outside the face are left alone. */
	private static void bolt(Canvas c, int face, int col, int row) {
		if (col < 0 || row < 0 || col >= FACE_W[face] || row >= FACE_H[face]) {
			return;
		}
		int x = FACE_X[face] + col;
		int y = FACE_Y[face] + row;
		c.light(x, y, BOLT, 1.0);
		if (col > 0) {
			c.light(x - 1, y, BOLT_GLOW, 0.3);
		}
		if (col < FACE_W[face] - 1) {
			c.light(x + 1, y, BOLT_GLOW, 0.3);
		}
	}

	/**
	 * Venom: a translucent green coat in patches, and slime streams that run down the body. Each stream ends in a bead.
	 * The streams wrap around the texture, so the flow repeats without a seam.
	 */
	private static void coat(Canvas c, int seed) {
		for (int y = 0; y < SIZE; y++) {
			for (int x = 0; x < SIZE; x++) {
				double patch = 0.6 * Paint.value(x, y, 4, seed) + 0.4 * Paint.value(x, y, 8, seed + 2);
				double cover = clamp01((patch - 0.5) * 2.2) * 0.4;
				if (cover > 0) {
					c.over(x, y, SLIME, cover);
				}
			}
		}
		for (int x = 0; x < SIZE; x++) {
			if (Paint.hash(x, 0, seed + 4) > 0.35) {
				continue;
			}
			int top = (int) (SIZE * Paint.hash(x, 1, seed + 5));
			int length = 10 + (int) (24 * Paint.hash(x, 2, seed + 6));
			boolean wide = Paint.hash(x, 3, seed + 7) > 0.5;
			for (int k = 0; k < length; k++) {
				double fade = 1 - 0.5 * k / length;
				c.over(x, top + k, SLIME_LIGHT, 0.6 * fade);
				if (wide) {
					c.over(x + 1, top + k, SLIME_LIGHT, 0.3 * fade);
				}
			}
			int end = top + length;
			c.over(x, end, SLIME_LIGHT, 0.9);
			c.over(x, end + 1, SLIME_LIGHT, 0.6);
			c.over(x - 1, end, SLIME_LIGHT, 0.45);
			c.over(x + 1, end, SLIME_LIGHT, 0.45);
			c.over(x - 1, end - 1, WHITE, 0.9);
		}
	}

	/** A sparkle: a bright centre and four arms that fade towards their tips. */
	private static void sparkle(Canvas c, int cx, int cy, int arm, int centre, int ray, double peak) {
		c.light(cx, cy, centre, peak);
		for (int k = 1; k <= arm; k++) {
			double amount = peak * 0.6 * (arm + 1 - k) / (arm + 1.0);
			c.light(cx + k, cy, ray, amount);
			c.light(cx - k, cy, ray, amount);
			c.light(cx, cy + k, ray, amount);
			c.light(cx, cy - k, ray, amount);
		}
	}

	/** A value in (-32, 32] for the offset of a position from a centre, so the patterns wrap around the texture. */
	private static double signedWrap(double offset) {
		return offset - SIZE * Math.floor((offset + SIZE / 2.0) / SIZE);
	}

	private static double clamp01(double v) {
		return Math.max(0, Math.min(1, v));
	}

	/**
	 * Premultiplied texels. For light, the colour is the light added to the picture. For a coat, the colour is the
	 * colour times its alpha, and the alpha is the opacity. Positions wrap around the texture.
	 */
	private static final class Canvas {
		private final double[] r = new double[SIZE * SIZE];
		private final double[] g = new double[SIZE * SIZE];
		private final double[] b = new double[SIZE * SIZE];
		private final double[] a = new double[SIZE * SIZE];

		/** Adds light of a colour to a texel. Light adds up, as the game adds it. */
		void light(int x, int y, int rgb, double amount) {
			int i = index(x, y);
			r[i] += unit(rgb, 16) * amount;
			g[i] += unit(rgb, 8) * amount;
			b[i] += unit(rgb, 0) * amount;
		}

		/** Lays a translucent colour over a texel: the premultiplied "over" operator. */
		void over(int x, int y, int rgb, double alpha) {
			int i = index(x, y);
			double keep = 1 - alpha;
			r[i] = unit(rgb, 16) * alpha + r[i] * keep;
			g[i] = unit(rgb, 8) * alpha + g[i] * keep;
			b[i] = unit(rgb, 0) * alpha + b[i] * keep;
			a[i] = alpha + a[i] * keep;
		}

		/**
		 * Writes a glint. The colour is the light and the alpha only marks the lit texels, so the game cuts out the rest.
		 * Unlit texels are black with alpha 0, which adds nothing even where their mipmap averages them in.
		 */
		void writeLight(BufferedImage img) {
			for (int i = 0; i < r.length; i++) {
				int rr = byteOf(r[i]);
				int gg = byteOf(g[i]);
				int bb = byteOf(b[i]);
				int lit = (rr | gg | bb) == 0 ? 0 : 0xFF;
				img.setRGB(i % SIZE, i / SIZE, (lit << 24) | (rr << 16) | (gg << 8) | bb);
			}
		}

		/**
		 * Writes a coat as straight alpha. Texels without coat keep the coat colour with alpha 0, so the mipmaps of the
		 * texture do not darken the colour at the edges of a patch.
		 */
		void writeCoat(BufferedImage img, int background) {
			for (int i = 0; i < r.length; i++) {
				int x = i % SIZE;
				int y = i / SIZE;
				if (a[i] <= 0) {
					img.setRGB(x, y, background & 0xFFFFFF);
					continue;
				}
				int rr = byteOf(r[i] / a[i]);
				int gg = byteOf(g[i] / a[i]);
				int bb = byteOf(b[i] / a[i]);
				img.setRGB(x, y, (byteOf(a[i]) << 24) | (rr << 16) | (gg << 8) | bb);
			}
		}

		private static int index(int x, int y) {
			return Math.floorMod(y, SIZE) * SIZE + Math.floorMod(x, SIZE);
		}

		private static double unit(int rgb, int shift) {
			return ((rgb >>> shift) & 0xFF) / 255.0;
		}

		private static int byteOf(double unit) {
			return (int) Math.round(clamp01(unit) * 255);
		}
	}
}
