package dev.hugoservice.mixin;

import dev.hugoservice.Config;
import dev.hugoservice.Fx;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.screen.ingame.HandledScreen;
import net.minecraft.client.util.Window;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.PlayerEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyArg;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(MinecraftClient.class)
public class MinecraftClientMixin {
    @Unique
    private boolean hugoservice$invScaled;

    /** Player glow, only for players you can actually see (no wallhack). */
    @Inject(method = "hasOutline", at = @At("HEAD"), cancellable = true)
    private void hugoservice$glow(Entity entity, CallbackInfoReturnable<Boolean> cir) {
        if (entity instanceof PlayerEntity && Fx.glowing(entity.getId())) cir.setReturnValue(true);
    }

    /** Inventory size: own GUI scale while a container screen is open (also after window resizes). */
    @ModifyArg(method = "onResolutionChanged", index = 0,
        at = @At(value = "INVOKE", target = "Lnet/minecraft/client/util/Window;calculateScaleFactor(IZ)I"))
    private int hugoservice$inventoryScale(int scale) {
        int s = Config.INSTANCE.inventoryScale;
        return s > 0 && ((MinecraftClient) (Object) this).currentScreen instanceof HandledScreen ? s : scale;
    }

    @Inject(method = "setScreen", at = @At("TAIL"))
    private void hugoservice$rescale(Screen screen, CallbackInfo ci) {
        MinecraftClient mc = (MinecraftClient) (Object) this;
        boolean want = Config.INSTANCE.inventoryScale > 0 && screen instanceof HandledScreen;
        if (want == hugoservice$invScaled) return;
        hugoservice$invScaled = want;
        // only the GUI scale changes, so skip the framebuffer work of onResolutionChanged()
        Window w = mc.getWindow();
        int base = mc.options.getGuiScale().getValue();
        w.setScaleFactor(w.calculateScaleFactor(want ? Config.INSTANCE.inventoryScale : base, mc.forcesUnicodeFont()));
        if (mc.currentScreen != null) mc.currentScreen.resize(w.getScaledWidth(), w.getScaledHeight());
        mc.mouse.onResolutionChanged();
    }
}
