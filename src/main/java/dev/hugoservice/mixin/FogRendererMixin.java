package dev.hugoservice.mixin;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.llamalad7.mixinextras.sugar.Local;
import dev.hugoservice.Config;
import net.minecraft.block.enums.CameraSubmersionType;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.render.fog.FogRenderer;
import net.minecraft.entity.effect.StatusEffects;
import org.joml.Vector4f;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

import java.nio.ByteBuffer;

/**
 * No fog: pushes the distance and terrain fog out of sight while you are in the open air. Water, lava, snow and the
 * blindness or darkness effects keep theirs, seeing through those would be an advantage.
 */
@Mixin(FogRenderer.class)
public class FogRendererMixin {
    @WrapOperation(method = "applyFog(Lnet/minecraft/client/render/Camera;ILnet/minecraft/client/render/RenderTickCounter;FLnet/minecraft/client/world/ClientWorld;)Lorg/joml/Vector4f;",
        require = 0, at = @At(value = "INVOKE", target = "Lnet/minecraft/client/render/fog/FogRenderer;applyFog(Ljava/nio/ByteBuffer;ILorg/joml/Vector4f;FFFFFF)V"))
    private void hugoservice$noFog(FogRenderer self, ByteBuffer buffer, int offset, Vector4f color, float envStart, float envEnd,
                                   float distStart, float distEnd, float skyEnd, float cloudEnd, Operation<Void> original,
                                   @Local CameraSubmersionType submersion) {
        var player = MinecraftClient.getInstance().player;
        boolean clear = Config.INSTANCE.noFog && submersion == CameraSubmersionType.NONE && player != null
            && !player.hasStatusEffect(StatusEffects.BLINDNESS) && !player.hasStatusEffect(StatusEffects.DARKNESS);
        if (clear) {
            envStart = distStart = 1e6f;
            envEnd = distEnd = 1e6f + 1;
        }
        original.call(self, buffer, offset, color, envStart, envEnd, distStart, distEnd, skyEnd, cloudEnd);
    }
}
