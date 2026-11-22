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
import dev.hugoservice.StyleScreen;
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

    @Inject(method={"hasOutline"}, at={@At(value="HEAD")}, cancellable=true)
    private void hugoservice$glow(Entity entity, CallbackInfoReturnable<Boolean> cir) {
        if (Config.INSTANCE.fx(Fx.GLOW) == null) {
            return;
        }
        if (entity instanceof PlayerEntity && Fx.glowing(entity.getId())) {
            cir.setReturnValue(true);
        }
    }

    @ModifyArg(method={"onResolutionChanged"}, index=0, at={@At(value="INVOKE", target="Lnet/minecraft/client/util/Window;setScaleFactor(I)V")})
    private int hugoservice$screenScale(int base) {
        MinecraftClient mc = (MinecraftClient)(Object)this;
        return MinecraftClientMixin.hugoservice$factorFor(mc, mc.currentScreen, base);
    }

    @Inject(method={"setScreen"}, at={@At(value="TAIL")})
    private void hugoservice$rescale(Screen screen, CallbackInfo ci) {
        MinecraftClient mc = (MinecraftClient)(Object)this;
        Window w = mc.getWindow();
        int base = w.calculateScaleFactor((Integer)mc.options.getGuiScale().getValue(), mc.forcesUnicodeFont());
        int want = MinecraftClientMixin.hugoservice$factorFor(mc, screen, base);
        if (want == w.getScaleFactor()) {
            return;
        }
        w.setScaleFactor(want);
        if (mc.currentScreen != null) {
            Mc.resizeScreen(mc.currentScreen, mc, w.getScaledWidth(), w.getScaledHeight());
        }
        mc.mouse.onResolutionChanged();
    }

    /** GUI scale while this screen is open: the inventory size option for containers, a fitting one for the menu. */
    @Unique
    private static int hugoservice$factorFor(MinecraftClient mc, Screen screen, int base) {
        if (Config.INSTANCE.inventoryScale > 0 && screen instanceof HandledScreen) {
            return mc.getWindow().calculateScaleFactor(Config.INSTANCE.inventoryScale, mc.forcesUnicodeFont());
        }
        return screen instanceof StyleScreen ? StyleScreen.fitScale(mc.getWindow(), base) : base;
    }
}

