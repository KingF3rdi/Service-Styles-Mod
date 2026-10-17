package dev.hugoservice.mixin;

import dev.hugoservice.KnifeAnimations;
import net.minecraft.client.network.AbstractClientPlayerEntity;
import net.minecraft.client.render.command.OrderedRenderCommandQueue;
import net.minecraft.client.render.item.HeldItemRenderer;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.item.ItemStack;
import net.minecraft.util.Hand;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Knife inspect / draw animations: an extra turn of the first-person item just before it is drawn. */
@Mixin(HeldItemRenderer.class)
public class HeldItemRendererMixin {
    // require = 0: if another mod rebuilds this method, only the knife animation is skipped
    @Inject(method = "renderFirstPersonItem", require = 0, at = @At(value = "INVOKE",
        target = "Lnet/minecraft/client/render/item/HeldItemRenderer;renderItem(Lnet/minecraft/entity/LivingEntity;Lnet/minecraft/item/ItemStack;Lnet/minecraft/item/ItemDisplayContext;Lnet/minecraft/client/util/math/MatrixStack;Lnet/minecraft/client/render/command/OrderedRenderCommandQueue;I)V"))
    private void hugoservice$knife(AbstractClientPlayerEntity player, float tickProgress, float pitch, Hand hand, float swingProgress,
                                   ItemStack item, float equipProgress, MatrixStack matrices, OrderedRenderCommandQueue queue,
                                   int light, CallbackInfo ci) {
        KnifeAnimations.apply(matrices, item, hand == Hand.MAIN_HAND);
    }
}
