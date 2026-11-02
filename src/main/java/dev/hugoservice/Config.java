package dev.hugoservice;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import net.fabricmc.loader.api.FabricLoader;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

/** Everything the user picked. Saved to config/hugoservice/config.json. */
public final class Config {
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    public static final Path DIR = FabricLoader.getInstance().getConfigDir().resolve("hugoservice");
    public static final Path UPLOADS = DIR.resolve("packs");
    public static final Path SKIES = DIR.resolve("skies");
    public static final Path CAPES = DIR.resolve("capes");
    private static final Path FILE = DIR.resolve("config.json");

    /** category id -> style id */
    public Map<String, String> loadout = new HashMap<>();
    /** "category:styleId" */
    public Set<String> favorites = new HashSet<>();
    /** category id -> RGB, only used when the id is in ownColorOn */
    public Map<String, Integer> ownColor = new HashMap<>();
    public Set<String> ownColorOn = new HashSet<>();
    public boolean greyWorld = false;
    public Set<String> keepColour = new HashSet<>(Set.of("Obsidian", "Anchor", "Glowstone", "Fire"));
    public boolean applyInstantly = false;
    public boolean pixelFont = false;
    public boolean blur = false;
    /** Painted crosshair, 15x15 ARGB, row by row; 0 = empty. */
    public int[] crosshair = new int[225];
    public int paintColor = 0xFFFFFF;
    // Own crosshair (Crosshair tab), drawn by the mod; sizes in crosshair units
    public boolean chOn = false;
    public String chStyle = "cross";
    public int chLength = 8, chGap = 4, chThickness = 2, chOutline = 1, chDot = 0, chColor = 0xFFFFFF, chAlpha = 100;
    public boolean chEntity = true, chEntityPlayers = false, chEntityMarker = true;
    public int chEntityColor = 0xFF3B3B;
    // Knives: tricks when you pull one out, slight sway while holding it
    public boolean knifeTricks = true, knifeSway = true;
    /** In-game effects: hit, hitbox, outline, cobweb, glow. */
    public Map<String, Integer> fxColor = new HashMap<>();
    public Set<String> fxOn = new HashSet<>();
    /** Cobweb outline thickness, 1..10 (3 = the old fixed width). */
    public int cobwebWidth = 3;
    /** Player outline: yourself in third person, rainbow, thickness (outline blur radius, 2 = vanilla; on Apply). */
    public boolean glowSelf = false, glowRainbow = false;
    public int glowWidth = 2;
    /** End crystal spin in percent (0 = still) and core colour (null = as the texture). */
    public int crystalSpin = 100;
    public Integer crystalCore = null;
    /** First-person fire height in percent (100 = vanilla); no distance fog in the open air. */
    public int fireHeight = 100;
    public boolean noFog = false;
    /** Hit flash strength in percent (vanilla 70) and block outline thickness (1 = vanilla). */
    public int hitStrength = 70, outlineWidth = 1;
    /** Shield and totem size in your hand and the totem pop size, percent (100 = vanilla). */
    public int shieldSize = 100, totemSize = 100, popSize = 100;
    /** Enchant glint: 0 vanilla, 1 own colour, 2 gradient, 3 rainbow. */
    public int glintMode = 0, glintColor = 0xB388FF, glintColor2 = 0x6CD3FF;
    /** Respawn anchor (on Apply): own charge colour (null = as the texture), full look from 1 charge, glowing charge pixels. */
    public Integer anchorGlow = null;
    public boolean anchorFullAtOne = false, anchorGlowPixels = false;
    /** Crosshair arms fade to a second colour. */
    public boolean chGradient = false;
    public int chColor2 = 0x4A86FF;
    /** Kill effect (index into KillEffects.NAMES, 0 = off), only for players you hit. */
    public int killEffect = 0;
    public boolean killOnlyMine = true;
    /** Spotify window size in percent; only show it for a moment when a song starts. */
    public int spotifyScale = 100;
    public boolean spotifyPeek = false;
    /** Social badge next to the recipe book in your inventory (empty name = your own). */
    public boolean badgeOn = false;
    public String badgeName = "";
    /** 10..100 percent. */
    public int crystalOpacity = 100, shieldOpacity = 100;
    /** Name tag size in percent (50..200). */
    public int nametagSize = 100;
    /** GUI scale while an inventory is open; 0 = same as normal. */
    public int inventoryScale = 0;
    // Spotify player: the client id of your own Spotify app; the login token stays on this PC
    public String spotifyClientId = "";
    public String spotifyRefresh = "";
    public boolean spotifyWindow = true, spotifyLyrics = true, spotifySky = false;
    public int spotifyCorner = 1; // 0 top left, 1 top right, 2 bottom left, 3 bottom right
    public int spotifySkySide = 0; // sky lyrics stay over one side: 0 north, 1 east, 2 south, 3 west
    public boolean spotifySkyFollow = true; // sky lyrics move to where you look, but only when a new line starts
    public String spotifyMode = "auto"; // auto = read Windows media (no login), account = Spotify Web API
    public boolean spotifyAnyApp = false; // auto mode: also other players (browser, YouTube...)
    public boolean hideInventoryModel = false;
    /** Name faker. */
    public boolean fakeOwn = false, fakeEnemies = false, fakeOwnSkin = false, fakeEnemySkin = false, hideTier = false;
    public String fakeName = "Player";
    /** Name for all enemies; empty = numbered "Enemy1234" names. */
    public String fakeEnemyName = "";
    /** Tier tags next to a faked name show that name's MCTiers tier. */
    public boolean fakeTier = true;
    /** Totem counter: pops over heads, your totems beside the hotbar, a chat line per pop. */
    public boolean totemPops = true, totemHud = true, totemChat = false;
    /** Name icon via the presence server (opt-in). */
    public boolean nameIcon = false;
    public String presenceUrl = "";
    /** Skybox options; skyTime -1 = server time. */
    public boolean skyRotate = false, skyKeepSun = false, skyNightFade = true;
    public int skyTime = -1;
    public String theme = "Aurora";
    /** 1 = switched to the Aurora look once (1.15). */
    public int uiVersion = 0;
    public int sortMode = 0;
    public String lastCategory = "crystal";

