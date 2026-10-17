// Based on Titus Cosmetics 1.3.22, MIT License, Copyright (c) 2026 Titus. See LICENSE_TitusCosmetics.txt.
package dev.hugoservice.cosmetics;

/**
 * One box of a cosmetic, in player model units: 16 units make one block, the player is 32 units tall. The coordinates
 * are relative to the pivot of the anchor part, with Y pointing down as in the vanilla player model. The top of the
 * head is therefore at {@code y = -8}, and a hat above it starts at a negative {@code y}. The box spans
 * {@code x..x+w}, {@code y..y+h} and {@code z..z+d}. Front of the player is {@code -z}, back is {@code +z}.
 *
 * <p>The sizes must be whole units, because the texture is laid out in whole texels.
 *
 * @param u    texture offset in the 64x64 cosmetic texture, laid out like a vanilla cube
 * @param v    texture offset in the 64x64 cosmetic texture
 * @param wing {@code true} for a box that beats with the wings of a {@link Motion#FLUTTER} cosmetic
 */
public record CosmeticBox(float x, float y, float z, float w, float h, float d, int u, int v, boolean wing) {

	/** A box that is not a wing. */
	public CosmeticBox(float x, float y, float z, float w, float h, float d, int u, int v) {
		this(x, y, z, w, h, d, u, v, false);
	}

	/** A box that beats with the wings of a {@link Motion#FLUTTER} cosmetic. */
	public static CosmeticBox wing(float x, float y, float z, float w, float h, float d, int u, int v) {
		return new CosmeticBox(x, y, z, w, h, d, u, v, true);
	}
}
