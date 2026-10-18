package dev.hugoservice;

import net.minecraft.client.render.VertexConsumer;
import net.minecraft.client.util.math.MatrixStack;

import java.util.Arrays;
import java.util.TreeSet;

/**
 * Smooth knife geometry built in code: blades are ground surfaces between a spine and an edge line (thick at the
 * spine, a flat grind down to a fine polished edge, optional fuller), handles, guards, pins and rings are rounded rods.
 * Stored as quads with normals, texture coordinates and a colour, in model units: 0..16, knife upright, plane z = 8.
 */
final class KnifeMesh {
    /** Regions of the 64x64 finish texture (u0, v0, u1, v1), see {@link Knives#texture}. */
    static final float[] BLADE = {0, 0, 0.5f, 1}, HANDLE = {0.5f, 0, 0.6875f, 1}, METAL = {0.75f, 0, 1, 0.5f},
        EDGE = {0.75f, 0.5f, 1, 1};
    private static final int STRIDE = 8; // x y z nx ny nz u v; colours apart (as floats, white would be a NaN the JIT may rewrite)
    private static final float[] Z = {0, 0, 1}, Y = {0, 1, 0};

    private float[] data = new float[STRIDE * 2048];
    private int[] colors = new int[2048];
    private int count;
    final float[] min = {Float.MAX_VALUE, Float.MAX_VALUE, Float.MAX_VALUE}, max = {-Float.MAX_VALUE, -Float.MAX_VALUE, -Float.MAX_VALUE};
    /** Bounds (x0, y0, x1, y1) of the last blade, so teeth can carry on its pattern. */
    float[] bladeBounds = {6, 6, 10, 14};

    int vertices() { return count; }

    float get(int vertex, int field) { return data[vertex * STRIDE + field]; }

    int color(int vertex) { return colors[vertex]; }

    void emit(MatrixStack.Entry e, VertexConsumer vc, int light, int overlay) {
        for (int v = 0, o = 0; v < count; v++, o += STRIDE) {
            vc.vertex(e, data[o], data[o + 1], data[o + 2]).color(colors[v])
                .texture(data[o + 6], data[o + 7]).overlay(overlay).light(light).normal(e, data[o + 3], data[o + 4], data[o + 5]);
        }
    }

    private void vertex(float[] p, float[] n, float u, float v, int color) {
        if ((count + 1) * STRIDE > data.length) { data = Arrays.copyOf(data, data.length * 2); colors = Arrays.copyOf(colors, colors.length * 2); }
        colors[count] = color;
        int o = count++ * STRIDE;
        data[o] = p[0]; data[o + 1] = p[1]; data[o + 2] = p[2];
        data[o + 3] = n[0]; data[o + 4] = n[1]; data[o + 5] = n[2];
        data[o + 6] = u; data[o + 7] = v;
        for (int k = 0; k < 3; k++) { min[k] = Math.min(min[k], p[k]); max[k] = Math.max(max[k], p[k]); }
    }

    // ================================================================ lines

    /** A line in the knife's plane; curves are sampled into points. */
    static final class Path {
        private float[] p = new float[32];
        private int n;

        Path(float x, float y) { add(x, y); }

        Path to(float x, float y) { add(x, y); return this; }

        Path quad(float cx, float cy, float x, float y) {
            float sx = p[n - 2], sy = p[n - 1];
            for (int i = 1; i <= 14; i++) {
                float t = i / 14f, a = (1 - t) * (1 - t), b = 2 * t * (1 - t), c = t * t;
                add(a * sx + b * cx + c * x, a * sy + b * cy + c * y);
            }
            return this;
        }

        Path cubic(float c1x, float c1y, float c2x, float c2y, float x, float y) {
            float sx = p[n - 2], sy = p[n - 1];
            for (int i = 1; i <= 20; i++) {
                float t = i / 20f, u = 1 - t, a = u * u * u, b = 3 * u * u * t, c = 3 * u * t * t, d = t * t * t;
                add(a * sx + b * c1x + c * c2x + d * x, a * sy + b * c1y + c * c2y + d * y);
            }
            return this;
        }

        static Path circle(float cx, float cy, float r) {
            Path c = new Path(cx + r, cy);
            for (int i = 1; i <= 48; i++) c.to(cx + r * (float) Math.cos(i * Math.PI / 24), cy + r * (float) Math.sin(i * Math.PI / 24));
            return c;
        }

