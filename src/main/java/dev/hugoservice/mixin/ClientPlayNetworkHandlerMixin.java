package dev.hugoservice.mixin;

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

/** Totem counter: notices totem pops; the handler deals with status 35 itself, so it never reaches the entity. */
@Mixin(ClientPlayNetworkHandler.class)
public class ClientPlayNetworkHandlerMixin {
    @Inject(method = "onEntityStatus", at = @At("TAIL"))
    private void hugoservice$pop(EntityStatusS2CPacket packet, CallbackInfo ci) {
        if (packet.getStatus() != EntityStatuses.USE_TOTEM_OF_UNDYING) return;
        Entity e = packet.getEntity(((ClientPlayNetworkHandler) (Object) this).getWorld());
        if (e instanceof LivingEntity living) TotemCounter.popped(living);
    }
}
