package dev.hugoservice;

import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.nio.file.FileSystem;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.Collections;
import java.util.stream.Stream;
import net.fabricmc.loader.api.FabricLoader;

final class BundledPacks {
    private BundledPacks() {
    }

    static void install() {
        int builtIn = copyTree("/hugoservice/bundled-packs/", Config.UPLOADS);
        HugoService.LOG.info("[7auction] installed {} catalog packs into {}", builtIn, Config.UPLOADS);
    }

    private static int copyTree(String resourceDir, Path destDir) {
        int n = 0;
        try {
            Files.createDirectories(destDir);
            var url = BundledPacks.class.getResource(resourceDir.endsWith("/") ? resourceDir + "." : resourceDir);
            if (url == null) {
                url = BundledPacks.class.getResource(resourceDir.substring(0, resourceDir.length() - 1));
            }
            if (url == null) {
                return copyFromModJar(resourceDir, destDir);
            }
            URI uri = url.toURI();
            if ("jar".equals(uri.getScheme())) {
                try (FileSystem fs = FileSystems.newFileSystem(uri, Collections.emptyMap())) {
                    Path root = fs.getPath(resourceDir);
                    n += copyDir(root, destDir);
                } catch (java.nio.file.FileSystemAlreadyExistsException e) {
                    FileSystem fs = FileSystems.getFileSystem(uri);
                    n += copyDir(fs.getPath(resourceDir), destDir);
                }
            } else {
                n += copyDir(Path.of(uri), destDir);
            }
        } catch (Exception e) {
            HugoService.LOG.warn("[7auction] bundled packs {}: {}", resourceDir, e.toString());
            try {
                n += copyFromModJar(resourceDir, destDir);
            } catch (Exception e2) {
                HugoService.LOG.warn("[7auction] bundled packs fallback {}: {}", resourceDir, e2.toString());
            }
        }
        return n;
    }

    private static int copyDir(Path root, Path destDir) throws IOException {
        if (!Files.exists(root)) {
            return 0;
        }
        int n = 0;
        try (Stream<Path> s = Files.list(root)) {
            for (Path p : s.toList()) {
                if (!Files.isRegularFile(p) || !p.getFileName().toString().toLowerCase().endsWith(".zip")) {
                    continue;
                }
                Path dest = destDir.resolve(p.getFileName().toString());
                if (Files.exists(dest) && Files.size(dest) > 8L) {
                    ++n;
                    continue;
                }
                Files.copy(p, dest, StandardCopyOption.REPLACE_EXISTING);
                ++n;
            }
        }
        return n;
    }

    private static int copyFromModJar(String resourceDir, Path destDir) throws IOException {
        Path mod = FabricLoader.getInstance().getModContainer("hugoservice").orElseThrow().getOrigin().getPaths().getFirst();
        if (!Files.isRegularFile(mod)) {
            return 0;
        }
        int n = 0;
        String prefix = resourceDir.startsWith("/") ? resourceDir.substring(1) : resourceDir;
        try (java.util.zip.ZipFile z = new java.util.zip.ZipFile(mod.toFile())) {
            var en = z.entries();
            while (en.hasMoreElements()) {
                var e = en.nextElement();
                String name = e.getName();
                if (e.isDirectory() || !name.startsWith(prefix) || !name.toLowerCase().endsWith(".zip")) {
                    continue;
                }
                String file = name.substring(name.lastIndexOf('/') + 1);
                Path dest = destDir.resolve(file);
                if (Files.exists(dest) && Files.size(dest) > 8L) {
                    ++n;
                    continue;
                }
                try (InputStream in = z.getInputStream(e)) {
                    Files.copy(in, dest, StandardCopyOption.REPLACE_EXISTING);
                }
                ++n;
            }
        }
        return n;
    }
}
