package dev.hugoservice;

import dev.hugoservice.Categories.Group;
import net.minecraft.client.MinecraftClient;
import net.minecraft.resource.InputSupplier;
import net.minecraft.resource.Resource;
import net.minecraft.resource.ResourceType;
import net.minecraft.util.Identifier;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Stream;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

/** Built-in presets (recoloured vanilla) and styles found in uploaded / installed resource packs. */
public final class Styles {
    public static final String VANILLA_ID = "vanilla";

    public interface Reader { byte[] read(String path) throws IOException; }

    public static final class Style {
        public final String id, name, desc;
        public final boolean vanilla, local;
        /** Preset colour, null for vanilla and packs. */
        public final Integer color;
        /** Pack file time for "Newest" sorting, 0 for built-ins. */
        public final long modified;
        private final Reader reader;

        Style(String id, String name, String desc, boolean vanilla, boolean local, Integer color, long modified, Reader reader) {
            this.id = id; this.name = name; this.desc = desc; this.vanilla = vanilla; this.local = local; this.color = color; this.modified = modified; this.reader = reader;
        }

        /** PNG bytes for a texture path, or null when this style does not provide it. */
        public byte[] read(String path) {
            try { return reader.read(path); } catch (Exception e) { return null; }
        }
    }

    private record Preset(String id, String name, int rgb) {}

    private static final List<Preset> PRESETS = List.of(
        new Preset("amethyst", "Amethyst", 0xB388FF),
        new Preset("rose", "Rose", 0xFF5CC8),
        new Preset("crimson", "Crimson", 0xFF4F4F),
        new Preset("amber", "Amber", 0xFFAD42),
        new Preset("mint", "Mint", 0x5CF07A),
        new Preset("aqua", "Aqua", 0x6CD3FF),
        new Preset("ocean", "Ocean", 0x4A86FF),
        new Preset("frost", "Frost", 0xE4F1FF),
        new Preset("mono", "Mono", 0xA0A0A0),
        new Preset("midnight", "Midnight", 0x3A4580)
    );

    // ---- vanilla ----

    public static byte[] vanilla(String path) {
        try {
            InputSupplier<InputStream> in = MinecraftClient.getInstance().getDefaultResourcePack()
                .open(ResourceType.CLIENT_RESOURCES, Identifier.ofVanilla(path));
            if (in == null) return null;
            try (InputStream s = in.get()) { return s.readAllBytes(); }
        } catch (Exception e) {
            return null;
        }
    }

    // ---- current (whatever your enabled resource packs show right now) ----

    public static final String CURRENT_ID = "current";

    /** Top-most texture from your enabled packs, ignoring our own generated pack. Null when vanilla. */
    public static byte[] current(String path) {
        Resource r = currentResource(path);
        if (r == null) return null;
        try (InputStream in = r.getInputStream()) { return in.readAllBytes(); } catch (Exception e) { return null; }
    }

    private static Resource currentResource(String path) {
        try {
            List<Resource> all = MinecraftClient.getInstance().getResourceManager().getAllResources(Identifier.ofVanilla(path));
            for (int i = all.size() - 1; i >= 0; i--) { // last = highest priority
                String pack = all.get(i).getPackId();
                if (PackBuilder.isMixerPack(pack)) continue;
                return pack.equals("vanilla") ? null : all.get(i);
            }
        } catch (Exception ignored) {}
        return null;
    }

    // ---- packs ----

    private record Pack(Path path, boolean zip, String name, long stamp, long modified, Map<String, String> entries) {
        byte[] read(String path) throws IOException {
            String entry = entries.get(path);
            if (entry == null) return null;
            if (!zip) return Files.readAllBytes(this.path.resolve(entry));
            try (ZipFile z = new ZipFile(this.path.toFile())) {
                ZipEntry e = z.getEntry(entry);
                if (e == null) return null;
                try (InputStream s = z.getInputStream(e)) { return s.readAllBytes(); }
            }
        }
    }

    private static final Map<Path, Pack> PACK_CACHE = new ConcurrentHashMap<>();
    private static volatile List<Pack> packs = List.of();
    private static final Set<String> WANTED = new HashSet<>();
    static {
        for (Group g : Categories.allGroups()) WANTED.addAll(g.files());
    }

