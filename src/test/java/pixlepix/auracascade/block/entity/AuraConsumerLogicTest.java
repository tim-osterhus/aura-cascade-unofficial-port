package pixlepix.auracascade.block.entity;

import org.junit.jupiter.api.Test;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.Item;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.LiquidBlock;
import net.minecraft.world.phys.Vec3;
import pixlepix.auracascade.support.TestMinecraftBootstrap;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

final class AuraConsumerLogicTest {
    @Test
    void commonConsumerProgressAndPowerPairsMatchTheLegacyTiles() {
        assertEquals(60, AuraConsumerVariant.PROCESSOR.maxProgress());
        assertEquals(150, AuraConsumerVariant.PROCESSOR.powerPerProgress());
        assertEquals(9, AuraConsumerVariant.PRISMATIC_PROCESSOR.maxProgress());
        assertEquals(1_000, AuraConsumerVariant.PRISMATIC_PROCESSOR.powerPerProgress());
        assertEquals(3, AuraConsumerVariant.SMELTER.maxProgress());
        assertEquals(190, AuraConsumerVariant.SMELTER.powerPerProgress());
        assertEquals(2, AuraConsumerVariant.GROWER.maxProgress());
        assertEquals(50, AuraConsumerVariant.GROWER.powerPerProgress());
        assertEquals(200, AuraConsumerVariant.FISHER.maxProgress());
        assertEquals(200, AuraConsumerVariant.FISHER.powerPerProgress());
        assertEquals(25, AuraConsumerVariant.BREWER.maxProgress());
        assertEquals(500, AuraConsumerVariant.BREWER.powerPerProgress());
        assertEquals(12, AuraConsumerVariant.COLORER.maxProgress());
        assertEquals(50, AuraConsumerVariant.COLORER.powerPerProgress());
        assertEquals(50, AuraConsumerVariant.SYNTHESIZER.maxProgress());
        assertEquals(10_000, AuraConsumerVariant.SYNTHESIZER.powerPerProgress());
        assertEquals(1_000, AuraConsumerVariant.ENCHANTER.maxProgress());
        assertEquals(500, AuraConsumerVariant.ENCHANTER.powerPerProgress());
    }

    @Test
    void processorFamilyKeepsGeometricThroughput() {
        assertEquals(0, AuraConsumerLogic.progressStepsForTick(149, AuraConsumerVariant.PROCESSOR.powerPerProgress()));
        assertEquals(1, AuraConsumerLogic.progressStepsForTick(150, AuraConsumerVariant.PROCESSOR.powerPerProgress()));
        assertEquals(2, AuraConsumerLogic.progressStepsForTick(450, AuraConsumerVariant.PROCESSOR.powerPerProgress()));
        assertEquals(9_150, AuraConsumerLogic.minimumCyclePower(AuraConsumerVariant.PROCESSOR));
        assertTrue(
            AuraConsumerLogic.minimumCyclePower(AuraConsumerVariant.PRISMATIC_PROCESSOR)
                > AuraConsumerLogic.minimumCyclePower(AuraConsumerVariant.PROCESSOR)
        );
    }

    @Test
    void smelterCycleFloorMatchesItsConfiguredPowerBudget() {
        assertEquals(760, AuraConsumerLogic.minimumCyclePower(AuraConsumerVariant.SMELTER));
        assertEquals(2, AuraConsumerLogic.progressStepsForTick(570, AuraConsumerVariant.SMELTER.powerPerProgress()));
    }

    @Test
    void growerRetainsLowMidgameCyclePower() {
        assertEquals(150, AuraConsumerLogic.minimumCyclePower(AuraConsumerVariant.GROWER));
        assertEquals(2, AuraConsumerLogic.progressStepsForTick(150, AuraConsumerVariant.GROWER.powerPerProgress()));
        assertEquals(50, AuraConsumerLogic.GROWER_RANDOM_TICKS);
    }

