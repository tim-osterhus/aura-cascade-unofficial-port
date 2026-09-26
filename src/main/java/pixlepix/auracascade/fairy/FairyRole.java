package pixlepix.auracascade.fairy;

import java.util.Arrays;
import java.util.Locale;
import net.minecraft.network.chat.Component;

public enum FairyRole {
    BASIC(0, "fairy", "Fairy", "text.aura.fairy_role.fairy", 20),
    FIGHTER(1, "fighter", "Fighter Fairy", 3),
    DEBUFFER(2, "debuffer", "Debuffer Fairy", 3),
    BUFFER(3, "buffer", "Buffer Fairy", 2400),
    STEALER(4, "stealer", "Stealer Fairy", 200),
    PUSHER(5, "pusher", "Pusher Fairy", 3),
    SHOOTER(6, "shooter", "Shooter Fairy", 3),
    SAVIOR(7, "savior", "Savior Fairy", 3),
    FETCHER(8, "fetcher", "Fetcher Fairy", 1),
    BAITER(9, "baiter", "Baiter Fairy", 1),
    BREEDER(10, "breeder", "Breeder Fairy", 3),
    SCARER(11, "scarer", "Scarer Fairy", 16),
    EXTINGUISHER(12, "extinguisher", "Extinguisher Fairy", 1),
    DIGGER(13, "digger", "Digger Fairy", 20),
    GLIDER(14, "glider", "Glider Fairy", 4),
    LIGHTER(15, "lighter", "Lighter Fairy", 1),
    TRAINER(16, "trainer", "Training Fairy", "text.aura.fairy_role.training", 1);

    private final int legacyIndex;
    private final String id;
    private final String displayName;
    private final String translationKey;
    private final int tickInterval;

    FairyRole(int legacyIndex, String id, String displayName, int tickInterval) {
        this(legacyIndex, id, displayName, "text.aura.fairy_role." + id, tickInterval);
    }

    FairyRole(int legacyIndex, String id, String displayName, String translationKey, int tickInterval) {
        this.legacyIndex = legacyIndex;
        this.id = id;
        this.displayName = displayName;
        this.translationKey = translationKey;
        this.tickInterval = tickInterval;
    }

    public int legacyIndex() {
        return legacyIndex;
    }

    public int storedIndex() {
        return this == LIGHTER ? 14 : this == GLIDER ? 15 : legacyIndex;
    }

    public String id() {
        return id;
    }

    public String displayName() {
        return displayName;
    }

    public String translationKey() {
        return translationKey;
    }

    public Component displayComponent() {
        return Component.translatable(translationKey);
    }

    public int tickInterval() {
        return tickInterval;
    }

    public FairyRole next() {
        FairyRole[] roles = values();
        return roles[(ordinal() + 1) % roles.length];
    }

    public static FairyRole byId(String id) {
        String normalized = normalize(id);
        return Arrays.stream(values())
            .filter(role -> role.matches(normalized))
            .findFirst()
            .orElse(defaultRole());
    }

    public static FairyRole byLegacyIndex(int legacyIndex) {
        return Arrays.stream(values())
            .filter(role -> role.legacyIndex == legacyIndex)
            .findFirst()
            .orElse(defaultRole());
    }

    public static FairyRole byStoredIndex(int storedIndex) {
        return storedIndex == 14 ? LIGHTER : storedIndex == 15 ? GLIDER : byLegacyIndex(storedIndex);
    }

    public static FairyRole fromUserFacingName(String name) {
        return byId(name);
    }

    public static FairyRole defaultRole() {
        return BASIC;
    }

    private boolean matches(String normalized) {
        return normalize(id).equals(normalized) || normalize(displayName).equals(normalized);
    }

    private static String normalize(String value) {
        return value.toLowerCase(Locale.ROOT).replace(" fairy", "").replace(' ', '_');
    }
}