    /** Old (1.8 – 1.21.1) names -> current names, so classic PvP packs work too. */
    private static final Map<String, String> LEGACY = Map.ofEntries(
        Map.entry("textures/entity/endercrystal/endercrystal.png", "textures/entity/end_crystal/end_crystal.png"),
        Map.entry("textures/entity/endercrystal/endercrystal_beam.png", "textures/entity/end_crystal/end_crystal_beam.png"),
        Map.entry("textures/item/apple_golden.png", "textures/item/golden_apple.png"),
        Map.entry("textures/item/totem.png", "textures/item/totem_of_undying.png"),
        Map.entry("textures/block/fire_layer_0.png", "textures/block/fire_0.png"),
        Map.entry("textures/block/fire_layer_1.png", "textures/block/fire_1.png"),
        Map.entry("textures/block/web.png", "textures/block/cobweb.png"),
        Map.entry("textures/block/grass_top.png", "textures/block/grass_block_top.png"),
        Map.entry("textures/block/grass_side.png", "textures/block/grass_block_side.png"),
        Map.entry("textures/block/grass_side_overlay.png", "textures/block/grass_block_side_overlay.png"),
        Map.entry("textures/environment/sun.png", "textures/environment/celestial/sun.png"),
        Map.entry("textures/models/armor/diamond_layer_1.png", "textures/entity/equipment/humanoid/diamond.png"),
        Map.entry("textures/models/armor/diamond_layer_2.png", "textures/entity/equipment/humanoid_leggings/diamond.png"),
        Map.entry("textures/models/armor/netherite_layer_1.png", "textures/entity/equipment/humanoid/netherite.png"),
        Map.entry("textures/models/armor/netherite_layer_2.png", "textures/entity/equipment/humanoid_leggings/netherite.png")
    );

    static String normalize(String entry) {
        String e = entry.replace('\\', '/');
        int i = e.indexOf("assets/minecraft/");
        if (i < 0) return null;
        String p = e.substring(i + "assets/minecraft/".length()).toLowerCase(java.util.Locale.ROOT);
        boolean meta = p.endsWith(".mcmeta");
        if (meta) p = p.substring(0, p.length() - ".mcmeta".length());
        p = p.replace("textures/blocks/", "textures/block/").replace("textures/items/", "textures/item/");
        p = LEGACY.getOrDefault(p, p);
        if (!WANTED.contains(p)) return null;
        return meta ? p + ".mcmeta" : p;
    }

    /** Re-reads both pack folders. Unchanged packs come from cache, so this is cheap. */
    public static void rescan() {
        List<Path> files = new ArrayList<>();
        list(Config.UPLOADS, files);
        list(MinecraftClient.getInstance().getResourcePackDir(), files);
        List<Pack> found = new ArrayList<>();
        Set<String> names = new HashSet<>();
        for (Path p : files) {
            String fileName = p.getFileName().toString();
            if (PackBuilder.isMixerPack(fileName)) continue;
            Pack pack = index(p);
            if (pack != null && !pack.entries.isEmpty() && names.add(pack.name.toLowerCase(java.util.Locale.ROOT))) found.add(pack);
        }
        found.sort((a, b) -> a.name.compareToIgnoreCase(b.name));
        packs = List.copyOf(found);
    }

    private static void list(Path dir, List<Path> out) {
        if (!Files.isDirectory(dir)) return;
        try (Stream<Path> s = Files.list(dir)) {
            s.filter(p -> Files.isDirectory(p) || p.toString().toLowerCase(java.util.Locale.ROOT).endsWith(".zip")).forEach(out::add);
        } catch (IOException ignored) {}
    }

