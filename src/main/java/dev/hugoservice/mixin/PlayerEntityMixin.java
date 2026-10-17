package dev.hugoservice.mixin;

import dev.hugoservice.Presence;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.text.Text;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** Name icon in front of the name tag of players who use the mod. */
@Mixin(PlayerEntity.class)
public class PlayerEntityMixin {
    @Inject(method = "getDisplayName", at = @At("RETURN"), cancellable = true)
    private void hugoservice$icon(CallbackInfoReturnable<Text> cir) {
        PlayerEntity self = (PlayerEntity) (Object) this;
        if (self.getEntityWorld() != null && self.getEntityWorld().isClient()) cir.setReturnValue(Presence.decorate(self.getUuid(), cir.getReturnValue()));
    }
}
