# HugoService Style

Fabric mod for **Minecraft 1.21.11**: a liquid-glass style archive menu where you pick single objects
(crystal, anchor, totem, tools, sky, …) from different texture packs, mix them and save the result
as a new resource pack.

Open the menu with **Right Shift** (changeable in the menu under *Extras → Menu key* or in the controls).

## Features

- **Collections** in sections (PvP, World, Interface) with sub-groups per object, e.g. Anchor → Anchors / Glowstone,
  Tools → Swords / Axes / Pickaxes / Shovels / Hoes / Spears. The sidebar shows what you picked in each collection,
  the **Loadout** bar on top shows it as previews, the right panel has **Inspector** and **Themes** tabs, and every
  item glows in its own colour behind its preview
- **Styles**: Vanilla, *Current* (your active resource packs), 10 built-in colour presets and every pack you upload
  (`Upload` button, drag & drop a `.zip`, or packs already in `resourcepacks/`; old 1.8 PvP pack paths are understood)
- 3D previews for blocks and the end crystal (vanilla's own model, as a placed crystal stands on obsidian), sorting (For you, A–Z, Newest, Packs, Presets), search, favourites
- **Apply** builds `resourcepacks/HugoService-Active` and reloads; **Save pack** writes a `.zip` to `resourcepacks/`;
  **Export** saves the `.zip` wherever you choose (e.g. to share it)
- **Texture editor** (*Edit texture* in the Inspector): edit the textures of a collection, starting from the style you
  look at (or vanilla): pen, fill and colour picker, your colour or the texture's own colours, brightness, undo
  (Ctrl+Z), wheel zoom, every frame of animated textures, *Import* for your own PNG. *Save* adds it as your own style
  of that collection (a folder pack in `config/hugoservice/packs`) and picks it
- **Extras**: hit colour (with strength), hitbox colour (F3+B), block outline (colour and thickness), cobweb outline
  (adjustable thickness), player glow (only while the player is visible, not through walls; also yourself in F5,
  rainbow, thickness), shield status (ready / disabled by axe), end crystal spin speed (or stopped) and core colour,
  low fire, no fog (open air only), shield and totem size in your hand, totem pop size with a test pop, enchant glint
  in your colour / a gradient / a rainbow, respawn anchor charges (own glow colour, full look from one charge, charge
  lights that glow in the dark), crosshair gradient, kill effects (lightning, shockwave, light pillar, their head popping up,
  shatter, soul rise; optionally only your kills), a social badge next to the recipe book,
  totem counter (pops over heads, your totems beside the hotbar, optional chat line per pop, a key that resets the pops),
  end crystal and shield opacity, name tag colour and size, inventory size, hide inventory model,
  name faker (own / enemy names; a real account name brings its skin and its MCTiers tier, else a default skin; hide tier tags), grey world, grass colour,
  6 UI themes, smooth/pixel font, optional background blur. Every effect has a live preview in the menu;
  every colour picker also offers grey and black.
- **Custom sky** (like Skyboxify): skyboxes from packs (OptiFine / MCPatcher `sky/world0` 3x2 layout) or your own
  images (upload or `config/hugoservice/skies`; 3:2 = cube layout, 2:1 = 360° panorama). Switches live, with
  options to rotate with time, keep sun/moon/stars, darken at night and a client-side time slider.
- **Crosshair tab**: your own crosshair drawn by the mod (cross, dot, circle, T, X, square or pixel art) with length,
  gap, thickness, outline, centre dot, colour and opacity, 12 presets, pixel art taken from your pack, and an
  **entity indicator** (other colour plus a hit marker while you aim at a mob or player). Changes show at once.
- **Knives**: eleven smooth 3D knives (Karambit, Talon, Butterfly, Bayonet, M9 Bayonet, Flip, Stiletto, Bowie,
  Huntsman, Gut, Push Dagger) drawn by the mod itself, not blocky pack models: ground blades with a fuller and a
  polished edge, rounded grips, guards and rings. They replace every sword in hand, hotbar, inventory and third
  person, combined with 23 finishes (two of them animated).
  Press **G** (changeable) to inspect: every knife type has its own keyframe animation (spins, flips, toss and catch,
  jabs). Pulling a knife out plays a trick (two per type, in turn: ring spins, tosses, flick opens); the butterfly
  really flips: its handles swing open and around their pivots. Tricks and sway can be turned off under Extras.
  The knife is swapped in at render time, so it also wins over server resource packs that re-skin swords
  (server items with their own model keep their look).
  Only your view changes, others see your normal sword.
- **Cosmetics** (based on Titus Cosmetics): capes, hats, bandanas, halos, back and face items, auras, skin overlays,
  pets, shields and accessories, plus 12 particle **trails** that follow your steps. Pick them under
  *Collections → Cosmetics* with a live 3D preview. Drop any `.png` into the Capes view for your **own cape**
  (a 64x32 cape texture, or any picture); own capes are only visible to you.
- **Spotify player**: a glass "now playing" window in a corner and time-synced lyrics (from LRCLIB) in the window or
  written on the sky as 3D text, seen in perspective (window size adjustable, or shown only when a song starts): it moves to where you look once a line is over (or stays over a
  fixed side). Lyrics are also found when Spotify names a song differently ("Song - Remix"), and asked again when LRCLIB is busy. **Automatic** (default, Windows): finds what plays in Spotify through the Windows media controls,
  no login, works with Spotify Free, play / pause / skip included (optionally any music app). **Spotify account**:
  your own app on developer.spotify.com (redirect URI `http://127.0.0.1:43219/callback`), controls need Premium.
- **Name icon & sharing** (opt-in): players who use the mod get an icon in front of their name and see each
  other's cosmetics. Needs the small Cloudflare Worker in [`server/`](server/README.md); only Minecraft UUIDs
  and cosmetic ids are sent to it.

## Build

Requires JDK 21.

```bash
./gradlew build
```

The mod jar ends up in `build/libs/`. Install it together with Fabric Loader and Fabric API.

## Credits

Menu font: [Poppins](https://github.com/itfoundry/Poppins) © The Poppins Project Authors, SIL Open Font License 1.1
(`src/main/resources/LICENSE_Poppins_OFL.txt`).

Cosmetics (`dev.hugoservice.cosmetics`): based on Titus Cosmetics 1.3.22, © 2026 Titus, MIT License
(`src/main/resources/LICENSE_TitusCosmetics.txt`).
