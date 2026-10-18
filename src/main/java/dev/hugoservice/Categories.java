package dev.hugoservice;

import net.minecraft.item.Item;
import net.minecraft.item.Items;

import java.util.Arrays;
import java.util.List;

/**
 * Every object you can mix. A category (sidebar entry) holds one or more groups;
 * each group gets its own style choice. Paths are relative to assets/minecraft/.
 */
public final class Categories {
    /** How the preview is drawn. */
    public enum Look { FLAT, WIDE, CUBE, CRYSTAL, SKY, KNIFE }

    /** Isometric cube faces; tint (RGB) multiplies the top, e.g. for grey grass. */
    public record Cube(String top, String side, Integer topTint, String sideOverlay) {}

    public record Group(String id, String name, String iconFile, Look look, Cube cube, List<String> files) {}

    public record Category(String id, String name, Item icon, String section, List<Group> groups) {}

    public static final List<String> SECTIONS = List.of("PvP", "World", "Interface");

    public static final List<Category> ALL = sorted(List.of(
        cat("crystal", "Crystal", Items.END_CRYSTAL,
            group("crystal", "Crystal", Look.CRYSTAL, null, "entity/end_crystal/end_crystal", "entity/end_crystal/end_crystal_beam")),
        cat("obsidian", "Obsidian", Items.OBSIDIAN,
            group("obsidian", "Obsidian", Look.CUBE, cube("block/obsidian", "block/obsidian"), "block/obsidian", "block/crying_obsidian")),
        cat("anchor", "Anchor", Items.RESPAWN_ANCHOR,
            group("anchor", "Anchors", Look.CUBE, cube("block/respawn_anchor_top", "block/respawn_anchor_side4"),
                "block/respawn_anchor_top", "block/respawn_anchor_top_off", "block/respawn_anchor_bottom",
                "block/respawn_anchor_side0", "block/respawn_anchor_side1", "block/respawn_anchor_side2",
                "block/respawn_anchor_side3", "block/respawn_anchor_side4"),
            group("glowstone", "Glowstone", Look.CUBE, cube("block/glowstone", "block/glowstone"), "block/glowstone")),
        cat("armor", "Armor", Items.NETHERITE_CHESTPLATE,
            group("armor", "Netherite", Look.FLAT, null,
                "item/netherite_chestplate", "item/netherite_helmet", "item/netherite_leggings", "item/netherite_boots",
                "entity/equipment/humanoid/netherite", "entity/equipment/humanoid_leggings/netherite"),
            group("armor_diamond", "Diamond", Look.FLAT, null,
                "item/diamond_chestplate", "item/diamond_helmet", "item/diamond_leggings", "item/diamond_boots",
                "entity/equipment/humanoid/diamond", "entity/equipment/humanoid_leggings/diamond")),
        cat("tools", "Tools", Items.NETHERITE_SWORD,
            group("tools", "Swords", Look.FLAT, null, "item/netherite_sword", "item/diamond_sword"),
            group("axes", "Axes", Look.FLAT, null, "item/netherite_axe", "item/diamond_axe"),
            group("pickaxes", "Pickaxes", Look.FLAT, null, "item/netherite_pickaxe", "item/diamond_pickaxe"),
            group("shovels", "Shovels", Look.FLAT, null, "item/netherite_shovel", "item/diamond_shovel"),
            group("hoes", "Hoes", Look.FLAT, null, "item/netherite_hoe", "item/diamond_hoe"),
            group("spears", "Spears", Look.FLAT, null, "item/netherite_spear", "item/diamond_spear")),
        cat("knives", "Knives", Items.IRON_SWORD,
            new Group(Knives.MODEL_GROUP, "Knife", "textures/item/iron_sword.png", Look.KNIFE, null, List.of()), // drawn by the mod, nothing in the pack
            new Group(Knives.FINISH_GROUP, "Finish", "textures/item/iron_sword.png", Look.KNIFE, null, List.of())),
        cat("gaps", "Gaps & Pearls", Items.GOLDEN_APPLE,
            group("gaps", "Gaps", Look.FLAT, null, "item/golden_apple"),
            group("pearls", "Pearls", Look.FLAT, null, "item/ender_pearl"),
            group("xp", "XP & Wind", Look.FLAT, null, "item/experience_bottle", "item/wind_charge")),
        cat("totem", "Totem", Items.TOTEM_OF_UNDYING,
            group("totem", "Totem", Look.FLAT, null, "item/totem_of_undying")),
        cat("hud", "HUD", Items.ITEM_FRAME,
            group("hud", "Hotbar & hearts", Look.WIDE, null, "gui/sprites/hud/hotbar", "gui/sprites/hud/hotbar_selection",
                "gui/sprites/hud/heart/full", "gui/sprites/hud/heart/half", "gui/sprites/hud/heart/container"),
            group("crosshair", "Crosshair", Look.FLAT, null, "gui/sprites/hud/crosshair")),
        cat("inventory", "Inventory", Items.CHEST,
            group("inventory", "Inventory", Look.WIDE, null, "gui/container/inventory")),
        cat("fire", "Fire", Items.FIRE_CHARGE,
            group("fire", "Fire", Look.FLAT, null, "block/fire_0", "block/fire_1")),
        cat("sky", "Sky", Items.CLOCK,
            skybox(),
            group("sky", "Sun", Look.FLAT, null, "environment/celestial/sun"),
            group("clouds", "Clouds", Look.FLAT, null, "environment/clouds")),
        cat("deepslate", "Deepslate", Items.DEEPSLATE,
            group("deepslate", "Deepslate", Look.CUBE, cube("block/deepslate_top", "block/deepslate"), "block/deepslate", "block/deepslate_top")),
        cat("stone", "Stone", Items.STONE,
            group("stone", "Stone", Look.CUBE, cube("block/stone", "block/stone"), "block/stone")),
        cat("snow", "Snow", Items.SNOW_BLOCK,
            group("snow", "Snow", Look.CUBE, cube("block/snow", "block/snow"), "block/snow")),
        cat("grass", "Grass", Items.GRASS_BLOCK,
            group("grass", "Grass", Look.CUBE, new Cube(tex("block/grass_block_top"), tex("block/grass_block_side"), 0x7CBD6B, tex("block/grass_block_side_overlay")),
                "block/grass_block_top", "block/grass_block_side", "block/grass_block_side_overlay", "colormap/grass")),
        cat("shield", "Shield", Items.SHIELD,
            group("shield", "Shield", Look.FLAT, null, "entity/shield_base_nopattern", "entity/shield_base")),
        cat("mace", "Mace", Items.MACE,
            group("mace", "Mace", Look.FLAT, null, "item/mace")),
        cat("cobweb", "Cobwebs", Items.COBWEB,
            group("cobweb", "Cobwebs", Look.FLAT, null, "block/cobweb"))
    ));

