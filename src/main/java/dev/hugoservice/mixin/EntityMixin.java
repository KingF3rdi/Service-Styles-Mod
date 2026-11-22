/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  net.minecraft.entity.Entity
 *  net.minecraft.entity.player.PlayerEntity
 *  org.spongepowered.asm.mixin.Mixin
 *  org.spongepowered.asm.mixin.injection.At
 *  org.spongepowered.asm.mixin.injection.Inject
 *  org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable
 */
package dev.hugoservice.mixin;

import dev.hugoservice.Fx;
import dev.hugoservice.HandPreview;
import net.minecraft.client.MinecraftClient;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.PlayerEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(value={Entity.class})
public class EntityMixin {
    @Inject(method={"getTeamColorValue"}, at={@At(value="HEAD")}, cancellable=true)
    private void hugoservice$glowColor(CallbackInfoReturnable<Integer> cir) {
        Entity self = (Entity)(Object)this;
        if (self instanceof PlayerEntity && Fx.glowing(self.getId())) {
            cir.setReturnValue(Fx.glowColor());
        }
    }

    /** Fire height slider dragged: you burn, so the fire overlay shows at the new height. */
    @Inject(method={"isOnFire"}, at={@At(value="HEAD")}, cancellable=true, require=0)
    private void hugoservice$previewFire(CallbackInfoReturnable<Boolean> cir) {
        if (HandPreview.scene() == HandPreview.Scene.FIRE && (Object)this == MinecraftClient.getInstance().player) {
            cir.setReturnValue(true);
        }
    }
}

