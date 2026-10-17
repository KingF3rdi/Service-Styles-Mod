// Based on Titus Cosmetics 1.3.22, MIT License, Copyright (c) 2026 Titus. See LICENSE_TitusCosmetics.txt.
package dev.hugoservice.cosmetics;

import java.awt.image.BufferedImage;
import java.util.Random;

/**
 * The 64x64 texture of each cosmetic, drawn pixel by pixel with a small palette and no blur. Randomness comes from a
 * {@link Random} seeded with the id, so the same cosmetic looks the same on every client.
 *
 * <p>Comments name the texel rectangles that a design lands on, as {@code x,y WxH}, following the cube layout of
 * {@link CosmeticBox}. The geometry textures are painted over the whole sheet. The aura orbs fade to transparent at
 * their edge.
 */
public final class CosmeticTextures {
	public static final int SIZE = 64;
	private static final int DARK = 0xFF0A0A0A;
	/** Feather layers of the angel wings and demon wings, as {@code {y, width, height}} of the front faces. */
	private static final int[][] ANGEL_LAYERS = {{1, 4, 10}, {13, 4, 8}, {25, 4, 6}};
	private static final int[][] DEMON_LAYERS = {{1, 4, 12}, {13, 4, 10}, {25, 3, 6}};
	/** Size of the vanilla textures that a cosmetic can be drawn with, see {@link Cosmetic#texture()}: the parrots are 32. */
	private static final int VANILLA_SIZE = 32;

	private CosmeticTextures() {
	}

	/**
	 * True for an overlay drawn as a translucent coat, with its alpha as the opacity. False for an overlay drawn as light,
	 * which the game adds to the skin: its colour is the light and its alpha only cuts out the unlit texels.
	 */
	public static boolean isCoat(Cosmetic cosmetic) {
		return cosmetic.category() == CosmeticCategory.OVERLAY && OverlayArt.isCoat(cosmetic.id());
	}

	/** True for an overlay whose pattern flows down the body, so the renderer scrolls it downwards. */
	public static boolean flowsDown(Cosmetic cosmetic) {
		return cosmetic.category() == CosmeticCategory.OVERLAY && OverlayArt.flowsDown(cosmetic.id());
	}

	/**
	 * The texture of a cosmetic as ARGB, {@link #SIZE} by {@link #SIZE} pixels. Overlays use the player skin layout, so
	 * their pattern lines up with the model parts; every other cosmetic uses the cube layout of its boxes.
	 */
	public static BufferedImage of(Cosmetic cosmetic) {
		BufferedImage image = new BufferedImage(SIZE, SIZE, BufferedImage.TYPE_INT_ARGB);
		Random random = new Random(cosmetic.id().hashCode());
		int accent = opaque(cosmetic.rarity().color());
		switch (cosmetic.category()) {
			case OVERLAY -> {
				OverlayArt.paint(image, cosmetic);
				return image;
			}
			case PET -> {
				PetArt.paint(image, cosmetic);
				return image;
			}
			case SHIELD -> {
				ShieldArt.paint(image, cosmetic);
				return image;
			}
			case ACCESSORY -> {
				AccessoryArt.paint(image, cosmetic);
				return image;
			}
			case BANDANA -> {
				BandanaArt.paint(image, cosmetic);
				return image;
			}
			default -> {
				// The categories below keep their own painters by id.
			}
		}
		switch (cosmetic.id()) {
			case "obsidian_cape" -> obsidianCape(image, random);
			case "ender_cape" -> enderCape(image, random);
			case "ember_cape" -> emberCape(image, random);
			case "frost_cape" -> frostCape(image, random);
			case "galaxy_cape" -> galaxyCape(image, random);
			case "lava_cape" -> lavaCape(image);
			case "rainbow_cape" -> rainbowCape(image);
			case "neon_grid_cape" -> neonGridCape(image);
			case "crown" -> crown(image, random);
			case "cat_ears" -> catEars(image, random);
			case "top_hat" -> topHat(image, random);
			case "halo" -> halo(image);
			case "angel_wings" -> angelWings(image, random);
			case "demon_wings" -> demonWings(image, random);
			case "shades" -> shades(image);
			case "pixel_mask" -> pixelMask(image, random);
			case "flame_aura", "soul_aura", "heart_aura", "spark_aura" -> orb(image, accent);
			default -> neutral(image, accent, random);
		}
		return image;
	}

