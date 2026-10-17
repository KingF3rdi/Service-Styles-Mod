// Based on Titus Cosmetics 1.3.22, MIT License, Copyright (c) 2026 Titus. See LICENSE_TitusCosmetics.txt.
package dev.hugoservice.cosmetics;

/**
 * The curves of the cosmetic motions, as pure functions of the age in ticks. They have no Minecraft code, so the tests
 * can check their ranges. The renderer turns the results into transforms, colours and texture offsets.
 */
public final class MotionMath {
	/** Largest bob of a hovering pet, in model units. */
	public static final float HOVER_BOB_UNITS = 1.2f;
	/** Largest turn of a hovering pet from side to side, in degrees. */
	public static final float HOVER_YAW_DEGREES = 22.0f;
	/** Largest beat of a wing of a fluttering pet, in degrees. */
	public static final float FLUTTER_DEGREES = 16.0f;
	/** Largest growth or shrink of a pulsing cosmetic, as a fraction of its size. */
	public static final float PULSE_FRACTION = 0.035f;
	/** Largest rise or fall of a box in a wave, in model units. */
	public static final float WAVE_UNITS = 1.2f;
	/** Lowest and highest brightness of a shimmering glint. */
	public static final float SHIMMER_LOW = 0.56f;
	public static final float SHIMMER_HIGH = 1.0f;
	/** Brightness of a flicker between flashes. */
	public static final float FLICKER_DARK = 0.08f;

	private MotionMath() {
	}

	/** Vertical offset of a hovering pet, in model units. */
	public static float hoverBob(float age) {
		return HOVER_BOB_UNITS * (float) Math.sin(age * 0.09);
	}

	/** Turn of a hovering pet around its vertical axis, in degrees. */
	public static float hoverYawDegrees(float age) {
		return HOVER_YAW_DEGREES * (float) Math.sin(age * 0.04);
	}

	/** Roll of a hovering pet around its forward axis, in degrees. */
	public static float hoverRollDegrees(float age) {
		return 4.0f * (float) Math.sin(age * 0.09 + 1.0);
	}

	/** Beat of the wings of a fluttering pet, in degrees. The left and right wings take the same value with opposite signs. */
	public static float flutterDegrees(float age) {
		return FLUTTER_DEGREES * (float) Math.sin(age * 0.35);
	}

	/** Scale of a pulsing cosmetic: 1 at rest, a little more or less while it breathes. */
	public static float pulseScale(float age) {
		return 1.0f + PULSE_FRACTION * (float) Math.sin(age * 0.1);
	}

	/** Phase of a box in a wave, from its centre in model units. Boxes that are close together share a phase. */
	public static float wavePhase(float centreX, float centreZ) {
		return (centreX + centreZ) * 0.35f;
	}

	/** Vertical offset of a box in a wave, in model units. */
	public static float waveOffset(float age, float phase) {
		return WAVE_UNITS * (float) Math.sin(age * 0.12 - phase);
	}

	/** Brightness of a shimmering glint, from {@link #SHIMMER_LOW} to {@link #SHIMMER_HIGH}. */
	public static float shimmerBrightness(float age) {
		double wobble = 0.7 * Math.sin(age * 0.023);
		double mid = (SHIMMER_HIGH + SHIMMER_LOW) / 2;
		double half = (SHIMMER_HIGH - SHIMMER_LOW) / 2;
		return (float) (mid + half * Math.sin(age * 0.11 + wobble));
	}

	/** Brightness of lightning: mostly {@link #FLICKER_DARK}, with short flashes up to 1. */
	public static float flickerBrightness(float age) {
		double pulse = Math.sin(age * 0.37) * Math.sin(age * 0.113 + 1.3);
		double flash = Math.pow(Math.max(0.0, pulse), 6);
		return (float) (FLICKER_DARK + (1.0 - FLICKER_DARK) * flash);
	}

	/** Opacity of a translucent coat, from 0.68 to 0.92. */
	public static float coatAlpha(float age) {
		return 0.8f + 0.12f * (float) Math.sin(age * 0.04);
	}

	/** Position of a scrolling texture across one repeat, from 0 up to but not including 1. */
	public static float scroll(float age) {
		double s = age * 0.015;
		return (float) (s - Math.floor(s));
	}
}
