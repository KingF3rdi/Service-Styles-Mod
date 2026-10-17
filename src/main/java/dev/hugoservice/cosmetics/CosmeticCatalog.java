// Based on Titus Cosmetics 1.3.22, MIT License, Copyright (c) 2026 Titus. See LICENSE_TitusCosmetics.txt.
package dev.hugoservice.cosmetics;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * Every cosmetic the mod knows. The geometry is defined here, in player model units (see {@link CosmeticBox}). The
 * textures are generated from the id by {@link CosmeticTextures}. All of this is original work of this mod.
 *
 * <p>Texture layout: each box is laid out like a vanilla cube, so its region in the 64x64 texture is
 * {@code (2w+2d) x (h+d)} texels from {@code (u, v)}. The comments next to the boxes name those regions, and the
 * painters in this package draw into them. Overlays use the player skin layout instead, see {@link CosmeticTextures}.
 */
public final class CosmeticCatalog {
	private static final List<Cosmetic> BASE = build();
	private static volatile List<Cosmetic> ALL = BASE;
	private static volatile Map<String, Cosmetic> BY_ID = index(BASE);
	/** Own capes (HugoService addition): cosmetic id to the PNG it is drawn from. */
	private static volatile Map<String, Path> OWN = Map.of();

	private CosmeticCatalog() {
	}

	private static Map<String, Cosmetic> index(List<Cosmetic> list) {
		return list.stream().collect(Collectors.toUnmodifiableMap(Cosmetic::id, Function.identity()));
	}

	/**
	 * Own capes (HugoService addition): every PNG in the folder becomes a cape that only you see, since other players
	 * do not have the file. The id carries a hash of the file, so an edited image gets a fresh texture.
	 */
	public static void loadOwnCapes(Path dir) {
		List<Cosmetic> own = new ArrayList<>();
		Map<String, Path> files = new HashMap<>();
		try (Stream<Path> list = Files.list(dir)) {
			for (Path p : list.filter(f -> f.getFileName().toString().toLowerCase(Locale.ROOT).endsWith(".png")).sorted().toList()) {
				String name = p.getFileName().toString();
				name = name.substring(0, name.length() - 4);
				String slug = name.toLowerCase(Locale.ROOT).replaceAll("[^a-z0-9]+", "_").replaceAll("^_|_$", "");
				slug = slug.substring(0, Math.min(24, slug.length()));
				long stamp = Files.getLastModifiedTime(p).toMillis() ^ Files.size(p);
				String id = "own_" + slug + "_" + Integer.toHexString((int) (stamp ^ stamp >>> 32) & 0xFFFFFF);
				if (files.putIfAbsent(id, p) != null) continue;
				own.add(new Cosmetic(id, name.length() > 28 ? name.substring(0, 28) : name, CosmeticCategory.CAPE,
						Rarity.COMMON, "Your own cape. Only you see it.", Anchor.BODY, Motion.SWAY,
						List.of(new CosmeticBox(-5, 0, 2.4f, 10, 16, 0.8f, 0, 0)), null));
			}
		} catch (IOException ignored) {
			// no folder yet: no own capes
		}
		List<Cosmetic> all = new ArrayList<>(BASE);
		all.addAll(own);
		OWN = Map.copyOf(files);
		ALL = List.copyOf(all);
		BY_ID = index(ALL);
	}

	/** The PNG of an own cape, or null for every other cosmetic. */
	public static Path ownFile(String id) {
		return OWN.get(id);
	}

	/** All cosmetics, grouped by category in sidebar order. */
	public static List<Cosmetic> all() {
		return ALL;
	}

	public static Optional<Cosmetic> byId(String id) {
		return id == null ? Optional.empty() : Optional.ofNullable(BY_ID.get(id));
	}

	public static List<Cosmetic> ofCategory(CosmeticCategory category) {
		return ALL.stream().filter(c -> c.category() == category).toList();
	}

