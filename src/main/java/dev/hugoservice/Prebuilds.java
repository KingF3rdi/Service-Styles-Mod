package dev.hugoservice;

import java.util.List;
import java.util.Locale;

final class Prebuilds {
    static final List<String> FILES = List.of(
            "vSwift_x_Calm.zip",
            "navik_restored_silver.zip",
            "Swetsy_purp.zip",
            "VoidRose_Private.zip",
            "navik_restored_v2.zip",
            "Gabi_Violet_Private.zip",
            "Spear_Mace_Pack.zip",
            "cowarted_purple.zip",
            "navik_silver_private.zip",
            "Ika_Christmas_Pack.zip",
            "Gray_Cowarted_Mashup.zip");

    private Prebuilds() {
    }

    /** Mixes you save with "Save pack": preset_<name>.zip in the packs folder. */
    static final String OWN = "preset_";

    static boolean isOwn(String fileName) {
        return fileName != null && fileName.startsWith(OWN) && fileName.endsWith(".zip");
    }

    /** List name for a pack file: your mix's own name, a marketplace mix's name, else the file-based one. */
    static String displayName(String fileName, String fallback) {
        if (isOwn(fileName)) {
            return fileName.substring(OWN.length(), fileName.length() - ".zip".length()).replace('_', ' ').trim();
        }
        return Marketplace.nameFor(fileName, fallback);
    }

    static boolean isPrebuild(String fileName) {
        if (fileName == null) {
            return false;
        }
        if (isOwn(fileName)) {
            return true; // saved by you
        }
        if (Marketplace.isMarket(fileName)) {
            return true; // installed from the marketplace
        }
        String n = fileName.toLowerCase(Locale.ROOT);
        for (String f : FILES) {
            if (f.toLowerCase(Locale.ROOT).equals(n)) {
                return true;
            }
        }
        return false;
    }

    static void applyLoadout(Config cfg, String styleId) {
        if (styleId == null || !styleId.startsWith("pack:")) {
            return;
        }
        for (Categories.Group g : Categories.allGroups()) {
            if (g.id().equals("mixes")) {
                cfg.loadout.put("mixes", styleId);
                continue;
            }
            for (Styles.Style s : Styles.forGroup(g)) {
                if (styleId.equals(s.id)) {
                    cfg.loadout.put(g.id(), styleId);
                    break;
                }
            }
        }
    }
}
