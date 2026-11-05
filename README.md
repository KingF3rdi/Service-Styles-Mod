# 7auction Styles

Fabric mod for **Minecraft 1.21.11**: mix single objects (crystal, armor, sky, extras) from texture packs and apply them as one pack. Open with **Right Shift**.

Display name **7auction Styles**. Mod id `hugoservice`. Version `1.0.0`.

Shipped jars are in [`dist/`](dist/):

| File | Minecraft |
| --- | --- |
| `7auction-Styles.jar` | 1.21.11 |
| `7auction-Styles-1.21.11.jar` | 1.21.11 |
| `7auction-Styles-1.21.10.jar` | 1.21.10 |
| `7auction-Styles-1.21.8.jar` | 1.21.8 |
| `7auction-Styles-1.21.6.jar` | 1.21.6 |

Product status: [`7auction-status.md`](7auction-status.md).

## Build

Requires JDK 21.

```bash
./gradlew build
```

The mod jar ends up in `build/libs/`. Install it together with Fabric Loader and Fabric API.

The checked-in Java tree is the previous GitHub snapshot plus 7auction branding, bundled packs, and the shipped `dist/` jars. The multi-version Fabric tree (`src/compat/v12111|v12110|v1218`, `Mc.java`, `StyleMenu.java`) was not available to copy into this commit.

## Credits

Menu font: [Poppins](https://github.com/itfoundry/Poppins) © The Poppins Project Authors, SIL Open Font License 1.1
(`src/main/resources/LICENSE_Poppins_OFL.txt`).
