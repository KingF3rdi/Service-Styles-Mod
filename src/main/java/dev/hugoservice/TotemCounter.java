package dev.hugoservice;

import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.rendering.v1.hud.HudElementRegistry;
import net.fabricmc.fabric.api.client.rendering.v1.hud.VanillaHudElements;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.render.RenderTickCounter;
import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.text.Text;
import net.minecraft.util.Arm;
import net.minecraft.util.Identifier;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/** Totem counter: pops per player (over their heads and in chat) and your own totems next to the hotbar. */
public final class TotemCounter {
    private static final Map<UUID, Integer> POPS = new ConcurrentHashMap<>();
    private static final ItemStack TOTEM = new ItemStack(Items.TOTEM_OF_UNDYING);

    static void init() {
        HudElementRegistry.attachElementAfter(VanillaHudElements.HOTBAR, Identifier.of("hugoservice", "totems"), TotemCounter::hud);
        ClientTickEvents.END_CLIENT_TICK.register(TotemCounter::tick);
    }

    /** A totem went off on this entity (entity status 35, see ClientPlayNetworkHandlerMixin). */
    public static void popped(LivingEntity e) {
        if (!(e instanceof PlayerEntity p)) return;
        int n = POPS.merge(p.getUuid(), 1, Integer::sum);
        if (!Config.INSTANCE.totemChat) return;
        MinecraftClient.getInstance().inGameHud.getChatHud().addMessage(Text.empty().append(p.getDisplayName())
            .append(Text.literal(" popped " + n + (n == 1 ? " totem" : " totems")).withColor(popColor(n))));
    }

    /** Counts start over when a player dies or leaves. */
    private static void tick(MinecraftClient mc) {
        if (mc.world == null || mc.getNetworkHandler() == null) { POPS.clear(); return; }
        POPS.keySet().removeIf(id -> {
            PlayerEntity p = mc.world.getPlayerByUuid(id);
            return p != null ? p.isDead() : mc.getNetworkHandler().getPlayerListEntry(id) == null;
        });
    }

    /** " -3" after a player's name tag, or null. */
    public static Text suffix(Entity e) {
        if (!Config.INSTANCE.totemPops || !(e instanceof PlayerEntity)) return null;
        Integer n = POPS.get(e.getUuid());
        return n == null ? null : Text.literal(" -" + n).withColor(popColor(n));
    }

    private static int popColor(int n) {
        return n <= 2 ? 0xFFD84A : n <= 5 ? 0xFF9A3C : 0xFF4F4F;
    }

    /** Your totems (inventory and offhand) beside the hotbar, on the side away from the offhand slot. */
    private static void hud(DrawContext ctx, RenderTickCounter tick) {
        MinecraftClient mc = MinecraftClient.getInstance();
        if (!Config.INSTANCE.totemHud || mc.player == null || mc.options.hudHidden || mc.player.isSpectator()) return;
        int n = mc.player.getInventory().count(Items.TOTEM_OF_UNDYING);
        int cx = ctx.getScaledWindowWidth() / 2, y = ctx.getScaledWindowHeight() - 19;
        int x = mc.options.getMainArm().getValue() == Arm.RIGHT ? cx + 91 + 8 : cx - 91 - 8 - 16;
        ctx.drawItem(TOTEM, x, y);
        String s = String.valueOf(n);
        int color = n >= 4 ? 0x5CF07A : n >= 2 ? 0xFFD84A : n == 1 ? 0xFF9A3C : 0xFF4F4F;
        ctx.drawText(mc.textRenderer, s, x + 17 - mc.textRenderer.getWidth(s), y + 9, 0xFF000000 | color, true);
    }

    private TotemCounter() {}
}
