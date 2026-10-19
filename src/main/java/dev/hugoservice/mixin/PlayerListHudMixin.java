package dev.hugoservice.mixin;

import dev.hugoservice.NameFaker;
import net.minecraft.client.gui.hud.PlayerListHud;
import net.minecraft.client.network.PlayerListEntry;
import net.minecraft.text.Text;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** Name faker in the tab list. */
@Mixin(PlayerListHud.class)
public class PlayerListHudMixin {
    @Inject(method = "getPlayerName", at = @At("RETURN"), cancellable = true)
    private void hugoservice$fake(PlayerListEntry entry, CallbackInfoReturnable<Text> cir) {
        cir.setReturnValue(dev.hugoservice.Presence.decorate(entry.getProfile().id(), NameFaker.apply(cir.getReturnValue(), true)));
    }
}
