/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.llamalad7.mixinextras.injector.wrapoperation.Operation
 *  com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation
 *  com.mojang.blaze3d.pipeline.RenderPipeline
 *  net.minecraft.client.gl.RenderPipelines
 *  net.minecraft.util.Identifier
 *  net.minecraft.client.gui.hud.InGameHud
 *  net.minecraft.client.gui.DrawContext
 *  org.spongepowered.asm.mixin.Mixin
 *  org.spongepowered.asm.mixin.injection.At
 */
package dev.hugoservice.mixin;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.mojang.blaze3d.pipeline.RenderPipeline;
import dev.hugoservice.Config;
import dev.hugoservice.Crosshair;
import net.minecraft.client.gl.RenderPipelines;
import net.minecraft.util.Identifier;
import net.minecraft.client.gui.hud.InGameHud;
import net.minecraft.client.gui.DrawContext;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

@Mixin(value={InGameHud.class})
public class InGameHudMixin {
    @WrapOperation(method={"renderCrosshair"}, require=0, at={@At(value="INVOKE", ordinal=0, target="Lnet/minecraft/client/gui/DrawContext;drawGuiTexture(Lcom/mojang/blaze3d/pipeline/RenderPipeline;Lnet/minecraft/util/Identifier;IIII)V")})
    private void hugoservice$crosshair(DrawContext ctx, RenderPipeline pipeline, Identifier sprite, int x, int y, int w, int h, Operation<Void> original) {
        if (Crosshair.active()) {
            Crosshair.drawCurrent(ctx, (float)x + (float)w / 2.0f, (float)y + (float)h / 2.0f);
            return;
        }
        boolean painted = "painted".equals(Config.INSTANCE.loadout.get("crosshair"));
        original.call(new Object[]{ctx, painted ? RenderPipelines.GUI_TEXTURED : pipeline, sprite, x, y, w, h});
    }
}