	/**
	 * Size in texels of the texture that the UVs of the boxes address. Generated textures are {@link #SIZE}. A cosmetic with
	 * a vanilla texture uses that texture's size, see {@link Cosmetic#texture()}.
	 */
	public static int uvSize(Cosmetic cosmetic) {
		return cosmetic.texture() == null ? SIZE : VANILLA_SIZE;
	}

	// ---- Capes: front face 1,1 10x16 and back face 12,1 10x16 ---------------------------------------------------

	/** Near-black glass with glowing purple cracks. */
	private static void obsidianCape(BufferedImage img, Random r) {
		fill(img, 0xFF0C0A12);
		for (int i = 0; i < 40; i++) {
			put(img, r.nextInt(SIZE), r.nextInt(SIZE), 0xFF1C1828);
		}
		for (int c = 0; c < 10; c++) {
			int x = faceX(r);
			int y = 2 + r.nextInt(14);
			int dx = r.nextBoolean() ? 1 : -1;
			for (int step = 0; step < 16; step++) {
				put(img, x, y, 0xFF5B2A8F);
				put(img, x + 1, y, 0xFF6A3AA0);
				put(img, x, y + 1, 0xFF6A3AA0);
				put(img, x, y, 0xFFC78BFF);
				if (r.nextInt(3) == 0) {
					y++;
				} else {
					x += dx;
				}
				if (r.nextInt(5) == 0) {
					dx = -dx;
				}
			}
		}
		capeEdges(img);
	}

	/** Deep purple with small white star specks. */
	private static void enderCape(BufferedImage img, Random r) {
		fill(img, 0xFF2A0F3D);
		for (int y = 0; y < SIZE; y += 4) {
			for (int x = 0; x < SIZE; x += 4) {
				if (r.nextInt(3) == 0) {
					rect(img, x, y, 4, 4, 0xFF351549);
				}
			}
		}
		for (int i = 0; i < 60; i++) {
			put(img, faceX(r), 1 + r.nextInt(16), 0xFFFFFFFF);
		}
		// A few bigger stars: a plus with a bright centre, kept inside the faces.
		for (int i = 0; i < 8; i++) {
			int x = (r.nextBoolean() ? 2 : 13) + r.nextInt(7);
			int y = 2 + r.nextInt(13);
			put(img, x, y, 0xFFFFFFFF);
			put(img, x - 1, y, 0xFFB38CFF);
			put(img, x + 1, y, 0xFFB38CFF);
			put(img, x, y - 1, 0xFFB38CFF);
			put(img, x, y + 1, 0xFFB38CFF);
		}
		capeEdges(img);
	}

	/** Red to orange gradient on wool, with bright embers. The gradient runs over the face height. */
	private static void emberCape(BufferedImage img, Random r) {
		int[] ramp = {0xFF7A0F0F, 0xFFA11A12, 0xFFC52B14, 0xFFE0481A, 0xFFF0701E};
		for (int y = 0; y < SIZE; y++) {
			int band = Math.max(0, Math.min(ramp.length - 1, (y - 1) * ramp.length / 16));
			for (int x = 0; x < SIZE; x++) {
				put(img, x, y, r.nextInt(6) == 0 ? darker(ramp[band]) : ramp[band]);
			}
		}
		for (int i = 0; i < 40; i++) {
			int x = faceX(r);
			int y = 3 + r.nextInt(14);
			put(img, x, y, 0xFFFFE070);
			if (r.nextBoolean()) {
				put(img, x, y - 1, 0xFFFFF5B0);
			}
		}
		capeEdges(img);
	}

