/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  net.minecraft.util.Identifier
 *  net.minecraft.client.MinecraftClient
 *  net.minecraft.resource.ResourceType
 *  net.minecraft.resource.Resource
 *  net.minecraft.resource.InputSupplier
 */
package dev.hugoservice;

import dev.hugoservice.Categories;
import dev.hugoservice.Config;
import dev.hugoservice.HugoService;
import dev.hugoservice.Images;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.FileVisitOption;
import java.nio.file.Files;
import java.nio.file.LinkOption;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Stream;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;
import net.minecraft.util.Identifier;
import net.minecraft.client.MinecraftClient;
import net.minecraft.resource.ResourceType;
import net.minecraft.resource.Resource;
import net.minecraft.resource.InputSupplier;

public final class Styles {
    public static final String VANILLA_ID = "vanilla";
    private static final List<Preset> PRESETS = List.of(new Preset("amethyst", "Amethyst", 11766015), new Preset("rose", "Rose", 16735432), new Preset("crimson", "Crimson", 0xFF4F4F), new Preset("amber", "Amber", 16756034), new Preset("mint", "Mint", 6090874), new Preset("aqua", "Aqua", 7132159), new Preset("ocean", "Ocean", 4884223), new Preset("frost", "Frost", 15004159), new Preset("mono", "Mono", 0xA0A0A0), new Preset("midnight", "Midnight", 3818880));
    public static final String CURRENT_ID = "current";
    private static final Map<Path, Pack> PACK_CACHE = new ConcurrentHashMap<Path, Pack>();
    private static volatile List<Pack> packs = List.of();
    private static final Set<String> WANTED = new HashSet<String>();
    private static final Map<String, String> LEGACY;

    public static byte[] vanilla(String path) {
        byte[] byArray;
        block9: {
            InputSupplier in = MinecraftClient.getInstance().getDefaultResourcePack().open(ResourceType.CLIENT_RESOURCES, Identifier.ofVanilla((String)path));
            if (in == null) {
                return null;
            }
            InputStream s;
            try {
                s = (InputStream) in.get();
            } catch (Exception e) {
                return null;
            }
            try {
                byArray = s.readAllBytes();
                if (s == null) break block9;
            }
            catch (Throwable throwable) {
                try {
                    if (s != null) {
                        try {
                            s.close();
                        }
                        catch (Throwable throwable2) {
                            throwable.addSuppressed(throwable2);
                        }
                    }
                    throw throwable;
                }
                catch (Exception e) {
                    return null;
                }
            }
            try { s.close(); } catch (Exception ignored) {}
        }
        return byArray;
    }

    public static byte[] current(String path) {
        byte[] byArray;
        block9: {
            Resource r = Styles.currentResource(path);
            if (r == null) {
                return null;
            }
            InputStream in;
            try {
                in = r.getInputStream();
            } catch (Exception e) {
                return null;
            }
            try {
                byArray = in.readAllBytes();
                if (in == null) break block9;
            }
            catch (Throwable throwable) {
                try {
                    if (in != null) {
                        try {
                            in.close();
                        }
                        catch (Throwable throwable2) {
                            throwable.addSuppressed(throwable2);
                        }
                    }
                    throw throwable;
                }
                catch (Exception e) {
                    return null;
                }
            }
            try { in.close(); } catch (Exception ignored) {}
        }
        return byArray;
    }

    private static Resource currentResource(String path) {
        try {
            List all = MinecraftClient.getInstance().getResourceManager().getAllResources(Identifier.ofVanilla((String)path));
            for (int i = all.size() - 1; i >= 0; --i) {
                String pack = ((Resource)all.get(i)).getPackId();
                if (PackBuilder.isMixerPack(pack)) continue;
                return pack.equals(VANILLA_ID) ? null : (Resource)all.get(i);
            }
        }
        catch (Exception exception) {
            // empty catch block
        }
        return null;
    }

    static String normalize(String entry) {
        String e = entry.replace('\\', '/');
        int i = e.indexOf("assets/minecraft/");
        if (i < 0) {
            return null;
        }
        String p = e.substring(i + "assets/minecraft/".length()).toLowerCase(Locale.ROOT);
        boolean meta = p.endsWith(".mcmeta");
        if (meta) {
            p = p.substring(0, p.length() - ".mcmeta".length());
        }
        p = p.replace("textures/blocks/", "textures/block/").replace("textures/items/", "textures/item/");
        if (!WANTED.contains(p = LEGACY.getOrDefault(p, p))) {
            return null;
        }
        return meta ? p + ".mcmeta" : p;
    }

    public static void rescan() {
        ArrayList<Path> files = new ArrayList<Path>();
        Styles.list(Config.UPLOADS, files);
        Styles.list(MinecraftClient.getInstance().getResourcePackDir(), files);
        ArrayList<Pack> found = new ArrayList<Pack>();
        HashSet<String> names = new HashSet<String>();
        for (Path p : files) {
            Pack pack;
            String fileName = p.getFileName().toString();
            if (PackBuilder.isMixerPack(fileName) || (pack = Styles.index(p)) == null || pack.entries.isEmpty() || !names.add(pack.name.toLowerCase(Locale.ROOT))) continue;
            found.add(pack);
        }
        found.sort((a, b) -> a.name.compareToIgnoreCase(b.name));
        packs = List.copyOf(found);
    }

