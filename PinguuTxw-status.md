# PinguuTxw Styles

Flavor of the Service-Styles mixer. Display **PinguuTxw Styles**. Mod id `hugoservice`. **`main` stays 7auction Styles** — do not merge this branch over 7auction identity.

Build PinguuTxw: `./gradlew remapJar` (this branch defaults to PinguuTxw).
Build 7auction from this tree: `./gradlew remapJar -Pproduct=7auction` (knives stay off).

Knives are on for PinguuTxw only. Shared extras (FPS culling, explosion optimize) and mixer packs apply to both flavors.

## Jars

Canonical 1.21.11 plus versioned 1.21.6–1.21.11:

- `dist/PinguuTxw-Styles.jar` / `PinguuTxw-Styles-1.21.11.jar`
- `dist/PinguuTxw-Styles-1.21.10.jar`
- `dist/PinguuTxw-Styles-1.21.9.jar` (world lyrics overlay not hooked; Fabric API 0.134 has no `WorldRenderEvents`)
- `dist/PinguuTxw-Styles-1.21.8.jar`
- `dist/PinguuTxw-Styles-1.21.7.jar`
- `dist/PinguuTxw-Styles-1.21.6.jar`

Same files under `/cursor/stores/self/uploads/`. 7auction jars in that folder were not overwritten.
