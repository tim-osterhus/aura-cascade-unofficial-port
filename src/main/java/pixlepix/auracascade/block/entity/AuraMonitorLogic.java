package pixlepix.auracascade.block.entity;

import java.util.Collection;
import pixlepix.auracascade.aura.AuraInspectionState;

public final class AuraMonitorLogic {
    private AuraMonitorLogic() {
    }

    public static int nodeSignal(AuraInspectionState inspectionState, int capacity) {
        return inspectionState.comparatorLevel(capacity);
    }

    public static int pumpSignal(AuraInspectionState inspectionState, int capacity, boolean active) {
        return active ? 15 : nodeSignal(inspectionState, capacity);
    }

    public static int aggregate(Collection<Integer> signals) {
        int strongest = 0;
        for (int signal : signals) {
            strongest = Math.max(strongest, signal);
        }
        return Math.min(15, Math.max(0, strongest));
    }
}