    private static void list(Path dir, List<Path> out) {
        if (!Files.isDirectory(dir, new LinkOption[0])) {
            return;
        }
        try (Stream<Path> s = Files.list(dir);){
            s.filter(p -> Files.isDirectory(p, new LinkOption[0]) || p.toString().toLowerCase(Locale.ROOT).endsWith(".zip")).forEach(out::add);
        }
        catch (IOException iOException) {
            // empty catch block
        }
    }

    private static Pack index(Path p) {
        try {
            String name;
            boolean zip = !Files.isDirectory(p, new LinkOption[0]);
            long stamp = zip ? Files.getLastModifiedTime(p, new LinkOption[0]).toMillis() ^ Files.size(p) : Files.getLastModifiedTime(p, new LinkOption[0]).toMillis();
            Pack cached = PACK_CACHE.get(p);
            if (cached != null && cached.stamp == stamp && zip) {
                return cached;
            }
            HashMap<String, String> entries = new HashMap<String, String>();
            if (zip) {
                try (ZipFile z = new ZipFile(p.toFile());){
                    Enumeration<? extends ZipEntry> en = z.entries();
                    while (en.hasMoreElements()) {
                        String n;
                        ZipEntry e = en.nextElement();
                        if (e.isDirectory() || (n = Styles.normalize(e.getName())) == null) continue;
                        entries.putIfAbsent(n, e.getName());
                    }
                }
            }
            Path assets = p.resolve("assets").resolve("minecraft").resolve("textures");
            if (Files.isDirectory(assets, new LinkOption[0])) {
                try (Stream<Path> s = Files.walk(assets, new FileVisitOption[0]);){
                    s.filter(x$0 -> Files.isRegularFile(x$0, new LinkOption[0])).forEach(f -> {
                        String rel = p.relativize((Path)f).toString().replace('\\', '/');
                        String n = Styles.normalize(rel);
                        if (n != null) {
                            entries.putIfAbsent(n, rel);
                        }
                    });
                }
            }
            Pack pack = new Pack(p, zip, (name = p.getFileName().toString().replaceAll("(?i)\\.zip$", "").replaceAll("\u00a7.", "").replace('_', ' ').trim()).isEmpty() ? "Pack" : name, stamp, Files.getLastModifiedTime(p, new LinkOption[0]).toMillis(), Map.copyOf(entries));
            PACK_CACHE.put(p, pack);
            return pack;
        }
        catch (Exception e) {
            HugoService.LOG.warn("Skipping unreadable pack {}", p, e);
            return null;
        }
    }

    public static List<Style> forGroup(Categories.Group c) {
        ArrayList<Style> out = new ArrayList<Style>();
        out.add(new Style(VANILLA_ID, "Vanilla", "Default look", true, false, null, 0L, Styles::vanilla));
        int overridden = 0;
        StringBuilder sources = new StringBuilder();
        for (String f : c.files()) {
            Resource r = Styles.currentResource(f);
            if (r == null) continue;
            ++overridden;
            sources.append(r.getPackId()).append(';');
        }
        if (overridden > 0) {
            out.add(new Style(CURRENT_ID, "Current", "Your active packs \u00b7 " + overridden + "/" + c.files().size() + " textures", false, false, null, sources.toString().hashCode(), Styles::current));
        }
        if (c.id().equals("crosshair")) {
            out.add(new Style("painted", "Painted", "Your own pixels", false, false, null, 0L, path -> {
                if (!path.endsWith("crosshair.png")) {
                    return null;
                }
                int[] px = (int[])Config.INSTANCE.crosshair.clone();
                BufferedImage img = new BufferedImage(15, 15, 2);
                img.setRGB(0, 0, 15, 15, px, 0, 15);
                return Images.encode(img);
            }));
        }
        boolean sky = c.look() == Categories.Look.SKY;
        boolean font = c.id().equals("font");
        if (sky) {
            out.addAll(Styles.skyImages());
        }
        String cloudCat = c.id().equals("spears") ? "spear" : c.id();
        for (StylesCloud.Style cloud : StylesCloud.of(cloudCat)) {
            StylesCloud.Style cs = cloud;
            out.add(new Style("cloud:" + cs.id(), cs.name(), StylesCloud.isReady(cs) ? "Cloud pack" : "Cloud · download on select", false, false, null, StylesCloud.stamp(cs.id()), path -> StylesCloud.read(cs, path)));
        }
        for (Preset preset : sky || font ? List.<Preset>of() : PRESETS) {
            out.add(new Style("preset:" + preset.id, preset.name, "Built-in preset", false, false, preset.rgb, 0L, path -> {
                byte[] v = Styles.vanilla(path);
                if (v == null) {
                    return null;
                }
                return path.endsWith(".png") ? Images.recolor(path, v, preset.rgb) : v;
            }));
        }
        for (Pack pack : packs) {
            int n = 0;
            for (String f : c.files()) {
                if (!pack.entries.containsKey(f)) continue;
                ++n;
            }
            if (n == 0) continue;
            out.add(new Style("pack:" + String.valueOf(pack.path.getFileName()), pack.name, (String)(sky ? "Sky from pack" : "From pack \u00b7 " + n + "/" + c.files().size() + " textures"), false, true, null, pack.modified, pack::read));
        }
        return out;
    }

