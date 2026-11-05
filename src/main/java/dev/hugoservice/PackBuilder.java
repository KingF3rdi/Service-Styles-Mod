/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  net.fabricmc.loader.api.FabricLoader
 *  net.minecraft.SharedConstants
 *  net.minecraft.client.MinecraftClient
 *  net.minecraft.resource.ResourceType
 *  net.minecraft.resource.ResourcePackManager
 */
package dev.hugoservice;

import dev.hugoservice.Categories;
import dev.hugoservice.Config;
import dev.hugoservice.CrystalCore;
import dev.hugoservice.HugoService;
import dev.hugoservice.Images;
import dev.hugoservice.Styles;
import java.awt.Color;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.FileVisitOption;
import java.nio.file.Files;
import java.nio.file.LinkOption;
import java.nio.file.OpenOption;
import java.nio.file.Path;
import java.nio.file.attribute.FileAttribute;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.SharedConstants;
import net.minecraft.client.MinecraftClient;
import net.minecraft.resource.ResourceType;
import net.minecraft.resource.ResourcePackManager;

public final class PackBuilder {
    public static final String ACTIVE = "7auction Styles";
    public static final String ACTIVE_ID = "file/7auction Styles";
    static final String[] LEGACY_ACTIVE = {"HugoService-Active", "SkellyHole-Active", "SkellyHole Styles"};

    public static Map<String, byte[]> collect(Config cfg, String description) throws IOException {
        byte[] v;
        LinkedHashMap<String, byte[]> out = new LinkedHashMap<String, byte[]>();
        String desc = description == null || description.isBlank() ? "7auction Styles" : description;
        out.put("pack.mcmeta", PackBuilder.mcmeta(desc));
        out.put("pack.png", Images.encode(PackBuilder.packIcon()));
        if (cfg.greyWorld) {
            PackBuilder.addGreyWorld(cfg, out);
        }
        if (cfg.anchorGlow != null || cfg.anchorFullAtOne || cfg.anchorGlowPixels) {
            PackBuilder.addAnchor(cfg, out);
        }
        if (cfg.glowWidth != 2 && (v = Styles.vanilla("post_effect/entity_outline.json")) != null) {
            out.put("assets/minecraft/post_effect/entity_outline.json", new String(v, StandardCharsets.UTF_8).replaceAll("(\"name\": \"Radius\",\\s*\"type\": \"float\",\\s*\"value\": )[0-9.]+", "$1" + Math.max(1, cfg.glowWidth) + ".0").getBytes(StandardCharsets.UTF_8));
        }
        for (Categories.Group c : Categories.allGroups()) {
            if (c.look() == Categories.Look.SKY) continue;
            Styles.Style style = Styles.find(c, cfg.styleOf(c.id()));
            Integer color = cfg.colorOf(c.id());
            if (style.vanilla && color == null) continue;
            for (String file : c.files()) {
                byte[] png = style.read(file);
                if (png == null) continue;
                if (color != null) {
                    png = Images.recolor(file, png, color);
                }
                out.put("assets/minecraft/" + file, png);
                byte[] meta = style.read(file + ".mcmeta");
                if (meta != null) {
                    out.put("assets/minecraft/" + file + ".mcmeta", meta);
                    continue;
                }
                out.remove("assets/minecraft/" + file + ".mcmeta");
            }
        }
        return out;
    }

    static boolean isMixerPack(String nameOrId) {
        if (nameOrId == null) {
            return false;
        }
        if (nameOrId.equals(ACTIVE) || nameOrId.equals(ACTIVE_ID)) {
            return true;
        }
        for (String old : LEGACY_ACTIVE) {
            if (nameOrId.equals(old) || nameOrId.equals("file/" + old)) {
                return true;
            }
        }
        return false;
    }