	private static List<Cosmetic> build() {
		List<Cosmetic> out = new ArrayList<>();

		// Capes hang from the shoulders behind the player and sway while it walks.
		cape(out, "obsidian_cape", "Obsidian Cape", Rarity.EPIC, "Black glass with glowing cracks");
		cape(out, "ender_cape", "Ender Cape", Rarity.EPIC, "Dark purple with stars");
		cape(out, "ember_cape", "Ember Cape", Rarity.RARE, "Glowing embers on red wool");
		cape(out, "frost_cape", "Frost Cape", Rarity.RARE, "Ice blue with frozen edges");
		cape(out, "galaxy_cape", "Galaxy Cape", Rarity.EPIC, "A dark violet nebula with a field of stars");
		cape(out, "lava_cape", "Lava Cape", Rarity.LEGENDARY, "Black basalt split by glowing lava cracks");
		cape(out, "rainbow_cape", "Rainbow Cape", Rarity.RARE, "Smooth stripes of every colour of the rainbow");
		cape(out, "neon_grid_cape", "Neon Grid Cape", Rarity.EPIC, "Glowing grid lines under a neon sunset");

		// Hats sit on the head.
		out.add(new Cosmetic("crown", "Crown", CosmeticCategory.HAT, Rarity.LEGENDARY, "Gold with four red gems",
				Anchor.HEAD, Motion.NONE, List.of(
				new CosmeticBox(-4.6f, -10, -4.6f, 9.2f, 1.8f, 9.2f, 0, 0),
				new CosmeticBox(-4.6f, -13, -4.6f, 1.6f, 3, 1.6f, 0, 16),
				new CosmeticBox(3.0f, -13, -4.6f, 1.6f, 3, 1.6f, 0, 16),
				new CosmeticBox(-4.6f, -13, 3.0f, 1.6f, 3, 1.6f, 0, 16),
				new CosmeticBox(3.0f, -13, 3.0f, 1.6f, 3, 1.6f, 0, 16),
				new CosmeticBox(-0.8f, -14.2f, -0.8f, 1.6f, 1.6f, 1.6f, 32, 0)), null));
		out.add(new Cosmetic("cat_ears", "Cat Ears", CosmeticCategory.HAT, Rarity.COMMON, "Soft ears with pink insides",
				Anchor.HEAD, Motion.NONE, List.of(
				new CosmeticBox(-4.5f, -12, -1.5f, 3, 4, 1.5f, 0, 0),
				new CosmeticBox(1.5f, -12, -1.5f, 3, 4, 1.5f, 0, 0)), null));
		out.add(new Cosmetic("top_hat", "Top Hat", CosmeticCategory.HAT, Rarity.RARE, "Black silk with a violet band",
				Anchor.HEAD, Motion.NONE, List.of(
				new CosmeticBox(-6, -9, -6, 12, 1, 12, 0, 0),
				new CosmeticBox(-4, -17, -4, 8, 8, 8, 0, 16)), null));

		// Bandanas wrap the forehead, with a tail behind the right ear. They share one shape and differ in their textures.
		bandana(out, "heavenly_bandana", "Heavenly Bandana", Rarity.LEGENDARY, "Gold leaf with a sparkling sheen");
		bandana(out, "summer_love_bandana", "Summer Love Bandana", Rarity.EPIC, "Pastel patches and white stripes like a collage");
		bandana(out, "plasma_bandana", "Plasma Bandana", Rarity.EPIC, "Blue plasma that crackles around your head");
		bandana(out, "inferno_bandana", "Inferno Bandana", Rarity.EPIC, "Orange flames that flicker over the cloth");
		bandana(out, "crimson_bandana", "Crimson Bandana", Rarity.LEGENDARY, "Deep red with gold diamonds");
		bandana(out, "pink_lightning_bandana", "Pink Lightning Bandana", Rarity.RARE, "Pink cloth with white lightning");
		bandana(out, "black_lightning_bandana", "Black Lightning Bandana", Rarity.RARE, "Black cloth with white lightning");
		bandana(out, "white_lightning_bandana", "White Lightning Bandana", Rarity.RARE, "White cloth with blue lightning");
		bandana(out, "red_lightning_bandana", "Red Lightning Bandana", Rarity.RARE, "Red cloth with yellow lightning");
		bandana(out, "green_lightning_bandana", "Green Lightning Bandana", Rarity.COMMON, "Green cloth with yellow lightning");
		bandana(out, "purple_lightning_bandana", "Purple Lightning Bandana", Rarity.RARE, "Violet cloth with pink lightning");
		bandana(out, "turquoise_lightning_bandana", "Turquoise Lightning Bandana", Rarity.RARE,
				"Turquoise cloth with white lightning");

		// The halo is a ring of small cubes above the head.
		out.add(new Cosmetic("halo", "Halo", CosmeticCategory.HALO, Rarity.EPIC, "A golden ring that turns slowly",
				Anchor.HEAD, Motion.SPIN, ring(6.0f, -17.5f), null));

		// Wings are layers of thin slabs behind the shoulders, mirrored to both sides.
		out.add(new Cosmetic("angel_wings", "Angel Wings", CosmeticCategory.BACK, Rarity.LEGENDARY,
				"White feathers with a golden shine", Anchor.BODY, Motion.FLAP,
				mirrored(List.of(
						new CosmeticBox(-5, 0, 2.6f, 4, 10, 0.8f, 0, 0),
						new CosmeticBox(-9, 1, 2.6f, 4, 8, 0.8f, 0, 12),
						new CosmeticBox(-13, 3, 2.6f, 4, 6, 0.8f, 0, 24))), null));
		out.add(new Cosmetic("demon_wings", "Demon Wings", CosmeticCategory.BACK, Rarity.EPIC,
				"Dark leather with sharp tips", Anchor.BODY, Motion.FLAP,
				mirrored(List.of(
						new CosmeticBox(-5, -2, 2.6f, 4, 12, 0.8f, 0, 0),
						new CosmeticBox(-9, -4, 2.6f, 4, 10, 0.8f, 0, 12),
						new CosmeticBox(-12, 0, 2.6f, 3, 6, 0.8f, 0, 24))), null));

		// Face pieces sit in front of the face.
		out.add(new Cosmetic("shades", "Shades", CosmeticCategory.FACE, Rarity.RARE, "Round lenses with a thin frame",
				Anchor.HEAD, Motion.NONE, List.of(
				new CosmeticBox(-4.5f, -5.5f, -4.8f, 4, 3, 1, 0, 0),
				new CosmeticBox(0.5f, -5.5f, -4.8f, 4, 3, 1, 0, 0),
				new CosmeticBox(-0.5f, -4.6f, -4.7f, 1, 1, 0.6f, 0, 8)), null));
		out.add(new Cosmetic("pixel_mask", "Pixel Mask", CosmeticCategory.FACE, Rarity.COMMON,
				"A simple mask with a pixel pattern", Anchor.HEAD, Motion.NONE, List.of(
				new CosmeticBox(-4.2f, -6.2f, -4.8f, 8.4f, 4.6f, 0.8f, 0, 0)), null));

		// Auras are particles around the player. Nothing is attached, the particles are spawned by the client.
		aura(out, "flame_aura", "Flame Aura", Rarity.RARE, "Little flames rising around you", "flame");
		aura(out, "soul_aura", "Soul Aura", Rarity.EPIC, "Blue soul flames drifting upwards", "soul_fire_flame");
		aura(out, "heart_aura", "Heart Aura", Rarity.COMMON, "Hearts floating around you", "heart");
		aura(out, "spark_aura", "Spark Aura", Rarity.RARE, "End rod sparks around you", "end_rod");

		// Trails are particles left behind while you move (HugoService addition, spawned by CosmeticAuras).
		trail(out, "flame_trail", "Flame Trail", Rarity.RARE, "Flames licking at your heels", "flame");
		trail(out, "soul_trail", "Soul Trail", Rarity.EPIC, "Blue soul fire where you walked", "soul_fire_flame");
		trail(out, "rainbow_trail", "Rainbow Trail", Rarity.LEGENDARY, "A colour wave following your steps", TRAIL_RAINBOW);
		trail(out, "sakura_trail", "Sakura Trail", Rarity.RARE, "Cherry petals drifting behind you", "cherry_leaves");
		trail(out, "star_trail", "Star Trail", Rarity.EPIC, "Glittering end rod sparks", "end_rod");
		trail(out, "note_trail", "Music Trail", Rarity.COMMON, "Notes bouncing out of every step", "note");
		trail(out, "totem_trail", "Totem Trail", Rarity.LEGENDARY, "The green and gold of a popped totem", "totem_of_undying");
		trail(out, "frost_trail", "Frost Trail", Rarity.RARE, "Snowflakes swirling behind you", "snowflake");
		trail(out, "spark_trail", "Spark Trail", Rarity.EPIC, "Electric sparks crackling at your feet", "electric_spark");
		trail(out, "rune_trail", "Rune Trail", Rarity.COMMON, "Enchanting runes rising from the ground", "enchant");
		trail(out, "portal_trail", "Portal Trail", Rarity.RARE, "Purple portal dust", "portal");
		trail(out, "heart_trail", "Heart Trail", Rarity.COMMON, "Hearts popping up where you walk", "heart");

		// Overlays are drawn over the whole skin. The glints share one design and differ in palette and pattern.
		overlay(out, "verdant_glint", "Verdant Glint", Rarity.COMMON, "Green circuit lines over the skin");
		overlay(out, "venom_coat", "Venom Coat", Rarity.COMMON, "A translucent green slime coat");
		overlay(out, "violet_swirl", "Violet Swirl", Rarity.RARE, "Curling violet light");
		overlay(out, "glacier_glint", "Glacier Glint", Rarity.RARE, "Ice facets that catch the light");
		overlay(out, "rose_glint", "Rose Glint", Rarity.RARE, "Pink petals that shimmer");
		overlay(out, "prism_glint", "Prism Glint", Rarity.EPIC, "Rainbow sparks across the whole body");
		overlay(out, "blaze_glint", "Blaze Glint", Rarity.EPIC, "Fire tongues licking over the skin");
		overlay(out, "storm_crackle", "Storm Crackle", Rarity.EPIC, "Blue lightning that flashes across you");
		overlay(out, "solar_sheen", "Solar Sheen", Rarity.LEGENDARY, "Golden light rolling over the skin");
		overlay(out, "void_glint", "Void Glint", Rarity.LEGENDARY, "A dark nebula with white stars");

		// Pets perch on the right shoulder at half size, see Anchor.PET. Their boxes stand on y = 0, the feet. The parrots
		// are drawn with the vanilla parrot textures, the others with their own textures from PetArt.
		pet(out, "parrot_red_blue", "Red Parrot", Rarity.RARE, "A red parrot with blue wings on your shoulder",
				Motion.HOVER, parrot(), "minecraft:textures/entity/parrot/parrot_red_blue.png");
		pet(out, "parrot_blue", "Blue Parrot", Rarity.RARE, "A blue parrot that hops along your shoulder",
				Motion.HOVER, parrot(), "minecraft:textures/entity/parrot/parrot_blue.png");
		pet(out, "parrot_green", "Green Parrot", Rarity.COMMON, "A green parrot that bobs on your shoulder",
				Motion.HOVER, parrot(), "minecraft:textures/entity/parrot/parrot_green.png");
		pet(out, "parrot_yellow_blue", "Yellow Parrot", Rarity.EPIC, "A yellow parrot with blue wings",
				Motion.HOVER, parrot(), "minecraft:textures/entity/parrot/parrot_yellow_blue.png");
		pet(out, "parrot_grey", "Grey Parrot", Rarity.COMMON, "A grey parrot with a yellow head",
				Motion.HOVER, parrot(), "minecraft:textures/entity/parrot/parrot_grey.png");
		out.add(new Cosmetic("glow_cube", "Glow Cube", CosmeticCategory.PET, Rarity.RARE,
				"A floating cube of glowing lines around a core", Anchor.PET, Motion.HOVER, glowCube(), null));
		pet(out, "lil_owl", "Lil Owl", Rarity.EPIC, "A round owl with huge gold eyes", Motion.HOVER, owl());
		pet(out, "pocket_ghost", "Pocket Ghost", Rarity.RARE, "A shy ghost with a wavy hem", Motion.WAVE, pocketGhost());
		pet(out, "baby_ghast", "Baby Ghast", Rarity.EPIC, "A tiny ghast whose tentacles wave under it", Motion.WAVE,
				babyGhast());
		pet(out, "ghast", "Ghast", Rarity.LEGENDARY, "A white ghast whose tentacles wave under it", Motion.WAVE,
				ghast());
		pet(out, "baby_ender_dragon", "Baby Ender Dragon", Rarity.LEGENDARY, "A small purple dragon with pink eyes",
				Motion.HOVER, babyDragon());
		pet(out, "baby_blaze", "Baby Blaze", Rarity.RARE, "A blaze with four rods turning around its head", Motion.HOVER,
				babyBlaze());

		// Shields surround the body with translucent cells. Their boxes are around the middle of the figure.
		shield(out, "aegis_ring", "Aegis Ring", Rarity.RARE, "A ring of cells turning around your waist",
				Motion.SPIN, aegisRing());
		shield(out, "frost_barrier", "Frost Barrier", Rarity.EPIC, "An icy cage that breathes around you",
				Motion.PULSE, frostBarrier());
		shield(out, "ember_shield", "Ember Shield", Rarity.LEGENDARY, "Embers rising in a ring around you",
				Motion.WAVE, emberShield());

		// Accessories are gear on the body, attached to the body like the capes and the wings.
		accessory(out, "jetpack", "Jetpack", Rarity.EPIC, "A jetpack with glowing nozzles on your back",
				Motion.NONE, jetpack());
		accessory(out, "katana_sheath", "Katana Sheath", Rarity.RARE, "A katana in a black sheath across your back",
				Motion.NONE, katanaSheath());
		accessory(out, "scarf", "Scarf", Rarity.COMMON, "A knitted scarf that trails in the wind",
				Motion.SWAY, scarf());
		accessory(out, "satchel", "Satchel", Rarity.COMMON, "A leather satchel with a brass buckle",
				Motion.NONE, satchel());

		return List.copyOf(out);
	}

