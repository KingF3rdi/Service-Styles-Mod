/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  net.minecraft.client.texture.NativeImageBackedTexture
 *  net.minecraft.client.render.OverlayTexture
 *  org.spongepowered.asm.mixin.Mixin
 *  org.spongepowered.asm.mixin.gen.Accessor
 */
package dev.hugoservice.mixin;

import net.minecraft.client.texture.NativeImageBackedTexture;
import net.minecraft.client.render.OverlayTexture;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(value={OverlayTexture.class})
public interface OverlayTextureAccessor {
    @Accessor(value="texture")
    public NativeImageBackedTexture hugoservice$texture();
}

