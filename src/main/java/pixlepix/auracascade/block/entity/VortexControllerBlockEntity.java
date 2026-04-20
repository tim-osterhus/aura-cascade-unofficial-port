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
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import pixlepix.auracascade.block.AuraContent;
import pixlepix.auracascade.data.recipe.AuraVortexRecipeCatalog;

public class VortexControllerBlockEntity extends net.minecraft.world.level.block.entity.BlockEntity implements AuraSignalSource {
    private static final String PROGRESS_TAG = "progress";

    private int progress;

    public VortexControllerBlockEntity(BlockPos pos, BlockState blockState) {
        super(AuraContent.VORTEX_CONTROLLER_BLOCK_ENTITY, pos, blockState);
    }

    public static void serverTick(Level level, BlockPos pos, BlockState state, VortexControllerBlockEntity blockEntity) {
        if (!level.isClientSide()) {
            blockEntity.serverTick(level, pos);
        }
    }

    private void serverTick(Level level, BlockPos pos) {
        int previousProgress = progress;
        CraftingState craftingState = craftingState(level, pos);

        if (craftingState.ready() && craftingState.match().isPresent()) {
            progress++;
            if (progress >= craftingState.match().get().recipe().maxProgress()) {
                completeCraft(level, pos, craftingState.pedestals(), craftingState.match().get());
                progress = 0;
            }
        } else if (progress > 0) {
            progress = Math.max(0, progress - 2);
        }

        if (progress != previousProgress) {
            setChanged();
            level.sendBlockUpdated(pos, getBlockState(), getBlockState(), 3);
        }
    }

    @Override
    public int auraSignal() {
        int maxProgress = AuraVortexRecipeCatalog.all().stream().mapToInt(recipe -> recipe.maxProgress()).max().orElse(100);
        return VortexCraftingLogic.progressSignal(progress, maxProgress);
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
                "Vortex Controller: crafting " + resultName + " (" + Math.min(progress, craftingState.match().get().recipe().maxProgress()) + "/"
                    + craftingState.match().get().recipe().maxProgress() + ")."
            );
        }
        return Component.literal("Vortex Controller: " + resultName + " selected, waiting for aura.");
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

    private static Map<BlockPos, VortexCraftingLogic.PedestalInput> inputsByPos(List<VortexCraftingLogic.PedestalInput> inputs) {
        LinkedHashMap<BlockPos, VortexCraftingLogic.PedestalInput> byPos = new LinkedHashMap<>();
        for (VortexCraftingLogic.PedestalInput input : inputs) {
            byPos.put(input.pos(), input);
        }
        return byPos;
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
            inputs.add(new VortexCraftingLogic.PedestalInput(pedestal.getBlockPos(), heldItem, pedestal.nodeState.storage()));
        }

        Optional<VortexCraftingLogic.RecipeMatch> match = loadedPedestals == 4
            ? VortexCraftingLogic.findMatch(inputs, AuraVortexRecipeCatalog.all())
            : Optional.empty();
        boolean ready = match.isPresent() && VortexCraftingLogic.ready(match.get(), inputsByPos(inputs));
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
    protected void loadAdditional(ValueInput input) {
        super.loadAdditional(input);
        progress = input.getIntOr(PROGRESS_TAG, 0);
    }

    @Override
    protected void saveAdditional(ValueOutput output) {
        super.saveAdditional(output);
        output.putInt(PROGRESS_TAG, progress);
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
}
