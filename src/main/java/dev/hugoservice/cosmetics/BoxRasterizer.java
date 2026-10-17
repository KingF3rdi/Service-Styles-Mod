// Based on Titus Cosmetics 1.3.22, MIT License, Copyright (c) 2026 Titus. See LICENSE_TitusCosmetics.txt.
package dev.hugoservice.cosmetics;

import java.awt.image.BufferedImage;
import java.util.Arrays;
import java.util.List;

/**
 * Software renderer for cosmetic boxes, used for the GUI icons. The boxes are projected orthographically from a
 * three-quarter view (yaw 35 degrees, pitch 20 degrees). Every face is shaded flat, with light from above and to the
 * left, and the cosmetic texture is sampled with the cube UV layout of {@link CosmeticBox}. A z-buffer keeps the nearest
 * face, and the image is supersampled 2x2 for smooth edges. The background stays transparent.
 */
final class BoxRasterizer {
	private static final double YAW = Math.toRadians(35);
	private static final double PITCH = Math.toRadians(20);
	/** Share of the icon the object may fill. The rest is a margin of 10 percent on each side. */
	private static final double FILL = 0.8;
	/** Samples per icon pixel along each axis. */
	private static final int SUPERSAMPLE = 2;
	private static final double AMBIENT = 0.55;
	private static final double DIFFUSE = 0.45;
	private static final double EPS = 1e-9;
	/** Texture corners of a face, in the order its model corners are given: left-top, right-top, right-bottom, left-bottom. */
	private static final double[] CORNER_S = {0, 1, 1, 0};
	private static final double[] CORNER_T = {0, 0, 1, 1};

	private final int size;
	private final int samples;
	private final int[] texels;
	private final int texW;
	private final int texH;
	private final int[] colour;
	private final double[] depth;
	/** Unit vector from the object towards the camera, in model space. */
	private final double[] cam;
	/** Unit vector pointing to the right of the icon, in model space. */
	private final double[] right;
	/** Unit vector pointing up in the icon, in model space. */
	private final double[] up;
	/** Unit vector towards the light, in model space. */
	private final double[] light;
	/** Centre of the bounding box of all boxes, in model units. */
	private final double[] centre;
	/** Icon samples per model unit. */
	private final double scale;

	private BoxRasterizer(List<CosmeticBox> boxes, BufferedImage texture, int size) {
		this.size = size;
		this.samples = size * SUPERSAMPLE;
		this.texW = texture.getWidth();
		this.texH = texture.getHeight();
		this.texels = texture.getRGB(0, 0, texW, texH, null, 0, texW);
		this.colour = new int[samples * samples];
		this.depth = new double[samples * samples];
		Arrays.fill(depth, Double.NEGATIVE_INFINITY);

		// Model space: y points down, the front of the player is -z. The camera is at the front-right and above.
		// StrictMath gives the same bits on every platform, so the icons are identical on every client.
		cam = new double[] {StrictMath.sin(YAW) * StrictMath.cos(PITCH), -StrictMath.sin(PITCH),
				-StrictMath.cos(YAW) * StrictMath.cos(PITCH)};
		double[] forward = {-cam[0], -cam[1], -cam[2]};
		right = normalize(cross(forward, new double[] {0, -1, 0}));
		up = cross(right, forward);
		light = normalize(new double[] {
				0.7 * up[0] - 0.5 * right[0] + 0.5 * cam[0],
				0.7 * up[1] - 0.5 * right[1] + 0.5 * cam[1],
				0.7 * up[2] - 0.5 * right[2] + 0.5 * cam[2]});

		// Fit the bounding box of all boxes into the icon, so every cosmetic fills it to the same share.
		double[] lo = {Double.POSITIVE_INFINITY, Double.POSITIVE_INFINITY, Double.POSITIVE_INFINITY};
		double[] hi = {Double.NEGATIVE_INFINITY, Double.NEGATIVE_INFINITY, Double.NEGATIVE_INFINITY};
		for (CosmeticBox b : boxes) {
			lo[0] = Math.min(lo[0], b.x());
			hi[0] = Math.max(hi[0], b.x() + b.w());
			lo[1] = Math.min(lo[1], b.y());
			hi[1] = Math.max(hi[1], b.y() + b.h());
			lo[2] = Math.min(lo[2], b.z());
			hi[2] = Math.max(hi[2], b.z() + b.d());
		}
		centre = new double[] {(lo[0] + hi[0]) / 2, (lo[1] + hi[1]) / 2, (lo[2] + hi[2]) / 2};
		double[] half = {(hi[0] - lo[0]) / 2, (hi[1] - lo[1]) / 2, (hi[2] - lo[2]) / 2};
		// Half the projected size of the box along the screen axes, which is exact for an axis-aligned box.
		double halfRight = Math.abs(right[0]) * half[0] + Math.abs(right[1]) * half[1] + Math.abs(right[2]) * half[2];
		double halfUp = Math.abs(up[0]) * half[0] + Math.abs(up[1]) * half[1] + Math.abs(up[2]) * half[2];
		scale = FILL * size / (2 * Math.max(halfRight, halfUp));
	}

