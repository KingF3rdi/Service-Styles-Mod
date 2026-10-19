package dev.hugoservice.mixin;

import dev.hugoservice.Config;
import dev.hugoservice.Fx;
import net.minecraft.client.render.command.LabelCommandRenderer;
import net.minecraft.text.Text;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyVariable;
import org.spongepowered.asm.mixin.injection.ModifyArgs;
import org.spongepowered.asm.mixin.injection.invoke.arg.Args;

/** Name tag tweaks: size and own colour. */
@Mixin(LabelCommandRenderer.Commands.class)
public class LabelCommandsMixin {
    @ModifyArgs(method = "add", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/util/math/MatrixStack;scale(FFF)V"))
    private void hugoservice$size(Args args) {
        float k = Config.INSTANCE.nametagSize / 100f;
        if (k == 1f) return;
        args.set(0, (float) args.get(0) * k);
        args.set(1, (float) args.get(1) * k);
        args.set(2, (float) args.get(2) * k);
    }

    @ModifyVariable(method = "add", at = @At("HEAD"), argsOnly = true)
    private Text hugoservice$color(Text text) {
        Integer c = Config.INSTANCE.fx(Fx.NAMETAG);
        text = dev.hugoservice.NameFaker.apply(text, true);
        return c == null ? text : Text.literal(text.getString()).withColor(c);
    }
}
