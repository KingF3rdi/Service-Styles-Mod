// Based on Titus Cosmetics 1.3.22, MIT License, Copyright (c) 2026 Titus. See LICENSE_TitusCosmetics.txt.
package dev.hugoservice.cosmetics;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import dev.hugoservice.HugoService;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ClientPlayerEntity;

import java.io.IOException;
import java.io.Reader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * What you and other players wear. Your loadout is saved in config/hugoservice/cosmetics.json; the loadouts of other
 * HugoService players arrive through the presence server ({@link dev.hugoservice.Presence}), only when sharing is on.
 */
public final class CosmeticSync {
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().disableHtmlEscaping().create();
    private static volatile Loadout local = Loadout.EMPTY;
    private static final Map<UUID, Loadout> REMOTE = new ConcurrentHashMap<>();
    private static volatile boolean changed;

    public static void init() {
        local = loadSaved();
        ClientPlayConnectionEvents.DISCONNECT.register((handler, client) -> REMOTE.clear());
    }

    /** The cosmetics you wear. */
    public static Loadout local() { return local; }

    /** Changes and saves your loadout; the presence server gets it with the next heartbeat. */
    public static void setLocal(Loadout loadout) {
        local = loadout == null ? Loadout.EMPTY : loadout;
        save(local);
        changed = true;
    }

    /** True once after every change, so the presence code can send it soon. */
    public static boolean takeChanged() {
        boolean c = changed;
        changed = false;
        return c;
    }

    /** What a player wears; never null. */
    public static Loadout ofPlayer(UUID player) {
        if (player == null) return Loadout.EMPTY;
        ClientPlayerEntity self = MinecraftClient.getInstance().player;
        if (self != null && player.equals(self.getUuid())) return local;
        return REMOTE.getOrDefault(player, Loadout.EMPTY);
    }

    /** Stores a loadout received from the presence server. */
    public static void store(UUID player, String encoded) {
        if (player == null) return;
        Loadout loadout = Loadout.decode(encoded);
        if (loadout.isEmpty()) REMOTE.remove(player); else REMOTE.put(player, loadout);
    }

    private static final class Saved {
        String loadout = "";
    }

    private static Path file() {
        return FabricLoader.getInstance().getConfigDir().resolve("hugoservice").resolve("cosmetics.json");
    }

    private static Loadout loadSaved() {
        Path file = file();
        if (!Files.isRegularFile(file)) return Loadout.EMPTY;
        try (Reader reader = Files.newBufferedReader(file, StandardCharsets.UTF_8)) {
            Saved saved = GSON.fromJson(reader, Saved.class);
            return saved == null ? Loadout.EMPTY : Loadout.decode(saved.loadout);
        } catch (IOException | RuntimeException e) {
            HugoService.LOG.warn("Could not read saved cosmetics, starting without any", e);
            return Loadout.EMPTY;
        }
    }

    private static void save(Loadout loadout) {
        Path file = file();
        Path tmp = file.resolveSibling(file.getFileName() + ".tmp");
        Saved data = new Saved();
        data.loadout = loadout.encode();
        try {
            Files.createDirectories(file.getParent());
            Files.writeString(tmp, GSON.toJson(data), StandardCharsets.UTF_8);
            Files.move(tmp, file, StandardCopyOption.REPLACE_EXISTING);
        } catch (IOException | RuntimeException e) {
            HugoService.LOG.warn("Could not save cosmetics", e);
        }
    }

    private CosmeticSync() {}
}
