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
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.render.RenderTickCounter;
import net.minecraft.client.util.Window;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(value={InGameHud.class})
public class InGameHudMixin {
    /** Inventory scale factor to put back after the HUD, 0 when not swapped. */
    @Unique
    private int hugoservice$invFactor;

    // The inventory size option and the menu change the window's GUI scale while they are open
    // (MinecraftClientMixin); the HUD is laid out at your own GUI scale and scaled to match, so it never changes size.
    @Inject(method={"render"}, at={@At(value="HEAD")}, require=0)
    private void hugoservice$hudScale(DrawContext ctx, RenderTickCounter tick, CallbackInfo ci) {
        MinecraftClient mc = MinecraftClient.getInstance();
        if (mc.currentScreen == null) {
            return;
        }
        Window w = mc.getWindow();
        int inv = w.getScaleFactor();
        int base = w.calculateScaleFactor((Integer)mc.options.getGuiScale().getValue(), mc.forcesUnicodeFont());
        if (inv == base) {
            return;
        }
        this.hugoservice$invFactor = inv;
        w.setScaleFactor(base);
        ctx.getMatrices().pushMatrix();
        ctx.getMatrices().scale((float)base / (float)inv, (float)base / (float)inv);
    }

    @Inject(method={"render"}, at={@At(value="RETURN")}, require=0)
    private void hugoservice$hudScaleEnd(DrawContext ctx, RenderTickCounter tick, CallbackInfo ci) {
        if (this.hugoservice$invFactor == 0) {
            return;
        }
        ctx.getMatrices().popMatrix();
        MinecraftClient.getInstance().getWindow().setScaleFactor(this.hugoservice$invFactor);
        this.hugoservice$invFactor = 0;
    }

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

