# 7auction Styles

Fabric mixer for **Minecraft 1.21.6–1.21.11**. Mix single objects (crystal, armor, sky, extras) from texture packs and apply them as one pack. Open with **Right Shift**.

Display name **7auction Styles**. Mod id `hugoservice`. Version `1.0.0.1`. No knives.

The PinguuTxw flavor (3D knives, lyrics, FPS extras) stays in this tree. Build it with `-Pproduct=pinguutxw`. The 7auction flavor stays knife-free.

Product status: [`7auction-status.md`](7auction-status.md). PinguuTxw notes: [`PinguuTxw-status.md`](PinguuTxw-status.md).

## Build

Requires JDK 21.

```bash
./gradlew remapJar
# PinguuTxw:
# ./gradlew remapJar -Pproduct=pinguutxw
# other versions:
# ./gradlew remapJar -Pmc_compat=v12110 -Pminecraft_version=1.21.10
# ./gradlew remapJar -Pmc_compat=v1219  -Pminecraft_version=1.21.9
# ./gradlew remapJar -Pmc_compat=v1218  -Pminecraft_version=1.21.8
```

The mod jar ends up in `build/libs/` as `7auction-Styles-1.0.0.1.jar` (or `PinguuTxw-Styles-1.0.0.1.jar` when `-Pproduct=pinguutxw`). Install it together with Fabric Loader and Fabric API.

## Credits

UI uses the vanilla Minecraft font.
