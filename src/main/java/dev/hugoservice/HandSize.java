/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  net.minecraft.item.ItemStack
 *  net.minecraft.item.Items
 *  net.minecraft.registry.tag.ItemTags
 */
package dev.hugoservice;

import dev.hugoservice.Config;
import java.util.List;
import java.util.function.Predicate;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.registry.Registries;
import net.minecraft.registry.tag.ItemTags;
import net.minecraft.util.Identifier;

public final class HandSize {
    public static final List<Entry> ITEMS = List.of(new Entry("totem", "Totem", s -> s.isOf(Items.TOTEM_OF_UNDYING)), new Entry("shield", "Shield", s -> s.isOf(Items.SHIELD)), new Entry("gap", "Gap", s -> s.isOf(Items.GOLDEN_APPLE) || s.isOf(Items.ENCHANTED_GOLDEN_APPLE)), new Entry("pearl", "Pearl", s -> s.isOf(Items.ENDER_PEARL)), new Entry("crystal", "Crystal", s -> s.isOf(Items.END_CRYSTAL)), new Entry("obsidian", "Obsidian", s -> s.isOf(Items.OBSIDIAN)), new Entry("anchor", "Anchor", s -> s.isOf(Items.RESPAWN_ANCHOR)), new Entry("glowstone", "Glowstone", s -> s.isOf(Items.GLOWSTONE)), new Entry("sword", "Sword", s -> s.isIn(ItemTags.SWORDS)), new Entry("axe", "Axe", s -> s.isIn(ItemTags.AXES)), new Entry("mace", "Mace", s -> s.isOf(Items.MACE)), new Entry("spear", "Spear", Mc::isSpear), new Entry("wind", "Wind charge", s -> s.isOf(Items.WIND_CHARGE)), new Entry("cobweb", "Cobweb", s -> s.isOf(Items.COBWEB)), new Entry("bow", "Bow", s -> s.isOf(Items.BOW) || s.isOf(Items.CROSSBOW)), new Entry("potion", "Potion", s -> s.isOf(Items.POTION) || s.isOf(Items.SPLASH_POTION) || s.isOf(Items.LINGERING_POTION)));

    public static int of(ItemStack stack) {
        if (Config.INSTANCE.handSize.isEmpty()) {
            return 100;
        }
        for (Entry e : ITEMS) {
            if (!e.matches().test(stack)) continue;
            return Config.INSTANCE.handSize.getOrDefault(e.id(), 100);
        }
        return 100;
    }

    public static ItemStack preview(String id) {
        if (id == null) {
            return ItemStack.EMPTY;
        }
        return switch (id) {
            case "totem" -> new ItemStack(Items.TOTEM_OF_UNDYING);
            case "shield" -> new ItemStack(Items.SHIELD);
            case "gap" -> new ItemStack(Items.GOLDEN_APPLE);
            case "pearl" -> new ItemStack(Items.ENDER_PEARL);
            case "crystal" -> new ItemStack(Items.END_CRYSTAL);
            case "obsidian" -> new ItemStack(Items.OBSIDIAN);
            case "anchor" -> new ItemStack(Items.RESPAWN_ANCHOR);
            case "glowstone" -> new ItemStack(Items.GLOWSTONE);
            case "sword" -> new ItemStack(Items.NETHERITE_SWORD);
            case "axe" -> new ItemStack(Items.NETHERITE_AXE);
            case "mace" -> new ItemStack(Items.MACE);
            case "spear" -> HandSize.spear();
            case "wind" -> new ItemStack(Items.WIND_CHARGE);
            case "cobweb" -> new ItemStack(Items.COBWEB);
            case "bow" -> new ItemStack(Items.BOW);
            case "potion" -> new ItemStack(Items.POTION);
            default -> ItemStack.EMPTY;
        };
    }

    private static ItemStack spear() {
        Item netherite = null;
        Item any = null;
        for (Item item : Registries.ITEM) {
            Identifier id = Registries.ITEM.getId(item);
            if (!id.getPath().contains("spear")) {
                continue;
            }
            if (id.getPath().equals("netherite_spear")) {
                netherite = item;
                break;
            }
            if (any == null) {
                any = item;
            }
        }
        if (netherite != null) {
            return new ItemStack(netherite);
        }
        if (any != null) {
            return new ItemStack(any);
        }
        return new ItemStack(Items.TRIDENT);
    }

    private HandSize() {
    }

    public record Entry(String id, String label, Predicate<ItemStack> matches) {
    }
}

