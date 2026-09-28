package pixlepix.auracascade.config;

import java.io.IOException;
import java.io.Reader;
import java.io.Writer;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Locale;
import java.util.Properties;
import net.neoforged.fml.loading.FMLPaths;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public final class AuraConfig {
    private static final String QUESTLINE_KEY = "questline";
    private static final Logger LOGGER = LoggerFactory.getLogger("AuraCascade");
    private static volatile boolean questlineEnabled = true;

    private AuraConfig() {
    }

    public static void bootstrap() {
        questlineEnabled = load(FMLPaths.CONFIGDIR.get().resolve("aura.properties"));
    }

    public static boolean questlineEnabled() {
        return questlineEnabled;
    }

    static boolean load(Path configFile) {
        Properties properties = new Properties();
        try {
            if (Files.exists(configFile)) {
                try (Reader reader = Files.newBufferedReader(configFile, StandardCharsets.UTF_8)) {
                    properties.load(reader);
                }
            }

            if (properties.getProperty(QUESTLINE_KEY) == null) {
                properties.setProperty(QUESTLINE_KEY, "true");
                Path parent = configFile.getParent();
                if (parent != null) {
                    Files.createDirectories(parent);
                }
                try (Writer writer = Files.newBufferedWriter(configFile, StandardCharsets.UTF_8)) {
                    properties.store(writer, "Aura Cascade configuration");
                }
            }
        } catch (IOException | IllegalArgumentException exception) {
            LOGGER.warn("Could not read Aura configuration at {}; keeping questline enabled", configFile, exception);
            return true;
        }

        String configured = properties.getProperty(QUESTLINE_KEY).trim().toLowerCase(Locale.ROOT);
        if (configured.equals("true")) {
            return true;
        }
        if (configured.equals("false")) {
            return false;
        }

        LOGGER.warn("Invalid '{}' value '{}' in {}; keeping questline enabled", QUESTLINE_KEY, configured, configFile);
        return true;
    }
}