	/** Ice blue with a white crystal pattern. */
	private static void frostCape(BufferedImage img, Random r) {
		fill(img, 0xFF8FD3F0);
		for (int y = 0; y < SIZE; y++) {
			for (int x = 0; x < SIZE; x++) {
				if (r.nextInt(4) == 0) {
					put(img, x, y, 0xFFA8E4F8);
				}
			}
		}
		for (int i = 0; i < 14; i++) {
			int x = faceX(r);
			int y = 3 + r.nextInt(12);
			int h = 2 + r.nextInt(2);
			for (int k = -h; k <= h; k++) {
				put(img, x, y + k, 0xFFFFFFFF);
			}
			put(img, x - 1, y, 0xFFE6F8FF);
			put(img, x + 1, y, 0xFFE6F8FF);
			put(img, x - 1, y - 1, 0xFF5FB8E0);
			put(img, x + 1, y + 1, 0xFF5FB8E0);
		}
		capeEdges(img);
	}

	/** A dark nebula: value noise in five violet tones with teal patches, and a field of stars. */
	private static void galaxyCape(BufferedImage img, Random r) {
		int[] nebula = {0xFF08061A, 0xFF150B3A, 0xFF2A1560, 0xFF4E2285, 0xFF7A3A9E};
		// The edges are the quintiles of the noise, so each of the five tones covers a fifth of the sheet.
		double[] edges = {0.40, 0.50, 0.58, 0.68};
		for (int y = 0; y < SIZE; y++) {
			for (int x = 0; x < SIZE; x++) {
				double n = Paint.fbm(x, y, 41);
				int level = 0;
				for (double edge : edges) {
					if (n >= edge) {
						level++;
					}
				}
				int c = nebula[level];
				if (level >= 3 && Paint.fbm(x + 29, y + 13, 7) > 0.6) {
					c = 0xFF18566E;
				}
				put(img, x, y, c);
			}
		}
		for (int i = 0; i < 70; i++) {
			put(img, faceX(r), 1 + r.nextInt(16), i % 3 == 0 ? 0xFFCFE6FF : 0xFFFFFFFF);
		}
		// Brighter stars: a white centre with a blue glow around it.
		for (int i = 0; i < 6; i++) {
			int x = faceX(r);
			int y = 3 + r.nextInt(12);
			put(img, x, y, 0xFFFFFFFF);
			put(img, x - 1, y, 0xFF8FB8FF);
			put(img, x + 1, y, 0xFF8FB8FF);
			put(img, x, y - 1, 0xFF8FB8FF);
			put(img, x, y + 1, 0xFF8FB8FF);
		}
		capeEdges(img);
	}

	/**
	 * Basalt plates split by glowing cracks. The cracks are the seams of a Voronoi pattern of twelve by twelve cells over the
	 * sheet, lit from white-hot at the seam to dark red at the edge of the glow.
	 */
	private static void lavaCape(BufferedImage img) {
		final int cells = 12;
		final double cell = SIZE / (double) cells;
		double[][] px = new double[cells][cells];
		double[][] py = new double[cells][cells];
		for (int cy = 0; cy < cells; cy++) {
			for (int cx = 0; cx < cells; cx++) {
				px[cy][cx] = (cx + 0.2 + 0.6 * Paint.hash(cx, cy, 11)) * cell;
				py[cy][cx] = (cy + 0.2 + 0.6 * Paint.hash(cx, cy, 12)) * cell;
			}
		}
		int[] basalt = {0xFF171010, 0xFF1F1614, 0xFF2A1D18};
		for (int y = 0; y < SIZE; y++) {
			for (int x = 0; x < SIZE; x++) {
				// The two nearest feature points around this texel. The cells wrap, so the pattern tiles without a seam.
				double near = Double.MAX_VALUE;
				double second = Double.MAX_VALUE;
				int gx = (int) Math.floor(x / cell);
				int gy = (int) Math.floor(y / cell);
				for (int dy = -1; dy <= 1; dy++) {
					for (int dx = -1; dx <= 1; dx++) {
						int nx = gx + dx;
						int ny = gy + dy;
						int cx = Math.floorMod(nx, cells);
						int cy = Math.floorMod(ny, cells);
						double fx = px[cy][cx] + (nx - cx) * cell;
						double fy = py[cy][cx] + (ny - cy) * cell;
						double d = Math.hypot(x + 0.5 - fx, y + 0.5 - fy);
						if (d < near) {
							second = near;
							near = d;
						} else if (d < second) {
							second = d;
						}
					}
				}
				double seam = second - near;
				int c = basalt[(int) (Paint.hash(x, y, 5) * basalt.length)];
				if (seam < 0.6) {
					c = 0xFFFFF3A8;
				} else if (seam < 1.1) {
					c = 0xFFFFC23A;
				} else if (seam < 1.6) {
					c = 0xFFFF7A1A;
				} else if (seam < 2.2) {
					c = blend(c, 0xFF8A1E08, 0.5);
				}
				put(img, x, y, c);
			}
		}
		capeEdges(img);
	}

