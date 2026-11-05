/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  net.minecraft.client.render.WorldRenderer
 *  org.spongepowered.asm.mixin.Mixin
 *  org.spongepowered.asm.mixin.injection.At
 *  org.spongepowered.asm.mixin.injection.ModifyArg
 */
package dev.hugoservice.mixin;

import dev.hugoservice.Config;
import net.minecraft.client.render.WorldRenderer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyArg;

@Mixin(value={WorldRenderer.class})
public class WorldRendererMixin {
    @ModifyArg(method={"renderTargetBlockOutline"}, index=7, require=0, at={@At(value="INVOKE", ordinal=1, target="Lnet/minecraft/client/render/WorldRenderer;drawBlockOutline(Lnet/minecraft/client/util/math/MatrixStack;Lnet/minecraft/client/render/VertexConsumer;DDDLnet/minecraft/client/render/state/OutlineRenderState;IF)V")})
    private float hugoservice$outlineWidth(float width) {
        return width * (1.0f + (float)(Math.max(1, Config.INSTANCE.outlineWidth) - 1) * 0.5f);
    }

    @ModifyArg(method={"renderTargetBlockOutline"}, index=6, at={@At(value="INVOKE", ordinal=1, target="Lnet/minecraft/client/render/WorldRenderer;drawBlockOutline(Lnet/minecraft/client/util/math/MatrixStack;Lnet/minecraft/client/render/VertexConsumer;DDDLnet/minecraft/client/render/state/OutlineRenderState;IF)V")})
    private int hugoservice$outlineColor(int color) {
        Integer c = Config.INSTANCE.fx("outline");
        return c == null ? color : 0xFF000000 | c;
    }
}

