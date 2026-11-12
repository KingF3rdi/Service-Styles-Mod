package dev.hugoservice.mixin;

import dev.hugoservice.Config;
import dev.hugoservice.CrystalCore;
import dev.hugoservice.Mc;
import net.minecraft.client.render.RenderLayer;
import net.minecraft.client.render.VertexConsumer;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.render.entity.EndCrystalEntityRenderer;
import net.minecraft.client.render.entity.model.EndCrystalEntityModel;
import net.minecraft.client.render.entity.state.EndCrystalEntityRenderState;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.entity.decoration.EndCrystalEntity;
import net.minecraft.util.Identifier;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(value={EndCrystalEntityRenderer.class})
public class EndCrystalEntityRendererMixin {
    @Redirect(method={"render(Lnet/minecraft/client/render/entity/state/EndCrystalEntityRenderState;Lnet/minecraft/client/util/math/MatrixStack;Lnet/minecraft/client/render/VertexConsumerProvider;I)V"}, at={@At(value="INVOKE", target="Lnet/minecraft/client/render/VertexConsumerProvider;getBuffer(Lnet/minecraft/client/render/RenderLayer;)Lnet/minecraft/client/render/VertexConsumer;")})
    private VertexConsumer hugoservice$layer(VertexConsumerProvider consumers, RenderLayer layer) {
        Identifier core = CrystalCore.texture();
        int opacity = Config.INSTANCE.crystalOpacity;
        if (core == null && opacity >= 100) {
            return consumers.getBuffer(layer);
        }
        Identifier tex = core != null ? core : Identifier.ofVanilla((String)"textures/entity/end_crystal/end_crystal.png");
        if (opacity >= 100) {
            return consumers.getBuffer(Mc.entityCutoutNoCull(tex));
        }
        return consumers.getBuffer(Mc.entityTranslucent(tex));
    }

    @Redirect(method={"render(Lnet/minecraft/client/render/entity/state/EndCrystalEntityRenderState;Lnet/minecraft/client/util/math/MatrixStack;Lnet/minecraft/client/render/VertexConsumerProvider;I)V"}, at={@At(value="INVOKE", target="Lnet/minecraft/client/render/entity/model/EndCrystalEntityModel;render(Lnet/minecraft/client/util/math/MatrixStack;Lnet/minecraft/client/render/VertexConsumer;II)V")})
    private void hugoservice$alpha(EndCrystalEntityModel model, MatrixStack matrices, VertexConsumer vertices, int light, int overlay) {
        int opacity = Config.INSTANCE.crystalOpacity;
        if (opacity >= 100) {
            model.render(matrices, vertices, light, overlay);
            return;
        }
        int alpha = Math.max(10, opacity) * 255 / 100;
        model.render(matrices, vertices, light, overlay, alpha << 24 | 0xFFFFFF);
    }

    @Inject(method={"updateRenderState(Lnet/minecraft/entity/decoration/EndCrystalEntity;Lnet/minecraft/client/render/entity/state/EndCrystalEntityRenderState;F)V"}, at={@At(value="TAIL")})
    private void hugoservice$spin(EndCrystalEntity entity, EndCrystalEntityRenderState state, float tickProgress, CallbackInfo ci) {
        if (Config.INSTANCE.crystalSpin == 100) {
            return;
        }
        state.age *= (float)Config.INSTANCE.crystalSpin / 100.0f;
    }
}
