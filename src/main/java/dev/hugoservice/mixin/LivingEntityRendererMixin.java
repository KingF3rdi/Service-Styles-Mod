package dev.hugoservice.mixin;

import dev.hugoservice.StyleScreen;
import net.minecraft.client.render.entity.LivingEntityRenderer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(LivingEntityRenderer.class)
public class LivingEntityRendererMixin {
    @Inject(method = {"hasLabel"}, at = {@At(value = "HEAD")}, cancellable = true, require = 0)
    private void hugoservice$hideMixer(CallbackInfoReturnable<Boolean> cir) {
        try {
            if (StyleScreen.coversWorld()) {
                cir.setReturnValue(false);
            }
        } catch (Throwable ignored) {
        }
    }
}