        /** Points as x, y, z triples in the knife's plane. */
        float[] line() {
            float[] out = new float[n / 2 * 3];
            for (int i = 0; i < n / 2; i++) { out[3 * i] = p[2 * i]; out[3 * i + 1] = p[2 * i + 1]; out[3 * i + 2] = 8; }
            return out;
        }

        private void add(float x, float y) {
            if (n + 2 > p.length) p = Arrays.copyOf(p, p.length * 2);
            p[n++] = x;
            p[n++] = y;
        }
    }

    /** Straight line between two points of the knife's plane. */
    static Path line(float x0, float y0, float x1, float y1) { return new Path(x0, y0).to(x1, y1); }

    /** Running length along a line of x, y, z triples. */
    private static float[] lengths(float[] l) {
        float[] acc = new float[l.length / 3];
        for (int i = 1; i < acc.length; i++)
            acc[i] = acc[i - 1] + (float) Math.sqrt(sq(l[3 * i] - l[3 * i - 3]) + sq(l[3 * i + 1] - l[3 * i - 2]) + sq(l[3 * i + 2] - l[3 * i - 1]));
        return acc;
    }

    /** Point at distance d along the line. */
    private static float[] at(float[] l, float[] acc, float d) {
        int last = acc.length - 1;
        d = Math.max(0, Math.min(acc[last], d));
        int i = 1;
        while (i < last && acc[i] < d) i++;
        float len = acc[i] - acc[i - 1], f = len < 1e-6f ? 0 : (d - acc[i - 1]) / len;
        return new float[] {lerp(l[3 * i - 3], l[3 * i], f), lerp(l[3 * i - 2], l[3 * i + 1], f), lerp(l[3 * i - 1], l[3 * i + 2], f)};
    }

    // ================================================================ parts

    /** How a blade is ground; the fuller is a groove between s0..s1 across and t0..t1 along the blade. */
    record Blade(float thick, float grind, boolean twoEdges, float f0, float f1, float ft0, float ft1, float fDepth) {
        static Blade of(float thick, float grind) { return new Blade(thick, grind, false, 0, 0, 0, 0, 0); }

        static Blade dagger(float thick) { return new Blade(thick, 0.5f, true, 0, 0, 0, 0, 0); }

        Blade fuller(float s0, float s1, float t0, float t1, float depth) { return new Blade(thick, grind, twoEdges, s0, s1, t0, t1, depth); }
    }

    /**
     * Blade between a spine line and an edge line (both from the base to the tip, where they meet). Across it, s runs
     * from the spine (0) to the edge (1); a dagger is ground from a middle ridge to both sides.
     */
    void blade(Path spine, Path edge, Blade b) {
        int rows = 72;
        float[][] sp = resample(spine.line(), rows), ed = resample(edge.line(), rows);
        float x0 = Float.MAX_VALUE, y0 = Float.MAX_VALUE, x1 = -Float.MAX_VALUE, y1 = -Float.MAX_VALUE;
        for (float[][] l : new float[][][] {sp, ed}) for (float[] p : l) {
            x0 = Math.min(x0, p[0]); x1 = Math.max(x1, p[0]); y0 = Math.min(y0, p[1]); y1 = Math.max(y1, p[1]);
        }
        float[] bounds = bladeBounds = new float[] {x0, y0, x1, y1};
        TreeSet<Float> across = new TreeSet<>();
        float polish = 0.84f;
        if (b.twoEdges) {
            for (float s : new float[] {0, 0.08f, 1 - polish, 0.3f, 0.42f, 0.5f, 0.58f, 0.7f, polish, 0.92f, 1}) across.add(s);
        } else {
            for (float s : new float[] {0, 0.03f, 0.06f, 0.2f, b.grind, (b.grind + polish) / 2, polish, 0.92f, 1}) across.add(s);
            if (b.fDepth > 0) for (int k = 0; k <= 6; k++) across.add(b.f0 + (b.f1 - b.f0) * k / 6);
        }
        float[] s = new float[across.size()];
        int k = 0;
        for (float v : across) s[k++] = v;
        // bands: polished edge(s) use the edge texture, the rest the blade texture
        float lo = b.twoEdges ? 1 - polish : 0;
        if (b.twoEdges) bladeBand(sp, ed, b, s, 0, lo, rows, EDGE, bounds, true);
        bladeBand(sp, ed, b, s, lo, polish, rows, BLADE, bounds, false);
        bladeBand(sp, ed, b, s, polish, 1, rows, EDGE, bounds, false);
        // spine and edge strips close the sides; the base sits in the guard but gets a cap anyway
        side(sp, ed, b, 0, rows, b.twoEdges ? EDGE : BLADE, bounds);
        side(sp, ed, b, 1, rows, EDGE, bounds);
        cap(sp, ed, b, s);
    }

