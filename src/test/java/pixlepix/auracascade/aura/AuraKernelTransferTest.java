package pixlepix.auracascade.aura;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import org.junit.jupiter.api.Test;
import pixlepix.auracascade.parity.AuraColor;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

final class AuraKernelTransferTest {
    @Test
    void scansStraightLineLinksAndStopsAtOpaqueOrNearerNodes() {
        BlockPos origin = new BlockPos(0, 0, 0);
        Set<BlockPos> nodes = Set.of(
            new BlockPos(0, 0, -3),
            new BlockPos(5, 0, 0),
            new BlockPos(7, 0, 0),
            new BlockPos(0, 2, 0),
            new BlockPos(0, 0, 4)
        );
        Set<BlockPos> opaque = Set.of(new BlockPos(0, 0, 2));

        Set<BlockPos> links = AuraKernel.scanStraightLineLinks(origin, nodes, opaque, AuraKernel.DEFAULT_LINK_RANGE);

        assertEquals(Set.of(
            new BlockPos(0, 0, -3),
            new BlockPos(5, 0, 0),
            new BlockPos(0, 2, 0)
        ), links);
    }

    @Test
    void plansNaturalTransferWithHorizontalAndDownwardBias() {
        BlockPos origin = new BlockPos(0, 10, 0);
        AuraNodeState source = new AuraNodeState();
        source.storage().set(AuraColor.WHITE, 500);
        source.storage().set(AuraColor.BLACK, 500);
        source.storage().set(AuraColor.ORANGE, 500);

        Map<BlockPos, AuraNodeState> connected = new LinkedHashMap<>();
        BlockPos horizontal = new BlockPos(1, 10, 0);
        BlockPos downward = new BlockPos(0, 9, 0);
        BlockPos upward = new BlockPos(0, 11, 0);
        connected.put(horizontal, new AuraNodeState());
        connected.put(downward, new AuraNodeState());
        connected.put(upward, new AuraNodeState());

        Map<BlockPos, AuraStorage> plans = AuraKernel.planNaturalTransfers(
            origin,
            source,
            connected,
            AuraTransferContext.natural(AuraEnvironment.CLEAR_DAY)
        );

        assertEquals(2, plans.size());
        assertEquals(160, plans.get(horizontal).get(AuraColor.WHITE));
        assertEquals(0, plans.get(horizontal).get(AuraColor.BLACK));
        assertEquals(160, plans.get(horizontal).get(AuraColor.ORANGE));
        assertEquals(160, plans.get(downward).get(AuraColor.WHITE));
        assertEquals(160, plans.get(downward).get(AuraColor.BLACK));
        assertEquals(0, plans.get(downward).get(AuraColor.ORANGE));
        assertTrue(!plans.containsKey(upward));
    }

    @Test
    void excludesPositionRejectedLinksBeforeNormalizingNaturalWeights() {
        BlockPos origin = new BlockPos(0, 10, 0);
        AuraNodeState source = new AuraNodeState(AuraStorage.of(AuraColor.WHITE, 1_000));
        BlockPos rejected = new BlockPos(1, 10, 0);
        BlockPos accepted = new BlockPos(2, 10, 0);
        Map<BlockPos, AuraNodeState> connected = new LinkedHashMap<>();
        connected.put(rejected, new AuraNodeState());
        connected.put(accepted, new AuraNodeState());

        Map<BlockPos, AuraStorage> plans = AuraKernel.planNaturalTransfers(
            origin,
            source,
            connected,
            new AuraTransferContext(AuraEnvironment.CLEAR_DAY, 25, 400, true),
            targetPos -> !targetPos.equals(rejected)
        );

        assertEquals(447, plans.get(accepted).get(AuraColor.WHITE));
        assertTrue(!plans.containsKey(rejected));
    }