	/**
	 * Rainbow stripes: seven bands of the rainbow, each a stripe, with smooth blends between them down the height of the
	 * cape. A soft highlight sits in the middle of the cape.
	 */
	private static void rainbowCape(BufferedImage img) {
		int[] bands = {0xFFFF3B3B, 0xFFFF9A2E, 0xFFFFE14D, 0xFF4DDB6B, 0xFF3BD6F0, 0xFF3B7BFF, 0xFFA052FF};
		for (int y = 0; y < SIZE; y++) {
			double pos = Math.max(0, y - 1) / 16.0 * bands.length;
			int k = (int) Math.floor(pos);
			double frac = pos - k;
			double smooth = frac * frac * (3 - 2 * frac);
			for (int x = 0; x < SIZE; x++) {
				int c = blend(bands[k % bands.length], bands[(k + 1) % bands.length], smooth);
				double highlight = 0.12 * Math.max(0, 1 - Math.abs(x - 6) / 6.0);
				put(img, x, y, blend(c, 0xFFFFFFFF, highlight));
			}
		}
		capeEdges(img);
	}

	/**
	 * A synthwave grid: a sunset of violet and pink above a dark floor, with a sun cut by two stripes on each face, and cyan
	 * and magenta lines on the floor that glow into their neighbours.
	 */
	private static void neonGridCape(BufferedImage img) {
		final int horizon = 10;
		int[] sky = {0xFF1A0B33, 0xFF4A1456, 0xFF8A1E68};
		int[] sun = {0xFFFFE45C, 0xFFFFA43D, 0xFFFF3D8B};
		int floor = 0xFF0B0718;
		for (int y = 0; y < SIZE; y++) {
			int row = y < horizon ? stops(sky, Math.max(0, y - 1) / (double) (horizon - 1)) : floor;
			rect(img, 0, y, SIZE, 1, row);
		}
		for (int cx : new int[] {5, 16}) {
			for (int y = 5; y < horizon; y++) {
				if (y == 7) {
					continue; // A stripe: the sky shows through.
				}
				for (int x = cx - 5; x <= cx + 5; x++) {
					if (Math.hypot(x - cx, y - horizon) <= 5.0) {
						put(img, x, y, stops(sun, (y - 5) / (double) (horizon - 5)));
					}
				}
			}
		}
		int cyan = 0xFF3DF2FF;
		int magenta = 0xFFFF3EC8;
		int[] verticals = {2, 5, 8, 13, 16, 19};
		int[] horizontals = {horizon, 12, 14, 16};
		// Glow first, so the lines are drawn over it.
		for (int x : verticals) {
			for (int y = horizon; y < SIZE; y++) {
				put(img, x - 1, y, blend(floor, cyan, 0.3));
				put(img, x + 1, y, blend(floor, cyan, 0.3));
			}
		}
		for (int row : horizontals) {
			for (int x = 1; x <= 21; x++) {
				if (x != 11) {
					put(img, x, row - 1, blend(floor, magenta, 0.3));
					put(img, x, row + 1, blend(floor, magenta, 0.3));
				}
			}
		}
		for (int x : verticals) {
			for (int y = horizon; y < SIZE; y++) {
				put(img, x, y, cyan);
			}
		}
		for (int row : horizontals) {
			for (int x = 1; x <= 21; x++) {
				if (x != 11) {
					put(img, x, row, magenta);
				}
			}
		}
		capeEdges(img);
	}

