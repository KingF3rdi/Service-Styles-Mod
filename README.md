# 7auction Styles

Fabric mod for **Minecraft 1.21–1.21.11**: mix single objects (crystal, armor, sky, extras) from texture packs and apply them as one pack. Open with **Right Shift**.

Display name **7auction Styles**. Mod id `hugoservice`. Version `1.0.0.1`.

Crystal tiles use the 3D end-crystal model. World extras (spin, opacity, core colour) are included on 1.21.2+. No knives.

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
| `7auction-Styles-1.21.5.jar` | 1.21.5 |
| `7auction-Styles-1.21.4.jar` | 1.21.4 |
| `7auction-Styles-1.21.3.jar` | 1.21.3 |
| `7auction-Styles-1.21.2.jar` | 1.21.2 |
| `7auction-Styles-1.21.1.jar` | 1.21.1 |
| `7auction-Styles-1.21.jar` | 1.21 |

Minecraft **26.1–26.3** are not shipped: official Yarn does not publish those versions, and Modern Yarn + Fabric API 26 rename too many mixer APIs (`ItemStack.isOf`, Fabric keybinding / world-render packages).

Product status: [`7auction-status.md`](7auction-status.md).

## Build

Requires JDK 21.

```bash
./gradlew remapJar
# other versions (pass matching yarn_mappings and fabric_api_version):
# ./gradlew remapJar -Pmc_compat=v12110 -Pminecraft_version=1.21.10
# ./gradlew remapJar -Pmc_compat=v1215  -Pminecraft_version=1.21.5
# ./gradlew remapJar -Pmc_compat=v1211  -Pminecraft_version=1.21.1
```

The mod jar ends up in `build/libs/`. Install it together with Fabric Loader and Fabric API.

## Credits

UI uses the vanilla Minecraft font.