	/** Renders the boxes into a transparent {@code size} by {@code size} image. */
	static BufferedImage render(List<CosmeticBox> boxes, BufferedImage texture, int size) {
		if (boxes.isEmpty()) {
			return new BufferedImage(size, size, BufferedImage.TYPE_INT_ARGB);
		}
		BoxRasterizer rasterizer = new BoxRasterizer(boxes, texture, size);
		for (CosmeticBox box : boxes) {
			rasterizer.box(box);
		}
		return rasterizer.downsample();
	}

	/** Draws the six faces of a box with the cube layout used by the in-world renderer. */
	private void box(CosmeticBox b) {
		double x0 = b.x();
		double y0 = b.y();
		double z0 = b.z();
		double x1 = x0 + b.w();
		double y1 = y0 + b.h();
		double z1 = z0 + b.d();
		int w = texelSize(b.w());
		int h = texelSize(b.h());
		int d = texelSize(b.d());
		int u = b.u();
		int v = b.v();
		quad(new double[][] {{x0, y0, z1}, {x1, y0, z1}, {x1, y0, z0}, {x0, y0, z0}}, u + d, v, w, d,
				0, -1, 0);
		quad(new double[][] {{x0, y1, z0}, {x1, y1, z0}, {x1, y1, z1}, {x0, y1, z1}}, u + d + w, v, w, d,
				0, 1, 0);
		quad(new double[][] {{x0, y0, z1}, {x0, y0, z0}, {x0, y1, z0}, {x0, y1, z1}}, u, v + d, d, h,
				-1, 0, 0);
		quad(new double[][] {{x1, y0, z0}, {x1, y0, z1}, {x1, y1, z1}, {x1, y1, z0}}, u + d + w, v + d, d, h,
				1, 0, 0);
		quad(new double[][] {{x0, y0, z0}, {x1, y0, z0}, {x1, y1, z0}, {x0, y1, z0}}, u + d, v + d, w, h,
				0, 0, -1);
		quad(new double[][] {{x1, y0, z1}, {x0, y0, z1}, {x0, y1, z1}, {x1, y1, z1}}, u + 2 * d + w, v + d, w, h,
				0, 0, 1);
	}

	/** One rectangle of a box, with its texel origin {@code tu, tv} and texel size {@code su x sv}. */
	private void quad(double[][] corners, int tu, int tv, int su, int sv, double nx, double ny, double nz) {
		double[] normal = {nx, ny, nz};
		if (dot(normal, cam) <= 0) {
			return; // Facing away from the camera.
		}
		double shade = AMBIENT + DIFFUSE * Math.max(0, dot(normal, light));
		double[] x = new double[4];
		double[] y = new double[4];
		double[] z = new double[4];
		double[] tx = new double[4];
		double[] ty = new double[4];
		for (int i = 0; i < 4; i++) {
			double[] p = project(corners[i][0], corners[i][1], corners[i][2]);
			x[i] = p[0];
			y[i] = p[1];
			z[i] = p[2];
			tx[i] = tu + CORNER_S[i] * su;
			ty[i] = tv + CORNER_T[i] * sv;
		}
		triangle(x, y, z, tx, ty, 0, 1, 2, shade);
		triangle(x, y, z, tx, ty, 0, 2, 3, shade);
	}