	/** Colour at {@code t} in [0, 1] along evenly spaced opaque colours. */
	private static int stops(int[] colours, double t) {
		double s = Math.max(0, Math.min(1, t)) * (colours.length - 1);
		int k = Math.min(colours.length - 2, (int) Math.floor(s));
		return blend(colours[k], colours[k + 1], s - k);
	}

	/** Opaque mix of two opaque colours. {@code t} is 0 for {@code a} and 1 for {@code b}. */
	private static int blend(int a, int b, double t) {
		return opaque(mix(a, b, t));
	}

	// ---- Hats, halo ---------------------------------------------------------------------------------------------

	/**
	 * Gold with four red gems. The four pillars share the footprint 2,16 (top face 2x2): that top is a red gem. The
	 * centre gem uses 32,0 8x4.
	 */
	private static void crown(BufferedImage img, Random r) {
		fill(img, 0xFFD9A21B);
		for (int y = 0; y < SIZE; y++) {
			for (int x = 0; x < SIZE; x++) {
				if (y % 8 == 7) {
					put(img, x, y, 0xFFA87612);
				} else if (r.nextInt(6) == 0) {
					put(img, x, y, 0xFFF4CC4A);
				}
			}
		}
		rect(img, 2, 16, 2, 2, 0xFFE0243A);
		put(img, 2, 16, 0xFFFF8A9A);
		rect(img, 32, 0, 8, 4, 0xFFB0001C);
		rect(img, 34, 0, 4, 2, 0xFFE0243A);
		put(img, 34, 0, 0xFFFF8A9A);
	}

	/** Black fur with a pink inner area on the front of each ear. The front face is 2,2 3x4. */
	private static void catEars(BufferedImage img, Random r) {
		fill(img, 0xFF141414);
		for (int i = 0; i < 30; i++) {
			put(img, r.nextInt(SIZE), r.nextInt(SIZE), 0xFF262626);
		}
		rect(img, 2, 2, 3, 4, 0xFFF08CB4);
		rect(img, 3, 3, 1, 2, 0xFFD8649A);
		rect(img, 2, 2, 3, 1, 0xFF141414);
	}

	/**
	 * Black silk with a violet band. The crown is the 8x8 footprint 8,16 (front face 8,24), the brim is 12,0 12x12 on
	 * top and 12,12 12x1 on its sides.
	 */
	private static void topHat(BufferedImage img, Random r) {
		fill(img, 0xFF121214);
		for (int x = 0; x < SIZE; x += 3) {
			for (int y = 0; y < SIZE; y++) {
				if (r.nextInt(4) == 0) {
					put(img, x, y, 0xFF1E1E24);
				}
			}
		}
		rect(img, 0, 28, 32, 1, 0xFF8A4FD0);
		rect(img, 0, 29, 32, 1, 0xFF5C2E99);
		rect(img, 0, 12, 48, 1, 0xFF26262C);
	}

	/**
	 * Bright gold with a soft glow. The ring is made of 2-unit cubes that use the footprint 0,0 8x4: top 2,0, bottom 4,0,
	 * right 0,2, front 2,2, left 4,2 and back 6,2.
	 */
	private static void halo(BufferedImage img) {
		for (int y = 0; y < SIZE; y++) {
			for (int x = 0; x < SIZE; x++) {
				double dx = (x + 0.5) / SIZE * 2 - 1;
				double dy = (y + 0.5) / SIZE * 2 - 1;
				double d = Math.sqrt(dx * dx + dy * dy);
				int c;
				if (d < 0.5) {
					c = 0xFFFFE27A;
				} else if (d < 0.8) {
					c = 0xFFFFC93A;
				} else {
					c = 0xFFB36B12;
				}
				put(img, x, y, c);
			}
		}
		rect(img, 2, 0, 2, 2, 0xFFFFF3B0);
		rect(img, 4, 0, 2, 2, 0xFFC98A1E);
		rect(img, 0, 2, 2, 2, 0xFFE8A823);
		rect(img, 2, 2, 2, 2, 0xFFFFD84A);
		rect(img, 4, 2, 2, 2, 0xFFE8A823);
		rect(img, 6, 2, 2, 2, 0xFFC98A1E);
	}

	// ---- Wings --------------------------------------------------------------------------------------------------

