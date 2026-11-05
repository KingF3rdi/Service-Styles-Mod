/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  net.minecraft.item.Item
 *  net.minecraft.item.Items
 */
package dev.hugoservice;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.LinkedHashSet;
import java.util.List;
import net.minecraft.item.Item;
import net.minecraft.item.Items;

public final class Categories {
    public static final List<String> SECTIONS = List.of("PvP", "World", "Interface");
    public static final List<Category> ALL = new ArrayList<>(Categories.sorted(List.of(Categories.cat("crystal", "Crystal", Items.END_CRYSTAL, Categories.group("crystal", "Crystal", Look.CRYSTAL, null, "entity/end_crystal/end_crystal", "entity/end_crystal/end_crystal_beam")), Categories.cat("obsidian", "Obsidian", Items.OBSIDIAN, Categories.group("obsidian", "Obsidian", Look.CUBE, Categories.cube("block/obsidian", "block/obsidian"), "block/obsidian", "block/crying_obsidian")), Categories.cat("anchor", "Anchor", Items.RESPAWN_ANCHOR, Categories.group("anchor", "Anchors", Look.CUBE, Categories.cube("block/respawn_anchor_top", "block/respawn_anchor_side4"), "block/respawn_anchor_top", "block/respawn_anchor_top_off", "block/respawn_anchor_bottom", "block/respawn_anchor_side0", "block/respawn_anchor_side1", "block/respawn_anchor_side2", "block/respawn_anchor_side3", "block/respawn_anchor_side4"), Categories.group("glowstone", "Glowstone", Look.CUBE, Categories.cube("block/glowstone", "block/glowstone"), "block/glowstone")), Categories.cat("armor", "Armor", Items.NETHERITE_CHESTPLATE, Categories.group("armor", "Netherite", Look.FLAT, null, "item/netherite_chestplate", "item/netherite_helmet", "item/netherite_leggings", "item/netherite_boots", "entity/equipment/humanoid/netherite", "entity/equipment/humanoid_leggings/netherite"), Categories.group("armor_diamond", "Diamond", Look.FLAT, null, "item/diamond_chestplate", "item/diamond_helmet", "item/diamond_leggings", "item/diamond_boots", "entity/equipment/humanoid/diamond", "entity/equipment/humanoid_leggings/diamond")), Categories.cat("tools", "Tools", Items.NETHERITE_SWORD, Categories.group("tools", "Swords", Look.FLAT, null, "item/netherite_sword", "item/diamond_sword"), Categories.group("axes", "Axes", Look.FLAT, null, "item/netherite_axe", "item/diamond_axe"), Categories.group("pickaxes", "Pickaxes", Look.FLAT, null, "item/netherite_pickaxe", "item/diamond_pickaxe"), Categories.group("shovels", "Shovels", Look.FLAT, null, "item/netherite_shovel", "item/diamond_shovel"), Categories.group("hoes", "Hoes", Look.FLAT, null, "item/netherite_hoe", "item/diamond_hoe"), Categories.group("spears", "Spears", Look.FLAT, null, "item/netherite_spear", "item/diamond_spear", "item/golden_spear", "item/iron_spear", "item/copper_spear", "item/stone_spear", "item/wooden_spear", "item/netherite_spear_in_hand", "item/diamond_spear_in_hand", "item/golden_spear_in_hand", "item/iron_spear_in_hand")), Categories.cat("gaps", "Gaps & Pearls", Items.GOLDEN_APPLE, Categories.group("gaps", "Gaps", Look.FLAT, null, "item/golden_apple"), Categories.group("pearls", "Pearls", Look.FLAT, null, "item/ender_pearl"), Categories.group("xpbottle", "XP bottle", Look.FLAT, null, "item/experience_bottle"), Categories.group("windcharge", "Wind charge", Look.FLAT, null, "item/wind_charge", "entity/projectiles/wind_charge")), Categories.cat("totem", "Totem", Items.TOTEM_OF_UNDYING, Categories.group("totem", "Totem", Look.FLAT, null, "item/totem_of_undying")), Categories.cat("hud", "HUD", Items.ITEM_FRAME, Categories.group("hud", "Hotbar & hearts", Look.WIDE, null, "gui/sprites/hud/hotbar", "gui/sprites/hud/hotbar_selection", "gui/sprites/hud/heart/full", "gui/sprites/hud/heart/half", "gui/sprites/hud/heart/container"), Categories.group("crosshair", "Crosshair", Look.FLAT, null, "gui/sprites/hud/crosshair")), Categories.cat("inventory", "Inventory", Items.CHEST, Categories.group("inventory", "Inventory", Look.WIDE, null, "gui/container/inventory")), Categories.cat("fire", "Fire", Items.FIRE_CHARGE, Categories.group("fire", "Fire", Look.FLAT, null, "block/fire_0", "block/fire_1")), Categories.cat("sky", "Sky", Items.CLOCK, Categories.skybox(), Categories.group("sky", "Sun", Look.FLAT, null, "environment/celestial/sun"), Categories.group("clouds", "Clouds", Look.FLAT, null, "environment/clouds")), Categories.cat("deepslate", "Deepslate", Items.DEEPSLATE, Categories.group("deepslate", "Deepslate", Look.CUBE, Categories.cube("block/deepslate_top", "block/deepslate"), "block/deepslate", "block/deepslate_top")), Categories.cat("stone", "Stone", Items.STONE, Categories.group("stone", "Stone", Look.CUBE, Categories.cube("block/stone", "block/stone"), "block/stone")), Categories.cat("snow", "Snow", Items.SNOW_BLOCK, Categories.group("snow", "Snow", Look.CUBE, Categories.cube("block/snow", "block/snow"), "block/snow")), Categories.cat("grass", "Grass", Items.GRASS_BLOCK, Categories.group("grass", "Grass", Look.CUBE, new Cube(Categories.tex("block/grass_block_top"), Categories.tex("block/grass_block_side"), 8174955, Categories.tex("block/grass_block_side_overlay")), "block/grass_block_top", "block/grass_block_side", "block/grass_block_side_overlay", "colormap/grass")), Categories.cat("shield", "Shield", Items.SHIELD, Categories.group("shield", "Shield", Look.FLAT, null, "entity/shield_base_nopattern", "entity/shield_base", "item/shield")), Categories.cat("mace", "Mace", Items.MACE, Categories.group("mace", "Mace", Look.FLAT, null, "item/mace")), Categories.cat("cobweb", "Cobwebs", Items.COBWEB, Categories.group("cobweb", "Cobwebs", Look.FLAT, null, "block/cobweb")), Categories.cat("trident", "Trident", Items.TRIDENT, Categories.group("trident", "Trident", Look.FLAT, null, "item/trident", "entity/trident")), Categories.cat("rails", "Rails", Items.RAIL, Categories.group("rails", "Rails", Look.FLAT, null, "block/rail", "block/rail_corner", "block/powered_rail", "block/powered_rail_on", "block/detector_rail", "block/detector_rail_on", "block/activator_rail", "block/activator_rail_on")), Categories.cat("tntcart", "TNT Minecart", Items.TNT_MINECART, Categories.group("tntcart", "TNT Minecart", Look.FLAT, null, "item/tnt_minecart", "entity/minecart", "block/tnt_top", "block/tnt_bottom", "block/tnt_side")), Categories.cat("elytra", "Elytra", Items.ELYTRA, Categories.group("elytra", "Elytra", Look.FLAT, null, "item/elytra", "item/broken_elytra", "entity/equipment/wings/elytra")), Categories.cat("font", "Font", Items.BOOK, Categories.group("font", "Font", Look.WIDE, null, "font/ascii", "font/ascii_sga", "font/accented", "font/nonlatin_european")))));
    public static final List<KeepGroup> KEEP_GROUPS = List.of(new KeepGroup("Obsidian", "obsidian"), new KeepGroup("Anchor", "respawn_anchor"), new KeepGroup("Glowstone", "glowstone"), new KeepGroup("Fire", "fire_0", "fire_1", "soul_fire", "campfire"), new KeepGroup("Ores", "_ore", "raw_", "ancient_debris"), new KeepGroup("TNT", "tnt"), new KeepGroup("Cobweb", "cobweb"), new KeepGroup("Lava", "lava"), new KeepGroup("Water", "water"), new KeepGroup("Grass & leaves", "grass", "leaves", "vine", "fern", "moss"));

