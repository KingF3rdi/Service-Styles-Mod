package dev.hugoservice.mixin;

import dev.hugoservice.Config;
import dev.hugoservice.Fx;
import net.minecraft.client.render.RenderLayer;
import net.minecraft.client.render.RenderLayers;
import net.minecraft.client.render.TexturedRenderLayers;
import net.minecraft.client.render.command.OrderedRenderCommandQueue;
import net.minecraft.client.render.item.model.special.ShieldModelRenderer;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.component.ComponentMap;
import net.minecraft.item.ItemDisplayContext;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyArg;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Shield status: tints your first-person shield green while ready and red while an axe disabled it. */
@Mixin(ShieldModelRenderer.class)
public class ShieldModelRendererMixin {
    @Unique
    private static boolean hugoservice$firstPerson;

    @Inject(method = "render(Lnet/minecraft/component/ComponentMap;Lnet/minecraft/item/ItemDisplayContext;Lnet/minecraft/client/util/math/MatrixStack;Lnet/minecraft/client/render/command/OrderedRenderCommandQueue;IIZI)V",
        at = @At("HEAD"))
    private void hugoservice$context(ComponentMap data, ItemDisplayContext context, MatrixStack matrices, OrderedRenderCommandQueue queue,
                                     int light, int overlay, boolean glint, int seed, CallbackInfo ci) {
        hugoservice$firstPerson = context == ItemDisplayContext.FIRST_PERSON_LEFT_HAND || context == ItemDisplayContext.FIRST_PERSON_RIGHT_HAND;
    }

    @ModifyArg(method = "render(Lnet/minecraft/component/ComponentMap;Lnet/minecraft/item/ItemDisplayContext;Lnet/minecraft/client/util/math/MatrixStack;Lnet/minecraft/client/render/command/OrderedRenderCommandQueue;IIZI)V",
        index = 8, at = @At(value = "INVOKE",
        target = "Lnet/minecraft/client/render/command/OrderedRenderCommandQueue;submitModelPart(Lnet/minecraft/client/model/ModelPart;Lnet/minecraft/client/util/math/MatrixStack;Lnet/minecraft/client/render/RenderLayer;IILnet/minecraft/client/texture/Sprite;ZZILnet/minecraft/client/render/command/ModelCommandRenderer$CrumblingOverlayCommand;I)V"))
    private int hugoservice$tint(int color) {
        if (!hugoservice$firstPerson) return color;
        Integer tint = Fx.shieldTint();
        int rgb = tint == null ? color & 0xFFFFFF : tint & 0xFFFFFF;
        int opacity = Config.INSTANCE.shieldOpacity;
        int alpha = opacity >= 100 ? 255 : Math.max(10, opacity) * 255 / 100;
        return alpha << 24 | rgb;
    }

    /** See-through shield needs a translucent layer (the vanilla one ignores alpha). */
    @ModifyArg(method = "render(Lnet/minecraft/component/ComponentMap;Lnet/minecraft/item/ItemDisplayContext;Lnet/minecraft/client/util/math/MatrixStack;Lnet/minecraft/client/render/command/OrderedRenderCommandQueue;IIZI)V",
        index = 2, at = @At(value = "INVOKE",
        target = "Lnet/minecraft/client/render/command/OrderedRenderCommandQueue;submitModelPart(Lnet/minecraft/client/model/ModelPart;Lnet/minecraft/client/util/math/MatrixStack;Lnet/minecraft/client/render/RenderLayer;IILnet/minecraft/client/texture/Sprite;ZZILnet/minecraft/client/render/command/ModelCommandRenderer$CrumblingOverlayCommand;I)V"))
    private RenderLayer hugoservice$layer(RenderLayer layer) {
        if (!hugoservice$firstPerson || Config.INSTANCE.shieldOpacity >= 100) return layer;
        return RenderLayers.entityTranslucent(TexturedRenderLayers.SHIELD_PATTERNS_ATLAS_TEXTURE);
    }
}