    static byte[] mcmeta(String description) {
        int format = 75;
        try {
            format = Mc.packFormat();
        }
        catch (Throwable ignored) {
        }
        String desc = description.replace("\\", "\\\\").replace("\"", "\\\"");
        return ("{\n  \"pack\": {\n    \"description\": \"" + desc + "\",\n    \"min_format\": " + format + ",\n    \"max_format\": " + format + "\n  }\n}\n").getBytes(StandardCharsets.UTF_8);
    }

    private static void addAnchor(Config cfg, Map<String, byte[]> out) throws IOException {
        String tex = "textures/block/respawn_anchor_";
        BufferedImage side0 = PackBuilder.image(out, tex + "side0.png");
        BufferedImage topOff = PackBuilder.image(out, tex + "top_off.png");
        if (side0 == null || topOff == null) {
            return;
        }
        for (String n : new String[]{"side1", "side2", "side3", "side4", "top"}) {
            BufferedImage img = PackBuilder.image(out, tex + n + ".png");
            if (img == null) continue;
            BufferedImage empty = n.equals("top") ? topOff : side0;
            BufferedImage glow = new BufferedImage(img.getWidth(), img.getHeight(), 2);
            for (int y = 0; y < img.getHeight(); ++y) {
                for (int x = 0; x < img.getWidth(); ++x) {
                    int e;
                    int c = img.getRGB(x, y);
                    if (Math.abs((c >> 16 & 0xFF) - ((e = empty.getRGB(x * empty.getWidth() / img.getWidth(), y % empty.getHeight())) >> 16 & 0xFF)) + Math.abs((c >> 8 & 0xFF) - (e >> 8 & 0xFF)) + Math.abs((c & 0xFF) - (e & 0xFF)) < 30) continue;
                    if (cfg.anchorGlow != null) {
                        float[] want = Color.RGBtoHSB(cfg.anchorGlow >> 16 & 0xFF, cfg.anchorGlow >> 8 & 0xFF, cfg.anchorGlow & 0xFF, null);
                        float[] has = Color.RGBtoHSB(c >> 16 & 0xFF, c >> 8 & 0xFF, c & 0xFF, null);
                        c = c & 0xFF000000 | Color.HSBtoRGB(want[0], want[1], has[2]) & 0xFFFFFF;
                        img.setRGB(x, y, c);
                    }
                    glow.setRGB(x, y, c);
                }
            }
            if (cfg.anchorGlow != null) {
                out.put("assets/minecraft/" + tex + n + ".png", Images.encode(img));
            }
            if (!cfg.anchorGlowPixels) continue;
            out.put("assets/minecraft/textures/block/hugoservice_anchor_" + n + "_glow.png", Images.encode(glow));
            byte[] meta = out.get("assets/minecraft/" + tex + n + ".png.mcmeta");
            if (meta == null) {
                meta = Styles.vanilla(tex + n + ".png.mcmeta");
            }
            if (meta == null) continue;
            out.put("assets/minecraft/textures/block/hugoservice_anchor_" + n + "_glow.png.mcmeta", meta);
        }
        if (cfg.anchorGlowPixels) {
            for (int n = 1; n <= 4; ++n) {
                out.put("assets/minecraft/models/block/respawn_anchor_" + n + ".json", PackBuilder.anchorModel(n));
            }
        }
        if (cfg.anchorFullAtOne) {
            StringBuilder sb = new StringBuilder("{\"variants\": {\"charges=0\": {\"model\": \"minecraft:block/respawn_anchor_0\"}");
            for (int n = 1; n <= 4; ++n) {
                sb.append(", \"charges=").append(n).append("\": {\"model\": \"minecraft:block/respawn_anchor_4\"}");
            }
            out.put("assets/minecraft/blockstates/respawn_anchor.json", sb.append("}}").toString().getBytes(StandardCharsets.UTF_8));
        }
    }