    private void bladeBand(float[][] sp, float[][] ed, Blade b, float[] s, float from, float to, int rows, float[] region,
                           float[] bounds, boolean mirrored) {
        int c0 = 0, c1 = s.length - 1;
        while (s[c0] < from - 1e-4f) c0++;
        while (s[c1] > to + 1e-4f) c1--;
        int cols = c1 - c0 + 1;
        for (int side = 1; side >= -1; side -= 2) {
            float[][][] p = new float[rows + 1][cols][], uv = new float[rows + 1][cols][];
            int[][] col = new int[rows + 1][cols];
            for (int i = 0; i <= rows; i++) for (int j = 0; j < cols; j++) {
                float si = s[c0 + j], t = i / (float) rows;
                float[] a = sp[i], e = ed[i];
                float x = lerp(a[0], e[0], si), y = lerp(a[1], e[1], si);
                float[] zc = thickness(b, si, t);
                p[i][j] = new float[] {x, y, 8 + side * zc[0]};
                col[i][j] = grey(zc[1]);
                if (region == BLADE) uv[i][j] = new float[] {(x - bounds[0]) / (bounds[2] - bounds[0]), (bounds[3] - y) / (bounds[3] - bounds[1])};
                else {
                    float across = (si - from) / (to - from);
                    uv[i][j] = new float[] {mirrored ? 1 - across : across, 1 - t};
                }
            }
            float dir = side;
            grid(p, uv, col, region, false, false, (i, j) -> new float[] {0, 0, dir});
        }
    }

    /** Half thickness at (s across, t along) and how bright (1, darker in the fuller). */
    private static float[] thickness(Blade b, float s, float t) {
        float half = b.thick / 2, edge = 0.025f, z, shade = 1;
        if (b.twoEdges) {
            z = half + (edge - half) * (float) Math.pow(Math.abs(2 * s - 1), 1.15);
        } else if (s <= b.grind) {
            z = half * (s < 0.06f ? 0.55f + 0.45f * (float) Math.sqrt(Math.max(0, 1 - sq(1 - s / 0.06f))) : 1);
        } else {
            z = half + (edge - half) * (s - b.grind) / (1 - b.grind);
        }
        if (b.fDepth > 0 && s > b.f0 && s < b.f1 && t > b.ft0 && t < b.ft1) {
            float ramp = Math.min(1, Math.min(t - b.ft0, b.ft1 - t) / 0.07f);
            float g = (float) Math.sin(Math.PI * (s - b.f0) / (b.f1 - b.f0)) * ramp;
            z = Math.max(edge, z - b.fDepth * g);
            shade = 1 - 0.32f * g;
        }
        z *= 0.06f + 0.94f * (float) Math.sqrt(Math.min(1, (1 - t) / 0.22f)); // thin towards the tip
        return new float[] {z, shade};
    }

