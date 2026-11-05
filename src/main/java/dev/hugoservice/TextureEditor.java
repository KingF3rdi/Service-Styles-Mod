/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.mojang.blaze3d.systems.RenderSystem
 *  com.mojang.blaze3d.textures.AddressMode
 *  com.mojang.blaze3d.textures.FilterMode
 *  net.minecraft.client.texture.NativeImage
 *  net.minecraft.client.texture.NativeImageBackedTexture
 *  net.minecraft.client.texture.AbstractTexture
 *  net.minecraft.util.Identifier
 *  net.minecraft.client.MinecraftClient
 */
package dev.hugoservice;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.textures.AddressMode;
import com.mojang.blaze3d.textures.FilterMode;
import dev.hugoservice.Categories;
import dev.hugoservice.Config;
import dev.hugoservice.Images;
import dev.hugoservice.PackBuilder;
import dev.hugoservice.Styles;
import java.awt.Color;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.LinkOption;
import java.nio.file.OpenOption;
import java.nio.file.Path;
import java.nio.file.attribute.FileAttribute;
import java.nio.file.attribute.FileTime;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import net.minecraft.client.texture.NativeImage;
import net.minecraft.client.texture.NativeImageBackedTexture;
import net.minecraft.client.texture.AbstractTexture;
import net.minecraft.util.Identifier;
import net.minecraft.client.MinecraftClient;

final class TextureEditor {
    private static final Identifier TEXTURE = Identifier.of((String)"hugoservice", (String)"editor");
    private static final int UNDO_MAX = 60;
    final Categories.Group group;
    final Styles.Style source;
    final List<String> files = new ArrayList<String>();
    private final Map<String, BufferedImage> images = new HashMap<String, BufferedImage>();
    private final Map<String, byte[]> metas = new HashMap<String, byte[]>();
    private final Deque<Step> undo = new ArrayDeque<Step>();
    float zoom = 1.0f;
    float viewX = Float.NaN;
    float viewY;
    private int file;
    private int frame;
    private boolean stroke;
    private boolean dirty = true;
    private int lastX = -1;
    private int lastY;
    private List<Integer> palette;
    private NativeImageBackedTexture tex;
    private static Identifier checker;
    private static final Identifier SV;
    private static final Identifier HUE;
    private static NativeImageBackedTexture sv;
    private static float svHue;
    private static boolean hue;

    TextureEditor(Categories.Group group, Styles.Style source) {
        this.group = group;
        this.source = source;
        for (String f : group.files()) {
            if (!this.load(f, source.read(f), source.read(f + ".mcmeta"))) continue;
            this.files.add(f);
        }
    }

    private boolean load(String f, byte[] png, byte[] meta) {
        if (png == null) {
            png = Styles.vanilla(f);
            meta = Styles.vanilla(f + ".mcmeta");
        }
        if (png == null) {
            return false;
        }
        try {
            this.images.put(f, Images.decode(png));
        }
        catch (IOException e) {
            return false;
        }
        if (meta != null) {
            this.metas.put(f, meta);
        } else {
            this.metas.remove(f);
        }
        return true;
    }

    String path() {
        return this.files.get(this.file);
    }

    BufferedImage image() {
        return this.images.get(this.path());
    }

    int width() {
        return this.image().getWidth();
    }

    int fileIndex() {
        return this.file;
    }

    int frame() {
        return this.frame;
    }

    int frameHeight() {
        BufferedImage img = this.image();
        int w = img.getWidth();
        int h = img.getHeight();
        return this.metas.containsKey(this.path()) && h > w && h % w == 0 ? w : h;
    }

    int frames() {
        return this.image().getHeight() / this.frameHeight();
    }

    void showFile(int i) {
        this.file = Math.floorMod(i, this.files.size());
        this.frame = 0;
        this.zoom = 1.0f;
        this.viewX = Float.NaN;
        this.changed();
    }

