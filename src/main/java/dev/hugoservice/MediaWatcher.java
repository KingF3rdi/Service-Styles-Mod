package dev.hugoservice;

import com.google.gson.Gson;
import com.google.gson.JsonObject;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Locale;

/**
 * Automatic player detection on Windows: a hidden PowerShell reads the system media controls (what the volume flyout
 * shows: title, artist, position, cover) and prints one JSON line per update. No login, works with Spotify Free.
 * The script ends by itself when the game closes.
 */
final class MediaWatcher {
    private static final Gson GSON = new Gson();
    private static final Path DIR = Config.DIR.resolve("media");
    private static Process process;
    private static boolean anyApp;
    private static long retryAt;
    static volatile String problem = "";

    static boolean supported() {
        return System.getProperty("os.name", "").toLowerCase(Locale.ROOT).contains("win");
    }

    /** Keeps the watcher running with the given filter (Spotify only, or any app). Cheap to call every tick. */
    static synchronized void ensure(boolean any) {
        if (!supported()) { problem = "Automatic mode needs Windows"; return; }
        if (process != null && process.isAlive() && any == anyApp) return;
        if (process != null) stop();
        if (System.currentTimeMillis() < retryAt) return;
        retryAt = System.currentTimeMillis() + 30_000; // if it dies, try again in 30 s
        try {
            Files.createDirectories(DIR);
            String script;
            try (InputStream in = MediaWatcher.class.getResourceAsStream("/assets/hugoservice/media_watch.ps1")) {
                script = new String(in.readAllBytes(), StandardCharsets.UTF_8);
            }
            script = script.replace("__PARENT__", String.valueOf(ProcessHandle.current().pid()))
                .replace("$__ANY__", any ? "$true" : "$false")
                .replace("__DIR__", DIR.toAbsolutePath().toString().replace('\\', '/').replace("'", "''"));
            Path file = DIR.resolve("media_watch.ps1");
            Files.writeString(file, "﻿" + script, StandardCharsets.UTF_8); // BOM: PowerShell 5 reads it as UTF-8
            process = new ProcessBuilder("powershell.exe", "-NoProfile", "-NonInteractive", "-WindowStyle", "Hidden",
                "-ExecutionPolicy", "Bypass", "-File", file.toString())
                .redirectErrorStream(true).start();
            anyApp = any;
            problem = "";
            Process p = process;
            Thread t = new Thread(() -> read(p), "HugoService-Media");
            t.setDaemon(true);
            t.start();
        } catch (Exception e) {
            problem = "Could not read Windows media: " + e.getMessage();
            HugoService.LOG.warn("Media watcher failed to start", e);
        }
    }

    private static void read(Process p) {
        try (BufferedReader r = new BufferedReader(new InputStreamReader(p.getInputStream(), StandardCharsets.UTF_8))) {
            String line;
            while ((line = r.readLine()) != null) {
                line = line.trim().replace("﻿", "");
                if (!line.startsWith("{")) { // PowerShell error text: keep the first line for the menu
                    if (!line.isEmpty() && problem.isEmpty()) problem = line.length() > 80 ? line.substring(0, 80) : line;
                    continue;
                }
                try {
                    Spotify.fromSystem(GSON.fromJson(line, JsonObject.class));
                } catch (Exception e) {
                    HugoService.LOG.debug("Bad media line {}", line, e);
                }
            }
        } catch (Exception ignored) {
            // process ended
        }
    }

    static synchronized void stop() {
        if (process != null) {
            process.destroy();
            process = null;
        }
    }

    static boolean running() {
        Process p = process;
        return p != null && p.isAlive();
    }

    /** Sends toggle / next / prev to the player; the watcher picks it up within a second. */
    static void command(String cmd) {
        try {
            Path tmp = DIR.resolve("command.tmp");
            Files.writeString(tmp, cmd);
            Files.move(tmp, DIR.resolve("command.txt"), java.nio.file.StandardCopyOption.REPLACE_EXISTING);
        } catch (Exception e) {
            HugoService.LOG.debug("Media command failed", e);
        }
    }

    private MediaWatcher() {}
}
