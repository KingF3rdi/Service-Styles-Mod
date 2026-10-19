package dev.hugoservice.mixin;

import dev.hugoservice.TotemCounter;
import net.minecraft.client.render.entity.EntityRenderer;
import net.minecraft.entity.Entity;
import net.minecraft.text.Text;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** Totem counter: pops after a player's name tag. require = 0: without it only the counter over heads is missing. */
@Mixin(EntityRenderer.class)
public class EntityRendererMixin {
    @Inject(method = "getDisplayName", at = @At("RETURN"), cancellable = true, require = 0)
    private void hugoservice$pops(Entity entity, CallbackInfoReturnable<Text> cir) {
        Text pops = TotemCounter.suffix(entity);
        if (pops != null && cir.getReturnValue() != null) cir.setReturnValue(cir.getReturnValue().copy().append(pops));
    }
}