    public static Category byId(String id) {
        for (Category c : ALL) {
            if (!c.id.equals(id)) continue;
            return c;
        }
        return ALL.getFirst();
    }

    public static List<Group> allGroups() {
        return ALL.stream().flatMap(c -> c.groups.stream()).toList();
    }

    private static Category cat(String id, String name, Item icon, Group ... groups) {
        String section = switch (id) {
            case "grass", "stone", "deepslate", "snow", "fire", "sky", "rails" -> "World";
            case "hud", "inventory", "font" -> "Interface";
            default -> "PvP";
        };
        return new Category(id, name, icon, section, List.of(groups));
    }

    private static List<Category> sorted(List<Category> raw) {
        return raw.stream().sorted(Comparator.comparingInt(c -> SECTIONS.indexOf(c.section()))).toList();
    }

    private static Group skybox() {
        List<String> files = List.of("optifine/sky/world0/sky1.png", "optifine/sky/world0/sky0.png", "optifine/sky/world0/sky2.png", "mcpatcher/sky/world0/sky1.png", "mcpatcher/sky/world0/sky0.png");
        return new Group("skybox", "Skybox", files.getFirst(), Look.SKY, null, files);
    }

    private static Group group(String id, String name, Look look, Cube cube, String ... files) {
        List<String> list = Arrays.stream(files).map(Categories::tex).toList();
        return new Group(id, name, list.getFirst(), look, cube, list);
    }

