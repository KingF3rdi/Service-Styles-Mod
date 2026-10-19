package dev.hugoservice.mixin;

import dev.hugoservice.NameFaker;
import net.minecraft.client.network.PlayerListEntry;
import net.minecraft.entity.player.SkinTextures;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** Name faker: the fake name's skin (or a default one) on heads in the tab list. */
@Mixin(PlayerListEntry.class)
public class PlayerListEntryMixin {
    @Inject(method = "getSkinTextures", at = @At("HEAD"), cancellable = true)
    private void hugoservice$skin(CallbackInfoReturnable<SkinTextures> cir) {
        PlayerListEntry self = (PlayerListEntry) (Object) this;
        SkinTextures skin = NameFaker.skin(self.getProfile().id());
        if (skin != null) cir.setReturnValue(skin);
    }
}
