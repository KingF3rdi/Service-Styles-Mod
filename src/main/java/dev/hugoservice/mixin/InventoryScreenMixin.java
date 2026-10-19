package dev.hugoservice.mixin;

import dev.hugoservice.Config;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.ingame.InventoryScreen;
import net.minecraft.entity.LivingEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Hide inventory model (skips the player drawn in the survival and creative inventory) and the social badge. */
@Mixin(InventoryScreen.class)
public abstract class InventoryScreenMixin {
    @org.spongepowered.asm.mixin.Shadow
    protected abstract net.minecraft.client.gui.ScreenPos getRecipeBookButtonPos();

    /** Social badge: a name tag next to the recipe book. */
    @Inject(method = "drawBackground", at = @At("TAIL"))
    private void hugoservice$badge(DrawContext ctx, float delta, int mouseX, int mouseY, CallbackInfo ci) {
        if (!Config.INSTANCE.badgeOn) return;
        var mc = net.minecraft.client.MinecraftClient.getInstance();
        String name = Config.INSTANCE.badgeName.isBlank() && mc.player != null ? mc.player.getGameProfile().name() : Config.INSTANCE.badgeName;
        net.minecraft.client.gui.ScreenPos p = getRecipeBookButtonPos();
        dev.hugoservice.StyleScreen.badgePill(ctx, p.x() + 22, p.y() + 3, name);
    }

    @Inject(method = "drawEntity(Lnet/minecraft/client/gui/DrawContext;IIIIIFFFLnet/minecraft/entity/LivingEntity;)V",
        at = @At("HEAD"), cancellable = true)
    private static void hugoservice$hideModel(DrawContext ctx, int x1, int y1, int x2, int y2, int size, float scale,
                                              float mouseX, float mouseY, LivingEntity entity, CallbackInfo ci) {
        var screen = net.minecraft.client.MinecraftClient.getInstance().currentScreen;
        boolean inventory = screen instanceof InventoryScreen || screen instanceof net.minecraft.client.gui.screen.ingame.CreativeInventoryScreen;
        if (Config.INSTANCE.hideInventoryModel && inventory) ci.cancel(); // not in our cosmetics preview
    }
}