    @Test
    void appliesTransferPowerAndSpecialUpwardRules() {
        AuraNodeState source = new AuraNodeState();
        AuraNodeState target = new AuraNodeState();
        source.storage().set(AuraColor.WHITE, 10);
        source.storage().set(AuraColor.BLACK, 10);
        source.storage().set(AuraColor.GREEN, 10);

        AuraTransferResult result = AuraKernel.applyTransfer(
            new BlockPos(0, 10, 0),
            source,
            new BlockPos(0, 8, 0),
            target,
            source.storage().copy(),
            AuraEnvironment.CLEAR_DAY
        );

        assertEquals(60, result.generatedPower());
        assertEquals(60, target.storedPower());
        assertEquals(0, source.storage().total());
        assertEquals(30, target.storage().total());

        AuraNodeState yellowSource = new AuraNodeState(AuraStorage.of(AuraColor.YELLOW, 200));
        AuraStorage yellowUp = AuraKernel.planControlledUpwardTransfer(
            new BlockPos(0, 0, 0),
            yellowSource,
            new BlockPos(0, 2, 0),
            new AuraNodeState(),
            20,
            AuraEnvironment.CLEAR_DAY,
            AuraKernel.DEFAULT_EQUILIBRIUM_THRESHOLD
        );
        assertEquals(20, yellowUp.get(AuraColor.YELLOW));

        AuraNodeState blueSource = new AuraNodeState(AuraStorage.of(AuraColor.BLUE, 200));
        AuraStorage blueUp = AuraKernel.planControlledUpwardTransfer(
            new BlockPos(0, 0, 0),
            blueSource,
            new BlockPos(0, 2, 0),
            new AuraNodeState(),
            20,
            AuraEnvironment.CLEAR_DAY,
            AuraKernel.DEFAULT_EQUILIBRIUM_THRESHOLD
        );
        assertEquals(5, blueUp.get(AuraColor.BLUE));

        AuraNodeState blackSource = new AuraNodeState(AuraStorage.of(AuraColor.BLACK, 200));
        AuraStorage blackUp = AuraKernel.planControlledUpwardTransfer(
            new BlockPos(0, 0, 0),
            blackSource,
            new BlockPos(0, 2, 0),
            new AuraNodeState(),
            20,
            AuraEnvironment.CLEAR_DAY,
            AuraKernel.DEFAULT_EQUILIBRIUM_THRESHOLD
        );
        assertEquals(0, blackUp.get(AuraColor.BLACK));
    }

    @Test
    void preservesRedExplosionLiftAndOrangeInducedCurrent() {
        AuraNodeState redNode = new AuraNodeState(AuraStorage.of(AuraColor.RED, 500));
        AuraStorage redLift = AuraKernel.planRedExplosionPushUp(
            new BlockPos(0, 0, 0),
            redNode,
            new BlockPos(0, 3, 0),
            200
        );
        assertEquals(66, redLift.get(AuraColor.RED));

        Map<BlockPos, Set<BlockPos>> networkLinks = Map.of(
            new BlockPos(0, 0, -1), Set.of(new BlockPos(1, 0, -1), new BlockPos(2, 0, -1)),
            new BlockPos(1, 0, 0), Set.of(new BlockPos(2, 0, 0)),
            new BlockPos(-1, 0, 0), Set.of(new BlockPos(0, 0, 0)),
            new BlockPos(0, 0, 1), Set.of(new BlockPos(0, 0, 2))
        );

        List<AuraInducedCurrent> induced = AuraKernel.planOrangeInducedCurrents(
            new BlockPos(0, 0, 0),
            Direction.EAST,
            40,
            networkLinks
        );

        assertEquals(2, induced.size());
        assertTrue(induced.stream().anyMatch(current -> current.nodePos().equals(new BlockPos(0, 0, -1))
            && current.downstreamTarget().equals(new BlockPos(1, 0, -1))
            && current.direction() == Direction.EAST && current.amount() == 40));
        assertTrue(induced.stream().anyMatch(current -> current.nodePos().equals(new BlockPos(0, 0, -1))
            && current.downstreamTarget().equals(new BlockPos(2, 0, -1))
            && current.direction() == Direction.EAST && current.amount() == 40));
    }

    @Test
    void reconcilesOrangeBurstAgainstTheOpposingRequestBeforeScaling() {
        AuraStorage source = new AuraStorage();
        source.set(AuraColor.WHITE, 100);

        AuraKernel.OrangeBurstPlan forward = AuraKernel.planOrangeBurst(source, 50, 20);
        AuraKernel.OrangeBurstPlan reverse = AuraKernel.planOrangeBurst(source, 20, 50);

        assertEquals(AuraStorage.of(AuraColor.WHITE, 30), forward.transfer());
        assertEquals(0, forward.opposingRemainder());
        assertTrue(reverse.transfer().isEmpty());
        assertEquals(30, reverse.opposingRemainder());
    }

    @Test
    void clampsNegativeLegacyOrangeScaleToZero() {
        AuraStorage source = new AuraStorage();
        source.set(AuraColor.WHITE, 100);
        source.set(AuraColor.ORANGE, 10);

        AuraKernel.OrangeBurstPlan plan = AuraKernel.planOrangeBurst(source, 30, 0);

        assertTrue(plan.transfer().isEmpty());
        assertEquals(0, plan.opposingRemainder());
    }
}