	/** Fills one triangle of a face with the texture, nearest sample, and the z-buffer. */
	private void triangle(double[] x, double[] y, double[] z, double[] tx, double[] ty, int a, int b, int c,
			double shade) {
		double denom = (x[b] - x[a]) * (y[c] - y[a]) - (x[c] - x[a]) * (y[b] - y[a]);
		if (Math.abs(denom) < EPS) {
			return;
		}
		int minX = Math.max(0, (int) Math.floor(Math.min(x[a], Math.min(x[b], x[c]))));
		int maxX = Math.min(samples - 1, (int) Math.ceil(Math.max(x[a], Math.max(x[b], x[c]))));
		int minY = Math.max(0, (int) Math.floor(Math.min(y[a], Math.min(y[b], y[c]))));
		int maxY = Math.min(samples - 1, (int) Math.ceil(Math.max(y[a], Math.max(y[b], y[c]))));
		for (int py = minY; py <= maxY; py++) {
			double sy = py + 0.5;
			for (int px = minX; px <= maxX; px++) {
				double sx = px + 0.5;
				double wb = ((sx - x[a]) * (y[c] - y[a]) - (sy - y[a]) * (x[c] - x[a])) / denom;
				double wc = ((x[b] - x[a]) * (sy - y[a]) - (y[b] - y[a]) * (sx - x[a])) / denom;
				double wa = 1 - wb - wc;
				if (wa < -EPS || wb < -EPS || wc < -EPS) {
					continue;
				}
				int i = py * samples + px;
				double dep = wa * z[a] + wb * z[b] + wc * z[c];
				if (dep <= depth[i]) {
					continue;
				}
				double u = wa * tx[a] + wb * tx[b] + wc * tx[c];
				double v = wa * ty[a] + wb * ty[b] + wc * ty[c];
				int texel = texel(u, v);
				if ((texel >>> 24) == 0) {
					continue; // Transparent texel: let the face behind show through.
				}
				depth[i] = dep;
				colour[i] = shadeColour(texel, shade);
			}
		}
	}

	private int texel(double u, double v) {
		int x = clamp((int) Math.floor(u), 0, texW - 1);
		int y = clamp((int) Math.floor(v), 0, texH - 1);
		return texels[y * texW + x];
	}

	/** Screen position in samples ({@code x, y, depth}) of a model point. Depth grows towards the camera. */
	private double[] project(double x, double y, double z) {
		double dx = x - centre[0];
		double dy = y - centre[1];
		double dz = z - centre[2];
		double sx = (dx * right[0] + dy * right[1] + dz * right[2]) * scale;
		double sy = (dx * up[0] + dy * up[1] + dz * up[2]) * scale;
		double depth = dx * cam[0] + dy * cam[1] + dz * cam[2];
		return new double[] {(sx + size / 2.0) * SUPERSAMPLE, (size / 2.0 - sy) * SUPERSAMPLE, depth};
	}

	/** Averages each block of samples into one icon pixel. Colour is weighted by alpha, so edges stay clean. */
	private BufferedImage downsample() {
		BufferedImage out = new BufferedImage(size, size, BufferedImage.TYPE_INT_ARGB);
		int count = SUPERSAMPLE * SUPERSAMPLE;
		for (int y = 0; y < size; y++) {
			for (int x = 0; x < size; x++) {
				int alphaSum = 0;
				int rSum = 0;
				int gSum = 0;
				int bSum = 0;
				for (int dy = 0; dy < SUPERSAMPLE; dy++) {
					for (int dx = 0; dx < SUPERSAMPLE; dx++) {
						int c = colour[(y * SUPERSAMPLE + dy) * samples + x * SUPERSAMPLE + dx];
						int a = c >>> 24;
						if (a == 0) {
							continue;
						}
						alphaSum += a;
						rSum += a * ((c >> 16) & 0xFF);
						gSum += a * ((c >> 8) & 0xFF);
						bSum += a * (c & 0xFF);
					}
				}
				if (alphaSum == 0) {
					continue;
				}
				int alpha = (alphaSum + count / 2) / count;
				out.setRGB(x, y, (alpha << 24) | ((rSum / alphaSum) << 16) | ((gSum / alphaSum) << 8)
						| (bSum / alphaSum));
			}
		}
		return out;
	}

	/** Texel size of one side of a box: the rounded model size, at least one texel. */
	private static int texelSize(float units) {
		return Math.max(1, Math.round(units));
	}

	private static int shadeColour(int texel, double shade) {
		int r = (int) Math.round(((texel >> 16) & 0xFF) * shade);
		int g = (int) Math.round(((texel >> 8) & 0xFF) * shade);
		int b = (int) Math.round((texel & 0xFF) * shade);
		return (texel & 0xFF000000) | (r << 16) | (g << 8) | b;
	}

	private static double dot(double[] a, double[] b) {
		return a[0] * b[0] + a[1] * b[1] + a[2] * b[2];
	}

	private static double[] cross(double[] a, double[] b) {
		return new double[] {a[1] * b[2] - a[2] * b[1], a[2] * b[0] - a[0] * b[2], a[0] * b[1] - a[1] * b[0]};
	}

	private static double[] normalize(double[] a) {
		double length = Math.sqrt(dot(a, a));
		return new double[] {a[0] / length, a[1] / length, a[2] / length};
	}

	private static int clamp(int value, int lo, int hi) {
		return Math.max(lo, Math.min(hi, value));
	}
}
