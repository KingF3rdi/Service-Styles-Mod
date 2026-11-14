# PinguuTxw Styles (this branch)

Fabric mixer for **Minecraft 1.21.6–1.21.11**. Open with **Right Shift**.

This branch is the **PinguuTxw Styles** flavor (`PinguuTxw-Styles.jar`, 3D knives, lyrics 20 blocks in front of the skin). **`main` stays 7auction Styles** — do not merge this over 7auction identity. Build 7auction from this tree with `-Pproduct=7auction` (knives stay off).

Display name **PinguuTxw Styles**. Mod id `hugoservice`. Version `1.0.0.1`.

Product status: [`PinguuTxw-status.md`](PinguuTxw-status.md). 7auction notes: [`7auction-status.md`](7auction-status.md).

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
