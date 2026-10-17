// Based on Titus Cosmetics 1.3.22, MIT License, Copyright (c) 2026 Titus. See LICENSE_TitusCosmetics.txt.
package dev.hugoservice.cosmetics;

import java.awt.image.BufferedImage;

/**
 * Drawing helpers for the cosmetic textures. Every value is either an integer or a deterministic float, so the same
 * cosmetic is painted identically on every client. Noise is periodic over the 64 texel texture, so a scrolling overlay
 * repeats without a seam.
 */
final class Paint {
	/** The colour of one texel, from its position inside a face. */
	interface Face {
		/** Colour at column {@code i} and row {@code j} of a face that is {@code w} by {@code h} texels. */
		int at(int i, int j, int w, int h);
	}

	private static final int SIZE = CosmeticTextures.SIZE;

	private Paint() {
	}

	static void put(BufferedImage img, int x, int y, int argb) {
		if (x >= 0 && y >= 0 && x < SIZE && y < SIZE) {
			img.setRGB(x, y, argb);
		}
	}

	/** Like {@link #put} but wraps around the texture, for patterns that repeat. */
	static void putWrapped(BufferedImage img, int x, int y, int argb) {
		put(img, Math.floorMod(x, SIZE), Math.floorMod(y, SIZE), argb);
	}

	/** Paints a rectangle of texels from a face function. */
	static void face(BufferedImage img, Face face, int x0, int y0, int w, int h) {
		for (int j = 0; j < h; j++) {
			for (int i = 0; i < w; i++) {
				put(img, x0 + i, y0 + j, face.at(i, j, w, h));
			}
		}
	}

	/**
	 * Paints the six faces of a box's region with the cube layout of {@link CosmeticBox}: top and bottom, the two side
	 * faces, the front and the back. Each face is a function of its own texels.
	 */
	static void cube(BufferedImage img, CosmeticBox b, Face top, Face bottom, Face side, Face front, Face back) {
		int w = Math.round(b.w());
		int h = Math.round(b.h());
		int d = Math.round(b.d());
		int u = b.u();
		int v = b.v();
		face(img, top, u + d, v, w, d);
		face(img, bottom, u + d + w, v, w, d);
		face(img, side, u, v + d, d, h);
		face(img, side, u + d + w, v + d, d, h);
		face(img, front, u + d, v + d, w, h);
		face(img, back, u + 2 * d + w, v + d, w, h);
	}

	/** A cube with one colour per face, the same on both sides. */
	static void solidCube(BufferedImage img, CosmeticBox b, int top, int bottom, int side, int front, int back) {
		cube(img, b, (i, j, w, h) -> top, (i, j, w, h) -> bottom, (i, j, w, h) -> side, (i, j, w, h) -> front,
				(i, j, w, h) -> back);
	}

	/** Opaque colour from 8-bit channels. */
	static int rgb(int r, int g, int b) {
		return 0xFF000000 | (clamp255(r) << 16) | (clamp255(g) << 8) | clamp255(b);
	}

	/** Colour from 8-bit channels with alpha. */
	static int argb(int a, int r, int g, int b) {
		return (clamp255(a) << 24) | (clamp255(r) << 16) | (clamp255(g) << 8) | clamp255(b);
	}

	/** Linear mix of two ARGB colours, channel by channel. {@code t} is 0 for {@code a} and 1 for {@code b}. */
	static int mix(int a, int b, double t) {
		double s = Math.max(0, Math.min(1, t));
		int aa = channel(a, 24) + (int) Math.round((channel(b, 24) - channel(a, 24)) * s);
		int ar = channel(a, 16) + (int) Math.round((channel(b, 16) - channel(a, 16)) * s);
		int ag = channel(a, 8) + (int) Math.round((channel(b, 8) - channel(a, 8)) * s);
		int ab = channel(a, 0) + (int) Math.round((channel(b, 0) - channel(a, 0)) * s);
		return argb(aa, ar, ag, ab);
	}

