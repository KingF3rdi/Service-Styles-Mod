package dev.hugoservice.mixin;

import com.mojang.blaze3d.buffers.GpuBufferSlice;
import dev.hugoservice.Config;
import dev.hugoservice.Sky;
import net.minecraft.client.render.SkyRendering;
import net.minecraft.client.render.WorldRenderer;
import net.minecraft.client.render.state.SkyRenderState;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.world.MoonPhase;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Custom skybox in the overworld sky pass (lambda method_62215): our cube replaces the top sky,
 * the sunrise glow and the dark lower sky; sun, moon and stars stay only if wanted.
 */
@Mixin(WorldRenderer.class)
public class WorldRendererSkyMixin {
    private static final String SKY_PASS = "method_62215";

    @Inject(method = SKY_PASS, at = @At("HEAD"))
    private static void hugoservice$state(GpuBufferSlice fog, SkyRenderState state, SkyRendering sky, CallbackInfo ci) {
        Sky.state = state;
    }

    @Redirect(method = SKY_PASS, at = @At(value = "INVOKE", target = "Lnet/minecraft/client/render/SkyRendering;renderTopSky(I)V"))
    private static void hugoservice$topSky(SkyRendering sky, int color) {
        if (!Sky.render()) sky.renderTopSky(color);
    }

    @Redirect(method = SKY_PASS, at = @At(value = "INVOKE",
        target = "Lnet/minecraft/client/render/SkyRendering;renderGlowingSky(Lnet/minecraft/client/util/math/MatrixStack;FI)V"))
    private static void hugoservice$glow(SkyRendering sky, MatrixStack matrices, float angle, int color) {
        if (!Sky.active()) sky.renderGlowingSky(matrices, angle, color);
    }

    @Redirect(method = SKY_PASS, at = @At(value = "INVOKE",
        target = "Lnet/minecraft/client/render/SkyRendering;renderCelestialBodies(Lnet/minecraft/client/util/math/MatrixStack;FFFLnet/minecraft/world/MoonPhase;FF)V"))
    private static void hugoservice$celestial(SkyRendering sky, MatrixStack matrices, float a, float b, float c, MoonPhase phase, float d, float e) {
        if (!Sky.active() || Config.INSTANCE.skyKeepSun) sky.renderCelestialBodies(matrices, a, b, c, phase, d, e);
    }

    @Redirect(method = SKY_PASS, at = @At(value = "INVOKE", target = "Lnet/minecraft/client/render/SkyRendering;renderSkyDark()V"))
    private static void hugoservice$dark(SkyRendering sky) {
        if (!Sky.active()) sky.renderSkyDark();
    }
}
