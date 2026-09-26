package pixlepix.auracascade.aura;

import pixlepix.auracascade.parity.AuraColor;

public final class AuraPalette {
    private AuraPalette() {
    }

    public static int rgb(AuraColor color) {
        return switch (color) {
            case WHITE -> 0xFFFFFF;
            case BLACK -> 0x1A1A1A;
            case ORANGE -> 0xFF8000;
            case RED -> 0xFF1A1A;
            case YELLOW -> 0xFFFF1A;
            case GREEN -> 0x1AFF1A;
            case BLUE -> 0x1A1AFF;
            case VIOLET -> 0xFF1AFF;
        };
    }
}
