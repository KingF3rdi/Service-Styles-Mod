// Based on Titus Cosmetics 1.3.22, MIT License, Copyright (c) 2026 Titus. See LICENSE_TitusCosmetics.txt.
package dev.hugoservice.cosmetics;

/** How a cosmetic moves while it is worn. The renderer decides the details, see {@link MotionMath} for the curves. */
public enum Motion {
	/** Does not move with the player's animation beyond the anchor. */
	NONE,
	/** Bobs up and down slowly. */
	FLOAT,
	/** Turns around its vertical axis. */
	SPIN,
	/** Wings: opens and closes. */
	FLAP,
	/** Hangs and sways with the walking animation, like a cape. */
	SWAY,
	/** A pet that hovers beside the body: it bobs, and it turns a little from side to side. */
	HOVER,
	/** Like {@link #HOVER}, and the boxes marked as wings beat up and down. */
	FLUTTER,
	/** Each box rises and falls with a phase that depends on where it is, so a wave runs along the cosmetic. */
	WAVE,
	/** Breathes: the whole cosmetic grows and shrinks a little around its anchor. */
	PULSE,
	/** Overlay: a glint that scrolls over the skin and shimmers in brightness. */
	SHIMMER,
	/** Overlay: lightning, dark most of the time with short bright flashes. */
	FLICKER,
	/** Overlay: a translucent coat that breathes slowly in opacity. */
	DRIFT
}
