/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.google.gson.Gson
 *  com.google.gson.GsonBuilder
 *  net.fabricmc.loader.api.FabricLoader
 */
package dev.hugoservice;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import dev.hugoservice.Crosshair;
import dev.hugoservice.HugoService;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.LinkOption;
import java.nio.file.OpenOption;
import java.nio.file.Path;
import java.nio.file.attribute.FileAttribute;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import net.fabricmc.loader.api.FabricLoader;

public final class Config {
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    public static final Path DIR = FabricLoader.getInstance().getConfigDir().resolve("hugoservice");
    public static final Path UPLOADS = DIR.resolve("packs");
    public static final Path SKIES = DIR.resolve("skies");
    public static final Path CAPES = DIR.resolve("capes");
    private static final Path FILE = DIR.resolve("config.json");
    public Map<String, String> loadout = new HashMap<String, String>();
    public Set<String> favorites = new HashSet<String>();
    public Map<String, Integer> ownColor = new HashMap<String, Integer>();
    public Set<String> ownColorOn = new HashSet<String>();
    public boolean greyWorld = false;
    public Set<String> keepColour = new HashSet<String>(Set.of("Obsidian", "Anchor", "Glowstone", "Fire"));
    public boolean applyInstantly = false;
    public boolean pixelFont = false;
    public boolean blur = false;
    public int[] crosshair = new int[225];
    public int paintColor = 0xFFFFFF;
    public boolean chOn = false;
    public String chStyle = "cross";
    public int chLength = 8;
    public int chGap = 4;
    public int chThickness = 2;
    public int chOutline = 1;
    public int chDot = 0;
    public int chColor = 0xFFFFFF;
    public int chAlpha = 100;
    public boolean chEntity = true;
    public boolean chEntityPlayers = false;
    public boolean chEntityMarker = true;
    public int chEntityColor = 0xFF3B3B;
    public Map<String, Integer> fxColor = new HashMap<String, Integer>();
    public Set<String> fxOn = new HashSet<String>();
    public int cobwebWidth = 3;
    public boolean glowSelf = false;
    public boolean glowRainbow = false;
    public int glowWidth = 2;
    public int crystalSpin = 100;
    public Integer crystalCore = null;
    public int fireHeight = 100;
    public boolean noFog = false;
    public int hitStrength = 70;
    public int outlineWidth = 1;
    public int shieldSize = 100;
    public int totemSize = 100;
    public int popSize = 100;
    public Map<String, Integer> handSize = new HashMap<String, Integer>();
    public int glintMode = 0;
    public int glintColor = 11766015;
    public int glintColor2 = 7132159;
    public Integer anchorGlow = null;
    public boolean anchorFullAtOne = false;
    public boolean anchorGlowPixels = false;
    public boolean chGradient = false;
    public int chColor2 = 4884223;
    public int killEffect = 0;
    public boolean killOnlyMine = true;
    public int spotifyScale = 100;
    public boolean spotifyPeek = false;
    public boolean badgeOn = false;
    public String badgeName = "";
    public int crystalOpacity = 100;
    public int shieldOpacity = 100;
    public int nametagSize = 100;
    public int inventoryScale = 0;
    public String spotifyClientId = "";
    public String spotifyRefresh = "";
    public boolean spotifyWindow = true;
    public boolean spotifyLyrics = true;
    public boolean spotifySky = false;
    public int spotifyCorner = 1;
    public int spotifySkySide = 0;
    public boolean spotifySkyFollow = true;
    public String spotifyMode = "auto";
    public boolean spotifyAnyApp = false;
    public boolean hideInventoryModel = false;
    public boolean fakeOwn = false;
    public boolean fakeEnemies = false;
    public boolean fakeOwnSkin = false;
    public boolean fakeEnemySkin = false;
    public boolean hideTier = false;
    public String fakeName = "Player";
    public String fakeEnemyName = "";
    public boolean fakeTier = true;
    public boolean totemPops = true;
    public boolean totemHud = true;
    public boolean totemChat = false;
    public boolean nameIcon = false;
    public String presenceUrl = "";
    public boolean skyRotate = false;
    public boolean skyKeepSun = false;
    public boolean skyNightFade = true;
    public int skyTime = -1;
    public int uiVersion = 0;
    public int sortMode = 0;
    public String lastCategory = "crystal";
    public String theme = "Aurora";
    public static Config INSTANCE = new Config();

