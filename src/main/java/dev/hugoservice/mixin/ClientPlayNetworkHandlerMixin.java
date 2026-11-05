/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  net.minecraft.entity.Entity
 *  net.minecraft.entity.LivingEntity
 *  net.minecraft.world.World
 *  net.minecraft.network.packet.s2c.play.EntityStatusS2CPacket
 *  net.minecraft.client.network.ClientPlayNetworkHandler
 *  org.spongepowered.asm.mixin.Mixin
 *  org.spongepowered.asm.mixin.injection.At
 *  org.spongepowered.asm.mixin.injection.Inject
 *  org.spongepowered.asm.mixin.injection.callback.CallbackInfo
 */
package dev.hugoservice.mixin;

import dev.hugoservice.KillEffects;
import dev.hugoservice.TotemCounter;
import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.world.World;
import net.minecraft.network.packet.s2c.play.EntityStatusS2CPacket;
import net.minecraft.client.network.ClientPlayNetworkHandler;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(value={ClientPlayNetworkHandler.class})
public class ClientPlayNetworkHandlerMixin {
    @Inject(method={"onEntityStatus"}, at={@At(value="TAIL")})
    private void hugoservice$pop(EntityStatusS2CPacket packet, CallbackInfo ci) {
        byte status = packet.getStatus();
        if (status != 35 && status != 3) {
            return;
        }
        Entity e = packet.getEntity((World)((ClientPlayNetworkHandler)(Object)this).getWorld());
        if (!(e instanceof LivingEntity)) {
            return;
        }
        LivingEntity living = (LivingEntity)e;
        if (status == 35) {
            TotemCounter.popped(living);
        } else {
            KillEffects.died(living);
        }
    }
}