    /** Spine (s = 0) or edge (s = 1) strip from the front face round to the back face. */
    private void side(float[][] sp, float[][] ed, Blade b, float s, int rows, float[] region, float[] bounds) {
        float[][][] p = new float[rows + 1][3][], uv = new float[rows + 1][3][];
        int[][] col = new int[rows + 1][3];
        for (int i = 0; i <= rows; i++) {
            float t = i / (float) rows;
            float[] a = s == 0 ? sp[i] : ed[i], o = s == 0 ? ed[i] : sp[i];
            float z = thickness(b, s, t)[0];
            // a slight bulge so the spine looks rounded, not cut
            float ox = a[0] - o[0], oy = a[1] - o[1], ol = (float) Math.max(1e-6, Math.hypot(ox, oy));
            float bulge = s == 0 && !b.twoEdges ? z * 0.35f : 0;
            p[i][0] = new float[] {a[0], a[1], 8 + z};
            p[i][1] = new float[] {a[0] + ox / ol * bulge, a[1] + oy / ol * bulge, 8};
            p[i][2] = new float[] {a[0], a[1], 8 - z};
            for (int j = 0; j < 3; j++) {
                col[i][j] = -1;
                uv[i][j] = region == BLADE
                    ? new float[] {(a[0] - bounds[0]) / (bounds[2] - bounds[0]), (bounds[3] - a[1]) / (bounds[3] - bounds[1])}
                    : new float[] {0.92f + 0.04f * j, 1 - t};
            }
        }
        grid(p, uv, col, region, false, false, (i, j) -> {
            float[] a = s == 0 ? sp[i] : ed[i], o = s == 0 ? ed[i] : sp[i];
            return new float[] {a[0] - o[0], a[1] - o[1], 0};
        });
    }

    /** Flat cap over the base of the blade. */
    private void cap(float[][] sp, float[][] ed, Blade b, float[] s) {
        int n = s.length;
        float[][] ring = new float[2 * n][];
        for (int j = 0; j < n; j++) {
            float x = lerp(sp[0][0], ed[0][0], s[j]), y = lerp(sp[0][1], ed[0][1], s[j]), z = thickness(b, s[j], 0)[0];
            ring[j] = new float[] {x, y, 8 + z};
            ring[2 * n - 1 - j] = new float[] {x, y, 8 - z};
        }
        float[] dir = norm(new float[] {-(sp[1][0] - sp[0][0] + ed[1][0] - ed[0][0]), -(sp[1][1] - sp[0][1] + ed[1][1] - ed[0][1]), 0});
        fan(ring, dir, 0.25f, 0.9f, BLADE);
    }

    private void fan(float[][] ring, float[] normal, float u, float v, float[] region) {
        float[] c = new float[3];
        for (float[] p : ring) for (int k = 0; k < 3; k++) c[k] += p[k] / ring.length;
        float tu = region[0] + (region[2] - region[0]) * u, tv = region[1] + (region[3] - region[1]) * v;
        for (int i = 0; i < ring.length; i++) {
            float[] a = ring[i], b = ring[(i + 1) % ring.length];
            vertex(c, normal, tu, tv, -1);
            vertex(a, normal, tu, tv, -1);
            vertex(b, normal, tu, tv, -1);
            vertex(b, normal, tu, tv, -1);
        }
    }

    /** Thickness or width along a rod, t 0..1. */
    interface Profile { float at(float t); }

    static Profile is(float v) { return t -> v; }

    /** Rounded rod in the knife's plane, ends rounded over {@code dome}. */
    void rod(Path line, float width, float depth, float power, float dome, float[] region) {
        rod(line.line(), is(width), is(depth), power, dome, region, Z, false);
    }

    void rod(Path line, Profile width, float depth, float power, float dome, float[] region) {
        rod(line.line(), width, is(depth), power, dome, region, Z, false);
    }

    /** Closed ring along a loop in the knife's plane (finger ring, guard ring). */
    void ring(Path loop, float width, float depth, float[] region) {
        rod(loop.line(), is(width), is(depth), 2.4f, 0, region, Z, true);
    }

    /** Round pin through the knife at (x, y), {@code length} deep. */
    void pin(float x, float y, float diameter, float length, float[] region) {
        rod(new float[] {x, y, 8 - length / 2, x, y, 8 + length / 2}, is(diameter), is(diameter), 2, diameter / 2, region, Y, false);
    }

