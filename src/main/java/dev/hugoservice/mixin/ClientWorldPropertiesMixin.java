package dev.hugoservice.mixin;

import dev.hugoservice.Config;
import net.minecraft.client.world.ClientWorld;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** Own sky time (client-side only, the server keeps its real time). */
@Mixin(ClientWorld.Properties.class)
public class ClientWorldPropertiesMixin {
    @Inject(method = "getTimeOfDay", at = @At("HEAD"), cancellable = true)
    private void hugoservice$time(CallbackInfoReturnable<Long> cir) {
        int t = Config.INSTANCE.skyTime;
        if (t >= 0) cir.setReturnValue((long) t);
    }
}
