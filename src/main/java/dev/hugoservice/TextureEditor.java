package dev.hugoservice;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.textures.FilterMode;
import dev.hugoservice.Categories.Group;
import dev.hugoservice.Styles.Style;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.texture.NativeImage;
import net.minecraft.client.texture.NativeImageBackedTexture;
import net.minecraft.util.Identifier;

import java.awt.image.BufferedImage;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.FileTime;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Texture editor behind the menu's "Edit texture": working copies of one collection's textures (taken from the style
 * you opened it on, vanilla where that style has none), painting, fill, undo, and saving them as your own style, a
 * folder pack in config/hugoservice/packs that then shows up in that collection like any other pack.
 */
final class TextureEditor {
    private static final Identifier TEXTURE = Identifier.of("hugoservice", "editor");
    private static final int UNDO_MAX = 60;

    private record Step(String file, BufferedImage img) {}

    final Group group;
    final Style source;
    final List<String> files = new ArrayList<>();
    private final Map<String, BufferedImage> images = new HashMap<>();
    private final Map<String, byte[]> metas = new HashMap<>();
    private final Deque<Step> undo = new ArrayDeque<>();
    /** View: zoom on top of fit-to-area, and the texel in the middle of the area (NaN = centred). */
    float zoom = 1, viewX = Float.NaN, viewY;
    private int file, frame;
    private boolean stroke, dirty = true;
    private int lastX = -1, lastY;
    private List<Integer> palette;
    private NativeImageBackedTexture tex;

    TextureEditor(Group group, Style source) {
        this.group = group;
        this.source = source;
        for (String f : group.files()) if (load(f, source.read(f), source.read(f + ".mcmeta"))) files.add(f);
    }

    /** One file from png bytes (vanilla when null); false when there is nothing to load. */
    private boolean load(String f, byte[] png, byte[] meta) {
        if (png == null) { png = Styles.vanilla(f); meta = Styles.vanilla(f + ".mcmeta"); }
        if (png == null) return false;
        try {
            images.put(f, Images.decode(png));
        } catch (IOException e) {
            return false;
        }
        if (meta != null) metas.put(f, meta); else metas.remove(f);
        return true;
    }

    String path() { return files.get(file); }
    BufferedImage image() { return images.get(path()); }
    int width() { return image().getWidth(); }
    int fileIndex() { return file; }
    int frame() { return frame; }

    /** Animated textures (with .mcmeta) are strips of square frames; one frame is edited at a time. */
    int frameHeight() {
        BufferedImage img = image();
        int w = img.getWidth(), h = img.getHeight();
        return metas.containsKey(path()) && h > w && h % w == 0 ? w : h;
    }

    int frames() { return image().getHeight() / frameHeight(); }

    void showFile(int i) {
        file = Math.floorMod(i, files.size());
        frame = 0;
        zoom = 1;
        viewX = Float.NaN;
        changed();
    }

    void showFrame(int i) {
        frame = Math.floorMod(i, frames());
        changed();
    }

    /** Short name of a file: "totem_of_undying", or "humanoid/netherite" when another file has the same name. */
    String label(String f) {
        String name = f.substring(f.lastIndexOf('/') + 1).replace(".png", "");
        for (String o : files) {
            if (o.equals(f) || !o.endsWith("/" + name + ".png")) continue;
            String dir = f.substring(0, f.lastIndexOf('/'));
            return dir.substring(dir.lastIndexOf('/') + 1) + "/" + name;
        }
        return name;
    }

    boolean inside(int x, int y) { return x >= 0 && y >= 0 && x < width() && y < frameHeight(); }

    int get(int x, int y) { return image().getRGB(x, frame * frameHeight() + y); }

    private void set(int x, int y, int argb) {
        if (inside(x, y)) image().setRGB(x, frame * frameHeight() + y, argb);
    }

    // ---- painting ----

    /** Pen: a line from the last point of this stroke, so fast drags leave no gaps. */
    void paint(int x, int y, int argb) {
        if (!stroke) { push(); stroke = true; lastX = -1; }
        int x0 = lastX < 0 ? x : lastX, y0 = lastX < 0 ? y : lastY;
        int dx = Math.abs(x - x0), dy = -Math.abs(y - y0), sx = x0 < x ? 1 : -1, sy = y0 < y ? 1 : -1, err = dx + dy;
        while (true) {
            set(x0, y0, argb);
            if (x0 == x && y0 == y) break;
            int e2 = 2 * err;
            if (e2 >= dy) { err += dy; x0 += sx; }
            if (e2 <= dx) { err += dx; y0 += sy; }
        }
        lastX = x;
        lastY = y;
        changed();
    }

    /** Fills the area of the same colour around (x, y) inside the frame. Once per click. */
    void fill(int x, int y, int argb) {
        if (stroke || !inside(x, y)) return;
        stroke = true;
        int from = get(x, y);
        if (from == argb) return;
        push();
        int w = width(), h = frameHeight();
        int[] stack = new int[w * h * 4 + 4];
        int n = 0;
        stack[n++] = y * w + x;
        while (n > 0) {
            int i = stack[--n], px = i % w, py = i / w;
            if (get(px, py) != from) continue;
            set(px, py, argb);
            if (px > 0) stack[n++] = i - 1;
            if (px < w - 1) stack[n++] = i + 1;
            if (py > 0) stack[n++] = i - w;
            if (py < h - 1) stack[n++] = i + w;
        }
        changed();
    }

