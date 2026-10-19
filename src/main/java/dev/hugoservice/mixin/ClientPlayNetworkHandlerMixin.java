package dev.hugoservice.mixin;

import dev.hugoservice.KillEffects;
import dev.hugoservice.TotemCounter;
import net.minecraft.client.network.ClientPlayNetworkHandler;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityStatuses;
import net.minecraft.entity.LivingEntity;
import net.minecraft.network.packet.s2c.play.EntityStatusS2CPacket;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Totem pops (status 35, handled here so it never reaches the entity) and deaths (status 3) for the kill effects. */
@Mixin(ClientPlayNetworkHandler.class)
public class ClientPlayNetworkHandlerMixin {
    @Inject(method = "onEntityStatus", at = @At("TAIL"))
    private void hugoservice$pop(EntityStatusS2CPacket packet, CallbackInfo ci) {
        byte status = packet.getStatus();
        if (status != EntityStatuses.USE_TOTEM_OF_UNDYING && status != EntityStatuses.PLAY_DEATH_SOUND_OR_ADD_PROJECTILE_HIT_PARTICLES) return;
        Entity e = packet.getEntity(((ClientPlayNetworkHandler) (Object) this).getWorld());
        if (!(e instanceof LivingEntity living)) return;
        if (status == EntityStatuses.USE_TOTEM_OF_UNDYING) TotemCounter.popped(living);
        else KillEffects.died(living);
    }
}
