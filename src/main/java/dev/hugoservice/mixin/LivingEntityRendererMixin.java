package dev.hugoservice.mixin;

import dev.hugoservice.Presence;
import net.minecraft.client.render.entity.LivingEntityRenderer;
import net.minecraft.entity.LivingEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(LivingEntityRenderer.class)
public class LivingEntityRendererMixin {
    @Inject(method = "hasLabel(Lnet/minecraft/entity/LivingEntity;)Z", at = @At("RETURN"), cancellable = true, require = 0)
    private void hugoservice$f5Self(LivingEntity entity, CallbackInfoReturnable<Boolean> cir) {
        if (Presence.showOwnLabel(entity)) {
            cir.setReturnValue(Boolean.TRUE);
        }
    }

    @Inject(method = "hasLabel(Lnet/minecraft/entity/LivingEntity;D)Z", at = @At("RETURN"), cancellable = true, require = 0)
    private void hugoservice$f5SelfDist(LivingEntity entity, double distance, CallbackInfoReturnable<Boolean> cir) {
        if (Presence.showOwnLabel(entity)) {
            cir.setReturnValue(Boolean.TRUE);
        }
    }
}
