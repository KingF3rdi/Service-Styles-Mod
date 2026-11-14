package dev.hugoservice.mixin;

import dev.hugoservice.Mc;
import dev.hugoservice.NameFaker;
import dev.hugoservice.Presence;
import net.minecraft.client.gui.hud.PlayerListHud;
import net.minecraft.client.network.PlayerListEntry;
import net.minecraft.text.Text;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(PlayerListHud.class)
public class PlayerListHudMixin {
    @Inject(method = "getPlayerName", at = @At("RETURN"), cancellable = true)
    private void hugoservice$icon(PlayerListEntry entry, CallbackInfoReturnable<Text> cir) {
        cir.setReturnValue(Presence.decorate(Mc.profileId(entry.getProfile()), NameFaker.apply((Text)cir.getReturnValue(), true)));
    }
}