    private static Cube cube(String top, String side) {
        return new Cube(Categories.tex(top), Categories.tex(side), null, null);
    }

    public static String tex(String s) {
        return s.startsWith("textures/") ? s : "textures/" + s + ".png";
    }


    public static Group groupById(String id) {
        for (Group g : allGroups()) {
            if (g.id.equals(id)) {
                return g;
            }
        }
        return null;
    }

    public static void unionFiles(String groupId, List<String> extra) {
        for (int i = 0; i < ALL.size(); ++i) {
            Category c = ALL.get(i);
            List<Group> gs = new ArrayList<>(c.groups());
            boolean changed = false;
            for (int j = 0; j < gs.size(); ++j) {
                Group g = gs.get(j);
                if (!g.id.equals(groupId)) continue;
                LinkedHashSet<String> files = new LinkedHashSet<>(g.files());
                for (String f : extra) {
                    if (f == null || f.isBlank() || f.contains("$")) {
                        continue;
                    }
                    files.add(f);
                }
                String icon = g.iconFile;
                if ((icon == null || icon.isEmpty()) && !files.isEmpty()) {
                    icon = files.iterator().next();
                }
                gs.set(j, new Group(g.id, g.name, icon, g.look, g.cube, List.copyOf(files)));
                changed = true;
            }
            if (changed) {
                ALL.set(i, new Category(c.id, c.name, c.icon, c.section, List.copyOf(gs)));
            }
        }
    }

    public static void appendCategory(Category c) {
        if (byIdOrNull(c.id) != null) {
            return;
        }
        ALL.add(c);
        ALL.sort(Comparator.comparingInt(cat -> SECTIONS.indexOf(cat.section())));
    }

    public static Category byIdOrNull(String id) {
        for (Category c : ALL) {
            if (c.id.equals(id)) return c;
        }
        return null;
    }

    private Categories() {
    }

    public record Category(String id, String name, Item icon, String section, List<Group> groups) {
    }

    public record Group(String id, String name, String iconFile, Look look, Cube cube, List<String> files) {
    }

    public static enum Look {
        FLAT,
        WIDE,
        CUBE,
        CRYSTAL,
        SKY;

    }

    public record Cube(String top, String side, Integer topTint, String sideOverlay) {
    }

    public record KeepGroup(String name, String... needles) {
        public boolean matches(String fileName) {
            for (String n : this.needles) {
                if (!fileName.contains(n)) continue;
                return true;
            }
            return false;
        }
    }
}