    @Test
    void fisherAcceptsStillAndFlowingWaterStates() {
        TestMinecraftBootstrap.ensureBootstrapped();
        assertTrue(AuraConsumerLogic.isFisherWater(Blocks.WATER.defaultBlockState()));
        assertTrue(AuraConsumerLogic.isFisherWater(Blocks.WATER.defaultBlockState().setValue(LiquidBlock.LEVEL, 8)));
        assertFalse(AuraConsumerLogic.isFisherWater(Blocks.AIR.defaultBlockState()));
    }

    @Test
    void optionalCommonOreDustTagsPairByMaterialWithoutInventingOutputs() {
        TagKey<Item> copperOre = TagKey.create(
            Registries.ITEM,
            ResourceLocation.fromNamespaceAndPath("c", "ores/copper")
        );
        TagKey<Item> modOre = TagKey.create(
            Registries.ITEM,
            ResourceLocation.fromNamespaceAndPath("example", "ores/copper")
        );
        TagKey<Item> aggregateOre = TagKey.create(
            Registries.ITEM,
            ResourceLocation.fromNamespaceAndPath("c", "ores")
        );

        assertEquals(
            "c:dusts/copper",
            AuraConsumerLogic.commonDustTagForOre(copperOre).orElseThrow().location().toString()
        );
        assertTrue(AuraConsumerLogic.commonDustTagForOre(modOre).isEmpty());
        assertTrue(AuraConsumerLogic.commonDustTagForOre(aggregateOre).isEmpty());
    }

    @Test
    void fisherNeedsSustainedPowerInsteadOfFlatTimerTicks() {
        assertEquals(40_200, AuraConsumerLogic.minimumCyclePower(AuraConsumerVariant.FISHER));
        assertEquals(3, AuraConsumerLogic.progressStepsForTick(1_400, AuraConsumerVariant.FISHER.powerPerProgress()));
    }

    @Test
    void brewerUsesTheSteepestMidgamePowerCurve() {
        assertEquals(13_000, AuraConsumerLogic.minimumCyclePower(AuraConsumerVariant.BREWER));
        assertEquals(2, AuraConsumerLogic.progressStepsForTick(1_500, AuraConsumerVariant.BREWER.powerPerProgress()));
    }

    @Test
    void colorerStaysCheapButStillUsesBurstThroughput() {
        assertEquals(12, AuraConsumerVariant.COLORER.maxProgress());
        assertEquals(650, AuraConsumerLogic.minimumCyclePower(AuraConsumerVariant.COLORER));
        assertEquals(3, AuraConsumerLogic.progressStepsForTick(350, AuraConsumerVariant.COLORER.powerPerProgress()));
    }

    @Test
    void consumerCadenceDecayAndBurstCostMatchTheLegacyTickLoop() {
        assertTrue(AuraConsumerLogic.shouldAdvanceProgress(1));
        assertTrue(AuraConsumerLogic.shouldAdvanceProgress(2));
        assertTrue(AuraConsumerLogic.shouldAdvanceProgress(21));
        assertTrue(AuraConsumerLogic.shouldAdvanceProgress(22));
        assertFalse(AuraConsumerLogic.shouldAdvanceProgress(0));
        assertFalse(AuraConsumerLogic.shouldAdvanceProgress(3));
        assertFalse(AuraConsumerLogic.shouldAdvanceProgress(20));

        assertEquals(200, AuraConsumerLogic.bleedStoredPower(800));
        assertEquals(0, AuraConsumerLogic.bleedStoredPower(3));
        assertEquals(3, AuraConsumerLogic.progressStepsForTick(1_050, 150));
        assertEquals(1, AuraConsumerLogic.progressStepsForTick(150, 150));
    }

