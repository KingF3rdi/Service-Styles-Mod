// Based on Titus Cosmetics 1.3.22, MIT License, Copyright (c) 2026 Titus. See LICENSE_TitusCosmetics.txt.
package dev.hugoservice.cosmetics;

/**
 * The point of the player model a cosmetic is attached to. The boxes of a cosmetic are relative to this point. Each
 * anchor is a part of the model plus an offset in model units, so a pet can perch on a shoulder and a shield can
 * surround the middle. An anchor also sets the size of its boxes, see {@link #scale()}. {@link #WHOLE} is the exception:
 * an overlay is drawn over every part of the model and has no point of its own.
 */
public enum Anchor {
	/** Top of the head. It turns with the head, so it looks where the player looks. */
	HEAD(0, 0, 0),
	/** The neck, the pivot of the body. It turns with the body, not with the head. */
	BODY(0, 0, 0),
	/** The middle of the figure, from the neck down 8 units. Shields surround the body from here. */
	CORE(0, 8, 0),
	/**
	 * On the +x side of the model, on top of the arm, which spans x 4 to 8 and whose top is at y 0. A pet perches here at
	 * half size, like a parrot on a shoulder: its boxes stand on {@code y = 0}, the feet. The anchor is seven units out, past the side of the head, and 0.6 units above the
	 * arm, so the bob of a pet (up to 0.6 units at this scale) never sinks into the arm.
	 */
	PET(7, -0.6f, 0, 0.5f),
	/** The whole model. Overlays use it: they are drawn over every visible part with the model's own geometry. */
	WHOLE(0, 0, 0);

	private final float x;
	private final float y;
	private final float z;
	private final float scale;

	Anchor(float x, float y, float z) {
		this(x, y, z, 1.0f);
	}

	Anchor(float x, float y, float z, float scale) {
		this.x = x;
		this.y = y;
		this.z = z;
		this.scale = scale;
	}

	/** Offset along x from the part pivot, in model units. */
	public float x() {
		return x;
	}

	/** Offset along y from the part pivot, in model units. */
	public float y() {
		return y;
	}

	/** Offset along z from the part pivot, in model units. */
	public float z() {
		return z;
	}

	/** Size of the boxes at this anchor, relative to their own units. Pets are drawn at half size, everything else at 1. */
	public float scale() {
		return scale;
	}
}
