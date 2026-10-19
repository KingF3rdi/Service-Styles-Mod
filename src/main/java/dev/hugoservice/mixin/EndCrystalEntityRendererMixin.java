package dev.hugoservice.mixin;

import dev.hugoservice.Config;
import dev.hugoservice.CrystalCore;
import net.minecraft.client.model.Model;
import net.minecraft.client.render.RenderLayer;
import net.minecraft.client.render.RenderLayers;
import net.minecraft.client.render.command.ModelCommandRenderer;
import net.minecraft.client.render.command.OrderedRenderCommandQueue;
import net.minecraft.client.render.entity.EndCrystalEntityRenderer;
import net.minecraft.client.render.entity.state.EndCrystalEntityRenderState;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.entity.decoration.EndCrystalEntity;
import net.minecraft.util.Identifier;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** End crystals: opacity (translucent layer and alpha below 100 %), own core colour and spin speed. */
@Mixin(EndCrystalEntityRenderer.class)
public class EndCrystalEntityRendererMixin {
    @SuppressWarnings({"rawtypes", "unchecked"})
    @Redirect(method = "render(Lnet/minecraft/client/render/entity/state/EndCrystalEntityRenderState;Lnet/minecraft/client/util/math/MatrixStack;Lnet/minecraft/client/render/command/OrderedRenderCommandQueue;Lnet/minecraft/client/render/state/CameraRenderState;)V",
        at = @At(value = "INVOKE",
        target = "Lnet/minecraft/client/render/command/OrderedRenderCommandQueue;submitModel(Lnet/minecraft/client/model/Model;Ljava/lang/Object;Lnet/minecraft/client/util/math/MatrixStack;Lnet/minecraft/client/render/RenderLayer;IIILnet/minecraft/client/render/command/ModelCommandRenderer$CrumblingOverlayCommand;)V"))
    private void hugoservice$opacity(OrderedRenderCommandQueue queue, Model model, Object state, MatrixStack matrices, RenderLayer layer,
                                     int light, int overlay, int outline, ModelCommandRenderer.CrumblingOverlayCommand crumbling) {
        Identifier core = CrystalCore.texture();
        Identifier tex = core != null ? core : Identifier.ofVanilla("textures/entity/end_crystal/end_crystal.png");
        int opacity = Config.INSTANCE.crystalOpacity;
        if (opacity >= 100) {
            queue.submitModel(model, state, matrices, core != null ? RenderLayers.entityCutoutNoCull(core) : layer, light, overlay, outline, crumbling);
        } else {
            int alpha = Math.max(10, opacity) * 255 / 100;
            queue.submitModel(model, state, matrices, RenderLayers.entityTranslucent(tex), light, overlay, alpha << 24 | 0xFFFFFF, null, outline, crumbling);
        }
    }

    /** Spin speed: the model turns and bobs with the render state's age; 0 % holds it still. */
    @Inject(method = "updateRenderState(Lnet/minecraft/entity/decoration/EndCrystalEntity;Lnet/minecraft/client/render/entity/state/EndCrystalEntityRenderState;F)V",
        at = @At("TAIL"))
    private void hugoservice$spin(EndCrystalEntity entity, EndCrystalEntityRenderState state, float tickProgress, CallbackInfo ci) {
        state.age *= Config.INSTANCE.crystalSpin / 100f;
    }
}