    /**
     * Rod along a line of x, y, z triples: cross-section a superellipse (power 2 = round, 4 = almost square) with the
     * given full width (across the line, in the plane that holds {@code up}'s normal) and depth (along up).
     */
    private void rod(float[] l, Profile width, Profile depth, float power, float dome, float[] region, float[] up, boolean closed) {
        float[] acc = lengths(l);
        float len = acc[acc.length - 1];
        TreeSet<Float> stations = new TreeSet<>();
        if (!closed && dome > 0) for (int k = 0; k <= 10; k++) {
            float d = Math.min(len / 2, dome) * (1 - (float) Math.cos(k / 10.0 * Math.PI / 2));
            stations.add(d);
            stations.add(len - d);
        }
        for (float d = 0; d < len; d += closed ? 0.12f : 0.2f) stations.add(d);
        stations.add(len);
        int rows = stations.size(), cols = 21, i = 0;
        float[][][] p = new float[rows][cols][], uv = new float[rows][cols][];
        float[][] centre = new float[rows][];
        float exp = 2 / power, dm = Math.min(len / 2, dome);
        for (float d : stations) {
            float[] c = at(l, acc, d);
            float[] ahead = closed ? at(l, acc, (d + 0.05f) % len) : at(l, acc, d + 0.05f);
            float[] behind = closed ? at(l, acc, (d - 0.05f + len) % len) : at(l, acc, d - 0.05f);
            float[] t = norm(sub(ahead, behind));
            float dot = dot(up, t);
            float[] a = norm(new float[] {up[0] - dot * t[0], up[1] - dot * t[1], up[2] - dot * t[2]});
            float[] n = norm(cross(t, a));
            float scale = 1;
            if (!closed && dm > 0) {
                float e = Math.min(d, len - d);
                scale = e >= dm ? 1 : (float) Math.sqrt(Math.max(0, 1 - sq(1 - e / dm)));
                scale = Math.max(scale, 0.02f);
            }
            float tt = len <= 0 ? 0 : d / len;
            float hw = width.at(tt) / 2 * scale, hd = depth.at(tt) / 2 * scale;
            centre[i] = c;
            for (int j = 0; j < cols; j++) {
                double th = 2 * Math.PI * j / (cols - 1);
                float cx = signPow((float) Math.cos(th), exp), cy = signPow((float) Math.sin(th), exp);
                p[i][j] = new float[] {c[0] + n[0] * hw * cx + a[0] * hd * cy, c[1] + n[1] * hw * cx + a[1] * hd * cy,
                    c[2] + n[2] * hw * cx + a[2] * hd * cy};
                uv[i][j] = new float[] {j / (float) (cols - 1), tt};
            }
            i++;
        }
        grid(p, uv, null, region, closed, true, (r, j) -> sub(p[r][j], centre[r]));
    }

    /** Flat-sided piece from a convex outline (x, y pairs), {@code depth} thick; texture mapped flat over {@code bounds}. */
    void prism(float[] outline, float depth, float[] region, float[] bounds) {
        int n = outline.length / 2;
        float cx = 0, cy = 0;
        for (int i = 0; i < n; i++) { cx += outline[2 * i] / n; cy += outline[2 * i + 1] / n; }
        float[][] front = new float[n][], back = new float[n][];
        for (int i = 0; i < n; i++) {
            front[i] = new float[] {outline[2 * i], outline[2 * i + 1], 8 + depth / 2};
            back[n - 1 - i] = new float[] {outline[2 * i], outline[2 * i + 1], 8 - depth / 2};
        }
        float u = region[0] + (region[2] - region[0]) * clamp01((cx - bounds[0]) / (bounds[2] - bounds[0]));
        float v = region[1] + (region[3] - region[1]) * clamp01((bounds[3] - cy) / (bounds[3] - bounds[1]));
        float uc = (u - region[0]) / (region[2] - region[0]), vc = (v - region[1]) / (region[3] - region[1]);
        fan(front, Z, uc, vc, region);
        fan(back, new float[] {0, 0, -1}, uc, vc, region);
        for (int i = 0; i < n; i++) {
            float[] a = front[i], b = front[(i + 1) % n];
            float[] nn = norm(new float[] {b[1] - a[1], a[0] - b[0], 0});
            float mx = (a[0] + b[0]) / 2 - cx, my = (a[1] + b[1]) / 2 - cy;
            if (nn[0] * mx + nn[1] * my < 0) nn = new float[] {-nn[0], -nn[1], 0};
            float[] ab = {a[0], a[1], 8 - depth / 2}, bb = {b[0], b[1], 8 - depth / 2};
            vertex(a, nn, u, v, -1);
            vertex(b, nn, u, v, -1);
            vertex(bb, nn, u, v, -1);
            vertex(ab, nn, u, v, -1);
        }
    }

    // ================================================================ grid surfaces

    private interface Hint { float[] at(int i, int j); }