    void showFrame(int i) {
        this.frame = Math.floorMod(i, this.frames());
        this.changed();
    }

    String label(String f) {
        String name = f.substring(f.lastIndexOf(47) + 1).replace(".png", "");
        for (String o : this.files) {
            if (o.equals(f) || !o.endsWith("/" + name + ".png")) continue;
            String dir = f.substring(0, f.lastIndexOf(47));
            return dir.substring(dir.lastIndexOf(47) + 1) + "/" + name;
        }
        return name;
    }

    boolean inside(int x, int y) {
        return x >= 0 && y >= 0 && x < this.width() && y < this.frameHeight();
    }

    int get(int x, int y) {
        return this.image().getRGB(x, this.frame * this.frameHeight() + y);
    }

    private void set(int x, int y, int argb) {
        if (this.inside(x, y)) {
            this.image().setRGB(x, this.frame * this.frameHeight() + y, argb);
        }
    }

    void paint(int x, int y, int argb, boolean mirror) {
        if (!this.stroke) {
            this.push();
            this.stroke = true;
            this.lastX = -1;
        }
        int x0 = this.lastX < 0 ? x : this.lastX;
        int y0 = this.lastX < 0 ? y : this.lastY;
        int dx = Math.abs(x - x0);
        int dy = -Math.abs(y - y0);
        int sx = x0 < x ? 1 : -1;
        int sy = y0 < y ? 1 : -1;
        int err = dx + dy;
        while (true) {
            this.set(x0, y0, argb);
            if (mirror) {
                this.set(this.width() - 1 - x0, y0, argb);
            }
            if (x0 == x && y0 == y) break;
            int e2 = 2 * err;
            if (e2 >= dy) {
                err += dy;
                x0 += sx;
            }
            if (e2 > dx) continue;
            err += dx;
            y0 += sy;
        }
        this.lastX = x;
        this.lastY = y;
        this.changed();
    }

    void fill(int x, int y, int argb, boolean mirror) {
        if (this.stroke || !this.inside(x, y)) {
            return;
        }
        this.stroke = true;
        this.push();
        this.flood(x, y, argb);
        if (mirror) {
            this.flood(this.width() - 1 - x, y, argb);
        }
        this.changed();
    }

    private void flood(int x, int y, int argb) {
        int from = this.get(x, y);
        if (from == argb) {
            return;
        }
        int w = this.width();
        int h = this.frameHeight();
        int[] stack = new int[w * h * 4 + 4];
        int n = 0;
        stack[n++] = y * w + x;
        while (n > 0) {
            int i = stack[--n];
            int px = i % w;
            int py = i / w;
            if (this.get(px, py) != from) continue;
            this.set(px, py, argb);
            if (px > 0) {
                stack[n++] = i - 1;
            }
            if (px < w - 1) {
                stack[n++] = i + 1;
            }
            if (py > 0) {
                stack[n++] = i - w;
            }
            if (py >= h - 1) continue;
            stack[n++] = i + w;
        }
    }

    void clear() {
        this.push();
        for (int y = 0; y < this.frameHeight(); ++y) {
            for (int x = 0; x < this.width(); ++x) {
                this.set(x, y, 0);
            }
        }
        this.changed();
    }

    boolean addFrame() {
        BufferedImage img = this.image();
        int w = img.getWidth();
        int h = img.getHeight();
        int fh = this.frameHeight();
        int at = (this.frame + 1) * fh;
        if (fh != w) {
            return false;
        }
        this.push();
        int[] px = img.getRGB(0, 0, w, h, null, 0, w);
        BufferedImage out = new BufferedImage(w, h + fh, 2);
        out.setRGB(0, 0, w, at, px, 0, w);
        out.setRGB(0, at, w, fh, px, this.frame * fh * w, w);
        if (h > at) {
            out.setRGB(0, at + fh, w, h - at, px, at * w, w);
        }
        this.images.put(this.path(), out);
        this.metas.putIfAbsent(this.path(), "{\"animation\":{\"frametime\":2}}".getBytes(StandardCharsets.UTF_8));
        ++this.frame;
        this.changed();
        return true;
    }

