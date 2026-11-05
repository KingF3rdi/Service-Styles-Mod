/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  net.minecraft.client.render.entity.state.EndCrystalEntityRenderState
 *  net.minecraft.client.render.command.OrderedRenderCommandQueue
 *  net.minecraft.client.render.command.ModelCommandRenderer$CrumblingOverlayCommand
 *  net.minecraft.client.render.RenderLayers
 *  net.minecraft.entity.decoration.EndCrystalEntity
 *  net.minecraft.client.render.RenderLayer
 *  net.minecraft.util.Identifier
 *  net.minecraft.client.model.Model
 *  net.minecraft.client.util.math.MatrixStack
 *  net.minecraft.client.render.entity.EndCrystalEntityRenderer
 *  org.spongepowered.asm.mixin.Mixin
 *  org.spongepowered.asm.mixin.injection.At
 *  org.spongepowered.asm.mixin.injection.Inject
 *  org.spongepowered.asm.mixin.injection.Redirect
 *  org.spongepowered.asm.mixin.injection.callback.CallbackInfo
 */
package dev.hugoservice.mixin;

import dev.hugoservice.Config;
import dev.hugoservice.CrystalCore;
import dev.hugoservice.Mc;
import net.minecraft.client.render.entity.state.EndCrystalEntityRenderState;
import net.minecraft.client.render.command.OrderedRenderCommandQueue;
import net.minecraft.client.render.command.ModelCommandRenderer;
import net.minecraft.client.render.RenderLayers;
import net.minecraft.entity.decoration.EndCrystalEntity;
import net.minecraft.client.render.RenderLayer;
import net.minecraft.util.Identifier;
import net.minecraft.client.model.Model;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.client.render.entity.EndCrystalEntityRenderer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(value={EndCrystalEntityRenderer.class})
public class EndCrystalEntityRendererMixin {
    @Redirect(method={"render(Lnet/minecraft/client/render/entity/state/EndCrystalEntityRenderState;Lnet/minecraft/client/util/math/MatrixStack;Lnet/minecraft/client/render/command/OrderedRenderCommandQueue;Lnet/minecraft/client/render/state/CameraRenderState;)V"}, at={@At(value="INVOKE", target="Lnet/minecraft/client/render/command/OrderedRenderCommandQueue;submitModel(Lnet/minecraft/client/model/Model;Ljava/lang/Object;Lnet/minecraft/client/util/math/MatrixStack;Lnet/minecraft/client/render/RenderLayer;IIILnet/minecraft/client/render/command/ModelCommandRenderer$CrumblingOverlayCommand;)V")})
    private void hugoservice$opacity(OrderedRenderCommandQueue queue, Model model, Object state, MatrixStack matrices, RenderLayer layer, int light, int overlay, int outline, ModelCommandRenderer.CrumblingOverlayCommand crumbling) {
        Identifier core = CrystalCore.texture();
        Identifier tex = core != null ? core : Identifier.ofVanilla((String)"textures/entity/end_crystal/end_crystal.png");
        int opacity = Config.INSTANCE.crystalOpacity;
        if (opacity >= 100) {
            queue.submitModel(model, state, matrices, core != null ? Mc.entityCutoutNoCull((Identifier)core) : layer, light, overlay, outline, crumbling);
        } else {
            int alpha = Math.max(10, opacity) * 255 / 100;
            queue.submitModel(model, state, matrices, Mc.entityTranslucent((Identifier)tex), light, overlay, alpha << 24 | 0xFFFFFF, null, outline, crumbling);
        }
    }

    @Inject(method={"updateRenderState(Lnet/minecraft/entity/decoration/EndCrystalEntity;Lnet/minecraft/client/render/entity/state/EndCrystalEntityRenderState;F)V"}, at={@At(value="TAIL")})
    private void hugoservice$spin(EndCrystalEntity entity, EndCrystalEntityRenderState state, float tickProgress, CallbackInfo ci) {
        state.age *= (float)Config.INSTANCE.crystalSpin / 100.0f;
    }
}

