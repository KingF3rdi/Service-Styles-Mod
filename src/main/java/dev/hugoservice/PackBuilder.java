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
        if (cfg.anchorGlow != null || cfg.anchorFullAtOne || cfg.anchorGlowPixels) addAnchor(cfg, out);
        if (cfg.glowWidth != 2) { // player outline thickness: the blur radius of the outline effect
            byte[] v = Styles.vanilla("post_effect/entity_outline.json");
            if (v != null) out.put("assets/minecraft/post_effect/entity_outline.json", new String(v, StandardCharsets.UTF_8)
                .replaceAll("(\"name\": \"Radius\",\\s*\"type\": \"float\",\\s*\"value\": )[0-9.]+", "$1" + Math.max(1, cfg.glowWidth) + ".0")
                .getBytes(StandardCharsets.UTF_8));
        }

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

    /**
     * Respawn anchor: the charge lights are the pixels that differ from the empty anchor (so it works with any pack).
     * They can get an own colour, look full from one charge on, and glow in the dark (an extra layer of just those
     * pixels with full light).
     */
    private static void addAnchor(Config cfg, Map<String, byte[]> out) throws IOException {
        String tex = "textures/block/respawn_anchor_";
        BufferedImage side0 = image(out, tex + "side0.png"), topOff = image(out, tex + "top_off.png");
        if (side0 == null || topOff == null) return;
        for (String n : new String[] {"side1", "side2", "side3", "side4", "top"}) {
            BufferedImage img = image(out, tex + n + ".png");
            if (img == null) continue;
            BufferedImage empty = n.equals("top") ? topOff : side0;
            BufferedImage glow = new BufferedImage(img.getWidth(), img.getHeight(), BufferedImage.TYPE_INT_ARGB);
            for (int y = 0; y < img.getHeight(); y++) for (int x = 0; x < img.getWidth(); x++) {
                int c = img.getRGB(x, y), e = empty.getRGB(x * empty.getWidth() / img.getWidth(), y % empty.getHeight());
                if (Math.abs((c >> 16 & 255) - (e >> 16 & 255)) + Math.abs((c >> 8 & 255) - (e >> 8 & 255)) + Math.abs((c & 255) - (e & 255)) < 30) continue;
                if (cfg.anchorGlow != null) {
                    float[] want = java.awt.Color.RGBtoHSB(cfg.anchorGlow >> 16 & 255, cfg.anchorGlow >> 8 & 255, cfg.anchorGlow & 255, null);
                    float[] has = java.awt.Color.RGBtoHSB(c >> 16 & 255, c >> 8 & 255, c & 255, null);
                    c = c & 0xFF000000 | java.awt.Color.HSBtoRGB(want[0], want[1], has[2]) & 0xFFFFFF;
                    img.setRGB(x, y, c);
                }
                glow.setRGB(x, y, c);
            }
            if (cfg.anchorGlow != null) out.put("assets/minecraft/" + tex + n + ".png", Images.encode(img));
            if (cfg.anchorGlowPixels) {
                out.put("assets/minecraft/textures/block/hugoservice_anchor_" + n + "_glow.png", Images.encode(glow));
                byte[] meta = out.get("assets/minecraft/" + tex + n + ".png.mcmeta");
                if (meta == null) meta = Styles.vanilla(tex + n + ".png.mcmeta");
                if (meta != null) out.put("assets/minecraft/textures/block/hugoservice_anchor_" + n + "_glow.png.mcmeta", meta);
            }
        }
        if (cfg.anchorGlowPixels) for (int n = 1; n <= 4; n++) out.put("assets/minecraft/models/block/respawn_anchor_" + n + ".json", anchorModel(n));
        if (cfg.anchorFullAtOne) {
            StringBuilder sb = new StringBuilder("{\"variants\": {\"charges=0\": {\"model\": \"minecraft:block/respawn_anchor_0\"}");
            for (int n = 1; n <= 4; n++) sb.append(", \"charges=").append(n).append("\": {\"model\": \"minecraft:block/respawn_anchor_4\"}");
            out.put("assets/minecraft/blockstates/respawn_anchor.json", sb.append("}}").toString().getBytes(StandardCharsets.UTF_8));
        }
    }

    /** Charged anchor: the normal cube, and over it the charge lights alone with full light (needs the cutout layer). */
    private static byte[] anchorModel(int n) {
        String sides = "\"north\": {\"texture\": \"#%1$s\", \"cullface\": \"north\"}, \"south\": {\"texture\": \"#%1$s\", \"cullface\": \"south\"}, "
            + "\"west\": {\"texture\": \"#%1$s\", \"cullface\": \"west\"}, \"east\": {\"texture\": \"#%1$s\", \"cullface\": \"east\"}";
        String json = "{\"parent\": \"minecraft:block/block\", \"textures\": {"
            + "\"particle\": \"minecraft:block/respawn_anchor_side" + n + "\", \"bottom\": \"minecraft:block/respawn_anchor_bottom\", "
            + "\"side\": \"minecraft:block/respawn_anchor_side" + n + "\", \"top\": \"minecraft:block/respawn_anchor_top\", "
            + "\"side_glow\": \"minecraft:block/hugoservice_anchor_side" + n + "_glow\", \"top_glow\": \"minecraft:block/hugoservice_anchor_top_glow\"}, "
            + "\"elements\": ["
            + "{\"from\": [0, 0, 0], \"to\": [16, 16, 16], \"faces\": {\"down\": {\"texture\": \"#bottom\", \"cullface\": \"down\"}, "
            + "\"up\": {\"texture\": \"#top\", \"cullface\": \"up\"}, " + String.format(sides, "side") + "}}, "
            + "{\"from\": [0, 0, 0], \"to\": [16, 16, 16], \"light_emission\": 15, \"shade\": false, \"faces\": {"
            + "\"up\": {\"texture\": \"#top_glow\", \"cullface\": \"up\"}, " + String.format(sides, "side_glow") + "}}]}";
        return json.getBytes(StandardCharsets.UTF_8);
    }

    /** A texture as the pack will have it: from the files collected so far, else vanilla. */
    private static BufferedImage image(Map<String, byte[]> out, String path) throws IOException {
        byte[] b = out.get("assets/minecraft/" + path);
        if (b == null) b = Styles.vanilla(path);
        return b == null ? null : Images.decode(b);
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
        CrystalCore.invalidate();
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

    /**
     * Up to 1.10 the knife lived in the active pack as block models over the swords; the mod draws it itself now.
     * Removes those files once, before the first resource load, so swords stay swords when no knife is chosen.
     */
    static void dropOldKnifeFiles() {
        Path dir = net.fabricmc.loader.api.FabricLoader.getInstance().getGameDir().resolve("resourcepacks").resolve(ACTIVE)
            .resolve("assets/minecraft");
        if (!Files.exists(dir.resolve("items/hugoservice_knife.json"))) return;
        try (Stream<Path> s = Files.walk(dir)) {
            for (Path p : s.filter(Files::isRegularFile).toList()) {
                String name = p.getFileName().toString(), parent = p.getParent().getFileName().toString();
                if (name.startsWith("hugoservice_") || parent.equals("item") && name.endsWith("_sword.json")) Files.delete(p);
            }
        } catch (IOException e) {
            HugoService.LOG.warn("Could not remove the old knife files from the HugoService pack", e);
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