    /** Grey-world exceptions: label -> substrings of block texture names that keep their colour. */
    public static final List<KeepGroup> KEEP_GROUPS = List.of(
        new KeepGroup("Obsidian", "obsidian"),
        new KeepGroup("Anchor", "respawn_anchor"),
        new KeepGroup("Glowstone", "glowstone"),
        new KeepGroup("Fire", "fire_0", "fire_1", "soul_fire", "campfire"),
        new KeepGroup("Ores", "_ore", "raw_", "ancient_debris"),
        new KeepGroup("TNT", "tnt"),
        new KeepGroup("Cobweb", "cobweb"),
        new KeepGroup("Lava", "lava"),
        new KeepGroup("Water", "water"),
        new KeepGroup("Grass & leaves", "grass", "leaves", "vine", "fern", "moss")
    );

    public record KeepGroup(String name, String... needles) {
        public boolean matches(String fileName) {
            for (String n : needles) if (fileName.contains(n)) return true;
            return false;
        }
    }

    public static Category byId(String id) {
        for (Category c : ALL) if (c.id.equals(id)) return c;
        return ALL.getFirst();
    }

    public static List<Group> allGroups() {
        return ALL.stream().flatMap(c -> c.groups.stream()).toList();
    }

    private static Category cat(String id, String name, Item icon, Group... groups) {
        String section = switch (id) {
            case "grass", "stone", "deepslate", "snow", "fire", "sky" -> "World";
            case "hud", "inventory" -> "Interface";
            default -> "PvP";
        };
        return new Category(id, name, icon, section, List.of(groups));
    }

    /** PvP first (crystal, anchor, totem...), then world blocks, then interface. Stable inside a section. */
    private static List<Category> sorted(List<Category> raw) {
        return raw.stream().sorted(java.util.Comparator.comparingInt(c -> SECTIONS.indexOf(c.section()))).toList();
    }

    /** Skybox: OptiFine / MCPatcher sky textures (paths are not under textures/). Not written into packs. */
    private static Group skybox() {
        List<String> files = List.of("optifine/sky/world0/sky1.png", "optifine/sky/world0/sky0.png", "optifine/sky/world0/sky2.png",
            "mcpatcher/sky/world0/sky1.png", "mcpatcher/sky/world0/sky0.png");
        return new Group("skybox", "Skybox", files.getFirst(), Look.SKY, null, files);
    }

    private static Group group(String id, String name, Look look, Cube cube, String... files) {
        List<String> list = Arrays.stream(files).map(Categories::tex).toList();
        return new Group(id, name, list.getFirst(), look, cube, list);
    }

    private static Cube cube(String top, String side) { return new Cube(tex(top), tex(side), null, null); }

    private static String tex(String s) { return s.startsWith("textures/") ? s : "textures/" + s + ".png"; }

    private Categories() {}
}
