# 7auction Styles

**Latest:** `/cursor/stores/self/uploads/7auction-Styles.jar` (`1.0.0`, Minecraft `~1.21.11`).

Display **7auction Styles**. Mod id `hugoservice`. Mixer kept. No knives/cosmetics.

## Icon

Unique 128×128 teal pack/mod icon (readable **7**, gold corners, not Bauloo/SkellyHole H-glyph):

- `/cursor/stores/self/media/7auction-icon.png`
- Baked as `assets/hugoservice/icon.png` and `assets/hugoservice/pack.png`
- Generated mix pack uses that `pack.png`; folder name **`7auction Styles`**

## Themes

Aurora (default), Ice, Midnight, Rose, Mint, Amber, Frost. Vanilla `widget/button*` controls.

## Jars that compiled

| File | Minecraft | Compat set |
| --- | --- | --- |
| `7auction-Styles.jar` | 1.21.11 | v12111 |
| `7auction-Styles-1.21.11.jar` | 1.21.11 | v12111 |
| `7auction-Styles-1.21.10.jar` | 1.21.10 | v12110 |
| `7auction-Styles-1.21.8.jar` | 1.21.8 | v1218 |
| `7auction-Styles-1.21.6.jar` | 1.21.6 | v1218 |

Gradle: `-Pmc_compat=v12111|v12110|v1218` plus Yarn/Fabric API properties. Source under `src/compat/<set>/`.

## Versions that did not compile

**1.21.5** (~96 errors on v1218). Breaks:

- `DrawContext.drawTexture`: `Function<Identifier, RenderLayer>` vs `RenderPipeline` (`RenderPipelines.GUI_TEXTURED`)
- `DrawContext.getMatrices()`: `Matrix3x2fStack` vs `MatrixStack`
- `net.fabricmc.fabric.api.client.rendering.v1.hud` (`HudElementRegistry`) missing
- `net.minecraft.client.render.fog.FogRenderer` missing
- `HugoService` `BlockRenderLayer` / `BlockRenderLayerMap` missing

**1.21.4** (~158 errors). Same as 1.21.5 plus:

- `com.mojang.blaze3d.pipeline.RenderPipeline` / `RenderPipelines` missing
- `com.mojang.blaze3d.textures` missing
- `SharedConstants.packVersion(ResourceType)` missing
- `TextFieldWidget.setTextShadow` missing
- `ItemDisplayContext` missing
- `World.playSoundClient` / `addParticleClient` missing

1.21.6/8 omit GPU sky overlay, hitbox mixin, name-faker skins, crystal/shield command-queue mixins (mixer + hand-size remain). 1.21.10+ has the full overlay set.