    private static List<Style> skyImages() {
        ArrayList<Style> out = new ArrayList<Style>();
        if (!Files.isDirectory(Config.SKIES, new LinkOption[0])) {
            return out;
        }
        try (Stream<Path> s = Files.list(Config.SKIES);){
            for (Path p : s.sorted().toList()) {
                String name = p.getFileName().toString();
                if (!name.toLowerCase(Locale.ROOT).matches(".*\\.(png|jpe?g)$")) continue;
                long modified = Files.getLastModifiedTime(p, new LinkOption[0]).toMillis();
                out.add(new Style("skyimg:" + name, name.replaceAll("\\.[^.]+$", ""), "Your image", false, true, null, modified, path -> Files.readAllBytes(p)));
            }
        }
        catch (IOException iOException) {
            // empty catch block
        }
        return out;
    }

    public static Style find(Categories.Group c, String id) {
        for (Style s : Styles.forGroup(c)) {
            if (!s.id.equals(id)) continue;
            return s;
        }
        return Styles.forGroup(c).getFirst();
    }

    private Styles() {
    }

    public static void rebuildWanted() {
        WANTED.clear();
        for (Categories.Group g : Categories.allGroups()) {
            WANTED.addAll(g.files());
        }
    }

    static {
        rebuildWanted();
        LEGACY = Map.ofEntries(Map.entry("textures/entity/endercrystal/endercrystal.png", "textures/entity/end_crystal/end_crystal.png"), Map.entry("textures/entity/endercrystal/endercrystal_beam.png", "textures/entity/end_crystal/end_crystal_beam.png"), Map.entry("textures/item/apple_golden.png", "textures/item/golden_apple.png"), Map.entry("textures/item/totem.png", "textures/item/totem_of_undying.png"), Map.entry("textures/block/fire_layer_0.png", "textures/block/fire_0.png"), Map.entry("textures/block/fire_layer_1.png", "textures/block/fire_1.png"), Map.entry("textures/block/web.png", "textures/block/cobweb.png"), Map.entry("textures/block/grass_top.png", "textures/block/grass_block_top.png"), Map.entry("textures/block/grass_side.png", "textures/block/grass_block_side.png"), Map.entry("textures/block/grass_side_overlay.png", "textures/block/grass_block_side_overlay.png"), Map.entry("textures/environment/sun.png", "textures/environment/celestial/sun.png"), Map.entry("textures/models/armor/diamond_layer_1.png", "textures/entity/equipment/humanoid/diamond.png"), Map.entry("textures/models/armor/diamond_layer_2.png", "textures/entity/equipment/humanoid_leggings/diamond.png"), Map.entry("textures/models/armor/netherite_layer_1.png", "textures/entity/equipment/humanoid/netherite.png"), Map.entry("textures/models/armor/netherite_layer_2.png", "textures/entity/equipment/humanoid_leggings/netherite.png"));
    }

    private record Pack(Path path, boolean zip, String name, long stamp, long modified, Map<String, String> entries) {
        byte[] read(String path) throws IOException {
            String entry = this.entries.get(path);
            if (entry == null) {
                return null;
            }
            if (!this.zip) {
                return Files.readAllBytes(this.path.resolve(entry));
            }
            try (ZipFile z = new ZipFile(this.path.toFile());){
                byte[] byArray;
                block15: {
                    ZipEntry e = z.getEntry(entry);
                    if (e == null) {
                        byte[] byArray2 = null;
                        return byArray2;
                    }
                    InputStream s = z.getInputStream(e);
                    try {
                        byArray = s.readAllBytes();
                        if (s == null) break block15;
                    }
                    catch (Throwable throwable) {
                        if (s != null) {
                            try {
                                s.close();
                            }
                            catch (Throwable throwable2) {
                                throwable.addSuppressed(throwable2);
                            }
                        }
                        throw throwable;
                    }
                    s.close();
                }
                return byArray;
            }
        }
    }

    public static final class Style {
        public final String id;
        public final String name;
        public final String desc;
        public final boolean vanilla;
        public final boolean local;
        public final Integer color;
        public final long modified;
        private final Reader reader;

        Style(String id, String name, String desc, boolean vanilla, boolean local, Integer color, long modified, Reader reader) {
            this.id = id;
            this.name = name;
            this.desc = desc;
            this.vanilla = vanilla;
            this.local = local;
            this.color = color;
            this.modified = modified;
            this.reader = reader;
        }

        public byte[] read(String path) {
            try {
                return this.reader.read(path);
            }
            catch (Exception e) {
                return null;
            }
        }
    }

    public static interface Reader {
        public byte[] read(String var1) throws IOException;
    }

    private record Preset(String id, String name, int rgb) {
    }
}

