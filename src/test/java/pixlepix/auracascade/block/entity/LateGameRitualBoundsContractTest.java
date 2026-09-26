package pixlepix.auracascade.block.entity;

import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertTrue;

final class LateGameRitualBoundsContractTest {
    @Test
    void ritualConversionIncludesTheInclusiveMaximumBuildY() throws Exception {
        String source = Files.readString(Path.of(
            "src/main/java/pixlepix/auracascade/block/entity/LateGameBlockEntity.java"));

        assertTrue(source.contains("for (int y = level.getMinY(); y <= level.getMaxY(); y++)"));
    }
}
