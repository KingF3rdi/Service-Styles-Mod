/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  org.joml.Matrix3f
 *  org.joml.Matrix4f
 *  org.joml.Matrix4fc
 *  org.joml.Quaternionf
 *  org.joml.Quaternionfc
 *  org.joml.Vector3f
 *  org.joml.Vector3fc
 */
package dev.hugoservice;

import dev.hugoservice.Categories;
import dev.hugoservice.Images;
import dev.hugoservice.Sky;
import dev.hugoservice.Styles;
import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.Image;
import java.awt.RenderingHints;
import java.awt.geom.AffineTransform;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import org.joml.Matrix3f;
import org.joml.Matrix4f;
import org.joml.Matrix4fc;
import org.joml.Quaternionf;
import org.joml.Quaternionfc;
import org.joml.Vector3f;
import org.joml.Vector3fc;

final class Previews {
    static final int SIZE = 96;
    private static final Vector3f LIGHT = new Vector3f(0.3f, 1.0f, 0.55f).normalize();

    static BufferedImage render(Categories.Group g, Styles.Style s, Integer tint) throws IOException {
        if (s.id.startsWith("cloud:")) {
            BufferedImage cloud = Previews.cloud(g, s, tint);
            if (cloud != null) {
                return cloud;
            }
        }
        return switch (g.look()) {
            default -> throw new MatchException(null, null);
            case Categories.Look.CUBE -> Previews.cube(g.cube(), s, tint);
            case Categories.Look.CRYSTAL -> Previews.crystal(s, tint);
            case Categories.Look.FLAT, Categories.Look.WIDE -> Previews.flat(g, s, tint);
            case Categories.Look.SKY -> {
                if (s.vanilla) {
                    yield Previews.vanillaSky();
                }
                yield Sky.preview(g, s);
            }
        };
    }

    private static BufferedImage cloud(Categories.Group g, Styles.Style s, Integer tint) throws IOException {
        StylesCloud.Style cs = StylesCloud.byId(s.id.substring("cloud:".length()));
        if (cs == null) {
            return null;
        }
        byte[] dedicated = StylesCloud.previewPng(cs);
        if (dedicated != null) {
            BufferedImage img = Previews.firstFrame(Images.decode(dedicated));
            if (tint != null) {
                Images.tint(img, tint);
            }
            return img;
        }
        CloudCategories.Def def = CloudCategories.get(cs.category());
        if (def != null && def.previewTop() != null) {
            String path = CloudCategories.toGroupPath(def.previewTop());
            byte[] png = s.read(path);
            if (png == null) {
                png = StylesCloud.readNamed(cs, def.previewTop());
            }
            if (png != null) {
                BufferedImage img = Previews.crop(path, Previews.firstFrame(Images.decode(png)), g);
                if (tint != null) {
                    Images.tint(img, tint);
                }
                return img;
            }
        }
        BufferedImage fromFiles = Previews.flat(g, s, tint);
        if (fromFiles != null) {
            return fromFiles;
        }
        byte[] pack = StylesCloud.readNamed(cs, "pack.png");
        if (pack == null) {
            return null;
        }
        BufferedImage img = Previews.firstFrame(Images.decode(pack));
        if (tint != null) {
            Images.tint(img, tint);
        }
        return img;
    }

    private static BufferedImage flat(Categories.Group g, Styles.Style s, Integer tint) throws IOException {
        BufferedImage img = null;
        for (String f : g.files()) {
            if (f == null || f.contains("$")) {
                continue;
            }
            byte[] png = s.read(f);
            if (png == null) {
                continue;
            }
            img = Previews.crop(f, Previews.firstFrame(Images.decode(png)), g);
            break;
        }
        if (img == null && g.id().equals("font")) {
            img = Previews.fontFallback(s);
        }
        if (img == null) {
            return null;
        }
        if (tint != null) {
            Images.tint(img, tint);
        }
        return img;
    }

