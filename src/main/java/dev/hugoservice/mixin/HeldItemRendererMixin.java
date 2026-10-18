package dev.hugoservice.mixin;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.llamalad7.mixinextras.sugar.Local;
import dev.hugoservice.KnifeAnimations;
import net.minecraft.client.render.command.OrderedRenderCommandQueue;
import net.minecraft.client.render.item.HeldItemRenderer;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.entity.LivingEntity;
import net.minecraft.item.ItemDisplayContext;
import net.minecraft.item.ItemStack;
import net.minecraft.util.Hand;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

/**
 * Knife animations: an extra move of the first-person item right before it is drawn (the butterfly handles swing in
 * KnifeRenderer). require = 0: if another mod rebuilds this method, only the animations are skipped.
 */
@Mixin(HeldItemRenderer.class)
public class HeldItemRendererMixin {
    @WrapOperation(method = "renderFirstPersonItem", require = 0, at = @At(value = "INVOKE",
        target = "Lnet/minecraft/client/render/item/HeldItemRenderer;renderItem(Lnet/minecraft/entity/LivingEntity;Lnet/minecraft/item/ItemStack;Lnet/minecraft/item/ItemDisplayContext;Lnet/minecraft/client/util/math/MatrixStack;Lnet/minecraft/client/render/command/OrderedRenderCommandQueue;I)V"))
    private void hugoservice$knife(HeldItemRenderer self, LivingEntity entity, ItemStack stack, ItemDisplayContext context,
                                   MatrixStack matrices, OrderedRenderCommandQueue queue, int light, Operation<Void> original,
                                   @Local(argsOnly = true) Hand hand, @Local(argsOnly = true, ordinal = 3) float equipProgress) {
        KnifeAnimations.apply(matrices, stack, hand == Hand.MAIN_HAND, equipProgress);
        original.call(self, entity, stack, context, matrices, queue, light);
    }
}
