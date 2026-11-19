package dev.hugoservice.mixin;

import dev.hugoservice.Config;
import net.minecraft.block.spawner.MobSpawnerLogic;
import net.minecraft.entity.Entity;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Vanilla {@code clientTick} freezes the inner mob's yaw when no player is within
 * {@code requiredPlayerRange} (16). On duel maps that leaves one cage spinning and
 * the other still. When the extra is on, keep the same rotation formula on every
 * loaded spawner and skip the far-cage freeze (particles stay vanilla-ranged).
 */
@Mixin(MobSpawnerLogic.class)
public abstract class MobSpawnerLogicMixin {
    @Shadow
    private double rotation;

    @Shadow
    private double lastRotation;

    @Shadow
    private int spawnDelay;

    @Shadow
    private Entity renderedEntity;

    @Shadow
    protected abstract boolean isPlayerInRange(World world, BlockPos pos);

    @Shadow
    public abstract Entity getRenderedEntity(World world, BlockPos pos);

    @Inject(method = "clientTick", at = @At("HEAD"), cancellable = true, require = 0)
    private void hugoservice$spinBoth(World world, BlockPos pos, CallbackInfo ci) {
        try {
            if (!Config.INSTANCE.spawnerSpin) {
                return;
            }
            if (this.isPlayerInRange(world, pos)) {
                return;
            }
            if (this.renderedEntity == null) {
                this.getRenderedEntity(world, pos);
            }
            this.lastRotation = this.rotation;
            if (this.renderedEntity != null) {
                this.rotation = (this.rotation + (double) (1000.0f / ((float) this.spawnDelay + 200.0f))) % 360.0;
            }
            ci.cancel();
        } catch (Throwable ignored) {
        }
    }
}
