package dev.hugoservice.mixin;

import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import com.llamalad7.mixinextras.sugar.Local;
import dev.hugoservice.Knives;
import net.minecraft.client.item.ItemModelManager;
import net.minecraft.item.ItemStack;
import net.minecraft.util.Identifier;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

/** Swords are drawn with the chosen knife model, also when a server resource pack re-skins swords. */
@Mixin(ItemModelManager.class)
public class ItemModelManagerMixin {
    @ModifyExpressionValue(method = "update", require = 0, at = @At(value = "INVOKE", ordinal = 0,
        target = "Lnet/minecraft/item/ItemStack;get(Lnet/minecraft/component/ComponentType;)Ljava/lang/Object;"))
    private Object hugoservice$knife(Object model, @Local(argsOnly = true) ItemStack stack) {
        return model instanceof Identifier id ? Knives.swap(stack, id) : model;
    }
}
