package dev.hugoservice.mixin;

import dev.hugoservice.NameFaker;
import net.minecraft.client.network.AbstractClientPlayerEntity;
import net.minecraft.client.util.DefaultSkinHelper;
import net.minecraft.entity.player.SkinTextures;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** Name faker: default skin instead of the real one. */
@Mixin(AbstractClientPlayerEntity.class)
public class AbstractClientPlayerEntityMixin {
    @Inject(method = "getSkin", at = @At("HEAD"), cancellable = true)
    private void hugoservice$skin(CallbackInfoReturnable<SkinTextures> cir) {
        AbstractClientPlayerEntity self = (AbstractClientPlayerEntity) (Object) this;
        if (NameFaker.hideSkin(self.getUuid())) cir.setReturnValue(DefaultSkinHelper.getSkinTextures(self.getUuid()));
    }
}