    /**
     * Quads through a grid of points (rows i, columns j), with smooth normals from the neighbours turned the way
     * {@code hint} points. A periodic direction repeats its first row/column as the last one.
     */
    private void grid(float[][][] p, float[][][] uv, int[][] col, float[] region, boolean periodicI, boolean periodicJ, Hint hint) {
        int rows = p.length, cols = p[0].length;
        float[][][] nrm = new float[rows][cols][];
        for (int i = 0; i < rows; i++) for (int j = 0; j < cols; j++) {
            int ia = periodicI ? (i == 0 ? rows - 2 : i - 1) : Math.max(0, i - 1), ib = periodicI ? (i == rows - 1 ? 1 : i + 1) : Math.min(rows - 1, i + 1);
            int ja = periodicJ ? (j == 0 ? cols - 2 : j - 1) : Math.max(0, j - 1), jb = periodicJ ? (j == cols - 1 ? 1 : j + 1) : Math.min(cols - 1, j + 1);
            float[] n = cross(sub(p[ib][j], p[ia][j]), sub(p[i][jb], p[i][ja]));
            float l = (float) Math.sqrt(dot(n, n));
            if (l < 1e-7f) continue;
            n = new float[] {n[0] / l, n[1] / l, n[2] / l};
            if (dot(n, hint.at(i, j)) < 0) n = new float[] {-n[0], -n[1], -n[2]};
            nrm[i][j] = n;
        }
        for (int i = 0; i < rows; i++) for (int j = 0; j < cols; j++) {
            if (nrm[i][j] != null) continue;
            for (int k = 1; k < rows && nrm[i][j] == null; k++) { // a collapsed row (tip, end of a rod): borrow from near rows
                if (i - k >= 0 && nrm[i - k][j] != null) nrm[i][j] = nrm[i - k][j];
                else if (i + k < rows && nrm[i + k][j] != null) nrm[i][j] = nrm[i + k][j];
            }
            if (nrm[i][j] == null) nrm[i][j] = norm(hint.at(i, j));
        }
        float inset = 0.5f / 64;
        for (int i = 0; i + 1 < rows; i++) for (int j = 0; j + 1 < cols; j++) {
            int[][] q = {{i, j}, {i + 1, j}, {i + 1, j + 1}, {i, j + 1}};
            for (int[] v : q) {
                float[] t = uv[v[0]][v[1]];
                float u = Math.max(region[0] + inset, Math.min(region[2] - inset, region[0] + (region[2] - region[0]) * t[0]));
                float w = Math.max(region[1] + inset, Math.min(region[3] - inset, region[1] + (region[3] - region[1]) * t[1]));
                vertex(p[v[0]][v[1]], nrm[v[0]][v[1]], u, w, col == null ? -1 : col[v[0]][v[1]]);
            }
        }
    }

    /** Points spread evenly along a line (rows + 1 of them). */
    private static float[][] resample(float[] l, int rows) {
        float[] acc = lengths(l);
        float[][] out = new float[rows + 1][];
        for (int i = 0; i <= rows; i++) out[i] = at(l, acc, acc[acc.length - 1] * i / rows);
        return out;
    }

    // ================================================================ maths

    private static int grey(float f) {
        int c = Math.round(255 * Math.max(0, Math.min(1, f)));
        return 0xFF000000 | c << 16 | c << 8 | c;
    }

    private static float signPow(float v, float e) { return Math.signum(v) * (float) Math.pow(Math.abs(v), e); }

    private static float clamp01(float v) { return Math.max(0, Math.min(1, v)); }

    private static float lerp(float a, float b, float f) { return a + (b - a) * f; }

    private static float sq(float v) { return v * v; }

    private static float[] sub(float[] a, float[] b) { return new float[] {a[0] - b[0], a[1] - b[1], a[2] - b[2]}; }

    private static float dot(float[] a, float[] b) { return a[0] * b[0] + a[1] * b[1] + a[2] * b[2]; }

    private static float[] cross(float[] a, float[] b) {
        return new float[] {a[1] * b[2] - a[2] * b[1], a[2] * b[0] - a[0] * b[2], a[0] * b[1] - a[1] * b[0]};
    }

    private static float[] norm(float[] v) {
        float l = (float) Math.sqrt(dot(v, v));
        return l < 1e-9f ? new float[] {0, 0, 1} : new float[] {v[0] / l, v[1] / l, v[2] / l};
    }
}
