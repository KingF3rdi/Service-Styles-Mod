/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.llamalad7.mixinextras.injector.wrapoperation.Operation
 *  com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation
 *  com.llamalad7.mixinextras.sugar.Local
 *  net.minecraft.entity.effect.StatusEffects
 *  net.minecraft.client.MinecraftClient
 *  net.minecraft.block.enums.CameraSubmersionType
 *  net.minecraft.client.network.ClientPlayerEntity
 *  net.minecraft.client.render.fog.FogRenderer
 *  org.joml.Vector4f
 *  org.spongepowered.asm.mixin.Mixin
 *  org.spongepowered.asm.mixin.injection.At
 */
package dev.hugoservice.mixin;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.llamalad7.mixinextras.sugar.Local;
import dev.hugoservice.Config;
import java.nio.ByteBuffer;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.client.MinecraftClient;
import net.minecraft.block.enums.CameraSubmersionType;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.client.render.fog.FogRenderer;
import org.joml.Vector4f;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

@Mixin(value={FogRenderer.class})
public class FogRendererMixin {
    @WrapOperation(method={"applyFog(Lnet/minecraft/client/render/Camera;ILnet/minecraft/client/render/RenderTickCounter;FLnet/minecraft/client/world/ClientWorld;)Lorg/joml/Vector4f;"}, require=0, at={@At(value="INVOKE", target="Lnet/minecraft/client/render/fog/FogRenderer;applyFog(Ljava/nio/ByteBuffer;ILorg/joml/Vector4f;FFFFFF)V")})
    private void hugoservice$noFog(FogRenderer self, ByteBuffer buffer, int offset, Vector4f color, float envStart, float envEnd, float distStart, float distEnd, float skyEnd, float cloudEnd, Operation<Void> original, @Local CameraSubmersionType submersion) {
        boolean clear;
        ClientPlayerEntity player = MinecraftClient.getInstance().player;
        boolean bl = clear = Config.INSTANCE.noFog && submersion == CameraSubmersionType.NONE && player != null && !player.hasStatusEffect(StatusEffects.BLINDNESS) && !player.hasStatusEffect(StatusEffects.DARKNESS);
        if (clear) {
            distStart = 1000000.0f;
            envStart = 1000000.0f;
            distEnd = 1000001.0f;
            envEnd = 1000001.0f;
        }
        original.call(new Object[]{self, buffer, offset, color, Float.valueOf(envStart), Float.valueOf(envEnd), Float.valueOf(distStart), Float.valueOf(distEnd), Float.valueOf(skyEnd), Float.valueOf(cloudEnd)});
    }
}

