package dev.hugoservice.mixin;

import dev.hugoservice.HandPreview;
import dev.hugoservice.StyleScreen;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.render.entity.LivingEntityRenderer;
import net.minecraft.entity.LivingEntity;
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

    /** Name tag size slider dragged: the camera goes third person, and vanilla never labels the camera entity. */
    @Inject(method = {"hasLabel(Lnet/minecraft/entity/LivingEntity;D)Z"}, at = {@At(value = "HEAD")}, cancellable = true, require = 0)
    private void hugoservice$ownTag(LivingEntity entity, double distance, CallbackInfoReturnable<Boolean> cir) {
        if (HandPreview.scene() == HandPreview.Scene.NAMETAG && entity == MinecraftClient.getInstance().player) {
            cir.setReturnValue(true);
        }
    }
}
