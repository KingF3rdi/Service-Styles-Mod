# 7auction Styles

**Latest:** `dist/7auction-Styles.jar` (`1.0.0`, Minecraft `~1.21.11`, 3,781,309 bytes). Same bytes at `7auction-Styles-1.21.11.jar`. Version jars `7auction-Styles-1.21.6.jar` … `1.21.11.jar`.

Display **7auction Styles**. Mod id `hugoservice`. Mixer kept. No knives. No Sharing card. Cobweb outline is a Collections mixer slot (`block/cobweb.png`), not extras ESP.

## This rebuild

- **3D crystals restored:** Collections Crystal tiles use the end-crystal glass/core model again (not 2D cloud plates). World extras (spin, opacity, core colour) run on 1.21.6–1.21.11.
- 1.21.6–8 now include `EndCrystalEntityRendererMixin` (was missing). Default extras still take the vanilla submit/getBuffer path so FPS does not rebuild textures every frame.
- Logo + 7 nav links kept. Color presets stay out of the catalog. Preview plates stay stripped on 2D thumbs. Click GUI unchanged.

Pushed to https://github.com/KingF3rdi/Service-Styles-Mod `main`.
