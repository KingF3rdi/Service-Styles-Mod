/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  net.minecraft.client.gui.hud.InGameOverlayRenderer
 *  org.spongepowered.asm.mixin.Mixin
 *  org.spongepowered.asm.mixin.injection.At
 *  org.spongepowered.asm.mixin.injection.ModifyArg
 *  org.spongepowered.asm.mixin.injection.ModifyArgs
 *  org.spongepowered.asm.mixin.injection.invoke.arg.Args
 */
package dev.hugoservice.mixin;

import dev.hugoservice.Config;
import net.minecraft.client.gui.hud.InGameOverlayRenderer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyArg;
import org.spongepowered.asm.mixin.injection.ModifyArgs;
import org.spongepowered.asm.mixin.injection.invoke.arg.Args;

@Mixin(value={InGameOverlayRenderer.class})
public class InGameOverlayRendererMixin {
    @ModifyArg(method={"renderFireOverlay"}, index=1, require=0, at={@At(value="INVOKE", target="Lnet/minecraft/client/util/math/MatrixStack;translate(FFF)V")})
    private static float hugoservice$lowFire(float y) {
        return y - (1.0f - (float)Config.INSTANCE.fireHeight / 100.0f) * 0.3f;
    }

    @ModifyArgs(method={"renderFloatingItem"}, require=0, at={@At(value="INVOKE", target="Lnet/minecraft/client/util/math/MatrixStack;scale(FFF)V")})
    private void hugoservice$popSize(Args args) {
        float k = (float)Config.INSTANCE.popSize / 100.0f;
        for (int i = 0; i < 3; ++i) {
            args.set(i, Float.valueOf(((Float)args.get(i)).floatValue() * k));
        }
    }
}

