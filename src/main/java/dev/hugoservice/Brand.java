package dev.hugoservice;

import net.fabricmc.loader.api.FabricLoader;
import net.fabricmc.loader.api.metadata.ModMetadata;
import net.fabricmc.loader.api.metadata.Person;

/**
 * Name and author from fabric.mod.json. Knives stay off for 7auction.
 */
public final class Brand {
    public static final String ID = "hugoservice";
    public static final String NAME;
    public static final String SHORT;
    public static final String AUTHOR;
    public static final String UA;
    public static final boolean KNIVES;

    static {
        ModMetadata meta = FabricLoader.getInstance().getModContainer(ID).map(c -> c.getMetadata()).orElse(null);
        String name = meta != null ? meta.getName() : "7auction Styles";
        String author = "7auction";
        if (meta != null) {
            for (Person p : meta.getAuthors()) {
                if (p.getName() != null && !p.getName().isBlank()) {
                    author = p.getName();
                    break;
                }
            }
        }
        NAME = name == null || name.isBlank() ? "7auction Styles" : name;
        AUTHOR = author;
        SHORT = NAME.endsWith(" Styles") ? NAME.substring(0, NAME.length() - " Styles".length()) : NAME;
        UA = NAME;
        KNIVES = !"7auction".equalsIgnoreCase(SHORT) && !"7auction".equalsIgnoreCase(AUTHOR);
    }

    private Brand() {
    }
}