	private static void cape(List<Cosmetic> out, String id, String name, Rarity rarity, String description) {
		out.add(new Cosmetic(id, name, CosmeticCategory.CAPE, rarity, description, Anchor.BODY, Motion.SWAY,
				List.of(new CosmeticBox(-5, 0, 2.4f, 10, 16, 0.8f, 0, 0)), null));
	}

	private static void aura(List<Cosmetic> out, String id, String name, Rarity rarity, String description,
			String particle) {
		out.add(new Cosmetic(id, name, CosmeticCategory.AURA, rarity, description, Anchor.BODY, Motion.NONE,
				List.of(), particle));
	}

	/** Particle name of the trail that cycles through the rainbow with coloured dust. */
	public static final String TRAIL_RAINBOW = "hugoservice_rainbow";

	private static void trail(List<Cosmetic> out, String id, String name, Rarity rarity, String description,
			String particle) {
		out.add(new Cosmetic(id, name, CosmeticCategory.TRAIL, rarity, description, Anchor.BODY, Motion.NONE,
				List.of(), particle));
	}

	/** An overlay has no boxes: it is the player model drawn again with the overlay texture. */
	private static void overlay(List<Cosmetic> out, String id, String name, Rarity rarity, String description) {
		Motion motion = switch (id) {
			case "storm_crackle" -> Motion.FLICKER;
			case "venom_coat" -> Motion.DRIFT;
			default -> Motion.SHIMMER;
		};
		out.add(new Cosmetic(id, name, CosmeticCategory.OVERLAY, rarity, description, Anchor.WHOLE, motion,
				List.of(), null));
	}

