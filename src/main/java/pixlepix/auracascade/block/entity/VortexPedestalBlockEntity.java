package pixlepix.auracascade.block.entity;

import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.minecraft.world.phys.AABB;
import pixlepix.auracascade.block.AuraContent;
import pixlepix.auracascade.data.recipe.AuraVortexRecipe;
import pixlepix.auracascade.parity.AuraColor;

public class VortexPedestalBlockEntity extends AuraNetworkBlockEntity {
    private static final String HELD_ITEM_TAG = "held_item";

    private ItemStack heldItem = ItemStack.EMPTY;

    public VortexPedestalBlockEntity(BlockPos pos, BlockState blockState) {
        super(AuraContent.VORTEX_PEDESTAL_BLOCK_ENTITY, pos, blockState);
    }

    public static void serverTick(Level level, BlockPos pos, BlockState state, VortexPedestalBlockEntity blockEntity) {
        if (!level.isClientSide()) {
            blockEntity.serverTick(level, pos);
        }
    }

    private void serverTick(Level level, BlockPos pos) {
        serverTickBase(level, pos);
        if (heldItem.isEmpty() && level.getGameTime() % 5L == 0L) {
            captureNearbyItem(level, pos);
        }
    }

    @Override
    protected boolean canSendAuraTo(BlockPos targetPos, AuraColor color) {
        return false;
    }

    @Override
    protected boolean canReceiveAuraFrom(BlockPos sourcePos, AuraColor color) {
        return true;
    }

    @Override
    protected int comparatorCapacity() {
        return 10_000;
    }

    public ItemStack heldItem() {
        return heldItem.copy();
    }

    public boolean canCraft(AuraVortexRecipe.Component component) {
        return heldItem.is(component.item()) && heldItem.getCount() >= component.count() && nodeState.storage().covers(component.auraRequirement());
    }

    public void consumeForRecipe(AuraVortexRecipe.Component component) {
        if (!canCraft(component)) {
            return;
        }

        heldItem.shrink(component.count());
        if (heldItem.isEmpty()) {
            heldItem = ItemStack.EMPTY;
        }
        nodeState.storage().subtractAll(component.auraRequirement());
        setChanged();
    }

    public void dropHeldItem(Level level, BlockPos pos) {
        if (!heldItem.isEmpty() && !level.isClientSide()) {
            net.minecraft.world.level.block.Block.popResource(level, pos.above(), heldItem.copy());
            heldItem = ItemStack.EMPTY;
            setChanged();
        }
    }

    private void captureNearbyItem(Level level, BlockPos pos) {
        AABB captureBox = new AABB(pos).move(0.15D, 0.9D, 0.15D).inflate(0.2D, 0.3D, 0.2D);
        for (ItemEntity itemEntity : level.getEntitiesOfClass(ItemEntity.class, captureBox)) {
            ItemStack stack = itemEntity.getItem();
            if (stack.isEmpty()) {
                continue;
            }

            heldItem = stack.copyWithCount(1);
            stack.shrink(1);
            if (stack.isEmpty()) {
                itemEntity.discard();
            }
            setChanged();
            level.sendBlockUpdated(pos, getBlockState(), getBlockState(), 3);
            return;
        }
    }

    @Override
    protected void loadAdditional(ValueInput input) {
        super.loadAdditional(input);
        heldItem = input.read(HELD_ITEM_TAG, ItemStack.OPTIONAL_CODEC).orElse(ItemStack.EMPTY);
    }

    @Override
    protected void saveAdditional(ValueOutput output) {
        super.saveAdditional(output);
        output.store(HELD_ITEM_TAG, ItemStack.OPTIONAL_CODEC, heldItem);
    }

    @Override
    public net.minecraft.nbt.CompoundTag getUpdateTag(HolderLookup.Provider registries) {
        return saveWithoutMetadata(registries);
    }

    @Override
    public Packet<ClientGamePacketListener> getUpdatePacket() {
        return ClientboundBlockEntityDataPacket.create(this);
    }
}
