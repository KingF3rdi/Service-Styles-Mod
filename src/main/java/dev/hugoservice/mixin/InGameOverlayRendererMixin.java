package dev.hugoservice.mixin;

import dev.hugoservice.Config;
import net.minecraft.client.gui.hud.InGameOverlayRenderer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyArg;
import org.spongepowered.asm.mixin.injection.ModifyArgs;
import org.spongepowered.asm.mixin.injection.invoke.arg.Args;

/** Low fire and totem pop size. require = 0: without them fire and pop just stay as they are. */
@Mixin(InGameOverlayRenderer.class)
public class InGameOverlayRendererMixin {
    @ModifyArg(method = "renderFireOverlay", index = 1, require = 0,
        at = @At(value = "INVOKE", target = "Lnet/minecraft/client/util/math/MatrixStack;translate(FFF)V"))
    private static float hugoservice$lowFire(float y) {
        return y - (1 - Config.INSTANCE.fireHeight / 100f) * 0.3f;
    }

    @ModifyArgs(method = "renderFloatingItem", require = 0,
        at = @At(value = "INVOKE", target = "Lnet/minecraft/client/util/math/MatrixStack;scale(FFF)V"))
    private void hugoservice$popSize(Args args) {
        float k = Config.INSTANCE.popSize / 100f;
        for (int i = 0; i < 3; i++) args.set(i, (float) args.get(i) * k);
    }
}
