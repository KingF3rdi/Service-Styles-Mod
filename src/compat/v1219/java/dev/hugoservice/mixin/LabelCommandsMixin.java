/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  net.minecraft.client.render.command.LabelCommandRenderer$Commands
 *  net.minecraft.text.Text
 *  org.spongepowered.asm.mixin.Mixin
 *  org.spongepowered.asm.mixin.injection.At
 *  org.spongepowered.asm.mixin.injection.ModifyArgs
 *  org.spongepowered.asm.mixin.injection.ModifyVariable
 *  org.spongepowered.asm.mixin.injection.invoke.arg.Args
 */
package dev.hugoservice.mixin;

import dev.hugoservice.Config;
import dev.hugoservice.NameFaker;
import net.minecraft.client.render.command.LabelCommandRenderer;
import net.minecraft.text.Text;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyArgs;
import org.spongepowered.asm.mixin.injection.ModifyVariable;
import org.spongepowered.asm.mixin.injection.invoke.arg.Args;

@Mixin(value={LabelCommandRenderer.Commands.class})
public class LabelCommandsMixin {
    @ModifyArgs(method={"add"}, at={@At(value="INVOKE", target="Lnet/minecraft/client/util/math/MatrixStack;scale(FFF)V")})
    private void hugoservice$size(Args args) {
        float k = (float)Config.INSTANCE.nametagSize / 100.0f;
        if (k == 1.0f) {
            return;
        }
        args.set(0, Float.valueOf(((Float)args.get(0)).floatValue() * k));
        args.set(1, Float.valueOf(((Float)args.get(1)).floatValue() * k));
        args.set(2, Float.valueOf(((Float)args.get(2)).floatValue() * k));
    }

    @ModifyVariable(method={"add"}, at={@At(value="HEAD")}, argsOnly=true)
    private Text hugoservice$color(Text text) {
        Integer c = Config.INSTANCE.fx("nametag");
        text = NameFaker.apply(text, true);
        return c == null ? text : Text.literal((String)text.getString()).withColor(c.intValue());
    }
}

