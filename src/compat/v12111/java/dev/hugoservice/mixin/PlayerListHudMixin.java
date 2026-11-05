/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  net.minecraft.text.Text
 *  net.minecraft.client.gui.hud.PlayerListHud
 *  net.minecraft.client.network.PlayerListEntry
 *  org.spongepowered.asm.mixin.Mixin
 *  org.spongepowered.asm.mixin.injection.At
 *  org.spongepowered.asm.mixin.injection.Inject
 *  org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable
 */
package dev.hugoservice.mixin;

import dev.hugoservice.NameFaker;
import dev.hugoservice.Presence;
import net.minecraft.text.Text;
import net.minecraft.client.gui.hud.PlayerListHud;
import net.minecraft.client.network.PlayerListEntry;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(value={PlayerListHud.class})
public class PlayerListHudMixin {
    @Inject(method={"getPlayerName"}, at={@At(value="RETURN")}, cancellable=true)
    private void hugoservice$fake(PlayerListEntry entry, CallbackInfoReturnable<Text> cir) {
        cir.setReturnValue(Presence.decorate(entry.getProfile().id(), NameFaker.apply((Text)cir.getReturnValue(), true)));
    }
}

