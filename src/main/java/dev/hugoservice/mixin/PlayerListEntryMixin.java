package dev.hugoservice.mixin;

import dev.hugoservice.NameFaker;
import net.minecraft.client.network.PlayerListEntry;
import net.minecraft.client.util.DefaultSkinHelper;
import net.minecraft.entity.player.SkinTextures;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** Name faker: default skin heads in the tab list. */
@Mixin(PlayerListEntry.class)
public class PlayerListEntryMixin {
    @Inject(method = "getSkinTextures", at = @At("HEAD"), cancellable = true)
    private void hugoservice$skin(CallbackInfoReturnable<SkinTextures> cir) {
        PlayerListEntry self = (PlayerListEntry) (Object) this;
        if (NameFaker.hideSkin(self.getProfile().id())) cir.setReturnValue(DefaultSkinHelper.getSkinTextures(self.getProfile().id()));
    }
}
