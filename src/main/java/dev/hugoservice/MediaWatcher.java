/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.google.gson.Gson
 *  com.google.gson.JsonObject
 */
package dev.hugoservice;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import dev.hugoservice.Config;
import dev.hugoservice.HugoService;
import dev.hugoservice.Spotify;
import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.OpenOption;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.nio.file.attribute.FileAttribute;
import java.util.Locale;

final class MediaWatcher {
    private static final Gson GSON = new Gson();
    private static final Path DIR = Config.DIR.resolve("media");
    private static Process process;
    private static boolean anyApp;
    private static long retryAt;
    static volatile String problem;

    static boolean supported() {
        return System.getProperty("os.name", "").toLowerCase(Locale.ROOT).contains("win");
    }

    static synchronized void ensure(boolean any) {
        if (!MediaWatcher.supported()) {
            problem = "Automatic mode needs Windows";
            return;
        }
        if (process != null && process.isAlive() && any == anyApp) {
            return;
        }
        if (process != null) {
            MediaWatcher.stop();
        }
        if (System.currentTimeMillis() < retryAt) {
            return;
        }
        retryAt = System.currentTimeMillis() + 30000L;
        try {
            String script;
            Files.createDirectories(DIR, new FileAttribute[0]);
            try (InputStream in = MediaWatcher.class.getResourceAsStream("/assets/hugoservice/media_watch.ps1");){
                script = new String(in.readAllBytes(), StandardCharsets.UTF_8);
            }
            script = script.replace("__PARENT__", String.valueOf(ProcessHandle.current().pid())).replace("$__ANY__", any ? "$true" : "$false").replace("__DIR__", DIR.toAbsolutePath().toString().replace('\\', '/').replace("'", "''"));
            Path file = DIR.resolve("media_watch.ps1");
            Files.writeString(file, (CharSequence)("\ufeff" + script), StandardCharsets.UTF_8, new OpenOption[0]);
            process = new ProcessBuilder("powershell.exe", "-NoProfile", "-NonInteractive", "-WindowStyle", "Hidden", "-ExecutionPolicy", "Bypass", "-File", file.toString()).redirectErrorStream(true).start();
            anyApp = any;
            problem = "";
            Process p = process;
            Thread t = new Thread(() -> MediaWatcher.read(p), "HugoService-Media");
            t.setDaemon(true);
            t.start();
        }
        catch (Exception e) {
            problem = "Could not read Windows media: " + e.getMessage();
            HugoService.LOG.warn("Media watcher failed to start", (Throwable)e);
        }
    }

    private static void read(Process p) {
        try (BufferedReader r = new BufferedReader(new InputStreamReader(p.getInputStream(), StandardCharsets.UTF_8));){
            String line;
            while ((line = r.readLine()) != null) {
                if (!(line = line.trim().replace("\ufeff", "")).startsWith("{")) {
                    if (line.isEmpty() || !problem.isEmpty()) continue;
                    problem = line.length() > 80 ? line.substring(0, 80) : line;
                    continue;
                }
                try {
                    Spotify.fromSystem((JsonObject)GSON.fromJson(line, JsonObject.class));
                }
                catch (Exception e) {
                    HugoService.LOG.debug("Bad media line {}", line, e);
                }
            }
        }
        catch (Exception exception) {
            // empty catch block
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

    static void command(String cmd) {
        try {
            Path tmp = DIR.resolve("command.tmp");
            Files.writeString(tmp, (CharSequence)cmd, new OpenOption[0]);
            Files.move(tmp, DIR.resolve("command.txt"), StandardCopyOption.REPLACE_EXISTING);
        }
        catch (Exception e) {
            HugoService.LOG.debug("Media command failed", (Throwable)e);
        }
    }

    private MediaWatcher() {
    }

    static {
        problem = "";
    }
}

