package dev.hugoservice.mixin;

import dev.hugoservice.Perf;
import net.minecraft.client.particle.Particle;
import net.minecraft.client.particle.ParticleManager;
import net.minecraft.particle.ParticleEffect;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Extra particle / explosion culling. {@code require = 0} so Sodium, VulkanMod, or Yarn drift cannot crash.
 */
@Mixin(ParticleManager.class)
public class ParticleManagerMixin {
    @Inject(
            method = "addParticle(Lnet/minecraft/particle/ParticleEffect;DDDDDD)Lnet/minecraft/client/particle/Particle;",
            at = @At("HEAD"),
            cancellable = true,
            require = 0
    )
    private void hugoservice$cullAdd(ParticleEffect parameters, double x, double y, double z, double velocityX, double velocityY, double velocityZ, CallbackInfoReturnable<Particle> cir) {
        try {
            if (Perf.skipParticle(parameters, x, y, z)) {
                cir.setReturnValue(null);
            }
        } catch (Throwable ignored) {
        }
    }
}
