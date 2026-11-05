/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  net.minecraft.client.util.Window
 *  net.minecraft.entity.Entity
 *  net.minecraft.entity.player.PlayerEntity
 *  net.minecraft.client.MinecraftClient
 *  net.minecraft.client.gui.screen.Screen
 *  net.minecraft.client.gui.screen.ingame.HandledScreen
 *  org.spongepowered.asm.mixin.Mixin
 *  org.spongepowered.asm.mixin.Unique
 *  org.spongepowered.asm.mixin.injection.At
 *  org.spongepowered.asm.mixin.injection.Inject
 *  org.spongepowered.asm.mixin.injection.ModifyArg
 *  org.spongepowered.asm.mixin.injection.callback.CallbackInfo
 *  org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable
 */
package dev.hugoservice.mixin;

import dev.hugoservice.Config;
import dev.hugoservice.Fx;
import dev.hugoservice.Mc;
import net.minecraft.client.util.Window;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.screen.ingame.HandledScreen;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyArg;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(value={MinecraftClient.class})
public class MinecraftClientMixin {
    @Unique
    private boolean hugoservice$invScaled;

    @Inject(method={"hasOutline"}, at={@At(value="HEAD")}, cancellable=true)
    private void hugoservice$glow(Entity entity, CallbackInfoReturnable<Boolean> cir) {
        if (entity instanceof PlayerEntity && Fx.glowing(entity.getId())) {
            cir.setReturnValue(true);
        }
    }

    @ModifyArg(method={"onResolutionChanged"}, index=0, at={@At(value="INVOKE", target="Lnet/minecraft/client/util/Window;calculateScaleFactor(IZ)I")})
    private int hugoservice$inventoryScale(int scale) {
        int s = Config.INSTANCE.inventoryScale;
        return s > 0 && ((MinecraftClient)(Object)this).currentScreen instanceof HandledScreen ? s : scale;
    }

    @Inject(method={"setScreen"}, at={@At(value="TAIL")})
    private void hugoservice$rescale(Screen screen, CallbackInfo ci) {
        boolean want;
        MinecraftClient mc = (MinecraftClient)(Object)this;
        boolean bl = want = Config.INSTANCE.inventoryScale > 0 && screen instanceof HandledScreen;
        if (want == this.hugoservice$invScaled) {
            return;
        }
        this.hugoservice$invScaled = want;
        Window w = mc.getWindow();
        int base = (Integer)mc.options.getGuiScale().getValue();
        w.setScaleFactor(w.calculateScaleFactor(want ? Config.INSTANCE.inventoryScale : base, mc.forcesUnicodeFont()));
        if (mc.currentScreen != null) {
            Mc.resizeScreen(mc.currentScreen, mc, w.getScaledWidth(), w.getScaledHeight());
        }
        mc.mouse.onResolutionChanged();
    }
}

