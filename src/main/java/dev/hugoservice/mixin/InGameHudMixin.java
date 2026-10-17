package dev.hugoservice.mixin;

import com.mojang.blaze3d.pipeline.RenderPipeline;
import dev.hugoservice.Config;
import net.minecraft.client.gl.RenderPipelines;
import net.minecraft.client.gui.hud.InGameHud;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyArg;

/** Vanilla draws the crosshair inverted; a painted one is drawn with its real colours. */
@Mixin(InGameHud.class)
public class InGameHudMixin {
    @ModifyArg(method = "renderCrosshair", index = 0, at = @At(value = "INVOKE", ordinal = 0,
        target = "Lnet/minecraft/client/gui/DrawContext;drawGuiTexture(Lcom/mojang/blaze3d/pipeline/RenderPipeline;Lnet/minecraft/util/Identifier;IIII)V"))
    private RenderPipeline hugoservice$crosshairColours(RenderPipeline pipeline) {
        return "painted".equals(Config.INSTANCE.loadout.get("crosshair")) ? RenderPipelines.GUI_TEXTURED : pipeline;
    }
}
