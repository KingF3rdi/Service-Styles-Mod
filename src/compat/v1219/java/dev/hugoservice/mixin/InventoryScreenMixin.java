/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  net.minecraft.entity.LivingEntity
 *  net.minecraft.client.MinecraftClient
 *  net.minecraft.client.gui.DrawContext
 *  net.minecraft.client.gui.screen.Screen
 *  net.minecraft.client.gui.screen.ingame.CreativeInventoryScreen
 *  net.minecraft.client.gui.screen.ingame.InventoryScreen
 *  net.minecraft.client.gui.ScreenPos
 *  org.spongepowered.asm.mixin.Mixin
 *  org.spongepowered.asm.mixin.Shadow
 *  org.spongepowered.asm.mixin.injection.At
 *  org.spongepowered.asm.mixin.injection.Inject
 *  org.spongepowered.asm.mixin.injection.callback.CallbackInfo
 */
package dev.hugoservice.mixin;

import dev.hugoservice.Config;
import dev.hugoservice.StyleScreen;
import net.minecraft.entity.LivingEntity;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.screen.ingame.CreativeInventoryScreen;
import net.minecraft.client.gui.screen.ingame.InventoryScreen;
import net.minecraft.client.gui.ScreenPos;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(value={InventoryScreen.class})
public abstract class InventoryScreenMixin {
    @Shadow
    protected abstract ScreenPos getRecipeBookButtonPos();

    @Inject(method={"drawBackground"}, at={@At(value="TAIL")})
    private void hugoservice$badge(DrawContext ctx, float delta, int mouseX, int mouseY, CallbackInfo ci) {
        if (!Config.INSTANCE.badgeOn) {
            return;
        }
        MinecraftClient mc = MinecraftClient.getInstance();
        String name = Config.INSTANCE.badgeName.isBlank() && mc.player != null ? mc.player.getGameProfile().name() : Config.INSTANCE.badgeName;
        ScreenPos p = this.getRecipeBookButtonPos();
        StyleScreen.badgePill(ctx, p.x() + 22, p.y() + 3, name);
    }

    @Inject(method={"drawEntity(Lnet/minecraft/client/gui/DrawContext;IIIIIFFFLnet/minecraft/entity/LivingEntity;)V"}, at={@At(value="HEAD")}, cancellable=true)
    private static void hugoservice$hideModel(DrawContext ctx, int x1, int y1, int x2, int y2, int size, float scale, float mouseX, float mouseY, LivingEntity entity, CallbackInfo ci) {
        boolean inventory;
        Screen screen = MinecraftClient.getInstance().currentScreen;
        boolean bl = inventory = screen instanceof InventoryScreen || screen instanceof CreativeInventoryScreen;
        if (Config.INSTANCE.hideInventoryModel && inventory) {
            ci.cancel();
        }
    }
}

