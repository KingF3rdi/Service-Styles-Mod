package dev.hugoservice.mixin;

import dev.hugoservice.Config;
import dev.hugoservice.Sky;
import net.minecraft.client.render.SkyRendering;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(SkyRendering.class)
public class SkyRenderingMixin {
    @Inject(method = "renderTopSky", at = @At("HEAD"), cancellable = true, require = 0)
    private void hugoservice$topSky(CallbackInfo ci) {
        if (Sky.render()) {
            ci.cancel();
        }
    }

    @Inject(method = "renderGlowingSky", at = @At("HEAD"), cancellable = true, require = 0)
    private void hugoservice$glow(CallbackInfo ci) {
        if (Sky.active()) {
            ci.cancel();
        }
    }

    @Inject(method = "renderCelestialBodies", at = @At("HEAD"), cancellable = true, require = 0)
    private void hugoservice$celestial(CallbackInfo ci) {
        if (Sky.active() && !Config.INSTANCE.skyKeepSun) {
            ci.cancel();
        }
    }

    @Inject(method = "renderSkyDark", at = @At("HEAD"), cancellable = true, require = 0)
    private void hugoservice$dark(CallbackInfo ci) {
        if (Sky.active()) {
            ci.cancel();
        }
    }
}
