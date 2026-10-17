// Based on Titus Cosmetics 1.3.22, MIT License, Copyright (c) 2026 Titus. See LICENSE_TitusCosmetics.txt.
package dev.hugoservice.cosmetics;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.mojang.logging.LogUtils;
import net.fabricmc.loader.api.FabricLoader;
import org.slf4j.Logger;

import java.io.IOException;
import java.io.Reader;
import java.nio.charset.StandardCharsets;
import java.nio.file.AtomicMoveNotSupportedException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.HashMap;
import java.util.Map;

/**
 * How fast and how strong the overlay cosmetics of the local player look. Only the local player sets these values, so
 * other players see the overlays with the default values. Saved as {@code config/hugoservice/overlays.json}.
 */
public final class OverlayTuning {
	public static final float SPEED_MIN = 0.25f;
	public static final float SPEED_MAX = 3.0f;
	public static final float STRENGTH_MIN = 0.2f;
	public static final float STRENGTH_MAX = 1.0f;
	private static final float DEFAULT = 1.0f;
	private static final Logger LOGGER = LogUtils.getLogger();
	private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();

	private static Map<String, Float> speeds = new HashMap<>();
	private static Map<String, Float> strengths = new HashMap<>();
	private static boolean loaded;

	private OverlayTuning() {
	}

	/**
	 * Animation speed of an overlay, 1 is the default. Never outside {@link #SPEED_MIN} to {@link #SPEED_MAX}. Read every
	 * frame, so it does not box the default.
	 */
	public static synchronized float speed(String cosmeticId) {
		ensureLoaded();
		return valueOf(speeds, cosmeticId, SPEED_MIN, SPEED_MAX);
	}

	/** Brightness of an overlay, 1 is the default. Never outside {@link #STRENGTH_MIN} to {@link #STRENGTH_MAX}. */
	public static synchronized float strength(String cosmeticId) {
		ensureLoaded();
		return valueOf(strengths, cosmeticId, STRENGTH_MIN, STRENGTH_MAX);
	}

	/** Sets the speed of an overlay, clamps it and saves the file. Unknown or invalid values are ignored. */
	public static synchronized void setSpeed(String cosmeticId, float value) {
		if (cosmeticId == null || Float.isNaN(value)) {
			return;
		}
		ensureLoaded();
		speeds.put(cosmeticId, clamp(value, SPEED_MIN, SPEED_MAX));
		save();
	}

	/** Sets the strength of an overlay, clamps it and saves the file. Unknown or invalid values are ignored. */
	public static synchronized void setStrength(String cosmeticId, float value) {
		if (cosmeticId == null || Float.isNaN(value)) {
			return;
		}
		ensureLoaded();
		strengths.put(cosmeticId, clamp(value, STRENGTH_MIN, STRENGTH_MAX));
		save();
	}

	/** The stored value of an overlay, or the default when it has none, clamped to the range. */
	private static float valueOf(Map<String, Float> values, String cosmeticId, float min, float max) {
		Float value = values.get(cosmeticId);
		return value == null ? DEFAULT : clamp(value, min, max);
	}

	/**
	 * Clamps a setting into its range. NaN gives the default. A pure function, so the tests can use it without the game.
	 */
	static float clamp(float value, float min, float max) {
		return Float.isNaN(value) ? DEFAULT : Math.max(min, Math.min(max, value));
	}

	/** File format. The field names are part of the stored data. */
	private static final class Saved {
		Map<String, Float> speed = new HashMap<>();
		Map<String, Float> strength = new HashMap<>();
	}

	private static Path file() {
		return FabricLoader.getInstance().getConfigDir().resolve("hugoservice").resolve("overlays.json");
	}

	private static void ensureLoaded() {
		if (loaded) {
			return;
		}
		loaded = true;
		Path file = file();
		if (!Files.isRegularFile(file)) {
			return;
		}
		try (Reader reader = Files.newBufferedReader(file, StandardCharsets.UTF_8)) {
			Saved saved = GSON.fromJson(reader, Saved.class);
			if (saved != null) {
				speeds = saved.speed == null ? new HashMap<>() : new HashMap<>(saved.speed);
				strengths = saved.strength == null ? new HashMap<>() : new HashMap<>(saved.strength);
			}
		} catch (IOException | RuntimeException e) {
			LOGGER.warn("Could not read the overlay settings from {}, using the defaults", file, e);
		}
	}

	/** Writes the file through a temporary file, so a crash cannot leave half of it behind. */
	private static void save() {
		Path file = file();
		Path tmp = file.resolveSibling(file.getFileName() + ".tmp");
		Saved data = new Saved();
		data.speed = speeds;
		data.strength = strengths;
		try {
			Files.createDirectories(file.getParent());
			Files.writeString(tmp, GSON.toJson(data), StandardCharsets.UTF_8);
			try {
				Files.move(tmp, file, StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE);
			} catch (AtomicMoveNotSupportedException e) {
				Files.move(tmp, file, StandardCopyOption.REPLACE_EXISTING);
			}
		} catch (IOException | RuntimeException e) {
			LOGGER.warn("Could not save the overlay settings to {}", file, e);
		}
	}
}
