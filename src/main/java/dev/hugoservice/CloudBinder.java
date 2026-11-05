package dev.hugoservice;

import java.util.List;
import net.minecraft.item.Items;

public final class CloudBinder {
    private static final List<String> NEW_MODULES = List.of("trident", "rails", "tntcart", "elytra", "font");

    private CloudBinder() {
    }

    public static void bind() {
        for (CloudCategories.Def def : CloudCategories.all()) {
            String groupId = mapGroup(def.id());
            List<String> files = CloudCategories.wantedFiles(def);
            if (Categories.groupById(groupId) != null) {
                Categories.unionFiles(groupId, files);
                continue;
            }
            if (!NEW_MODULES.contains(def.id())) {
                HugoService.LOG.info("[Cloud] extra '{}' slot {} has no 7auction group; skipped (not a new module)", def.id(), def.slot());
                continue;
            }
            Categories.appendCategory(categoryFor(def, files));
        }
        Styles.rebuildWanted();
        HugoService.LOG.info("[Cloud] bound {} extra category defs", CloudCategories.all().size());
    }

    static String mapGroup(String cloudId) {
        return switch (cloudId) {
            case "spear" -> "spears";
            default -> cloudId;
        };
    }

    private static Categories.Category categoryFor(CloudCategories.Def def, List<String> files) {
        String section = switch (def.id()) {
            case "rails" -> "World";
            case "font" -> "Interface";
            default -> "PvP";
        };
        String iconFile = files.isEmpty() ? "textures/item/paper.png" : files.getFirst();
        Categories.Look look = def.id().equals("font") ? Categories.Look.WIDE : Categories.Look.FLAT;
        Categories.Group group = new Categories.Group(def.id(), def.name(), iconFile, look, null, files);
        var icon = switch (def.id()) {
            case "trident" -> Items.TRIDENT;
            case "rails" -> Items.RAIL;
            case "tntcart" -> Items.TNT_MINECART;
            case "elytra" -> Items.ELYTRA;
            case "font" -> Items.BOOK;
            default -> Items.PAPER;
        };
        return new Categories.Category(def.id(), def.name(), icon, section, List.of(group));
    }
}