    boolean removeFrame() {
        if (this.frames() < 2) {
            return false;
        }
        this.push();
        BufferedImage img = this.image();
        int w = img.getWidth();
        int h = img.getHeight();
        int fh = this.frameHeight();
        int after = h - (this.frame + 1) * fh;
        int[] px = img.getRGB(0, 0, w, h, null, 0, w);
        BufferedImage out = new BufferedImage(w, h - fh, 2);
        if (this.frame > 0) {
            out.setRGB(0, 0, w, this.frame * fh, px, 0, w);
        }
        if (after > 0) {
            out.setRGB(0, this.frame * fh, w, after, px, (this.frame + 1) * fh * w, w);
        }
        this.images.put(this.path(), out);
        this.frame = Math.min(this.frame, this.frames() - 1);
        this.changed();
        return true;
    }

    void endStroke() {
        this.stroke = false;
        this.palette = null;
    }

    private void push() {
        if (this.undo.size() >= 60) {
            this.undo.removeLast();
        }
        this.undo.push(new Step(this.path(), TextureEditor.copy(this.image())));
    }

    boolean undo() {
        Step s = this.undo.poll();
        if (s == null) {
            return false;
        }
        this.images.put(s.file(), s.img());
        this.file = this.files.indexOf(s.file());
        this.frame = Math.min(this.frame, this.frames() - 1);
        this.changed();
        return true;
    }

    void replace(BufferedImage img) {
        this.push();
        this.images.put(this.path(), img);
        this.frame = Math.min(this.frame, this.frames() - 1);
        this.zoom = 1.0f;
        this.viewX = Float.NaN;
        this.changed();
    }

    void revert(boolean vanilla) {
        this.push();
        String f = this.path();
        if (vanilla) {
            this.load(f, null, null);
        } else {
            this.load(f, this.source.read(f), this.source.read(f + ".mcmeta"));
        }
        this.frame = Math.min(this.frame, this.frames() - 1);
        this.changed();
    }

    private void changed() {
        this.dirty = true;
        if (!this.stroke) {
            this.palette = null;
        }
    }

    List<Integer> palette(int max) {
        if (this.palette != null) {
            return this.palette;
        }
        HashMap<Integer, Integer> count = new HashMap<Integer, Integer>();
        for (int y = 0; y < this.frameHeight(); ++y) {
            for (int x = 0; x < this.width(); ++x) {
                int c = this.get(x, y);
                if (c >>> 24 == 0) continue;
                count.merge(c | 0xFF000000, 1, Integer::sum);
            }
        }
        this.palette = count.entrySet().stream().sorted(Map.Entry.<Integer,Integer>comparingByValue().reversed()).limit(max).map(Map.Entry::getKey).toList();
        return this.palette;
    }

    Identifier texture() {
        BufferedImage img = this.image();
        if (this.tex == null || this.tex.getImage().getWidth() != img.getWidth() || this.tex.getImage().getHeight() != img.getHeight()) {
            this.tex = new NativeImageBackedTexture(() -> "hugoservice editor", new NativeImage(img.getWidth(), img.getHeight(), false));
            MinecraftClient.getInstance().getTextureManager().registerTexture(TEXTURE, (AbstractTexture)this.tex);
            this.dirty = true;
        }
        if (this.dirty) {
            NativeImage ni = this.tex.getImage();
            for (int y = 0; y < img.getHeight(); ++y) {
                for (int x = 0; x < img.getWidth(); ++x) {
                    ni.setColorArgb(x, y, img.getRGB(x, y));
                }
            }
            this.tex.upload();
            this.dirty = false;
        }
        return TEXTURE;
    }

