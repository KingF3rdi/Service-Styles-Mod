package dev.hugoservice.mixin;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.llamalad7.mixinextras.sugar.Local;
import dev.hugoservice.HandPreview;
import net.minecraft.client.render.item.HeldItemRenderer;
import net.minecraft.entity.LivingEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.util.Hand;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(HeldItemRenderer.class)
public class HandPreviewBlockMixin {
    @Unique
    private boolean hugoservice$blocking;

    @Inject(method = "renderFirstPersonItem", at = @At("HEAD"), require = 0)
    private void hugoservice$start(CallbackInfo ci, @Local(argsOnly = true) Hand hand, @Local(argsOnly = true) ItemStack stack) {
        ItemStack preview = HandPreview.stack();
        this.hugoservice$blocking = hand == Hand.MAIN_HAND && preview != null && preview.isOf(Items.SHIELD) && stack.isOf(Items.SHIELD);
    }

    @WrapOperation(method = "renderFirstPersonItem", require = 0, at = @At(value = "INVOKE", target = "Lnet/minecraft/entity/LivingEntity;isUsingItem()Z"))
    private boolean hugoservice$using(LivingEntity player, Operation<Boolean> original) {
        return this.hugoservice$blocking || original.call(player);
    }

    @WrapOperation(method = "renderFirstPersonItem", require = 0, at = @At(value = "INVOKE", target = "Lnet/minecraft/entity/LivingEntity;getItemUseTimeLeft()I"))
    private int hugoservice$timeLeft(LivingEntity player, Operation<Integer> original) {
        return this.hugoservice$blocking ? 72000 : original.call(player);
    }

    @WrapOperation(method = "renderFirstPersonItem", require = 0, at = @At(value = "INVOKE", target = "Lnet/minecraft/entity/LivingEntity;getActiveHand()Lnet/minecraft/util/Hand;"))
    private Hand hugoservice$activeHand(LivingEntity player, Operation<Hand> original) {
        return this.hugoservice$blocking ? Hand.MAIN_HAND : original.call(player);
    }
}