	private static void pet(List<Cosmetic> out, String id, String name, Rarity rarity, String description,
			Motion motion, List<CosmeticBox> boxes) {
		pet(out, id, name, rarity, description, motion, boxes, null);
	}

	/** A pet drawn with a vanilla texture, or with its generated texture when {@code texture} is null. */
	private static void pet(List<Cosmetic> out, String id, String name, Rarity rarity, String description,
			Motion motion, List<CosmeticBox> boxes, String texture) {
		out.add(new Cosmetic(id, name, CosmeticCategory.PET, rarity, description, Anchor.PET, motion, boxes, null,
				texture));
	}

	private static void shield(List<Cosmetic> out, String id, String name, Rarity rarity, String description,
			Motion motion, List<CosmeticBox> boxes) {
		out.add(new Cosmetic(id, name, CosmeticCategory.SHIELD, rarity, description, Anchor.CORE, motion, boxes,
				null));
	}

	private static void accessory(List<Cosmetic> out, String id, String name, Rarity rarity, String description,
			Motion motion, List<CosmeticBox> boxes) {
		out.add(new Cosmetic(id, name, CosmeticCategory.ACCESSORY, rarity, description, Anchor.BODY, motion, boxes,
				null));
	}

	// ---- Pets ---------------------------------------------------------------------------------------------------