    private static byte[] anchorModel(int n) {
        String sides = "\"north\": {\"texture\": \"#%1$s\", \"cullface\": \"north\"}, \"south\": {\"texture\": \"#%1$s\", \"cullface\": \"south\"}, \"west\": {\"texture\": \"#%1$s\", \"cullface\": \"west\"}, \"east\": {\"texture\": \"#%1$s\", \"cullface\": \"east\"}";
        String json = "{\"parent\": \"minecraft:block/block\", \"textures\": {\"particle\": \"minecraft:block/respawn_anchor_side" + n + "\", \"bottom\": \"minecraft:block/respawn_anchor_bottom\", \"side\": \"minecraft:block/respawn_anchor_side" + n + "\", \"top\": \"minecraft:block/respawn_anchor_top\", \"side_glow\": \"minecraft:block/hugoservice_anchor_side" + n + "_glow\", \"top_glow\": \"minecraft:block/hugoservice_anchor_top_glow\"}, \"elements\": [{\"from\": [0, 0, 0], \"to\": [16, 16, 16], \"faces\": {\"down\": {\"texture\": \"#bottom\", \"cullface\": \"down\"}, \"up\": {\"texture\": \"#top\", \"cullface\": \"up\"}, " + String.format(sides, "side") + "}}, {\"from\": [0, 0, 0], \"to\": [16, 16, 16], \"light_emission\": 15, \"shade\": false, \"faces\": {\"up\": {\"texture\": \"#top_glow\", \"cullface\": \"up\"}, " + String.format(sides, "side_glow") + "}}]}";
        return json.getBytes(StandardCharsets.UTF_8);
    }

    private static BufferedImage image(Map<String, byte[]> out, String path) throws IOException {
        byte[] b = out.get("assets/minecraft/" + path);
        if (b == null) {
            b = Styles.vanilla(path);
        }
        return b == null ? null : Images.decode(b);
    }

    private static void addGreyWorld(Config cfg, Map<String, byte[]> out) {
        List<Categories.KeepGroup> keep = Categories.KEEP_GROUPS.stream().filter(g -> cfg.keepColour.contains(g.name())).toList();
        MinecraftClient.getInstance().getDefaultResourcePack().findResources(ResourceType.CLIENT_RESOURCES, "minecraft", "textures/block", (id, supplier) -> {
            String path = id.getPath();
            String name = path.substring(path.lastIndexOf(47) + 1);
            for (Categories.KeepGroup g : keep) {
                if (!g.matches(name)) continue;
                return;
            }
            try (InputStream in = (InputStream)supplier.get();){
                byte[] data = in.readAllBytes();
                if (path.endsWith(".png")) {
                    data = Images.transform(data, Images::grey);
                } else if (!path.endsWith(".mcmeta")) {
                    return;
                }
                out.put("assets/minecraft/" + path, data);
            }
            catch (Exception e) {
                HugoService.LOG.debug("grey world skipped {}", path, e);
            }
        });
    }

    private static InputStream firstIconStream() {
        InputStream in = PackBuilder.class.getResourceAsStream("/assets/hugoservice/pack.png");
        if (in != null) {
            return in;
        }
        return PackBuilder.class.getResourceAsStream("/assets/hugoservice/icon.png");
    }

    private static BufferedImage packIcon() {
        try (InputStream in = firstIconStream()) {
            if (in != null) {
                BufferedImage src = Images.decode(in.readAllBytes());
                if (src != null) {
                    BufferedImage out = new BufferedImage(128, 128, BufferedImage.TYPE_INT_ARGB);
                    java.awt.Graphics2D g = out.createGraphics();
                    g.drawImage(src, 0, 0, 128, 128, null);
                    g.dispose();
                    return out;
                }
            }
        }
        catch (Exception ignored) {
        }
        return PackBuilder.fallbackIcon();
    }

    private static BufferedImage fallbackIcon() {
        BufferedImage img = new BufferedImage(128, 128, BufferedImage.TYPE_INT_ARGB);
        for (int y = 0; y < 128; ++y) {
            for (int x = 0; x < 128; ++x) {
                int c = 0xFF0B1A2E;
                int dx = x - 64;
                int dy = y - 64;
                if (dx * dx + dy * dy < 48 * 48) {
                    c = 0xFF1FA3A0;
                }
                img.setRGB(x, y, c);
            }
        }
        return img;
    }

