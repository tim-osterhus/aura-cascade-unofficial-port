package pixlepix.auracascade.block.entity;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import pixlepix.auracascade.block.AuraContent;
import pixlepix.auracascade.data.recipe.AuraVortexRecipeCatalog;
import pixlepix.auracascade.parity.AuraColor;

public class VortexControllerBlockEntity extends net.minecraft.world.level.block.entity.BlockEntity implements AuraSignalSource {
    public VortexControllerBlockEntity(BlockPos pos, BlockState blockState) {
        super(AuraContent.VORTEX_CONTROLLER_BLOCK_ENTITY, pos, blockState);
    }

    public static void serverTick(Level level, BlockPos pos, BlockState state, VortexControllerBlockEntity blockEntity) {
        if (!level.isClientSide()) {
            blockEntity.serverTick(level, pos);
        }
    }

    private void serverTick(Level level, BlockPos pos) {
        CraftingState initial = craftingState(level, pos);
        for (var entry : initial.pedestals().entrySet()) {
            var component = initial.match().map(value -> value.assignments().get(entry.getKey())).orElse(null);
            entry.getValue().setRequirement(component == null ? null : component.requiredColor(),
                component == null ? 0 : component.requiredPower());
        }
        CraftingState craftingState = craftingState(level, pos);
        if (craftingState.ready() && craftingState.match().isPresent()) {
            completeCraft(level, pos, craftingState.pedestals(), craftingState.match().get());
            setChanged();
            level.sendBlockUpdated(pos, getBlockState(), getBlockState(), 3);
        }
    }

    @Override
    public int auraSignal() {
        if (level == null) {
            return 0;
        }
        InspectionSnapshot snapshot = inspectionSnapshot(level, getBlockPos());
        return VortexCraftingLogic.progressSignal(snapshot.receivedPower(), snapshot.requiredPower());
    }

    public InspectionSnapshot inspectionSnapshot(Level level, BlockPos pos) {
        CraftingState state = craftingState(level, pos);
        if (state.match().isEmpty()) {
            return new InspectionSnapshot("", "", 0, 0, List.of());
        }
        var match = state.match().get();
        ArrayList<PedestalInspection> pedestals = new ArrayList<>();
        int received = 0;
        int required = 0;
        for (Direction direction : List.of(Direction.NORTH, Direction.EAST, Direction.SOUTH, Direction.WEST)) {
            BlockPos pedestalPos = pos.relative(direction);
            var component = match.assignments().get(pedestalPos);
            var pedestal = state.pedestals().get(pedestalPos);
            if (component == null || pedestal == null) {
                continue;
            }
            var receipt = pedestal.inspectionSnapshot();
            int credited = receipt.color() == component.requiredColor() && receipt.required() == component.requiredPower()
                ? Math.min(receipt.received(), component.requiredPower()) : 0;
            pedestals.add(new PedestalInspection(pedestalPos, component.item().getDescriptionId(), component.requiredColor(), credited,
                component.requiredPower()));
            received += credited;
            required += component.requiredPower();
        }
        return new InspectionSnapshot(match.recipe().id(), match.recipe().result().getDescriptionId(),
            received, required, List.copyOf(pedestals));
    }

    public Component statusMessage(Level level, BlockPos pos) {
        CraftingState craftingState = craftingState(level, pos);
        if (craftingState.pedestals().size() < 4) {
            return Component.literal("Vortex Controller: missing pedestals (" + craftingState.pedestals().size() + "/4).");
        }
        if (craftingState.loadedPedestals() < 4) {
            return Component.literal("Vortex Controller: waiting for items (" + craftingState.loadedPedestals() + "/4).");
        }
        if (craftingState.match().isEmpty()) {
            return Component.literal("Vortex Controller: recipe not recognized.");
        }

        ItemStack result = craftingState.match().get().recipe().result();
        String resultName = result.getHoverName().getString();
        if (craftingState.ready()) {
            return Component.literal(
                "Vortex Controller: completing " + resultName + "."
            );
        }
        InspectionSnapshot snapshot = inspectionSnapshot(level, pos);
        return Component.literal("Vortex Controller: making " + resultName + " (power received "
            + snapshot.receivedPower() + "/" + snapshot.requiredPower() + ").");
    }

