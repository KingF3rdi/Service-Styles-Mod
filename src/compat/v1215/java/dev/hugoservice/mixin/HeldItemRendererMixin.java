package dev.hugoservice.mixin;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import dev.hugoservice.HandSize;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.render.item.HeldItemRenderer;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.entity.LivingEntity;
import net.minecraft.item.ItemDisplayContext;
import net.minecraft.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

@Mixin(HeldItemRenderer.class)
public class HeldItemRendererMixin {
    @WrapOperation(
            method = {"renderFirstPersonItem"},
            require = 0,
            at = {@At(value = "INVOKE", target = "Lnet/minecraft/client/render/item/HeldItemRenderer;renderItem(Lnet/minecraft/entity/LivingEntity;Lnet/minecraft/item/ItemStack;Lnet/minecraft/item/ItemDisplayContext;ZLnet/minecraft/client/util/math/MatrixStack;Lnet/minecraft/client/render/VertexConsumerProvider;I)V")}
    )
    private void hugoservice$handSize(HeldItemRenderer self, LivingEntity entity, ItemStack stack, ItemDisplayContext context, boolean leftHanded, MatrixStack matrices, VertexConsumerProvider consumers, int light, Operation<Void> original) {
        int size = HandSize.of(stack);
        if (size == 100) {
            original.call(self, entity, stack, context, leftHanded, matrices, consumers, light);
            return;
        }
        matrices.push();
        matrices.scale((float) size / 100.0f, (float) size / 100.0f, (float) size / 100.0f);
        original.call(self, entity, stack, context, leftHanded, matrices, consumers, light);
        matrices.pop();
    }
}