	/**
	 * A parrot with the boxes and texture offsets of the vanilla parrot model, without its tilts, so every box is axis
	 * aligned. The sizes are the vanilla ones in whole units, so the vanilla 32 by 32 texture lays out as it does in the
	 * game. The feet stand at y = 0 and the front of the bird is -z. Body 2,8 3x6x3; wings 19,8 1x5x3, on both sides;
	 * tail 22,1 3x4x1; head 2,2 2x3x2; top plate 10,0 2x1x4; beaks 11,7 and 16,7 1x2x1; crest 2,18 1x5x4, widened from
	 * the vanilla zero width; legs 14,18 1x2x1, on both sides.
	 */
	private static List<CosmeticBox> parrot() {
		return List.of(
				new CosmeticBox(-1.5f, -7.5f, -3.45f, 3, 6, 3, 2, 8),
				new CosmeticBox(1, -7.06f, -3.21f, 1, 5, 3, 19, 8),
				new CosmeticBox(-2, -7.06f, -3.21f, 1, 5, 3, 19, 8),
				new CosmeticBox(-1.5f, -4, 1.21f, 3, 4, 1, 22, 1),
				new CosmeticBox(-1, -9.81f, -2.71f, 2, 3, 2, 2, 2),
				new CosmeticBox(-1, -10.81f, -4.71f, 2, 1, 4, 10, 0),
				new CosmeticBox(-0.5f, -9.81f, -3.71f, 1, 2, 1, 11, 7),
				new CosmeticBox(-0.5f, -10.06f, -4.66f, 1, 2, 1, 16, 7),
				new CosmeticBox(-0.5f, -14.46f, -3.56f, 1, 5, 4, 2, 18),
				new CosmeticBox(0.5f, -2, -0.5f, 1, 2, 1, 14, 18),
				new CosmeticBox(-1.5f, -2, -0.5f, 1, 2, 1, 14, 18));
	}

	/**
	 * A floating cube made of glowing edges around a bright core. The twelve edges share the region 0,0 14x7; the core
	 * is 16,0 8x4. It hovers a little above the shoulder, beside the head: the cube is moved 4.8 units up, so its bottom is
	 * 1.8 units above the feet line of the other pets.
	 */
	private static List<CosmeticBox> glowCube() {
		List<CosmeticBox> boxes = new ArrayList<>();
		for (float y : new float[] {-3, 2}) {
			for (float z : new float[] {-3, 2}) {
				boxes.add(new CosmeticBox(-2, y, z, 4, 1, 1, 0, 0));
			}
		}
		for (float x : new float[] {-3, 2}) {
			for (float z : new float[] {-3, 2}) {
				boxes.add(new CosmeticBox(x, -3, z, 1, 6, 1, 0, 0));
			}
		}
		for (float x : new float[] {-3, 2}) {
			for (float y : new float[] {-3, 2}) {
				boxes.add(new CosmeticBox(x, y, -3, 1, 1, 6, 0, 0));
			}
		}
		boxes.add(new CosmeticBox(-1, -1, -1, 2, 2, 2, 16, 0));
		return moved(boxes, 0, -4.8f, 0);
	}