	/**
	 * Dark red leather with black edges and diagonal ridges. Layers are {@code {y, width, height}} of the front faces;
	 * the back face of a layer starts at {@code x = 2 + width}.
	 */
	private static void demonWings(BufferedImage img, Random r) {
		fill(img, 0xFF6A1A1A);
		for (int y = 0; y < SIZE; y++) {
			for (int x = 0; x < SIZE; x++) {
				if ((x + y) % 6 == 0) {
					put(img, x, y, 0xFF3A0C0C);
				} else if (r.nextInt(14) == 0) {
					put(img, x, y, 0xFF8A2424);
				}
			}
		}
		wingEdges(img, DEMON_LAYERS, 0xFF0A0505);
	}

	/** White feathers with a gold tint: diagonal feather lines, golden tips and dark warm edges. */
	private static void angelWings(BufferedImage img, Random r) {
		fill(img, 0xFFF4F1E6);
		for (int y = 0; y < SIZE; y++) {
			for (int x = 0; x < SIZE; x++) {
				if ((x + y) % 5 == 0) {
					put(img, x, y, 0xFFDCD3BC);
				} else if (r.nextInt(10) == 0) {
					put(img, x, y, 0xFFFFFCF2);
				}
			}
		}
		// Golden tips with a dark edge at the very bottom. The top stays open, where the wing meets the body.
		for (int[] layer : ANGEL_LAYERS) {
			for (int x0 : new int[] {1, 2 + layer[1]}) {
				int bottom = layer[0] + layer[2] - 1;
				rect(img, x0, bottom - 1, layer[1], 1, 0xFFE8C36A);
				rect(img, x0, bottom, layer[1], 1, 0xFF3B3226);
			}
		}
	}

	/** Outlines the front and back face of every wing layer in the given colour. */
	private static void wingEdges(BufferedImage img, int[][] layers, int argb) {
		for (int[] layer : layers) {
			outline(img, 1, layer[0], layer[1], layer[2], argb);
			outline(img, 2 + layer[1], layer[0], layer[1], layer[2], argb);
		}
	}

	// ---- Face pieces --------------------------------------------------------------------------------------------

	/**
	 * Black lenses with a silver frame, a silver bridge and a small highlight. Each lens front is the 4x3 face 1,1, and
	 * the bridge is the 1x1 face 1,9 of the cube at 1,8. The rest of the sheet is silver, which is never visible.
	 */
	private static void shades(BufferedImage img) {
		final int silver = 0xFFC8CDD4;
		fill(img, 0xFF8A9099);
		rect(img, 1, 0, 4, 1, silver);
		rect(img, 0, 1, 1, 3, 0xFF9AA0A8);
		rect(img, 5, 1, 1, 3, 0xFF9AA0A8);
		rect(img, 6, 1, 4, 3, 0xFF0B0B0F);
		rect(img, 0, 8, 4, 2, silver);
		// Both lenses use this face, so one drawing covers them. The lens is black with a glint, the silver
		// shows on its top, its sides and the bridge.
		rect(img, 1, 1, 4, 3, 0xFF0B0B0F);
		put(img, 1, 1, 0xFF3A4150);
		put(img, 2, 1, 0xFF2A2F3A);
		rect(img, 5, 0, 4, 1, silver);
	}

	/** A mask with a symmetric pixel pattern in four colours and two dark eye slits. The front is 1,1 8x5. */
	private static void pixelMask(BufferedImage img, Random r) {
		int[] palette = {0xFFEEEEEE, 0xFF2EC4B6, 0xFFFF6B6B, 0xFFFFD166};
		fill(img, 0xFF222222);
		for (int y = 1; y < 6; y++) {
			for (int x = 1; x < 5; x++) {
				int c = palette[r.nextInt(palette.length)];
				put(img, x, y, c);
				put(img, 9 - x, y, c);
			}
		}
		rect(img, 2, 2, 2, 1, 0xFF111111);
		rect(img, 5, 2, 2, 1, 0xFF111111);
		outline(img, 1, 1, 8, 5, DARK);
	}

	// ---- Auras and unknown ids ---------------------------------------------------------------------------------