    static Identifier checker() {
        if (checker == null) {
            NativeImage ni = new NativeImage(2, 2, false);
            for (int i = 0; i < 4; ++i) {
                ni.setColorArgb(i % 2, i / 2, (i % 2 + i / 2) % 2 == 0 ? -12958638 : -13880256);
            }
            checker = Identifier.of((String)"hugoservice", (String)"checker");
            MinecraftClient.getInstance().getTextureManager().registerTexture(checker, (AbstractTexture)new NativeImageBackedTexture(() -> "hugoservice checker", ni){
                {
                    Mc.applyNearestSampler(this);
                }
            });
        }
        return checker;
    }

    static Identifier svSquare(float h) {
        if (sv == null) {
            sv = new NativeImageBackedTexture(() -> "hugoservice editor sv", new NativeImage(32, 32, false)){
                {
                    Mc.applyClampLinearSampler(this);
                }
            };
            MinecraftClient.getInstance().getTextureManager().registerTexture(SV, (AbstractTexture)sv);
        }
        if (!(Math.abs(h - svHue) <= 1.0E-4f)) {
            NativeImage ni = sv.getImage();
            for (int y = 0; y < 32; ++y) {
                for (int x = 0; x < 32; ++x) {
                    ni.setColorArgb(x, y, 0xFF000000 | Color.HSBtoRGB(h, (float)x / 31.0f, 1.0f - (float)y / 31.0f));
                }
            }
            sv.upload();
            svHue = h;
        }
        return SV;
    }

    static Identifier hueBar() {
        if (!hue) {
            NativeImage ni = new NativeImage(128, 1, false);
            for (int x = 0; x < 128; ++x) {
                ni.setColorArgb(x, 0, 0xFF000000 | Color.HSBtoRGB((float)x / 128.0f, 1.0f, 1.0f));
            }
            MinecraftClient.getInstance().getTextureManager().registerTexture(HUE, (AbstractTexture)new NativeImageBackedTexture(() -> "hugoservice editor hue", ni));
            hue = true;
        }
        return HUE;
    }

    static String clean(String name) {
        return name.replaceAll("[\\\\/:*?\"<>|]", "").trim();
    }

    Path save(String name) throws IOException {
        Path dir = Config.UPLOADS.resolve(TextureEditor.clean(name));
        Files.createDirectories(dir, new FileAttribute[0]);
        Path mcmeta = dir.resolve("pack.mcmeta");
        if (!Files.exists(mcmeta, new LinkOption[0])) {
            Files.write(mcmeta, PackBuilder.mcmeta(TextureEditor.clean(name) + " - edited with 7auction Styles"), new OpenOption[0]);
        }
        for (String f : this.files) {
            Path p = dir.resolve("assets/minecraft/" + f);
            Path m = dir.resolve("assets/minecraft/" + f + ".mcmeta");
            Files.createDirectories(p.getParent(), new FileAttribute[0]);
            Files.write(p, Images.encode(this.images.get(f)), new OpenOption[0]);
            if (this.metas.containsKey(f)) {
                Files.write(m, this.metas.get(f), new OpenOption[0]);
                continue;
            }
            Files.deleteIfExists(m);
        }
        Files.setLastModifiedTime(dir, FileTime.fromMillis(System.currentTimeMillis()));
        return dir;
    }

    private static BufferedImage copy(BufferedImage img) {
        BufferedImage out = new BufferedImage(img.getWidth(), img.getHeight(), 2);
        out.setRGB(0, 0, img.getWidth(), img.getHeight(), img.getRGB(0, 0, img.getWidth(), img.getHeight(), null, 0, img.getWidth()), 0, img.getWidth());
        return out;
    }

    static {
        SV = Identifier.of((String)"hugoservice", (String)"editor_sv");
        HUE = Identifier.of((String)"hugoservice", (String)"editor_hue");
        svHue = Float.NaN;
    }

    private record Step(String file, BufferedImage img) {
    }
}