    private static Pack index(Path p) {
        try {
            boolean zip = !Files.isDirectory(p);
            long stamp = zip ? Files.getLastModifiedTime(p).toMillis() ^ Files.size(p) : Files.getLastModifiedTime(p).toMillis();
            Pack cached = PACK_CACHE.get(p);
            if (cached != null && cached.stamp == stamp && zip) return cached;

            Map<String, String> entries = new HashMap<>();
            if (zip) {
                try (ZipFile z = new ZipFile(p.toFile())) {
                    Enumeration<? extends ZipEntry> en = z.entries();
                    while (en.hasMoreElements()) {
                        ZipEntry e = en.nextElement();
                        if (e.isDirectory()) continue;
                        String n = normalize(e.getName());
                        if (n != null) entries.putIfAbsent(n, e.getName());
                    }
                }
            } else {
                Path assets = p.resolve("assets").resolve("minecraft").resolve("textures");
                if (Files.isDirectory(assets)) {
                    try (Stream<Path> s = Files.walk(assets)) {
                        s.filter(Files::isRegularFile).forEach(f -> {
                            String rel = p.relativize(f).toString().replace('\\', '/');
                            String n = normalize(rel);
                            if (n != null) entries.putIfAbsent(n, rel);
                        });
                    }
                }
            }
            String name = p.getFileName().toString().replaceAll("(?i)\\.zip$", "").replaceAll("§.", "").replace('_', ' ').trim();
            Pack pack = new Pack(p, zip, name.isEmpty() ? "Pack" : name, stamp, Files.getLastModifiedTime(p).toMillis(), Map.copyOf(entries));
            PACK_CACHE.put(p, pack);
            return pack;
        } catch (Exception e) {
            HugoService.LOG.warn("Skipping unreadable pack {}", p, e);
            return null;
        }
    }

    // ---- styles per category ----

    public static List<Style> forGroup(Group c) {
        List<Style> out = new ArrayList<>();
        out.add(new Style(VANILLA_ID, "Vanilla", "Default look", true, false, null, 0, Styles::vanilla));
        int overridden = 0;
        StringBuilder sources = new StringBuilder();
        for (String f : c.files()) {
            Resource r = currentResource(f);
            if (r != null) { overridden++; sources.append(r.getPackId()).append(';'); }
        }
        // "modified" = which packs supply it, so the cached preview refreshes when you switch packs
        if (overridden > 0) out.add(new Style(CURRENT_ID, "Current", "Your active packs · " + overridden + "/" + c.files().size() + " textures",
            false, false, null, sources.toString().hashCode(), Styles::current));
        if (c.id().equals("crosshair")) out.add(new Style("painted", "Painted", "Your own pixels", false, false, null, 0, path -> {
            if (!path.endsWith("crosshair.png")) return null;
            int[] px = Config.INSTANCE.crosshair.clone();
            java.awt.image.BufferedImage img = new java.awt.image.BufferedImage(15, 15, java.awt.image.BufferedImage.TYPE_INT_ARGB);
            img.setRGB(0, 0, 15, 15, px, 0, 15);
            return Images.encode(img);
        }));
        boolean sky = c.look() == Categories.Look.SKY, knife = Knives.isKnifeGroup(c);
        if (sky) out.addAll(skyImages());
        if (knife) out.addAll(Knives.styles(c));
        for (Preset pr : sky || knife ? List.<Preset>of() : PRESETS) {
            out.add(new Style("preset:" + pr.id, pr.name, "Built-in preset", false, false, pr.rgb, 0, path -> {
                byte[] v = vanilla(path);
                if (v == null) return null;
                return path.endsWith(".png") ? Images.recolor(path, v, pr.rgb) : v;
            }));
        }
        for (Pack pack : packs) {
            int n = 0;
            for (String f : c.files()) if (pack.entries.containsKey(f)) n++;
            if (n == 0) continue;
            out.add(new Style("pack:" + pack.path.getFileName(), pack.name,
                sky ? "Sky from pack" : "From pack · " + n + "/" + c.files().size() + " textures", false, true, null, pack.modified, pack::read));
        }
        return out;
    }

    /** Your own sky images in config/hugoservice/skies (3:2 cube layout or 360° panorama). */
    private static List<Style> skyImages() {
        List<Style> out = new ArrayList<>();
        if (!Files.isDirectory(Config.SKIES)) return out;
        try (Stream<Path> s = Files.list(Config.SKIES)) {
            for (Path p : s.sorted().toList()) {
                String name = p.getFileName().toString();
                if (!name.toLowerCase(java.util.Locale.ROOT).matches(".*\\.(png|jpe?g)$")) continue;
                long modified = Files.getLastModifiedTime(p).toMillis();
                out.add(new Style("skyimg:" + name, name.replaceAll("\\.[^.]+$", ""), "Your image", false, true, null, modified,
                    path -> Files.readAllBytes(p)));
            }
        } catch (IOException ignored) {}
        return out;
    }

    public static Style find(Group c, String id) {
        for (Style s : forGroup(c)) if (s.id.equals(id)) return s;
        return forGroup(c).getFirst();
    }

    private Styles() {}
}
