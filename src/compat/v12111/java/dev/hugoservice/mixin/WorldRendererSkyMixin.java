package dev.hugoservice.mixin;

import com.mojang.blaze3d.buffers.GpuBufferSlice;
import dev.hugoservice.Sky;
import net.minecraft.client.render.Camera;
import net.minecraft.client.render.FrameGraphBuilder;
import net.minecraft.client.render.WorldRenderer;
import net.minecraft.client.render.state.WorldRenderState;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(WorldRenderer.class)
public class WorldRendererSkyMixin {
    @Shadow
    @Final
    private WorldRenderState worldRenderState;

    @Inject(method = "renderSky", at = @At("HEAD"), require = 0)
    private void hugoservice$captureSky(FrameGraphBuilder frameGraphBuilder, Camera camera, GpuBufferSlice fog, CallbackInfo ci) {
        if (this.worldRenderState != null) {
            Sky.state = this.worldRenderState.skyRenderState;
        }
    }
}