	/**
	 * A small glowing orb in the rarity colour: a light core that fades into the colour, then into nothing at the edge.
	 * The aura icon scales this texture up smoothly.
	 */
	private static void orb(BufferedImage img, int accent) {
		int core = lighten(accent, 0.7);
		for (int y = 0; y < SIZE; y++) {
			for (int x = 0; x < SIZE; x++) {
				double dx = (x + 0.5) / SIZE * 2 - 1;
				double dy = (y + 0.5) / SIZE * 2 - 1;
				double d = Math.sqrt(dx * dx + dy * dy);
				double alpha = Math.max(0, Math.min(1, (0.98 - d) / 0.4));
				int rgb = mix(core, accent, Math.min(1, d / 0.6));
				put(img, x, y, ((int) Math.round(alpha * 255) << 24) | rgb);
			}
		}
	}

	/** A plain design built from the rarity colour, for ids the mod does not know. */
	private static void neutral(BufferedImage img, int accent, Random r) {
		fill(img, darker(accent));
		for (int y = 0; y < SIZE; y++) {
			for (int x = 0; x < SIZE; x++) {
				if (r.nextInt(5) == 0) {
					put(img, x, y, accent);
				}
			}
		}
		outline(img, 0, 0, SIZE, SIZE, DARK);
	}

	// ---- Helpers ------------------------------------------------------------------------------------------------

	/** A random column inside the front or the back face of a cape. */
	private static int faceX(Random r) {
		return (r.nextBoolean() ? 1 : 12) + r.nextInt(10);
	}

	/** Dark outline around the front and back face of a cape. */
	private static void capeEdges(BufferedImage img) {
		outline(img, 1, 1, 10, 16, DARK);
		outline(img, 12, 1, 10, 16, DARK);
	}

	private static void put(BufferedImage img, int x, int y, int argb) {
		if (x >= 0 && y >= 0 && x < SIZE && y < SIZE) {
			img.setRGB(x, y, argb);
		}
	}

	private static void fill(BufferedImage img, int argb) {
		rect(img, 0, 0, SIZE, SIZE, argb);
	}

	private static void rect(BufferedImage img, int x0, int y0, int w, int h, int argb) {
		for (int y = y0; y < y0 + h; y++) {
			for (int x = x0; x < x0 + w; x++) {
				put(img, x, y, argb);
			}
		}
	}

	/** A one-texel outline of a rectangle. */
	private static void outline(BufferedImage img, int x0, int y0, int w, int h, int argb) {
		rect(img, x0, y0, w, 1, argb);
		rect(img, x0, y0 + h - 1, w, 1, argb);
		rect(img, x0, y0, 1, h, argb);
		rect(img, x0 + w - 1, y0, 1, h, argb);
	}

	private static int opaque(int argb) {
		return 0xFF000000 | (argb & 0x00FFFFFF);
	}

	private static int darker(int argb) {
		return scale(argb, 0.5);
	}

	private static int lighten(int argb, double amount) {
		int r = channel(argb, 16);
		int g = channel(argb, 8);
		int b = channel(argb, 0);
		r = (int) Math.round(r + (255 - r) * amount);
		g = (int) Math.round(g + (255 - g) * amount);
		b = (int) Math.round(b + (255 - b) * amount);
		return 0xFF000000 | (r << 16) | (g << 8) | b;
	}

	/** Linear mix of the RGB parts of two colours, without alpha. */
	private static int mix(int a, int b, double t) {
		int r = (int) Math.round(channel(a, 16) * (1 - t) + channel(b, 16) * t);
		int g = (int) Math.round(channel(a, 8) * (1 - t) + channel(b, 8) * t);
		int bl = (int) Math.round(channel(a, 0) * (1 - t) + channel(b, 0) * t);
		return (r << 16) | (g << 8) | bl;
	}

	private static int scale(int argb, double factor) {
		int r = (int) Math.round(channel(argb, 16) * factor);
		int g = (int) Math.round(channel(argb, 8) * factor);
		int b = (int) Math.round(channel(argb, 0) * factor);
		return 0xFF000000 | (r << 16) | (g << 8) | b;
	}

	private static int channel(int argb, int shift) {
		return (argb >> shift) & 0xFF;
	}
}
