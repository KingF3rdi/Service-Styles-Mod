package dev.hugoservice.mixin;

import dev.hugoservice.Config;
import dev.hugoservice.Fx;
import dev.hugoservice.Mc;
import net.minecraft.client.render.RenderLayer;
import net.minecraft.client.render.command.OrderedRenderCommandQueue;
import net.minecraft.client.render.entity.model.ShieldEntityModel;
import net.minecraft.client.render.item.model.special.ShieldModelRenderer;
import net.minecraft.client.render.model.ModelBaker;
import net.minecraft.client.texture.SpriteHolder;
import net.minecraft.client.util.SpriteIdentifier;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.component.ComponentMap;
import net.minecraft.component.DataComponentTypes;
import net.minecraft.component.type.BannerPatternsComponent;
import net.minecraft.item.ItemDisplayContext;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyArg;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

// priority below the default: a lower-priority mixin is applied first, so this HEAD callback runs before
// other mods' (ShieldStatus cancels the vanilla draw there; checked in game with ShieldStatus 4.1.2)
@Mixin(value={ShieldModelRenderer.class}, priority=900)
public class ShieldModelRendererMixin {
    @Shadow @Final private ShieldEntityModel model;
    @Shadow @Final private SpriteHolder spriteHolder;
    @Unique
    private static boolean hugoservice$firstPerson;

    /** See-through shield: drawn here and the vanilla draw skipped, so a mod that replaces the vanilla draw can't undo it. */
    @Inject(method={"render(Lnet/minecraft/component/ComponentMap;Lnet/minecraft/item/ItemDisplayContext;Lnet/minecraft/client/util/math/MatrixStack;Lnet/minecraft/client/render/command/OrderedRenderCommandQueue;IIZI)V"}, at={@At(value="HEAD")}, cancellable=true)
    private void hugoservice$context(ComponentMap data, ItemDisplayContext context, MatrixStack matrices, OrderedRenderCommandQueue queue, int light, int overlay, boolean glint, int seed, CallbackInfo ci) {
        hugoservice$firstPerson = context == ItemDisplayContext.FIRST_PERSON_LEFT_HAND || context == ItemDisplayContext.FIRST_PERSON_RIGHT_HAND;
        int opacity = Config.INSTANCE.shieldOpacity;
        if (!hugoservice$firstPerson || opacity >= 100) {
            return;
        }
        Integer tint = Fx.shieldTint();
        int color = Math.max(10, opacity) * 255 / 100 << 24 | (tint == null ? 0xFFFFFF : tint & 0xFFFFFF);
        // ponytail: banner patterns are left off while see-through; draw them with BannerBlockEntityRenderer.renderCanvas if missed
        boolean patterned = data != null && (!data.getOrDefault(DataComponentTypes.BANNER_PATTERNS, BannerPatternsComponent.DEFAULT).layers().isEmpty() || data.get(DataComponentTypes.BASE_COLOR) != null);
        SpriteIdentifier id = patterned ? ModelBaker.SHIELD_BASE : ModelBaker.SHIELD_BASE_NO_PATTERN;
        RenderLayer layer = Mc.entityTranslucent(id.getAtlasId());
        matrices.push();
        matrices.scale(1.0f, -1.0f, -1.0f);
        queue.submitModelPart(this.model.getHandle(), matrices, layer, light, overlay, this.spriteHolder.getSprite(id), false, false, color, null, 0);
        queue.submitModelPart(this.model.getPlate(), matrices, layer, light, overlay, this.spriteHolder.getSprite(id), false, glint, color, null, seed);
        matrices.pop();
        ci.cancel();
    }

    @ModifyArg(method={"render(Lnet/minecraft/component/ComponentMap;Lnet/minecraft/item/ItemDisplayContext;Lnet/minecraft/client/util/math/MatrixStack;Lnet/minecraft/client/render/command/OrderedRenderCommandQueue;IIZI)V"}, index=8, at={@At(value="INVOKE", target="Lnet/minecraft/client/render/command/OrderedRenderCommandQueue;submitModelPart(Lnet/minecraft/client/model/ModelPart;Lnet/minecraft/client/util/math/MatrixStack;Lnet/minecraft/client/render/RenderLayer;IILnet/minecraft/client/texture/Sprite;ZZILnet/minecraft/client/render/command/ModelCommandRenderer$CrumblingOverlayCommand;I)V")})
    private int hugoservice$tint(int color) {
        Integer tint = hugoservice$firstPerson ? Fx.shieldTint() : null;
        return tint == null ? color : 0xFF000000 | tint;
    }
}
