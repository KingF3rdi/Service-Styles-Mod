# HugoService Style

Fabric mod for **Minecraft 1.21.11**: a liquid-glass style archive menu where you pick single objects
(crystal, anchor, totem, tools, sky, …) from different texture packs, mix them and save the result
as a new resource pack.

Open the menu with **Right Shift** (changeable in the menu under *Extras → Menu key* or in the controls).

## Features

- **Collections** in sections (PvP, World, Interface) with sub-groups per object, e.g. Anchor → Anchors / Glowstone,
  Tools → Swords / Axes / Pickaxes / Shovels / Hoes / Spears (quick-access bar on top)
- **Styles**: Vanilla, *Current* (your active resource packs), 10 built-in colour presets and every pack you upload
  (`Upload` button, drag & drop a `.zip`, or packs already in `resourcepacks/`; old 1.8 PvP pack paths are understood)
- 3D previews for blocks and the end crystal (as it stands in the world), sorting (For you, A–Z, Newest, Packs, Presets), search, favourites
- **Apply** builds `resourcepacks/HugoService-Active` and reloads; **Save pack** writes a `.zip` to `resourcepacks/`
- **Extras**: hit colour, hitbox colour (F3+B), block outline, cobweb outline,
  player glow (only while the player is visible, not through walls), shield status (ready / disabled by axe),
  end crystal and shield opacity, name tag colour and size, inventory size, hide inventory model,
  name faker (own / enemy names, default skins, hide tier tags), grey world, grass colour,
  6 UI themes, smooth/pixel font, optional background blur. Every effect has a live preview in the menu.
- **Custom sky** (like Skyboxify): skyboxes from packs (OptiFine / MCPatcher `sky/world0` 3x2 layout) or your own
  images (upload or `config/hugoservice/skies`; 3:2 = cube layout, 2:1 = 360° panorama). Switches live, with
  options to rotate with time, keep sun/moon/stars, darken at night and a client-side time slider.
- **Crosshair tab**: your own crosshair drawn by the mod (cross, dot, circle, T, X, square or pixel art) with length,
  gap, thickness, outline, centre dot, colour and opacity, 12 presets, pixel art taken from your pack, and an
  **entity indicator** (other colour plus a hit marker while you aim at a mob or player). Changes show at once.
- **Knives**: eleven own 3D knife models (Karambit, Talon, Butterfly, Bayonet, M9 Bayonet, Flip, Stiletto, Bowie,
  Huntsman, Gut, Push Dagger) that replace every sword, combined with 23 finishes (two of them animated).
  Press **G** (changeable) to inspect: every knife type has its own keyframe animation (spins, flips, toss and catch,
  jabs); switching to the knife plays a draw move. Only your view changes, others see your normal sword.
- **Cosmetics** (based on Titus Cosmetics): capes, hats, bandanas, halos, back and face items, auras, skin overlays,
  pets, shields and accessories, plus 12 particle **trails** that follow your steps. Pick them under
  *Collections → Cosmetics* with a live 3D preview. Drop any `.png` into the Capes view for your **own cape**
  (a 64x32 cape texture, or any picture); own capes are only visible to you.
- **Spotify player**: a glass "now playing" window in a corner and time-synced lyrics (from LRCLIB) in the window or
  written on the sky. Needs your own app on developer.spotify.com (redirect URI `http://127.0.0.1:43219/callback`);
  play / pause / skip need Spotify Premium. The login token stays in your config.
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
