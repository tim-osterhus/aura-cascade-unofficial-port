package pixlepix.auracascade.block.entity;

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

}
