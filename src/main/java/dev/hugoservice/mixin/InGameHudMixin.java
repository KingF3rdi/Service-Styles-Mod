package dev.hugoservice.mixin;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.mojang.blaze3d.pipeline.RenderPipeline;
import dev.hugoservice.Config;
import dev.hugoservice.Crosshair;
import net.minecraft.client.gl.RenderPipelines;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.hud.InGameHud;
import net.minecraft.util.Identifier;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

/**
 * The crosshair sprite: the own crosshair (Crosshair tab) is drawn in its place; a painted pack crosshair is drawn
 * with its real colours instead of vanilla's inverted ones. The attack indicator and the F5 / debug rules stay vanilla.
 */
@Mixin(InGameHud.class)
public class InGameHudMixin {
    @WrapOperation(method = "renderCrosshair", require = 0, at = @At(value = "INVOKE", ordinal = 0,
        target = "Lnet/minecraft/client/gui/DrawContext;drawGuiTexture(Lcom/mojang/blaze3d/pipeline/RenderPipeline;Lnet/minecraft/util/Identifier;IIII)V"))
    private void hugoservice$crosshair(DrawContext ctx, RenderPipeline pipeline, Identifier sprite, int x, int y, int w, int h,
                                       Operation<Void> original) {
        if (Crosshair.active()) {
            Crosshair.drawCurrent(ctx, x + w / 2f, y + h / 2f);
            return;
        }
        boolean painted = "painted".equals(Config.INSTANCE.loadout.get("crosshair"));
        original.call(ctx, painted ? RenderPipelines.GUI_TEXTURED : pipeline, sprite, x, y, w, h);
    }
}