    /** Mouse released: the next press starts a new undo step. */
    void endStroke() {
        stroke = false;
        palette = null;
    }

    private void push() {
        if (undo.size() >= UNDO_MAX) undo.removeLast();
        undo.push(new Step(path(), copy(image())));
    }

    boolean undo() {
        Step s = undo.poll();
        if (s == null) return false;
        images.put(s.file(), s.img());
        file = files.indexOf(s.file());
        frame = Math.min(frame, frames() - 1);
        changed();
        return true;
    }

    /** Your own image in place of the current file (any size, e.g. HD). */
    void replace(BufferedImage img) {
        push();
        images.put(path(), img);
        frame = Math.min(frame, frames() - 1);
        zoom = 1;
        viewX = Float.NaN;
        changed();
    }

    /** The current file as vanilla has it, or as the style you opened has it. */
    void revert(boolean vanilla) {
        push();
        String f = path();
        if (vanilla) load(f, null, null); else load(f, source.read(f), source.read(f + ".mcmeta"));
        frame = Math.min(frame, frames() - 1);
        changed();
    }

    private void changed() {
        dirty = true;
        if (!stroke) palette = null; // while painting it is counted again once the stroke ends
    }

    /** The colours of the current frame, most used first. */
    List<Integer> palette(int max) {
        if (palette != null) return palette;
        Map<Integer, Integer> count = new HashMap<>();
        for (int y = 0; y < frameHeight(); y++) for (int x = 0; x < width(); x++) {
            int c = get(x, y);
            if (c >>> 24 != 0) count.merge(c | 0xFF000000, 1, Integer::sum);
        }
        palette = count.entrySet().stream().sorted(Map.Entry.<Integer, Integer>comparingByValue().reversed())
            .limit(max).map(Map.Entry::getKey).toList();
        return palette;
    }

    /** The current file as a GPU texture, uploaded again after each change. Render thread only. */
    Identifier texture() {
        BufferedImage img = image();
        if (tex == null || tex.getImage().getWidth() != img.getWidth() || tex.getImage().getHeight() != img.getHeight()) {
            tex = new NativeImageBackedTexture(() -> "hugoservice editor", new NativeImage(img.getWidth(), img.getHeight(), false));
            MinecraftClient.getInstance().getTextureManager().registerTexture(TEXTURE, tex); // closes the previous one
            dirty = true;
        }
        if (dirty) {
            NativeImage ni = tex.getImage();
            for (int y = 0; y < img.getHeight(); y++) for (int x = 0; x < img.getWidth(); x++) ni.setColorArgb(x, y, img.getRGB(x, y));
            tex.upload();
            dirty = false;
        }
        return TEXTURE;
    }

    private static Identifier checker;

    /** A 2x2 checkerboard that repeats: the whole see-through background in one quad (one fill per cell was slow). */
    static Identifier checker() {
        if (checker == null) {
            NativeImage ni = new NativeImage(2, 2, false);
            for (int i = 0; i < 4; i++) ni.setColorArgb(i % 2, i / 2, (i % 2 + i / 2) % 2 == 0 ? 0xFF3A4452 : 0xFF2C3440);
            checker = Identifier.of("hugoservice", "checker");
            MinecraftClient.getInstance().getTextureManager().registerTexture(checker, new NativeImageBackedTexture(() -> "hugoservice checker", ni) {
                { sampler = RenderSystem.getSamplerCache().getRepeated(FilterMode.NEAREST); }
            });
        }
        return checker;
    }

    /** Folder name a save goes to. */
    static String clean(String name) {
        return name.replaceAll("[\\\\/:*?\"<>|]", "").trim();
    }

    /**
     * Writes all textures of the collection into config/hugoservice/packs/&lt;name&gt; (the same name again replaces
     * them, other files in that folder stay). Returns the folder.
     */
    Path save(String name) throws IOException {
        Path dir = Config.UPLOADS.resolve(clean(name));
        Files.createDirectories(dir);
        Path mcmeta = dir.resolve("pack.mcmeta");
        if (!Files.exists(mcmeta)) Files.write(mcmeta, PackBuilder.mcmeta(clean(name) + " - edited with HugoService Style"));
        for (String f : files) {
            Path p = dir.resolve("assets/minecraft/" + f), m = dir.resolve("assets/minecraft/" + f + ".mcmeta");
            Files.createDirectories(p.getParent());
            Files.write(p, Images.encode(images.get(f)));
            if (metas.containsKey(f)) Files.write(m, metas.get(f)); else Files.deleteIfExists(m);
        }
        Files.setLastModifiedTime(dir, FileTime.fromMillis(System.currentTimeMillis())); // previews key on it
        return dir;
    }

    private static BufferedImage copy(BufferedImage img) {
        BufferedImage out = new BufferedImage(img.getWidth(), img.getHeight(), BufferedImage.TYPE_INT_ARGB);
        out.setRGB(0, 0, img.getWidth(), img.getHeight(), img.getRGB(0, 0, img.getWidth(), img.getHeight(), null, 0, img.getWidth()), 0, img.getWidth());
        return out;
    }
}