    public static void load() {
        try {
            Config c;
            Files.createDirectories(UPLOADS, new FileAttribute[0]);
            Files.createDirectories(SKIES, new FileAttribute[0]);
            Files.createDirectories(CAPES, new FileAttribute[0]);
            if (Files.exists(FILE, new LinkOption[0]) && (c = (Config)GSON.fromJson(Files.readString(FILE, StandardCharsets.UTF_8), Config.class)) != null) {
                INSTANCE = c.fixNulls();
            }
        }
        catch (Exception e) {
            HugoService.LOG.warn("Could not read HugoService config, using defaults", (Throwable)e);
        }
    }

    public static void save() {
        try {
            Files.createDirectories(DIR, new FileAttribute[0]);
            Files.writeString(FILE, (CharSequence)GSON.toJson(INSTANCE), StandardCharsets.UTF_8, new OpenOption[0]);
        }
        catch (IOException e) {
            HugoService.LOG.warn("Could not save HugoService config", (Throwable)e);
        }
    }

    public Config copy() {
        return ((Config)GSON.fromJson(GSON.toJson((Object)this), Config.class)).fixNulls();
    }

    public String styleOf(String cat) {
        String v = this.loadout.get(cat);
        if (v != null) {
            return v;
        }
        if (cat.equals("xpbottle") || cat.equals("windcharge")) {
            return this.loadout.getOrDefault("xp", "vanilla");
        }
        return "vanilla";
    }

    public Integer colorOf(String cat) {
        return cat.equals("grass") && this.ownColorOn.contains(cat) ? this.ownColor.getOrDefault(cat, 8174955) : null;
    }

    public Integer fx(String key) {
        return this.fxOn.contains(key) ? Integer.valueOf(this.fxColorOf(key)) : null;
    }

    public int fxColorOf(String key) {
        return this.fxColor.getOrDefault(key, switch (key) {
            case "shield" -> 0x7CFF7C;
            case "shield_off" -> 0xFF5A5A;
            case "hit" -> 0xFF0000;
            default -> 9099007;
        });
    }

    private Config fixNulls() {
        if (this.loadout == null) {
            this.loadout = new HashMap<String, String>();
        }
        if (this.favorites == null) {
            this.favorites = new HashSet<String>();
        }
        if (this.ownColor == null) {
            this.ownColor = new HashMap<String, Integer>();
        }
        if (this.ownColorOn == null) {
            this.ownColorOn = new HashSet<String>();
        }
        if (this.keepColour == null) {
            this.keepColour = new HashSet<String>();
        }
        if (this.lastCategory == null) {
            this.lastCategory = "crystal";
        }
        if (this.theme == null || this.theme.isBlank()) {
            this.theme = "Aurora";
        }
        if (this.uiVersion < 3) {
            this.uiVersion = 3;
        }
        if (this.fakeName == null || this.fakeName.isBlank()) {
            this.fakeName = "Player";
        }
        if (this.fakeEnemyName == null) {
            this.fakeEnemyName = "";
        }
        if (this.badgeName == null) {
            this.badgeName = "";
        }
        if (this.presenceUrl == null) {
            this.presenceUrl = "";
        }
        if (this.spotifyClientId == null) {
            this.spotifyClientId = "";
        }
        if (this.spotifyRefresh == null) {
            this.spotifyRefresh = "";
        }
        if (this.spotifyMode == null) {
            this.spotifyMode = "auto";
        }
        if (this.chStyle == null || !Crosshair.STYLES.contains(this.chStyle)) {
            this.chStyle = "cross";
        }
        if (this.crosshair == null || this.crosshair.length != 225) {
            this.crosshair = new int[225];
        }
        if (this.fxColor == null) {
            this.fxColor = new HashMap<String, Integer>();
        }
        if (this.handSize == null) {
            this.handSize = new HashMap<String, Integer>();
        }
        if (this.shieldSize != 100) {
            this.handSize.putIfAbsent("shield", this.shieldSize);
            this.shieldSize = 100;
        }
        if (this.totemSize != 100) {
            this.handSize.putIfAbsent("totem", this.totemSize);
            this.totemSize = 100;
        }
        if (this.fxOn == null) {
            this.fxOn = new HashSet<String>();
        }
        return this;
    }
}