    public static void writeActiveAndReload(Map<String, byte[]> files) throws IOException {
        CrystalCore.invalidate();
        MinecraftClient mc = MinecraftClient.getInstance();
        Path packs = mc.getResourcePackDir();
        for (String old : LEGACY_ACTIVE) {
            try {
                PackBuilder.deleteTree(packs.resolve(old));
            }
            catch (IOException ignored) {
            }
        }
        Path dir = packs.resolve(ACTIVE);
        PackBuilder.deleteTree(dir);
        for (Map.Entry<String, byte[]> e : files.entrySet()) {
            Path f = dir.resolve(e.getKey());
            Files.createDirectories(f.getParent(), new FileAttribute[0]);
            Files.write(f, e.getValue(), new OpenOption[0]);
        }
        ResourcePackManager m = mc.getResourcePackManager();
        m.scanPacks();
        if (!m.getEnabledIds().contains(ACTIVE_ID)) {
            m.enable(ACTIVE_ID);
            mc.options.refreshResourcePacks(m);
        } else {
            mc.reloadResources();
        }
    }

    static void dropOldKnifeFiles() {
        Path dir = FabricLoader.getInstance().getGameDir().resolve("resourcepacks").resolve(ACTIVE).resolve("assets/minecraft");
        if (!Files.exists(dir.resolve("items/hugoservice_knife.json"), new LinkOption[0])) {
            return;
        }
        try (Stream<Path> s = Files.walk(dir, new FileVisitOption[0]);){
            for (Path p : s.filter(x$0 -> Files.isRegularFile(x$0, new LinkOption[0])).toList()) {
                String name = p.getFileName().toString();
                String parent = p.getParent().getFileName().toString();
                if (!name.startsWith("hugoservice_") && (!parent.equals("item") || !name.endsWith("_sword.json"))) continue;
                Files.delete(p);
            }
        }
        catch (IOException e) {
                HugoService.LOG.warn("Could not remove the old knife files from the 7auction Styles pack", (Throwable)e);
        }
    }

    public static void disableActive() {
        MinecraftClient mc = MinecraftClient.getInstance();
        ResourcePackManager m = mc.getResourcePackManager();
        m.scanPacks();
        boolean changed = false;
        for (String id : List.copyOf(m.getEnabledIds())) {
            if (!PackBuilder.isMixerPack(id)) continue;
            m.disable(id);
            changed = true;
        }
        if (changed) {
            mc.options.refreshResourcePacks(m);
        }
    }

    public static Path saveZip(Map<String, byte[]> files, String name) throws IOException {
        String clean = name.replaceAll("[\\\\/:*?\"<>|]", "").trim();
        if (clean.isEmpty()) {
            clean = "My Mix";
        }
        return PackBuilder.writeZip(files, MinecraftClient.getInstance().getResourcePackDir().resolve(clean + ".zip"));
    }

    public static Path writeZip(Map<String, byte[]> files, Path target) throws IOException {
        Files.createDirectories(target.getParent(), new FileAttribute[0]);
        try (OutputStream os = Files.newOutputStream(target, new OpenOption[0]);
             ZipOutputStream zip = new ZipOutputStream(os);){
            for (Map.Entry<String, byte[]> e : files.entrySet()) {
                zip.putNextEntry(new ZipEntry(e.getKey()));
                zip.write(e.getValue());
                zip.closeEntry();
            }
        }
        return target;
    }

    static void deleteTree(Path dir) throws IOException {
        if (!Files.exists(dir, new LinkOption[0])) {
            return;
        }
        try (Stream<Path> s = Files.walk(dir, new FileVisitOption[0]);){
            for (Path p : s.sorted(Comparator.reverseOrder()).toList()) {
                Files.delete(p);
            }
        }
    }

    private PackBuilder() {
    }
}