	/** A baby ghast: a white cube with a face, and four tentacles under it. The tentacles use the region 0,24 of the texture. */
	private static List<CosmeticBox> babyGhast() {
		List<CosmeticBox> boxes = new ArrayList<>(List.of(new CosmeticBox(-4, -15, -4, 8, 8, 8, 0, 0)));
		for (float x : new float[] {-3, 1}) {
			for (float z : new float[] {-3, 2}) {
				boxes.add(new CosmeticBox(x, -7, z, 1, 7, 1, 0, 24));
			}
		}
		return boxes;
	}

	/**
	 * A ghast: a white cube with a face, and six tentacles under it. The body is 0,0 32x16; the tentacles use the region 0,24 of the
	 * texture, 4x8 texels.
	 */
	private static List<CosmeticBox> ghast() {
		List<CosmeticBox> boxes = new ArrayList<>(List.of(new CosmeticBox(-4, -15, -4, 8, 8, 8, 0, 0)));
		for (float x : new float[] {-3, -1, 1}) {
			for (float z : new float[] {-3, 2}) {
				boxes.add(new CosmeticBox(x, -7, z, 1, 7, 1, 0, 24));
			}
		}
		return boxes;
	}

	/** A baby ender dragon: a purple body with a head on top, two wings, and four short legs. */
	private static List<CosmeticBox> babyDragon() {
		List<CosmeticBox> boxes = new ArrayList<>(List.of(
				new CosmeticBox(-3, -11, -2, 6, 5, 6, 0, 0),
				new CosmeticBox(-2, -15, -4, 4, 4, 4, 0, 16),
				new CosmeticBox(3, -13, -2, 1, 5, 6, 24, 0),
				new CosmeticBox(-4, -13, -2, 1, 5, 6, 40, 0)));
		for (float x : new float[] {-3, 2}) {
			for (float z : new float[] {-2, 3}) {
				boxes.add(new CosmeticBox(x, -5, z, 1, 3, 1, 0, 24));
			}
		}
		return boxes;
	}

	/** A baby blaze: a yellow head, and four rods standing at its corners. */
	private static List<CosmeticBox> babyBlaze() {
		List<CosmeticBox> boxes = new ArrayList<>(List.of(new CosmeticBox(-3, -8, -3, 6, 6, 6, 0, 0)));
		for (float x : new float[] {-4, 3}) {
			for (float z : new float[] {-4, 3}) {
				boxes.add(new CosmeticBox(x, -11, z, 1, 7, 1, 24, 0));
			}
		}
		return boxes;
	}

	/**
	 * A sheet ghost. Body 0,0 28x14; dome 32,0 20x6; the hem 0,24 16x9 (four boxes of different height, so a wave runs
	 * along them). The eyes are painted on the front of the body.
	 */
	private static List<CosmeticBox> pocketGhost() {
		List<CosmeticBox> boxes = new ArrayList<>(List.of(
				new CosmeticBox(-4, -6, -3, 8, 8, 6, 0, 0),
				new CosmeticBox(-3, -8, -2, 6, 2, 4, 32, 0)));
		boxes.add(new CosmeticBox(-4, 2, -3, 2, 3, 6, 0, 24));
		boxes.add(new CosmeticBox(-2, 2, -3, 2, 2, 6, 0, 24));
		boxes.add(new CosmeticBox(0, 2, -3, 2, 3, 6, 0, 24));
		boxes.add(new CosmeticBox(2, 2, -3, 2, 2, 6, 0, 24));
		// The hem stands on the feet line. The body is 8 units wide, so it spans the whole top of the arm.
		return moved(boxes, 0, -5, 0);
	}

	/**
	 * Round owl with ear tufts and folded wings. Body 0,0 22x12; head 24,0 26x10; eye 0,12 8x4; beak 8,12 6x3; tuft
	 * 16,12 6x3; wing 0,16 12x11; feet 16,20 8x3; tail 24,12 10x4.
	 */
	private static List<CosmeticBox> owl() {
		List<CosmeticBox> boxes = new ArrayList<>(List.of(
				new CosmeticBox(-3, -3, -2, 6, 7, 5, 0, 0),
				new CosmeticBox(-4, -8, -3, 8, 5, 5, 24, 0),
				new CosmeticBox(-1, -5, -5, 2, 2, 1, 8, 12),
				new CosmeticBox(-2, 1, 3, 4, 3, 1, 24, 12)));
		boxes.addAll(mirrored(List.of(
				new CosmeticBox(-3, -7, -4, 3, 3, 1, 0, 12),
				new CosmeticBox(-4, -10, -1, 2, 2, 1, 16, 12),
				new CosmeticBox(3, -2, -2, 1, 6, 5, 0, 16),
				new CosmeticBox(-2, 4, -2, 2, 1, 2, 16, 20))));
		// The feet stand on the feet line. The wings stay folded, as a perched owl's do.
		return moved(boxes, 0f, -5, 0);
	}

