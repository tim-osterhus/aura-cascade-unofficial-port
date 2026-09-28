package pixlepix.auracascade.parity;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

final class ClientTextColorAuditTest {
    private static final Path SOURCE_ROOT = Path.of("src/main/java/pixlepix/auracascade");
    private static final Pattern DRAW_STRING = Pattern.compile("graphics\\.drawString\\([\\s\\S]*?;");
    private static final Pattern COLOR = Pattern.compile("0x[0-9A-Fa-f]{6,8}");

    @Test
    void directGuiTextColorsHaveVisibleAlpha() throws Exception {
        for (String file : new String[] {
            "client/AuraCascadeClient.java",
            "client/screen/BookshelfCoordinatorScreen.java",
            "compat/client/AuraAccessoryScreen.java"
        }) {
            String source = Files.readString(SOURCE_ROOT.resolve(file));
            Matcher calls = DRAW_STRING.matcher(source);
            int literalColors = 0;
            while (calls.find()) {
                literalColors += assertOpaqueLiterals(calls.group(), file);
            }
            assertTrue(literalColors > 0, "No direct GUI text colors audited in " + file);
        }

        String bookshelf = Files.readString(SOURCE_ROOT.resolve("client/screen/BookshelfCoordinatorScreen.java"));
        Matcher helpers = Pattern.compile("private int (statusColor|resultColor)\\(\\) \\{([^}]*)\\}").matcher(bookshelf);
        int helperCount = 0;
        while (helpers.find()) {
            assertEquals(2, assertOpaqueLiterals(helpers.group(2), helpers.group(1)));
            helperCount++;
        }
        assertEquals(2, helperCount, "Bookshelf status/result color helpers must both be audited");
    }

    private static int assertOpaqueLiterals(String source, String context) {
        Matcher colors = COLOR.matcher(source);
        int count = 0;
        while (colors.find()) {
            long color = Long.parseUnsignedLong(colors.group().substring(2), 16);
            assertEquals(0xFF, (color >>> 24) & 0xFF, context + ": " + colors.group());
            count++;
        }
        return count;
    }
}
