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
# other versions: ./gradlew build -Pmc_compat=v12110
#                 ./gradlew build -Pmc_compat=v1218
```

The mod jar ends up in `build/libs/`. Install it together with Fabric Loader and Fabric API.

## Credits

Menu font: [Poppins](https://github.com/itfoundry/Poppins) © The Poppins Project Authors, SIL Open Font License 1.1
(`LICENSE_Poppins_OFL.txt`).
