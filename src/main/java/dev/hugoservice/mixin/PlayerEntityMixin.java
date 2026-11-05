/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  net.minecraft.entity.player.PlayerEntity
 *  net.minecraft.text.Text
 *  org.spongepowered.asm.mixin.Mixin
 *  org.spongepowered.asm.mixin.injection.At
 *  org.spongepowered.asm.mixin.injection.Inject
 *  org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable
 */
package dev.hugoservice.mixin;

import dev.hugoservice.Mc;
import dev.hugoservice.Presence;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.text.Text;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(value={PlayerEntity.class})
public class PlayerEntityMixin {
    @Inject(method={"getDisplayName"}, at={@At(value="RETURN")}, cancellable=true)
    private void hugoservice$icon(CallbackInfoReturnable<Text> cir) {
        PlayerEntity self = (PlayerEntity)(Object)this;
        if (Mc.clientSide(self)) {
            cir.setReturnValue(Presence.decorate(self.getUuid(), (Text)cir.getReturnValue()));
        }
    }
}

