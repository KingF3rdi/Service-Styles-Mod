/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents
 *  net.fabricmc.fabric.api.client.rendering.v1.hud.HudElementRegistry
 *  net.fabricmc.fabric.api.client.rendering.v1.hud.VanillaHudElements
 *  net.minecraft.entity.Entity
 *  net.minecraft.util.Arm
 *  net.minecraft.entity.LivingEntity
 *  net.minecraft.entity.player.PlayerEntity
 *  net.minecraft.item.ItemStack
 *  net.minecraft.item.Items
 *  net.minecraft.item.ItemConvertible
 *  net.minecraft.particle.ParticleEffect
 *  net.minecraft.particle.ParticleTypes
 *  net.minecraft.text.Text
 *  net.minecraft.util.Identifier
 *  net.minecraft.client.MinecraftClient
 *  net.minecraft.client.gui.DrawContext
 *  net.minecraft.sound.SoundEvents
 *  net.minecraft.sound.SoundCategory
 *  net.minecraft.client.render.RenderTickCounter
 */
package dev.hugoservice;

import dev.hugoservice.Config;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.rendering.v1.hud.HudElementRegistry;
import net.fabricmc.fabric.api.client.rendering.v1.hud.VanillaHudElements;
import net.minecraft.entity.Entity;
import net.minecraft.util.Arm;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.item.ItemConvertible;
import net.minecraft.particle.ParticleEffect;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.sound.SoundEvents;
import net.minecraft.sound.SoundCategory;
import net.minecraft.client.render.RenderTickCounter;

public final class TotemCounter {
    private static final Map<UUID, Integer> POPS = new ConcurrentHashMap<UUID, Integer>();
    private static final ItemStack TOTEM = new ItemStack((ItemConvertible)Items.TOTEM_OF_UNDYING);

    static void init() {
        HudElementRegistry.attachElementAfter((Identifier)VanillaHudElements.HOTBAR, (Identifier)Identifier.of((String)"hugoservice", (String)"totems"), TotemCounter::hud);
        ClientTickEvents.END_CLIENT_TICK.register(TotemCounter::tick);
    }

    public static void popped(LivingEntity e) {
        if (!(e instanceof PlayerEntity)) {
            return;
        }
        PlayerEntity p = (PlayerEntity)e;
        int n = POPS.merge(p.getUuid(), 1, Integer::sum);
        if (!Config.INSTANCE.totemChat) {
            return;
        }
        MinecraftClient.getInstance().inGameHud.getChatHud().addMessage((Text)Text.empty().append(p.getDisplayName()).append((Text)Text.literal((String)(" popped " + n + (n == 1 ? " totem" : " totems"))).withColor(TotemCounter.popColor(n))));
    }

    public static void testPop() {
        MinecraftClient mc = MinecraftClient.getInstance();
        if (mc.player == null || mc.world == null) {
            return;
        }
        mc.gameRenderer.showFloatingItem(TOTEM);
        mc.particleManager.addEmitter((Entity)mc.player, (ParticleEffect)ParticleTypes.TOTEM_OF_UNDYING, 30);
        mc.world.playSoundClient(mc.player.getX(), mc.player.getY(), mc.player.getZ(), SoundEvents.ITEM_TOTEM_USE, SoundCategory.PLAYERS, 1.0f, 1.0f, false);
    }

    public static void reset() {
        POPS.clear();
    }

    public static int counted() {
        return POPS.size();
    }

    private static void tick(MinecraftClient mc) {
        if (mc.world == null || mc.getNetworkHandler() == null) {
            POPS.clear();
            return;
        }
        POPS.keySet().removeIf(id -> {
            PlayerEntity p = mc.world.getPlayerByUuid(id);
            return p != null ? p.isDead() : mc.getNetworkHandler().getPlayerListEntry(id) == null;
        });
    }

    public static Text suffix(Entity e) {
        if (!Config.INSTANCE.totemPops || !(e instanceof PlayerEntity)) {
            return null;
        }
        Integer n = POPS.get(e.getUuid());
        return n == null ? null : Text.literal((String)(" -" + n)).withColor(TotemCounter.popColor(n));
    }

    private static int popColor(int n) {
        return n <= 2 ? 16767050 : (n <= 5 ? 16751164 : 0xFF4F4F);
    }

    private static void hud(DrawContext ctx, RenderTickCounter tick) {
        MinecraftClient mc = MinecraftClient.getInstance();
        if (!Config.INSTANCE.totemHud || mc.player == null || mc.options.hudHidden || mc.player.isSpectator()) {
            return;
        }
        int n = mc.player.getInventory().count(Items.TOTEM_OF_UNDYING);
        int cx = ctx.getScaledWindowWidth() / 2;
        int y = ctx.getScaledWindowHeight() - 19;
        int x = mc.options.getMainArm().getValue() == Arm.RIGHT ? cx + 91 + 8 : cx - 91 - 8 - 16;
        ctx.drawItem(TOTEM, x, y);
        String s = String.valueOf(n);
        int color = n >= 4 ? 6090874 : (n >= 2 ? 16767050 : (n == 1 ? 16751164 : 0xFF4F4F));
        ctx.drawText(mc.textRenderer, s, x + 17 - mc.textRenderer.getWidth(s), y + 9, 0xFF000000 | color, true);
    }

    private TotemCounter() {
    }
}

