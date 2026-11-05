/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  net.minecraft.client.world.ClientWorld$Properties
 *  org.spongepowered.asm.mixin.Mixin
 *  org.spongepowered.asm.mixin.injection.At
 *  org.spongepowered.asm.mixin.injection.Inject
 *  org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable
 */
package dev.hugoservice.mixin;

import dev.hugoservice.Config;
import net.minecraft.client.world.ClientWorld;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(value={ClientWorld.Properties.class})
public class ClientWorldPropertiesMixin {
    @Inject(method={"getTimeOfDay"}, at={@At(value="HEAD")}, cancellable=true)
    private void hugoservice$time(CallbackInfoReturnable<Long> cir) {
        int t = Config.INSTANCE.skyTime;
        if (t >= 0) {
            cir.setReturnValue((long) t);
        }
    }
}