	// ---- Shields ------------------------------------------------------------------------------------------------

	/**
	 * A bandana: four strips around the forehead, and a tail behind the right ear. The front and back strips are 9 by 3 by 1, the
	 * sides 1 by 3 by 7 and the tail 1 by 4 by 2. The strips use the regions 0,0 and 0,8 and the tail 0,20, so every bandana has the
	 * same layout and only its pattern differs.
	 */
	private static List<CosmeticBox> bandanaBoxes() {
		return List.of(
				new CosmeticBox(-4.5f, -7, -4.6f, 9, 3, 1, 0, 0),
				new CosmeticBox(-4.5f, -7, 3.6f, 9, 3, 1, 0, 0),
				new CosmeticBox(-4.6f, -7, -3.5f, 1, 3, 7, 0, 8),
				new CosmeticBox(3.6f, -7, -3.5f, 1, 3, 7, 0, 8),
				new CosmeticBox(3.6f, -4, 1.7f, 1, 4, 2, 0, 20));
	}

	private static void bandana(List<Cosmetic> out, String id, String name, Rarity rarity, String description) {
		out.add(new Cosmetic(id, name, CosmeticCategory.BANDANA, rarity, description, Anchor.HEAD, Motion.NONE,
				bandanaBoxes(), null));
	}

	/** Sixteen cells of size 4 on a ring of radius 12 around the middle. Region 0,0 16x8. */
	private static List<CosmeticBox> aegisRing() {
		return ringOf(12, -2, 16, 4, 0, 0, 0);
	}

	/**
	 * A thin hexagon of twelve cells of size 3 around the middle of the figure. Six cells sit at the corners, 10.4 units
	 * from the axis, and six at the midpoints of the edges, 9 units from it. Neighbouring cells are 5.2 units apart, so
	 * there are clear gaps and the player shows through. Region 0,0 12x6.
	 */
	private static List<CosmeticBox> frostBarrier() {
		List<CosmeticBox> boxes = new ArrayList<>();
		for (int i = 0; i < 12; i++) {
			// Even steps are the corners, at 0, 60, ... degrees. Odd steps are the edge midpoints, at 30, 90, ... degrees.
			double radius = i % 2 == 0 ? 10.4 : 9.0;
			double angle = StrictMath.PI * i / 6.0;
			float cx = (float) (StrictMath.cos(angle) * radius);
			float cz = (float) (StrictMath.sin(angle) * radius);
			boxes.add(new CosmeticBox(cx - 1.5f, -1.5f, cz - 1.5f, 3, 3, 3, 0, 0));
		}
		return boxes;
	}

	/** Two rings of cells of size 3 and six embers of size 2 at other heights. Cells 0,0 12x6; embers 16,0 8x4. */
	private static List<CosmeticBox> emberShield() {
		List<CosmeticBox> boxes = new ArrayList<>(ringOf(11, -6, 14, 3, 0, 0, 0));
		boxes.addAll(ringOf(13, 4, 16, 3, 0, 0, 0.5));
		float[] embers = {-10, -4, 2, 8, -7, 5};
		for (int i = 0; i < embers.length; i++) {
			double angle = 2 * StrictMath.PI * (i + 0.3) / embers.length;
			float cx = (float) (StrictMath.cos(angle) * 14);
			float cz = (float) (StrictMath.sin(angle) * 14);
			boxes.add(new CosmeticBox(cx - 1, embers[i] - 1, cz - 1, 2, 2, 2, 16, 0));
		}
		return boxes;
	}

	/**
	 * Given cells on a ring: {@code count} cubes of {@code size} on a circle of {@code radius} around the Y axis, centred
	 * at {@code centreY}. The angle of the first cube is {@code offset} steps.
	 */
	private static List<CosmeticBox> ringOf(float radius, float centreY, int count, float size, int u, int v,
			double offset) {
		List<CosmeticBox> boxes = new ArrayList<>();
		for (int i = 0; i < count; i++) {
			double angle = StrictMath.PI * 2 * (i + offset) / count;
			float cx = (float) (StrictMath.cos(angle) * radius);
			float cz = (float) (StrictMath.sin(angle) * radius);
			boxes.add(new CosmeticBox(cx - size / 2, centreY - size / 2, cz - size / 2, size, size, size, u, v));
		}
		return boxes;
	}

