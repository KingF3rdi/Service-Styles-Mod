// Based on Titus Cosmetics 1.3.22, MIT License, Copyright (c) 2026 Titus. See LICENSE_TitusCosmetics.txt.
package dev.hugoservice.cosmetics;

import java.util.ArrayList;
import java.util.Collections;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * What one player wears: at most one cosmetic per category. Instances never change, so the render thread, the GUI and
 * the network thread can share them without locks. Every change returns a new loadout.
 */
public final class Loadout {
	public static final Loadout EMPTY = new Loadout(Map.of());
	/** Longest text {@link #decode} reads. Anything after it is ignored, so a broken packet cannot flood the client. */
	public static final int MAX_LENGTH = 256;

	private final Map<CosmeticCategory, String> worn;

	private Loadout(Map<CosmeticCategory, String> worn) {
		this.worn = worn;
	}

	/** Id of the cosmetic worn in this category, or {@code null}. */
	public String idOf(CosmeticCategory category) {
		return worn.get(category);
	}

	/** The cosmetic worn in this category, or {@code null}. */
	public Cosmetic get(CosmeticCategory category) {
		String id = worn.get(category);
		return id == null ? null : CosmeticCatalog.byId(id).orElse(null);
	}

	/** Everything worn, in sidebar order. */
	public List<Cosmetic> all() {
		List<Cosmetic> list = new ArrayList<>();
		for (CosmeticCategory category : CosmeticCategory.values()) {
			Cosmetic cosmetic = get(category);
			if (cosmetic != null) {
				list.add(cosmetic);
			}
		}
		return list;
	}

	public boolean isEmpty() {
		return worn.isEmpty();
	}

	/** The same loadout with this cosmetic in its category. What was worn there is replaced. */
	public Loadout with(Cosmetic cosmetic) {
		Map<CosmeticCategory, String> next = copyOfWorn();
		next.put(cosmetic.category(), cosmetic.id());
		return new Loadout(Collections.unmodifiableMap(next));
	}

	/** The same loadout with nothing worn in this category. */
	public Loadout without(CosmeticCategory category) {
		if (!worn.containsKey(category)) {
			return this;
		}
		Map<CosmeticCategory, String> next = copyOfWorn();
		next.remove(category);
		return new Loadout(Collections.unmodifiableMap(next));
	}

	/** A mutable copy. An EnumMap cannot be built from an empty map that is not an EnumMap, so it is filled by hand. */
	private Map<CosmeticCategory, String> copyOfWorn() {
		Map<CosmeticCategory, String> copy = new EnumMap<>(CosmeticCategory.class);
		copy.putAll(worn);
		return copy;
	}

	/** Text form, for example {@code CAPE=obsidian_cape;HAT=crown}. Categories are written in sidebar order. */
	public String encode() {
		StringBuilder text = new StringBuilder();
		for (CosmeticCategory category : CosmeticCategory.values()) {
			String id = worn.get(category);
			if (id != null) {
				if (!text.isEmpty()) {
					text.append(';');
				}
				text.append(category.name()).append('=').append(id);
			}
		}
		return text.toString();
	}

	/**
	 * Reads the text form made by {@link #encode()}. Unknown categories and unknown cosmetic ids are skipped, and text
	 * after {@link #MAX_LENGTH} characters is ignored, so this never fails and never returns something unusable.
	 */
	public static Loadout decode(String text) {
		if (text == null || text.isEmpty()) {
			return EMPTY;
		}
		String clipped = text.length() > MAX_LENGTH ? text.substring(0, MAX_LENGTH) : text;
		Map<CosmeticCategory, String> worn = new EnumMap<>(CosmeticCategory.class);
		for (String pair : clipped.split(";")) {
			int equals = pair.indexOf('=');
			if (equals <= 0) {
				continue;
			}
			CosmeticCategory category = categoryNamed(pair.substring(0, equals));
			String id = pair.substring(equals + 1);
			if (category == null || CosmeticCatalog.byId(id).isEmpty()) {
				continue;
			}
			Cosmetic cosmetic = CosmeticCatalog.byId(id).orElseThrow();
			if (cosmetic.category() == category) {
				worn.put(category, id);
			}
		}
		return worn.isEmpty() ? EMPTY : new Loadout(Collections.unmodifiableMap(worn));
	}

	private static CosmeticCategory categoryNamed(String name) {
		for (CosmeticCategory category : CosmeticCategory.values()) {
			if (category.name().equals(name)) {
				return category;
			}
		}
		return null;
	}

	@Override
	public boolean equals(Object o) {
		return o instanceof Loadout other && worn.equals(other.worn);
	}

	@Override
	public int hashCode() {
		return Objects.hash(worn);
	}

	@Override
	public String toString() {
		return "Loadout[" + encode() + "]";
	}
}
