# HugoService Style

Fabric mod for **Minecraft 1.21.11**: a glassy style archive menu where you pick single objects
(crystal, anchor, totem, tools, sky, …) from different texture packs, mix them and save the result
as a new resource pack.

Open the menu with **Right Shift** (changeable in the menu under *Extras → Menu key* or in the controls).

## Features

- **Collections** in sections (PvP, World, Interface) with sub-groups per object, e.g. Anchor → Anchors / Glowstone,
  Tools → Swords / Axes / Pickaxes / Shovels / Hoes / Spears (quick-access bar on top)
- **Styles**: Vanilla, *Current* (your active resource packs), 10 built-in colour presets and every pack you upload
  (`Upload` button, drag & drop a `.zip`, or packs already in `resourcepacks/`; old 1.8 PvP pack paths are understood)
- 3D previews for blocks and the end crystal, sorting (For you, A–Z, Newest, Packs, Presets), search, favourites
- **Apply** builds `resourcepacks/HugoService-Active` and reloads; **Save pack** writes a `.zip` to `resourcepacks/`
- **Extras**: crosshair pixel painter, hit colour, hitbox colour (F3+B), block outline, cobweb outline,
  player glow (only while the player is visible, not through walls), shield status (ready / disabled by axe),
  end crystal and shield opacity, name tag colour and size, inventory size, hide inventory model,
  name faker (own / enemy names, default skins, hide tier tags), grey world, grass colour,
  6 UI themes, smooth/pixel font, optional background blur. Every effect has a live preview in the menu.
- **Custom sky** (like Skyboxify): skyboxes from packs (OptiFine / MCPatcher `sky/world0` 3x2 layout) or your own
  images (upload or `config/hugoservice/skies`; 3:2 = cube layout, 2:1 = 360° panorama). Switches live, with
  options to rotate with time, keep sun/moon/stars, darken at night and a client-side time slider.
- **Name icon** (opt-in): players who use the mod get an icon in front of their name. Needs the small
  Cloudflare Worker in [`server/`](server/README.md); only Minecraft UUIDs are sent to it.

## Build

Requires JDK 21.

```bash
./gradlew build
```

The mod jar ends up in `build/libs/`. Install it together with Fabric Loader and Fabric API.

## Credits

Menu font: [Poppins](https://github.com/itfoundry/Poppins) © The Poppins Project Authors, SIL Open Font License 1.1
(`src/main/resources/LICENSE_Poppins_OFL.txt`).
