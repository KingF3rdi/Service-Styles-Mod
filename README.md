# 7auction Styles

Fabric mod for **Minecraft 1.21.6–1.21.11**: mix single objects (crystal, armor, sky, extras) from texture packs and apply them as one pack. Open with **Right Shift**.

Display name **7auction Styles**. Mod id `hugoservice`. Version `1.0.0`.

Crystal tiles use the 3D end-crystal model. World extras (spin, opacity, core colour) are included. No knives.

Shipped jars are in [`dist/`](dist/):

| File | Minecraft |
| --- | --- |
| `7auction-Styles.jar` | 1.21.11 |
| `7auction-Styles-1.21.11.jar` | 1.21.11 |
| `7auction-Styles-1.21.10.jar` | 1.21.10 |
| `7auction-Styles-1.21.9.jar` | 1.21.9 |
| `7auction-Styles-1.21.8.jar` | 1.21.8 |
| `7auction-Styles-1.21.7.jar` | 1.21.7 |
| `7auction-Styles-1.21.6.jar` | 1.21.6 |

Product status: [`7auction-status.md`](7auction-status.md).

## Build

Requires JDK 21.

```bash
./gradlew remapJar
# other versions:
# ./gradlew remapJar -Pmc_compat=v12110 -Pminecraft_version=1.21.10
# ./gradlew remapJar -Pmc_compat=v1219  -Pminecraft_version=1.21.9
# ./gradlew remapJar -Pmc_compat=v1218  -Pminecraft_version=1.21.8
```

The mod jar ends up in `build/libs/`. Install it together with Fabric Loader and Fabric API.

## Credits

UI uses the vanilla Minecraft font.
