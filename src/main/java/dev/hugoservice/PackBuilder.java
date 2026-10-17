package dev.hugoservice;

import dev.hugoservice.Categories.Group;
import dev.hugoservice.Styles.Style;
import net.minecraft.SharedConstants;
import net.minecraft.client.MinecraftClient;
import net.minecraft.resource.ResourcePackManager;
import net.minecraft.resource.ResourceType;

import java.awt.image.BufferedImage;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.stream.Stream;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

/** Turns the current loadout into a resource pack (folder for "apply", zip for "save"). */
public final class PackBuilder {
    public static final String ACTIVE = "HugoService-Active";
    public static final String ACTIVE_ID = "file/" + ACTIVE;

    /** Collects all files of the mixed pack: zip path -> bytes. Safe to call off the render thread. */
    public static Map<String, byte[]> collect(Config cfg, String description) throws IOException {
        Map<String, byte[]> out = new LinkedHashMap<>();
        int format = SharedConstants.getGameVersion().packVersion(ResourceType.CLIENT_RESOURCES).major();
        String desc = description.replace("\\", "\\\\").replace("\"", "\\\"");
        out.put("pack.mcmeta", ("{\n  \"pack\": {\n    \"description\": \"" + desc + "\",\n    \"min_format\": " + format
            + ",\n    \"max_format\": " + format + "\n  }\n}\n").getBytes(StandardCharsets.UTF_8));
        out.put("pack.png", Images.encode(packIcon()));

        if (cfg.greyWorld) addGreyWorld(cfg, out);

        for (Group c : Categories.allGroups()) {
            if (c.look() == Categories.Look.SKY) continue; // the skybox is drawn by the mod, not by the pack
            Style style = Styles.find(c, cfg.styleOf(c.id()));
            Integer color = cfg.colorOf(c.id());
            if (style.vanilla && color == null) continue; // vanilla (or grey world) already covers it
            for (String file : c.files()) {
                byte[] png = style.read(file);
                if (png == null) continue;
                if (color != null) png = Images.recolor(file, png, color);
                out.put("assets/minecraft/" + file, png);
                byte[] meta = style.read(file + ".mcmeta");
                if (meta != null) out.put("assets/minecraft/" + file + ".mcmeta", meta);
                else out.remove("assets/minecraft/" + file + ".mcmeta");
            }
        }
        return out;
    }

    private static void addGreyWorld(Config cfg, Map<String, byte[]> out) {
        var keep = Categories.KEEP_GROUPS.stream().filter(g -> cfg.keepColour.contains(g.name())).toList();
        MinecraftClient.getInstance().getDefaultResourcePack().findResources(ResourceType.CLIENT_RESOURCES, "minecraft", "textures/block", (id, supplier) -> {
            String path = id.getPath();
            String name = path.substring(path.lastIndexOf('/') + 1);
            for (var g : keep) if (g.matches(name)) return;
            try (InputStream in = supplier.get()) {
                byte[] data = in.readAllBytes();
                if (path.endsWith(".png")) data = Images.transform(data, Images::grey);
                else if (!path.endsWith(".mcmeta")) return;
                out.put("assets/minecraft/" + path, data);
            } catch (Exception e) {
                HugoService.LOG.debug("grey world skipped {}", path, e);
            }
        });
    }

    private static BufferedImage packIcon() {
        BufferedImage img = new BufferedImage(64, 64, BufferedImage.TYPE_INT_ARGB);
        for (int y = 0; y < 64; y++) for (int x = 0; x < 64; x++) {
            int d = Math.abs(x - 32) + Math.abs(y - 32);
            int c = d < 22 ? (d < 12 ? 0xFF9FE6FF : 0xFF5BC0EB) : (0xFF0E1621 + ((y / 4) << 8));
            img.setRGB(x, y, c);
        }
        return img;
    }

    /** Writes the active folder pack, enables it on top and reloads resources. Call on the render thread. */
    public static void writeActiveAndReload(Map<String, byte[]> files) throws IOException {
        MinecraftClient mc = MinecraftClient.getInstance();
        Path dir = mc.getResourcePackDir().resolve(ACTIVE);
        deleteTree(dir);
        for (var e : files.entrySet()) {
            Path f = dir.resolve(e.getKey());
            Files.createDirectories(f.getParent());
            Files.write(f, e.getValue());
        }
        ResourcePackManager m = mc.getResourcePackManager();
        m.scanPacks();
        if (!m.getEnabledIds().contains(ACTIVE_ID)) {
            m.enable(ACTIVE_ID);
            mc.options.refreshResourcePacks(m); // list changed -> reloads
        } else {
            mc.reloadResources();
        }
    }

    public static void disableActive() {
        MinecraftClient mc = MinecraftClient.getInstance();
        ResourcePackManager m = mc.getResourcePackManager();
        m.scanPacks();
        if (m.getEnabledIds().contains(ACTIVE_ID)) {
            m.disable(ACTIVE_ID);
            mc.options.refreshResourcePacks(m);
        }
    }

    public static Path saveZip(Map<String, byte[]> files, String name) throws IOException {
        String clean = name.replaceAll("[\\\\/:*?\"<>|]", "").trim();
        if (clean.isEmpty()) clean = "My Mix";
        Path target = MinecraftClient.getInstance().getResourcePackDir().resolve(clean + ".zip");
        Files.createDirectories(target.getParent());
        try (OutputStream os = Files.newOutputStream(target); ZipOutputStream zip = new ZipOutputStream(os)) {
            for (var e : files.entrySet()) {
                zip.putNextEntry(new ZipEntry(e.getKey()));
                zip.write(e.getValue());
                zip.closeEntry();
            }
        }
        return target;
    }

    static void deleteTree(Path dir) throws IOException {
        if (!Files.exists(dir)) return;
        try (Stream<Path> s = Files.walk(dir)) {
            for (Path p : s.sorted(Comparator.reverseOrder()).toList()) Files.delete(p);
        }
    }

    private PackBuilder() {}
}
