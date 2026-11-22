package dev.hugoservice.mixin;

import dev.hugoservice.Config;
import dev.hugoservice.Fx;
import dev.hugoservice.Mc;
import net.minecraft.client.render.RenderLayer;
import net.minecraft.client.render.VertexConsumer;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.render.entity.model.ShieldEntityModel;
import net.minecraft.client.render.item.ItemRenderer;
import net.minecraft.client.render.item.model.special.ShieldModelRenderer;
import net.minecraft.client.render.model.ModelBaker;
import net.minecraft.client.util.SpriteIdentifier;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.component.ComponentMap;
import net.minecraft.component.DataComponentTypes;
import net.minecraft.component.type.BannerPatternsComponent;
import net.minecraft.item.ItemDisplayContext;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** First-person shield opacity and status colour for 1.21.6-1.21.8, drawn here in place of the vanilla draw. */
@Mixin(value={ShieldModelRenderer.class}, priority=900)
public class ShieldModelRendererMixin {
    @Shadow @Final private ShieldEntityModel model;

    @Inject(method={"render(Lnet/minecraft/component/ComponentMap;Lnet/minecraft/item/ItemDisplayContext;Lnet/minecraft/client/util/math/MatrixStack;Lnet/minecraft/client/render/VertexConsumerProvider;IIZ)V"}, at={@At(value="HEAD")}, cancellable=true)
    private void hugoservice$firstPerson(ComponentMap data, ItemDisplayContext context, MatrixStack matrices, VertexConsumerProvider consumers, int light, int overlay, boolean glint, CallbackInfo ci) {
        if (context != ItemDisplayContext.FIRST_PERSON_LEFT_HAND && context != ItemDisplayContext.FIRST_PERSON_RIGHT_HAND) {
            return;
        }
        Integer tint = Fx.shieldTint();
        int opacity = Config.INSTANCE.shieldOpacity;
        if (tint == null && opacity >= 100) {
            return;
        }
        int alpha = opacity >= 100 ? 255 : Math.max(10, opacity) * 255 / 100;
        int color = alpha << 24 | (tint == null ? 0xFFFFFF : tint & 0xFFFFFF);
        // ponytail: banner patterns are left off while see-through or tinted; draw them with BannerBlockEntityRenderer.renderCanvas if missed
        boolean patterned = data != null && (!data.getOrDefault(DataComponentTypes.BANNER_PATTERNS, BannerPatternsComponent.DEFAULT).layers().isEmpty() || data.get(DataComponentTypes.BASE_COLOR) != null);
        SpriteIdentifier id = patterned ? ModelBaker.SHIELD_BASE : ModelBaker.SHIELD_BASE_NO_PATTERN;
        RenderLayer layer = opacity >= 100 ? this.model.getLayer(id.getAtlasId()) : Mc.entityTranslucent(id.getAtlasId());
        VertexConsumer vc = id.getSprite().getTextureSpecificVertexConsumer(ItemRenderer.getItemGlintConsumer(consumers, layer, false, glint));
        matrices.push();
        matrices.scale(1.0f, -1.0f, -1.0f);
        this.model.getHandle().render(matrices, vc, light, overlay, color);
        this.model.getPlate().render(matrices, vc, light, overlay, color);
        matrices.pop();
        ci.cancel();
    }
}
