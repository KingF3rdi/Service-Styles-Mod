/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  net.minecraft.entity.Entity
 *  net.minecraft.text.Text
 *  net.minecraft.client.render.entity.EntityRenderer
 *  org.spongepowered.asm.mixin.Mixin
 *  org.spongepowered.asm.mixin.injection.At
 *  org.spongepowered.asm.mixin.injection.Inject
 *  org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable
 */
package dev.hugoservice.mixin;

import dev.hugoservice.TotemCounter;
import net.minecraft.entity.Entity;
import net.minecraft.text.Text;
import net.minecraft.client.render.entity.EntityRenderer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(value={EntityRenderer.class})
public class EntityRendererMixin {
    @Inject(method={"getDisplayName"}, at={@At(value="RETURN")}, cancellable=true, require=0)
    private void hugoservice$pops(Entity entity, CallbackInfoReturnable<Text> cir) {
        Text pops = TotemCounter.suffix(entity);
        if (pops != null && cir.getReturnValue() != null) {
            cir.setReturnValue(((Text)cir.getReturnValue()).copy().append(pops));
        }
    }
}

