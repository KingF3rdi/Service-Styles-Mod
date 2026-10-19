package dev.hugoservice.mixin;

import dev.hugoservice.Config;
import dev.hugoservice.Fx;
import net.minecraft.client.render.WorldRenderer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyArg;

/** Block outline colour and thickness. Ordinal 1 is the normal outline (0 is the high-contrast backdrop). */
@Mixin(WorldRenderer.class)
public class WorldRendererMixin {
    /** Thickness: 1 = vanilla, every step adds half the normal width. */
    @ModifyArg(method = "renderTargetBlockOutline", index = 7, require = 0, at = @At(value = "INVOKE", ordinal = 1,
        target = "Lnet/minecraft/client/render/WorldRenderer;drawBlockOutline(Lnet/minecraft/client/util/math/MatrixStack;Lnet/minecraft/client/render/VertexConsumer;DDDLnet/minecraft/client/render/state/OutlineRenderState;IF)V"))
    private float hugoservice$outlineWidth(float width) {
        return width * (1 + (Math.max(1, Config.INSTANCE.outlineWidth) - 1) * 0.5f);
    }

    @ModifyArg(method = "renderTargetBlockOutline", index = 6, at = @At(value = "INVOKE", ordinal = 1,
        target = "Lnet/minecraft/client/render/WorldRenderer;drawBlockOutline(Lnet/minecraft/client/util/math/MatrixStack;Lnet/minecraft/client/render/VertexConsumer;DDDLnet/minecraft/client/render/state/OutlineRenderState;IF)V"))
    private int hugoservice$outlineColor(int color) {
        Integer c = Config.INSTANCE.fx(Fx.OUTLINE);
        return c == null ? color : 0xFF000000 | c;
    }
}
