package dev.hugoservice.mixin;

import dev.hugoservice.HandPreview;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.render.item.HeldItemRenderer;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(HeldItemRenderer.class)
public class HandPreviewMixin {
    @Shadow
    private ItemStack mainHand;
    @Shadow
    private float equipProgressMainHand;
    @Shadow
    private float lastEquipProgressMainHand;
    @Unique
    private ItemStack hugoservice$realMainHand;
    @Unique
    private float hugoservice$realProgress;
    @Unique
    private float hugoservice$realLastProgress;

    @Inject(
            method = "renderItem(FLnet/minecraft/client/util/math/MatrixStack;Lnet/minecraft/client/render/VertexConsumerProvider$Immediate;Lnet/minecraft/client/network/ClientPlayerEntity;I)V",
            at = @At("HEAD"),
            require = 0
    )
    private void hugoservice$showPreview(float tickProgress, MatrixStack matrices, VertexConsumerProvider.Immediate consumers, ClientPlayerEntity player, int light, CallbackInfo ci) {
        ItemStack preview = HandPreview.stack();
        if (preview == null) {
            return;
        }
        this.hugoservice$realMainHand = this.mainHand;
        this.hugoservice$realProgress = this.equipProgressMainHand;
        this.hugoservice$realLastProgress = this.lastEquipProgressMainHand;
        this.mainHand = preview;
        this.equipProgressMainHand = 1.0f;
        this.lastEquipProgressMainHand = 1.0f;
    }

    @Inject(
            method = "renderItem(FLnet/minecraft/client/util/math/MatrixStack;Lnet/minecraft/client/render/VertexConsumerProvider$Immediate;Lnet/minecraft/client/network/ClientPlayerEntity;I)V",
            at = @At("RETURN"),
            require = 0
    )
    private void hugoservice$restore(float tickProgress, MatrixStack matrices, VertexConsumerProvider.Immediate consumers, ClientPlayerEntity player, int light, CallbackInfo ci) {
        if (this.hugoservice$realMainHand != null) {
            this.mainHand = this.hugoservice$realMainHand;
            this.equipProgressMainHand = this.hugoservice$realProgress;
            this.lastEquipProgressMainHand = this.hugoservice$realLastProgress;
            this.hugoservice$realMainHand = null;
        }
    }
}
