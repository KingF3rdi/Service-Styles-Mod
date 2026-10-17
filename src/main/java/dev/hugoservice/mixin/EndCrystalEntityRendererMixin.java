package dev.hugoservice.mixin;

import dev.hugoservice.Config;
import net.minecraft.client.model.Model;
import net.minecraft.client.render.RenderLayer;
import net.minecraft.client.render.RenderLayers;
import net.minecraft.client.render.command.ModelCommandRenderer;
import net.minecraft.client.render.command.OrderedRenderCommandQueue;
import net.minecraft.client.render.entity.EndCrystalEntityRenderer;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.util.Identifier;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

/** End crystal opacity: draws with a translucent layer and alpha when below 100 %. */
@Mixin(EndCrystalEntityRenderer.class)
public class EndCrystalEntityRendererMixin {
    @Unique
    private static final RenderLayer hugoservice$TRANSLUCENT =
        RenderLayers.entityTranslucent(Identifier.ofVanilla("textures/entity/end_crystal/end_crystal.png"));

    @SuppressWarnings({"rawtypes", "unchecked"})
    @Redirect(method = "render(Lnet/minecraft/client/render/entity/state/EndCrystalEntityRenderState;Lnet/minecraft/client/util/math/MatrixStack;Lnet/minecraft/client/render/command/OrderedRenderCommandQueue;Lnet/minecraft/client/render/state/CameraRenderState;)V",
        at = @At(value = "INVOKE",
        target = "Lnet/minecraft/client/render/command/OrderedRenderCommandQueue;submitModel(Lnet/minecraft/client/model/Model;Ljava/lang/Object;Lnet/minecraft/client/util/math/MatrixStack;Lnet/minecraft/client/render/RenderLayer;IIILnet/minecraft/client/render/command/ModelCommandRenderer$CrumblingOverlayCommand;)V"))
    private void hugoservice$opacity(OrderedRenderCommandQueue queue, Model model, Object state, MatrixStack matrices, RenderLayer layer,
                                     int light, int overlay, int outline, ModelCommandRenderer.CrumblingOverlayCommand crumbling) {
        int opacity = Config.INSTANCE.crystalOpacity;
        if (opacity >= 100) {
            queue.submitModel(model, state, matrices, layer, light, overlay, outline, crumbling);
        } else {
            int alpha = Math.max(10, opacity) * 255 / 100;
            queue.submitModel(model, state, matrices, hugoservice$TRANSLUCENT, light, overlay, alpha << 24 | 0xFFFFFF, null, outline, crumbling);
        }
    }
}
