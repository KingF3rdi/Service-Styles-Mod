package dev.hugoservice;

import net.minecraft.item.ItemStack;

public final class HandPreview {
    private static volatile ItemStack stack;

    private HandPreview() {
    }

    public static ItemStack stack() {
        return stack;
    }

    public static void show(ItemStack item) {
        stack = item;
    }

    public static void clear() {
        stack = null;
    }
}
