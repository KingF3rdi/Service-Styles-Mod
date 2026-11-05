package dev.hugoservice.mixin;

import dev.hugoservice.Config;
import net.minecraft.client.render.debug.EntityHitboxDebugRenderer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyVariable;

@Mixin(value={EntityHitboxDebugRenderer.class})
public class EntityHitboxDebugRendererMixin {
    @ModifyVariable(method={"drawHitbox"}, at={@At(value="STORE")}, ordinal=0)
    private int hugoservice$hitboxColor(int color) {
        Integer c = Config.INSTANCE.fx("hitbox");
        return c != null && color == -1 ? 0xFF000000 | c : color;
    }
}