    private static BufferedImage cube(Categories.Cube c, Styles.Style s, Integer tint) throws IOException {
        BufferedImage top = Previews.load(s, c.top());
        BufferedImage side = Previews.load(s, c.side());
        if (top == null || side == null) {
            return null;
        }
        if (c.topTint() != null) {
            int grass = tint != null ? tint : (s.color != null ? s.color : c.topTint());
            tint = null;
            Previews.multiply(top, grass);
            BufferedImage overlay = Previews.load(s, c.sideOverlay());
            if (overlay != null && overlay.getWidth() == side.getWidth() && overlay.getHeight() == side.getHeight()) {
                Previews.multiply(overlay, grass);
                Graphics2D g = side.createGraphics();
                g.drawImage((Image)overlay, 0, 0, null);
                g.dispose();
            }
        }
        if (tint != null) {
            Images.tint(top, tint);
            Images.tint(side, tint);
        }
        BufferedImage out = new BufferedImage(96, 96, 2);
        Graphics2D g = out.createGraphics();
        Previews.drawCube(g, top, side, side, 48.0f, 8.0f, 82.0f);
        g.dispose();
        return out;
    }

    private static BufferedImage crystal(Styles.Style s, Integer tint) throws IOException {
        byte[] obsidianPng;
        BufferedImage tex = Previews.load(s, "textures/entity/end_crystal/end_crystal.png");
        if (tex == null) {
            return null;
        }
        if (tint != null) {
            Images.tint(tex, tint);
        }
        BufferedImage obsidian = (obsidianPng = Styles.vanilla("textures/block/obsidian.png")) == null ? null : Images.decode(obsidianPng);
        int k = Math.max(1, tex.getWidth() / 64);
        Matrix3f view = new Matrix3f().rotateX((float)Math.toRadians(22.0)).rotateY((float)Math.toRadians(-30.0));
        float spin = (float)Math.toRadians(40.0);
        float tilt = 1.0471976f;
        float s45 = (float)Math.sin(0.7853981633974483);
        float bob = -16.0f;
        Matrix4f outer = new Matrix4f().scale(2.0f).translate(0.0f, -0.5f, 0.0f).translate(0.0f, (24.0f + bob / 2.0f) / 16.0f, 0.0f).rotate((Quaternionfc)new Quaternionf().rotationY(spin).rotateAxis(tilt, s45, 0.0f, s45));
        Matrix4f inner = new Matrix4f((Matrix4fc)outer).rotate((Quaternionfc)new Quaternionf().setAngleAxis(tilt, s45, 0.0f, s45).rotateY(spin)).scale(0.875f);
        Matrix4f core = new Matrix4f((Matrix4fc)inner).rotate((Quaternionfc)new Quaternionf().setAngleAxis(tilt, s45, 0.0f, s45).rotateY(spin)).scale(0.765625f);
        ArrayList<Face> faces = new ArrayList<Face>();
        if (obsidian != null) {
            Previews.block(faces, Previews.firstFrame(obsidian), view);
        }
        Previews.cuboid(faces, tex, k, view, core, 32, 0);
        Previews.cuboid(faces, tex, k, view, inner, 0, 0);
        Previews.cuboid(faces, tex, k, view, outer, 0, 0);
        float minX = Float.MAX_VALUE;
        float minY = Float.MAX_VALUE;
        float maxX = -3.4028235E38f;
        float maxY = -3.4028235E38f;
        for (Face f : faces) {
            for (float[] p : f.corners()) {
                minX = Math.min(minX, p[0]);
                maxX = Math.max(maxX, p[0]);
                minY = Math.min(minY, p[1]);
                maxY = Math.max(maxY, p[1]);
            }
        }
        int size = 192;
        float sc = Math.min((float)(size - 12) / (maxX - minX), (float)(size - 12) / (maxY - minY));
        float ox = ((float)size - (maxX - minX) * sc) / 2.0f - minX * sc;
        float oy = ((float)size - (maxY - minY) * sc) / 2.0f - minY * sc;
        faces.sort(Comparator.comparingDouble(Face::depth));
        BufferedImage out = new BufferedImage(size, size, 2);
        Graphics2D g = out.createGraphics();
        g.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_NEAREST_NEIGHBOR);
        for (Face f : faces) {
            Previews.face(g, f.img(), f.ox() * sc + ox, f.oy() * sc + oy, f.ax() * sc + ox, f.ay() * sc + oy, f.bx() * sc + ox, f.by() * sc + oy);
        }
        g.dispose();
        return out;
    }

    private static void cuboid(List<Face> out, BufferedImage tex, int k, Matrix3f view, Matrix4f m, int u, int v) {
        float a = -4.0f;
        float b = 4.0f;
        float s = 8.0f;
        Previews.quad(out, tex.getSubimage((u + 8) * k, v * k, 8 * k, 8 * k), view, m, a, a, a, s, 0.0f, 0.0f, 0.0f, 0.0f, s, -1.0f);
        Previews.quad(out, tex.getSubimage((u + 16) * k, v * k, 8 * k, 8 * k), view, m, a, b, b, s, 0.0f, 0.0f, 0.0f, 0.0f, -s, -1.0f);
        Previews.quad(out, tex.getSubimage(u * k, (v + 8) * k, 8 * k, 8 * k), view, m, a, b, a, 0.0f, 0.0f, s, 0.0f, -s, 0.0f, -1.0f);
        Previews.quad(out, tex.getSubimage((u + 8) * k, (v + 8) * k, 8 * k, 8 * k), view, m, b, b, a, -s, 0.0f, 0.0f, 0.0f, -s, 0.0f, -1.0f);
        Previews.quad(out, tex.getSubimage((u + 16) * k, (v + 8) * k, 8 * k, 8 * k), view, m, b, b, b, 0.0f, 0.0f, -s, 0.0f, -s, 0.0f, -1.0f);
        Previews.quad(out, tex.getSubimage((u + 24) * k, (v + 8) * k, 8 * k, 8 * k), view, m, a, b, b, s, 0.0f, 0.0f, 0.0f, -s, 0.0f, -1.0f);
    }

    private static void block(List<Face> out, BufferedImage tex, Matrix3f view) {
        Matrix4f m = new Matrix4f().translate(-0.5f, -1.0f, -0.5f);
        float s = 16.0f;
        Previews.quad(out, tex, view, m, 0.0f, 16.0f, 0.0f, s, 0.0f, 0.0f, 0.0f, 0.0f, s, 1.0f);
        Previews.quad(out, tex, view, m, 0.0f, 16.0f, 0.0f, 0.0f, 0.0f, s, 0.0f, -s, 0.0f, 0.6f);
        Previews.quad(out, tex, view, m, 16.0f, 16.0f, 0.0f, -s, 0.0f, 0.0f, 0.0f, -s, 0.0f, 0.8f);
        Previews.quad(out, tex, view, m, 16.0f, 16.0f, 16.0f, 0.0f, 0.0f, -s, 0.0f, -s, 0.0f, 0.6f);
        Previews.quad(out, tex, view, m, 0.0f, 16.0f, 16.0f, s, 0.0f, 0.0f, 0.0f, -s, 0.0f, 0.8f);
    }

    private static void quad(List<Face> out, BufferedImage img, Matrix3f view, Matrix4f m, float ox, float oy, float oz, float ax, float ay, float az, float bx, float by, float bz, float shade) {
        Vector3f o = m.transformPosition(new Vector3f(ox / 16.0f, oy / 16.0f, oz / 16.0f));
        Vector3f a = m.transformDirection(new Vector3f(ax / 16.0f, ay / 16.0f, az / 16.0f));
        Vector3f b = m.transformDirection(new Vector3f(bx / 16.0f, by / 16.0f, bz / 16.0f));
        float light = shade >= 0.0f ? shade : 0.78f + 0.22f * Math.abs(new Vector3f((Vector3fc)a).cross((Vector3fc)b).normalize().dot((Vector3fc)LIGHT));
        view.transform(o);
        view.transform(a);
        view.transform(b);
        float depth = o.z + (a.z + b.z) / 2.0f;
        out.add(new Face(Previews.shade(img, light), o.x, -o.y, o.x + a.x, -(o.y + a.y), o.x + b.x, -(o.y + b.y), depth));
    }

    private static BufferedImage vanillaSky() {
        BufferedImage out = new BufferedImage(192, 96, 2);
        for (int y = 0; y < 96; ++y) {
            float t = (float)y / 95.0f;
            int r = (int)(120.0f + 72.0f * t);
            int g = (int)(167.0f + 49.0f * t);
            int b = 255;
            for (int x = 0; x < 192; ++x) {
                out.setRGB(x, y, 0xFF000000 | r << 16 | g << 8 | b);
            }
        }
        return out;
    }

    private static BufferedImage doll() throws IOException {
        byte[] png = Styles.vanilla("textures/entity/player/wide/steve.png");
        if (png == null) {
            return null;
        }
        BufferedImage skin = Images.decode(png);
        BufferedImage out = new BufferedImage(16, 32, 2);
        Graphics2D g = out.createGraphics();
        Previews.part(g, skin, 8, 8, 8, 8, 4, 0);
        Previews.part(g, skin, 40, 8, 8, 8, 4, 0);
        Previews.part(g, skin, 20, 20, 8, 12, 4, 8);
        Previews.part(g, skin, 44, 20, 4, 12, 0, 8);
        Previews.part(g, skin, 36, 52, 4, 12, 12, 8);
        Previews.part(g, skin, 4, 20, 4, 12, 4, 20);
        Previews.part(g, skin, 20, 52, 4, 12, 8, 20);
        g.dispose();
        return out;
    }

    private static void part(Graphics2D g, BufferedImage skin, int u, int v, int w, int h, int x, int y) {
        g.drawImage((Image)skin.getSubimage(u, v, w, h), x, y, null);
    }

    static BufferedImage hit(int rgb) throws IOException {
        BufferedImage d = Previews.doll();
        if (d == null) {
            return null;
        }
        int tr = rgb >> 16 & 0xFF;
        int tg = rgb >> 8 & 0xFF;
        int tb = rgb & 0xFF;
        for (int y = 0; y < d.getHeight(); ++y) {
            for (int x = 0; x < d.getWidth(); ++x) {
                int c = d.getRGB(x, y);
                if (c >>> 24 == 0) continue;
                int r = ((c >> 16 & 0xFF) * 2 + tr * 3) / 5;
                int gg = ((c >> 8 & 0xFF) * 2 + tg * 3) / 5;
                int b = ((c & 0xFF) * 2 + tb * 3) / 5;
                d.setRGB(x, y, 0xFF000000 | r << 16 | gg << 8 | b);
            }
        }
        return d;
    }

    static BufferedImage glow(int rgb) throws IOException {
        BufferedImage d = Previews.doll();
        if (d == null) {
            return null;
        }
        BufferedImage out = new BufferedImage(20, 36, 2);
        for (int y = 0; y < 36; ++y) {
            for (int x = 0; x < 20; ++x) {
                boolean edge = false;
                for (int dy = -1; dy <= 1 && !edge; ++dy) {
                    for (int dx = -1; dx <= 1 && !edge; ++dx) {
                        int sx = x - 2 + dx;
                        int sy = y - 2 + dy;
                        edge = sx >= 0 && sy >= 0 && sx < 16 && sy < 32 && d.getRGB(sx, sy) >>> 24 != 0;
                    }
                }
                if (!edge) continue;
                out.setRGB(x, y, 0xFF000000 | rgb);
            }
        }
        Graphics2D g = out.createGraphics();
        g.drawImage((Image)d, 2, 2, null);
        g.dispose();
        return out;
    }

    static BufferedImage outline(int rgb) throws IOException {
        byte[] png = Styles.vanilla("textures/block/stone.png");
        if (png == null) {
            return null;
        }
        BufferedImage stone = Images.decode(png);
        return Previews.wireCube(stone, stone, rgb, true);
    }

    static BufferedImage cobweb(int rgb) throws IOException {
        byte[] png = Styles.vanilla("textures/block/cobweb.png");
        if (png == null) {
            return null;
        }
        BufferedImage out = Previews.wireCube(null, null, rgb, false);
        Graphics2D g = out.createGraphics();
        g.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_NEAREST_NEIGHBOR);
        g.drawImage(Images.decode(png), 24, 26, 48, 48, null);
        g.dispose();
        return out;
    }

    static BufferedImage shield(int ready, int disabled) throws IOException {
        byte[] png = Styles.vanilla("textures/entity/shield_base_nopattern.png");
        if (png == null) {
            return null;
        }
        BufferedImage tex = Images.decode(png);
        int k = Math.max(1, tex.getWidth() / 64);
        BufferedImage face = tex.getSubimage(k, k, 12 * k, 22 * k);
        BufferedImage out = new BufferedImage(30 * k, 24 * k, 2);
        Graphics2D g = out.createGraphics();
        BufferedImage a = Previews.copy(face);
        BufferedImage b = Previews.copy(face);
        Previews.multiply(a, ready);
        Previews.multiply(b, disabled);
        g.drawImage((Image)a, k, k, null);
        g.drawImage((Image)b, 17 * k, k, null);
        g.dispose();
        return out;
    }

    static BufferedImage shieldFace() throws IOException {
        byte[] png = Styles.vanilla("textures/entity/shield_base_nopattern.png");
        if (png == null) {
            return null;
        }
        BufferedImage tex = Images.decode(png);
        int k = Math.max(1, tex.getWidth() / 64);
        return Previews.copy(tex.getSubimage(k, k, 12 * k, 22 * k));
    }

    static BufferedImage alpha(BufferedImage img, int percent) {
        if (img == null || percent >= 100) {
            return img;
        }
        for (int y = 0; y < img.getHeight(); ++y) {
            for (int x = 0; x < img.getWidth(); ++x) {
                int c = img.getRGB(x, y);
                img.setRGB(x, y, (c >>> 24) * percent / 100 << 24 | c & 0xFFFFFF);
            }
        }
        return img;
    }

    private static BufferedImage copy(BufferedImage src) {
        BufferedImage c = new BufferedImage(src.getWidth(), src.getHeight(), 2);
        Graphics2D g = c.createGraphics();
        g.drawImage((Image)src, 0, 0, null);
        g.dispose();
        return c;
    }

    private static BufferedImage wireCube(BufferedImage top, BufferedImage side, int rgb, boolean faces) {
        BufferedImage out = new BufferedImage(96, 96, 2);
        Graphics2D g = out.createGraphics();
        float cx = 48.0f;
        float y0 = 8.0f;
        float w = 82.0f;
        if (faces && top != null) {
            Previews.drawCube(g, top, side, side, cx, y0, w);
        }
        float half = w / 2.0f;
        float q = w / 4.0f;
        float h = w * 0.58f;
        int[][] e = new int[][]{{(int)(cx - half), (int)(y0 + q), (int)cx, (int)y0}, {(int)cx, (int)y0, (int)(cx + half), (int)(y0 + q)}, {(int)(cx + half), (int)(y0 + q), (int)cx, (int)(y0 + 2.0f * q)}, {(int)cx, (int)(y0 + 2.0f * q), (int)(cx - half), (int)(y0 + q)}, {(int)(cx - half), (int)(y0 + q), (int)(cx - half), (int)(y0 + q + h)}, {(int)cx, (int)(y0 + 2.0f * q), (int)cx, (int)(y0 + 2.0f * q + h)}, {(int)(cx + half), (int)(y0 + q), (int)(cx + half), (int)(y0 + q + h)}, {(int)(cx - half), (int)(y0 + q + h), (int)cx, (int)(y0 + 2.0f * q + h)}, {(int)cx, (int)(y0 + 2.0f * q + h), (int)(cx + half), (int)(y0 + q + h)}};
        g.setColor(new Color(0xFF000000 | rgb, true));
        g.setStroke(new BasicStroke(2.5f));
        for (int[] l : e) {
            g.drawLine(l[0], l[1], l[2], l[3]);
        }
        g.dispose();
        return out;
    }

    static void drawCube(Graphics2D g, BufferedImage top, BufferedImage left, BufferedImage right, float cx, float y0, float w) {
        g.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_NEAREST_NEIGHBOR);
        float half = w / 2.0f;
        float q = w / 4.0f;
        float h = w * 0.58f;
        float lx = cx - half;
        float ly = y0 + q;
        float rx = cx + half;
        float ry = y0 + q;
        float ccx = cx;
        float ccy = y0 + 2.0f * q;
        Previews.face(g, top, lx, ly, cx, y0, ccx, ccy);
        Previews.face(g, Previews.shade(left, 0.82f), lx, ly, ccx, ccy, lx, ly + h);
        Previews.face(g, Previews.shade(right, 0.62f), ccx, ccy, rx, ry, ccx, ccy + h);
    }

    static void face(Graphics2D g, BufferedImage img, float ox, float oy, float ax, float ay, float bx, float by) {
        AffineTransform at = new AffineTransform(ax - ox, ay - oy, bx - ox, by - oy, ox, oy);
        at.scale(1.012 / (double)img.getWidth(), 1.012 / (double)img.getHeight());
        g.drawImage(img, at, null);
    }

    private static BufferedImage load(Styles.Style s, String path) throws IOException {
        if (path == null) {
            return null;
        }
        byte[] png = s.read(path);
        if (png == null) {
            png = Styles.vanilla(path);
        }
        return png == null ? null : Previews.firstFrame(Images.decode(png));
    }

    private static BufferedImage crop(String path, BufferedImage img) {
        return Previews.crop(path, img, null);
    }

    private static BufferedImage crop(String path, BufferedImage img, Categories.Group g) {
        if (img == null) {
            return null;
        }
        CloudCategories.Def def = g == null ? null : CloudCategories.forGroup(g.id());
        if (def != null && def.crop() != null && def.previewTop() != null
                && path.equals(CloudCategories.toGroupPath(def.previewTop()))) {
            BufferedImage cropped = Previews.cropFrac(img, def.crop());
            if (cropped != null) {
                return cropped;
            }
        }
        if (path.contains("entity/shield") && img.getWidth() >= 64) {
            int k = img.getWidth() / 64;
            return img.getSubimage(k, k, 12 * k, 22 * k);
        }
        if (path.contains("equipment/wings/elytra") && img.getWidth() >= 64) {
            int k = Math.max(1, img.getWidth() / 64);
            int x = Math.min(img.getWidth() - 1, (int) Math.round(0.515625 * img.getWidth()));
            int w = Math.max(1, Math.min(img.getWidth() - x, (int) Math.round(0.203125 * img.getWidth())));
            int h = Math.max(1, Math.min(img.getHeight(), (int) Math.round(0.6875 * img.getHeight())));
            return img.getSubimage(x, 0, w, h);
        }
        if (path.endsWith("font/ascii.png") && img.getHeight() >= 8) {
            int y = img.getHeight() / 4;
            int h = Math.max(1, img.getHeight() / 4);
            return img.getSubimage(0, Math.min(y, img.getHeight() - h), img.getWidth(), h);
        }
        if (!path.endsWith("gui/container/inventory.png") || img.getWidth() < 256) {
            return img;
        }
        int k = img.getWidth() / 256;
        return img.getSubimage(0, 0, Math.min(176 * k, img.getWidth()), Math.min(166 * k, img.getHeight()));
    }

    private static BufferedImage cropFrac(BufferedImage img, double[] crop) {
        int x = (int) Math.round(crop[0] * img.getWidth());
        int y = (int) Math.round(crop[1] * img.getHeight());
        int w = Math.max(1, (int) Math.round(crop[2] * img.getWidth()));
        int h = Math.max(1, (int) Math.round(crop[3] * img.getHeight()));
        if (x < 0 || y < 0 || x + w > img.getWidth() || y + h > img.getHeight()) {
            return img;
        }
        return img.getSubimage(x, y, w, h);
    }

    private static BufferedImage fontFallback(Styles.Style s) throws IOException {
        for (String path : new String[]{"textures/font/ascii.png", "textures/font/ascii_sga.png", "textures/font/accented.png"}) {
            byte[] png = s.read(path);
            if (png == null) {
                png = Styles.vanilla(path);
            }
            if (png == null) {
                continue;
            }
            return Previews.crop(path, Previews.firstFrame(Images.decode(png)), Categories.groupById("font"));
        }
        BufferedImage out = new BufferedImage(96, 24, 2);
        Graphics2D g = out.createGraphics();
        g.setColor(new Color(220, 220, 220));
        g.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
        g.drawString("AaBbCc 0123", 4, 16);
        g.dispose();
        return out;
    }

    private static BufferedImage firstFrame(BufferedImage img) {
        int w = img.getWidth();
        int h = img.getHeight();
        return h > w && h % w == 0 ? img.getSubimage(0, 0, w, w) : img;
    }

    static BufferedImage shade(BufferedImage src, float f) {
        BufferedImage copy = new BufferedImage(src.getWidth(), src.getHeight(), 2);
        Graphics2D g = copy.createGraphics();
        g.drawImage((Image)src, 0, 0, null);
        g.dispose();
        Images.darken(copy, f);
        return copy;
    }

    private static void multiply(BufferedImage img, int rgb) {
        int mr = rgb >> 16 & 0xFF;
        int mg = rgb >> 8 & 0xFF;
        int mb = rgb & 0xFF;
        for (int y = 0; y < img.getHeight(); ++y) {
            for (int x = 0; x < img.getWidth(); ++x) {
                int c = img.getRGB(x, y);
                int r = (c >> 16 & 0xFF) * mr / 255;
                int gg = (c >> 8 & 0xFF) * mg / 255;
                int b = (c & 0xFF) * mb / 255;
                img.setRGB(x, y, c & 0xFF000000 | r << 16 | gg << 8 | b);
            }
        }
    }

    private Previews() {
    }

    private record Face(BufferedImage img, float ox, float oy, float ax, float ay, float bx, float by, float depth) {
        float[][] corners() {
            return new float[][]{{this.ox, this.oy}, {this.ax, this.ay}, {this.bx, this.by}, {this.ax + this.bx - this.ox, this.ay + this.by - this.oy}};
        }
    }
}

