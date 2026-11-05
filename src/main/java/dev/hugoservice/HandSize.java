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
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.registry.tag.ItemTags;

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

    private HandSize() {
    }

    public record Entry(String id, String label, Predicate<ItemStack> matches) {
    }
}