    private static Map<BlockPos, VortexPedestalBlockEntity> pedestals(Level level, BlockPos controllerPos) {
        LinkedHashMap<BlockPos, VortexPedestalBlockEntity> pedestals = new LinkedHashMap<>();
        for (Direction direction : List.of(Direction.NORTH, Direction.EAST, Direction.SOUTH, Direction.WEST)) {
            BlockPos pedestalPos = controllerPos.relative(direction);
            if (level.getBlockEntity(pedestalPos) instanceof VortexPedestalBlockEntity pedestal) {
                pedestals.put(pedestalPos, pedestal);
            }
        }
        return pedestals;
    }

    private CraftingState craftingState(Level level, BlockPos pos) {
        Map<BlockPos, VortexPedestalBlockEntity> pedestalMap = pedestals(level, pos);
        List<VortexCraftingLogic.PedestalInput> inputs = new ArrayList<>();
        int loadedPedestals = 0;
        for (VortexPedestalBlockEntity pedestal : pedestalMap.values()) {
            ItemStack heldItem = pedestal.heldItem();
            if (heldItem.isEmpty()) {
                continue;
            }
            loadedPedestals++;
            var receipt = pedestal.inspectionSnapshot();
            inputs.add(new VortexCraftingLogic.PedestalInput(pedestal.getBlockPos(), heldItem,
                receipt.received(), receipt.color(), receipt.required()));
        }

        Optional<VortexCraftingLogic.RecipeMatch> match = loadedPedestals == 4
            ? VortexCraftingLogic.findMatch(inputs, AuraVortexRecipeCatalog.all())
            : Optional.empty();
        boolean ready = match.isPresent() && match.get().assignments().entrySet().stream().allMatch(entry -> {
            var pedestal = pedestalMap.get(entry.getKey());
            return pedestal != null && pedestal.canCraft(entry.getValue());
        });
        return new CraftingState(pedestalMap, inputs, loadedPedestals, match, ready);
    }

    private void completeCraft(
        Level level,
        BlockPos pos,
        Map<BlockPos, VortexPedestalBlockEntity> pedestals,
        VortexCraftingLogic.RecipeMatch match
    ) {
        for (Map.Entry<BlockPos, pixlepix.auracascade.data.recipe.AuraVortexRecipe.Component> entry : match.assignments().entrySet()) {
            VortexPedestalBlockEntity pedestal = pedestals.get(entry.getKey());
            if (pedestal == null) {
                return;
            }
            pedestal.consumeForRecipe(entry.getValue());
            level.sendBlockUpdated(pedestal.getBlockPos(), pedestal.getBlockState(), pedestal.getBlockState(), 3);
        }

        ItemEntity output = new ItemEntity(
            level,
            pos.getX() + 0.5D,
            pos.getY() + 1.2D,
            pos.getZ() + 0.5D,
            match.recipe().result().copy()
        );
        output.setDeltaMovement(0.0D, 0.05D, 0.0D);
        level.addFreshEntity(output);
    }

    @Override
    public net.minecraft.nbt.CompoundTag getUpdateTag(HolderLookup.Provider registries) {
        return saveWithoutMetadata(registries);
    }

    @Override
    public Packet<ClientGamePacketListener> getUpdatePacket() {
        return ClientboundBlockEntityDataPacket.create(this);
    }

    private record CraftingState(
        Map<BlockPos, VortexPedestalBlockEntity> pedestals,
        List<VortexCraftingLogic.PedestalInput> inputs,
        int loadedPedestals,
        Optional<VortexCraftingLogic.RecipeMatch> match,
        boolean ready
    ) {
    }

    public record PedestalInspection(BlockPos pos, String itemDescriptionId, AuraColor color, int received, int required) {
    }

    public record InspectionSnapshot(String recipeId, String resultDescriptionId, int receivedPower,
                                     int requiredPower, List<PedestalInspection> pedestals) {
        public InspectionSnapshot {
            pedestals = List.copyOf(pedestals);
        }
    }
}