    @Test
    void idleProgressSpendsPowerAndLegacyCycleResetsBeforeNextCostCheck() {
        int[] completionAttempts = {0};
        AuraConsumerLogic.ProgressState idle = AuraConsumerLogic.advanceProgress(
            0,
            150,
            AuraConsumerVariant.PROCESSOR,
            () -> completionAttempts[0]++
        );
        assertEquals(1, idle.progress());
        assertEquals(0, idle.storedPower());
        assertEquals(0, completionAttempts[0]);

        AuraConsumerLogic.ProgressState boundary = AuraConsumerLogic.advanceProgress(
            AuraConsumerVariant.PROCESSOR.maxProgress(),
            150,
            AuraConsumerVariant.PROCESSOR,
            () -> completionAttempts[0]++
        );
        assertEquals(0, boundary.progress());
        assertEquals(0, boundary.storedPower());
        assertEquals(1, completionAttempts[0]);

        AuraConsumerLogic.ProgressState unaffordable = AuraConsumerLogic.advanceProgress(
            AuraConsumerVariant.PROCESSOR.maxProgress() + 1,
            149,
            AuraConsumerVariant.PROCESSOR,
            () -> completionAttempts[0]++
        );
        assertEquals(0, unaffordable.progress());
        assertEquals(149, unaffordable.storedPower());
        assertEquals(2, completionAttempts[0]);
    }

    @Test
    void processorDustConversionPrecedesCatalogRecipes() {
        assertEquals(AuraConsumerLogic.ProcessorRoute.DUST, AuraConsumerLogic.processorRoute(true, true));
        assertEquals(AuraConsumerLogic.ProcessorRoute.RECIPE, AuraConsumerLogic.processorRoute(false, true));
        assertEquals(AuraConsumerLogic.ProcessorRoute.NONE, AuraConsumerLogic.processorRoute(false, false));
    }

    @Test
    void growerBatchStopsWhenTheTargetBlockTypeChanges() {
        TestMinecraftBootstrap.ensureBootstrapped();
        assertTrue(AuraConsumerLogic.shouldContinueGrowerBatch(Blocks.WHEAT, Blocks.WHEAT.defaultBlockState()));
        assertFalse(AuraConsumerLogic.shouldContinueGrowerBatch(Blocks.WHEAT, Blocks.DIRT.defaultBlockState()));
    }

    @Test
    void processorAndSmelterReplacementDropsKeepTheInputDropContext() {
        TestMinecraftBootstrap.ensureBootstrapped();
        // Empty payloads isolate entity context without requiring a live world's registry provider.
        ItemEntity source = new ItemEntity(null, 4.25D, 8.5D, -2.75D, ItemStack.EMPTY,
            0.25D, -0.5D, 0.75D);
        source.setPickUpDelay(37);

        ItemEntity output = AuraConsumerBlockEntity.replacementDrop(null, source, ItemStack.EMPTY);

        assertEquals(4.25D, output.getX(), 0.0D);
        assertEquals(8.5D, output.getY(), 0.0D);
        assertEquals(-2.75D, output.getZ(), 0.0D);
        assertEquals(new Vec3(0.25D, -0.5D, 0.75D), output.getDeltaMovement());
        net.minecraft.nbt.CompoundTag outputData = new net.minecraft.nbt.CompoundTag();
        output.addAdditionalSaveData(outputData);
        assertEquals(37, outputData.getShort("PickupDelay"));
    }

    @Test
    void synthesizerMatchesLegacyAngelsteelCycleBudget() {
        assertEquals(510_000, AuraConsumerLogic.minimumCyclePower(AuraConsumerVariant.SYNTHESIZER));
        assertEquals(2, AuraConsumerLogic.progressStepsForTick(30_000, AuraConsumerVariant.SYNTHESIZER.powerPerProgress()));
    }

    @Test
    void enchanterRetainsItsLongLateGameRamp() {
        assertEquals(500_500, AuraConsumerLogic.minimumCyclePower(AuraConsumerVariant.ENCHANTER));
        assertEquals(2, AuraConsumerLogic.progressStepsForTick(1_500, AuraConsumerVariant.ENCHANTER.powerPerProgress()));
    }
}
