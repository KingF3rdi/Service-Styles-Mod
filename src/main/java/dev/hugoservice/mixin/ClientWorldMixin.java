package dev.hugoservice.mixin;

import dev.hugoservice.Perf;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.particle.ParticleEffect;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Distance / explosion particle cull on the world spawn path. {@code require = 0} for Sodium/Vulkan. */
@Mixin(ClientWorld.class)
public class ClientWorldMixin {
    @Inject(
            method = "addParticleClient(Lnet/minecraft/particle/ParticleEffect;DDDDDD)V",
            at = @At("HEAD"),
            cancellable = true,
            require = 0
    )
    private void hugoservice$cull(ParticleEffect parameters, double x, double y, double z, double velocityX, double velocityY, double velocityZ, CallbackInfo ci) {
        try {
            if (Perf.skipParticle(parameters, x, y, z)) {
                ci.cancel();
            }
        } catch (Throwable ignored) {
        }
    }

    @Inject(
            method = "addParticleClient(Lnet/minecraft/particle/ParticleEffect;ZZDDDDDD)V",
            at = @At("HEAD"),
            cancellable = true,
            require = 0
    )
    private void hugoservice$cullForced(ParticleEffect parameters, boolean forceAlwaysRender, boolean canSpawnOnMinimal, double x, double y, double z, double velocityX, double velocityY, double velocityZ, CallbackInfo ci) {
        try {
            if (Perf.skipParticle(parameters, x, y, z)) {
                ci.cancel();
            }
        } catch (Throwable ignored) {
        }
    }
}
