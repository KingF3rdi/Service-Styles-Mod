package dev.hugoservice.mixin;

import dev.hugoservice.Config;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.ingame.InventoryScreen;
import net.minecraft.entity.LivingEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Hide inventory model: skips the player drawn in the survival and creative inventory. */
@Mixin(InventoryScreen.class)
public class InventoryScreenMixin {
    @Inject(method = "drawEntity(Lnet/minecraft/client/gui/DrawContext;IIIIIFFFLnet/minecraft/entity/LivingEntity;)V",
        at = @At("HEAD"), cancellable = true)
    private static void hugoservice$hideModel(DrawContext ctx, int x1, int y1, int x2, int y2, int size, float scale,
                                              float mouseX, float mouseY, LivingEntity entity, CallbackInfo ci) {
        if (Config.INSTANCE.hideInventoryModel) ci.cancel();
    }
}
