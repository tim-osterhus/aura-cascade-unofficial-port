package pixlepix.auracascade.parity;

import java.util.Arrays;
import java.util.Set;
import java.util.stream.Collectors;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

final class AuraColorTest {
    @Test
    void preservesLegacyEightColorBaseline() {
        assertEquals(8, AuraColor.values().length);

        Set<String> ids = Arrays.stream(AuraColor.values())
            .map(AuraColor::id)
            .collect(Collectors.toSet());
        Set<String> displayNames = Arrays.stream(AuraColor.values())
            .map(AuraColor::displayName)
            .collect(Collectors.toSet());

        assertEquals(8, ids.size());
        assertEquals(Set.of("white", "black", "orange", "red", "yellow", "green", "blue", "violet"), ids);
        assertEquals(Set.of("White", "Black", "Orange", "Red", "Yellow", "Green", "Blue", "Violet"), displayNames);
    }
}