    public static Config INSTANCE = new Config();

    public static void load() {
        try {
            Files.createDirectories(UPLOADS);
            Files.createDirectories(SKIES);
            Files.createDirectories(CAPES);
            if (Files.exists(FILE)) {
                Config c = GSON.fromJson(Files.readString(FILE, StandardCharsets.UTF_8), Config.class);
                if (c != null) INSTANCE = c.fixNulls();
            }
        } catch (Exception e) {
            HugoService.LOG.warn("Could not read HugoService config, using defaults", e);
        }
    }

    public static void save() {
        try {
            Files.createDirectories(DIR);
            Files.writeString(FILE, GSON.toJson(INSTANCE), StandardCharsets.UTF_8);
        } catch (IOException e) {
            HugoService.LOG.warn("Could not save HugoService config", e);
        }
    }

    /** Snapshot for background builds, so the UI can keep editing meanwhile. */
    public Config copy() { return GSON.fromJson(GSON.toJson(this), Config.class).fixNulls(); }

    public String styleOf(String cat) { return loadout.getOrDefault(cat, Styles.VANILLA_ID); }

    /** Own colour for a category, or null when off. */
    public Integer colorOf(String cat) {
        // only grass keeps an own colour; the other groups use styles
        return cat.equals("grass") && ownColorOn.contains(cat) ? ownColor.getOrDefault(cat, 0x7CBD6B) : null;
    }

    /** Effect colour (RGB) or null when the effect is off. */
    public Integer fx(String key) { return fxOn.contains(key) ? fxColorOf(key) : null; }

    /** Chosen colour of an effect, whether it is on or not. */
    public int fxColorOf(String key) {
        return fxColor.getOrDefault(key, switch (key) {
            case "shield" -> 0x7CFF7C;
            case "shield_off" -> 0xFF5A5A;
            case "hit" -> 0xFF0000;
            default -> 0x8AD6FF;
        });
    }

    private Config fixNulls() {
        if (loadout == null) loadout = new HashMap<>();
        if (favorites == null) favorites = new HashSet<>();
        if (ownColor == null) ownColor = new HashMap<>();
        if (ownColorOn == null) ownColorOn = new HashSet<>();
        if (keepColour == null) keepColour = new HashSet<>();
        if (lastCategory == null) lastCategory = "crystal";
        if (theme == null) theme = "Aurora";
        if (uiVersion < 1) { theme = "Aurora"; uiVersion = 1; }
        if (fakeName == null || fakeName.isBlank()) fakeName = "Player";
        if (fakeEnemyName == null) fakeEnemyName = "";
        if (badgeName == null) badgeName = "";
        if (presenceUrl == null) presenceUrl = "";
        if (spotifyClientId == null) spotifyClientId = "";
        if (spotifyRefresh == null) spotifyRefresh = "";
        if (spotifyMode == null) spotifyMode = "auto";
        if (chStyle == null || !Crosshair.STYLES.contains(chStyle)) chStyle = "cross";
        if (crosshair == null || crosshair.length != 225) crosshair = new int[225];
        if (fxColor == null) fxColor = new HashMap<>();
        if (fxOn == null) fxOn = new HashSet<>();
        return this;
    }
}
