package dev.hugoservice.mixin;

import dev.hugoservice.Config;
import dev.hugoservice.Fx;
import net.minecraft.client.render.debug.EntityHitboxDebugRenderer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyVariable;

/** F3+B hitbox colour: replaces the white main box (green = server-side box stays). */
@Mixin(EntityHitboxDebugRenderer.class)
public class EntityHitboxDebugRendererMixin {
    @ModifyVariable(method = "drawHitbox", at = @At("STORE"), ordinal = 0)
    private int hugoservice$hitboxColor(int color) {
        Integer c = Config.INSTANCE.fx(Fx.HITBOX);
        return c != null && color == -1 ? 0xFF000000 | c : color;
    }
}
