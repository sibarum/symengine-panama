package sibarum.symengine;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;

/**
 * Locates and loads the SymEngine native library plus its runtime dependencies, in dependency order.
 *
 * <p>Search order: the {@code symengine.native.dir} system property (a directory containing the DLLs),
 * then the bundled classpath resources under {@code /natives/windows-x86_64/}, which are extracted to a
 * temp directory. Loading is idempotent and thread-safe.
 */
final class NativeLib {

    /** MinGW runtime deps first, then symengine itself — System.load resolves eagerly on Windows. */
    private static final String[] LOAD_ORDER = {
            "libwinpthread-1.dll", "libgcc_s_seh-1.dll", "libstdc++-6.dll", "libgmp-10.dll",
            "libsymengine-0.14.dll",
    };

    private static volatile boolean loaded;

    static synchronized void ensureLoaded() {
        if (loaded) {
            return;
        }
        String dir = System.getProperty("symengine.native.dir");
        try {
            Path base = dir != null ? Path.of(dir) : extractBundled();
            for (String name : LOAD_ORDER) {
                Path p = base.resolve(name);
                if (Files.exists(p)) {   // dep DLLs may legitimately come from elsewhere on the PATH
                    System.load(p.toAbsolutePath().toString());
                }
            }
        } catch (IOException | UnsatisfiedLinkError e) {
            throw new SymEngineException("failed to load SymEngine native library", e);
        }
        loaded = true;
    }

    private static Path extractBundled() throws IOException {
        Path tmp = Files.createTempDirectory("symengine-panama");
        tmp.toFile().deleteOnExit();
        for (String name : LOAD_ORDER) {
            try (InputStream in = NativeLib.class.getResourceAsStream("/natives/windows-x86_64/" + name)) {
                if (in == null) {
                    throw new IOException("bundled native missing: " + name
                            + " (build native/dist first — see native/build.sh — or set -Dsymengine.native.dir)");
                }
                Path out = tmp.resolve(name);
                Files.copy(in, out, StandardCopyOption.REPLACE_EXISTING);
                out.toFile().deleteOnExit();
            }
        }
        return tmp;
    }

    private NativeLib() {
    }
}