	// ---- Accessories --------------------------------------------------------------------------------------------

	/**
	 * A frame plate on the back with two tanks hanging from it, nozzles under the tanks and flames under the nozzles.
	 * Frame 0,0 18x12; tank 20,0 12x13; nozzle 34,0 8x4; flame 44,0 8x3.
	 */
	private static List<CosmeticBox> jetpack() {
		List<CosmeticBox> boxes = new ArrayList<>(List.of(new CosmeticBox(-4, 0, 2, 8, 11, 1, 0, 0)));
		boxes.addAll(mirrored(List.of(
				new CosmeticBox(-5, 0, 3, 3, 10, 3, 20, 0),
				new CosmeticBox(-4, 10, 3, 2, 2, 2, 34, 0),
				new CosmeticBox(-4, 12, 3, 2, 1, 2, 44, 0))));
		return boxes;
	}

	/**
	 * A katana on a diagonal across the back: six cells of the sheath from the lower left to the upper right, the grip
	 * and the guard at the top. Sheath 0,0 10x5; grip 16,0 12x4; guard 32,0 8x6.
	 */
	private static List<CosmeticBox> katanaSheath() {
		List<CosmeticBox> boxes = new ArrayList<>();
		for (int i = 0; i < 6; i++) {
			boxes.add(new CosmeticBox(-6 + 2 * i, 10 - 2 * i, 2, 3, 3, 2, 0, 0));
		}
		boxes.add(new CosmeticBox(7, -3, 2, 4, 2, 2, 16, 0));
		boxes.add(new CosmeticBox(5, -4, 2, 2, 4, 2, 32, 0));
		return boxes;
	}

	/**
	 * A scarf around the neck with two tails. The wrap is 0,0 28x8; the front tail 32,0 8x11 and the back tail 32,12
	 * 6x10. The tails sway with the walking animation, pivoting at their top edge.
	 */
	private static List<CosmeticBox> scarf() {
		return new ArrayList<>(List.of(
				new CosmeticBox(-4, 0, -3, 8, 2, 6, 0, 0),
				new CosmeticBox(-1, 2, -4, 3, 10, 1, 32, 0),
				new CosmeticBox(2, 2, 2, 2, 9, 1, 32, 12)));
	}

	/**
	 * A leather bag on the front hip with a flap, a diagonal strap over the chest and a buckle. Bag 0,0 16x8; flap
	 * 16,0 12x3; strap 32,0 6x3; buckle 40,0 6x3.
	 */
	private static List<CosmeticBox> satchel() {
		List<CosmeticBox> boxes = new ArrayList<>(List.of(
				new CosmeticBox(2, 7, -5, 5, 5, 3, 0, 0),
				new CosmeticBox(2, 7, -6, 5, 2, 1, 16, 0),
				new CosmeticBox(3, 8, -6, 2, 2, 1, 40, 0)));
		for (float[] step : new float[][] {{-4, 0}, {-2, 2}, {0, 4}, {2, 6}}) {
			boxes.add(new CosmeticBox(step[0], step[1], -3, 2, 2, 1, 32, 0));
		}
		return boxes;
	}

	/** The boxes moved by an offset, so a pet built around its own origin can stand where its anchor puts it. */
	private static List<CosmeticBox> moved(List<CosmeticBox> boxes, float dx, float dy, float dz) {
		List<CosmeticBox> out = new ArrayList<>();
		for (CosmeticBox b : boxes) {
			out.add(new CosmeticBox(b.x() + dx, b.y() + dy, b.z() + dz, b.w(), b.h(), b.d(), b.u(), b.v(), b.wing()));
		}
		return out;
	}

	/** The boxes plus their mirror images on the other side of the body. Wings stay wings. */
	private static List<CosmeticBox> mirrored(List<CosmeticBox> left) {
		List<CosmeticBox> boxes = new ArrayList<>(left);
		for (CosmeticBox b : left) {
			boxes.add(new CosmeticBox(-(b.x() + b.w()), b.y(), b.z(), b.w(), b.h(), b.d(), b.u(), b.v(), b.wing()));
		}
		return boxes;
	}

	/** Cubes on a circle around the vertical axis through the head. Sixteen cubes make a round ring. */
	private static List<CosmeticBox> ring(float radius, float y) {
		List<CosmeticBox> boxes = new ArrayList<>();
		for (int i = 0; i < 16; i++) {
			double angle = Math.PI * 2 * i / 16;
			float cx = (float) (Math.cos(angle) * radius);
			float cz = (float) (Math.sin(angle) * radius);
			boxes.add(new CosmeticBox(cx - 1, y, cz - 1, 2, 1.6f, 2, 0, 0));
		}
		return boxes;
	}
}