	/** Scales the red, green and blue of a colour. The alpha stays. */
	static int shade(int argb, double factor) {
		return (argb & 0xFF000000) | (clamp255((int) Math.round(channel(argb, 16) * factor)) << 16)
				| (clamp255((int) Math.round(channel(argb, 8) * factor)) << 8)
				| clamp255((int) Math.round(channel(argb, 0) * factor));
	}

	/** Colour at {@code t} in [0, 1] along evenly spaced ARGB stops. */
	static int ramp(int[] stops, double t) {
		double s = Math.max(0, Math.min(1, t)) * (stops.length - 1);
		int k = Math.min(stops.length - 2, (int) Math.floor(s));
		return mix(stops[k], stops[k + 1], s - k);
	}

	/** Hue of an HSV colour, 0 to 1, as ARGB with full alpha. */
	static int hsv(double hue, double saturation, double value) {
		double h = (hue - Math.floor(hue)) * 6;
		int sector = (int) Math.floor(h);
		double f = h - sector;
		double p = value * (1 - saturation);
		double q = value * (1 - saturation * f);
		double t = value * (1 - saturation * (1 - f));
		double r;
		double g;
		double b;
		switch (sector) {
			case 0 -> {
				r = value;
				g = t;
				b = p;
			}
			case 1 -> {
				r = q;
				g = value;
				b = p;
			}
			case 2 -> {
				r = p;
				g = value;
				b = t;
			}
			case 3 -> {
				r = p;
				g = q;
				b = value;
			}
			case 4 -> {
				r = t;
				g = p;
				b = value;
			}
			default -> {
				r = value;
				g = p;
				b = q;
			}
		}
		return rgb((int) Math.round(r * 255), (int) Math.round(g * 255), (int) Math.round(b * 255));
	}

	/** Deterministic pseudo-random value in [0, 1) for an integer lattice point. */
	static double hash(int x, int y, int seed) {
		int h = x * 374761393 + y * 668265263 + seed * 982451653;
		h = (h ^ (h >>> 13)) * 1274126177;
		h = h ^ (h >>> 16);
		return (h & 0x7fffffff) / (double) 0x80000000L;
	}

	/**
	 * Smooth value noise in [0, 1) over the texture. The lattice has {@code cells} cells per 64 texels, and it repeats
	 * every 64 texels in both directions.
	 */
	static double value(double x, double y, int cells, int seed) {
		double fx = x * cells / SIZE;
		double fy = y * cells / SIZE;
		int ix = (int) Math.floor(fx);
		int iy = (int) Math.floor(fy);
		double tx = smooth(fx - ix);
		double ty = smooth(fy - iy);
		double a = lattice(ix, iy, cells, seed);
		double b = lattice(ix + 1, iy, cells, seed);
		double c = lattice(ix, iy + 1, cells, seed);
		double d = lattice(ix + 1, iy + 1, cells, seed);
		return (a + (b - a) * tx) + ((c + (d - c) * tx) - (a + (b - a) * tx)) * ty;
	}

	/** Sum of three octaves of {@link #value}, normalised to [0, 1). The cells are 4, 8 and 16 per texture. */
	static double fbm(double x, double y, int seed) {
		return (value(x, y, 4, seed) * 0.5 + value(x, y, 8, seed + 11) * 0.3 + value(x, y, 16, seed + 23) * 0.2);
	}

	private static double lattice(int ix, int iy, int cells, int seed) {
		return hash(Math.floorMod(ix, cells), Math.floorMod(iy, cells), seed);
	}

	private static double smooth(double t) {
		return t * t * (3 - 2 * t);
	}

	/** Distance between two positions on the repeating texture, along one axis. */
	static double wrapDelta(double a, double b) {
		double d = Math.abs(a - b) % SIZE;
		return Math.min(d, SIZE - d);
	}

	private static int channel(int argb, int shift) {
		return (argb >>> shift) & 0xFF;
	}

	static int clamp255(int value) {
		return Math.max(0, Math.min(255, value));
	}
}
