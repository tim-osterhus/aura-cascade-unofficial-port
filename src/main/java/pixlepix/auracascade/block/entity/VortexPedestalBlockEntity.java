package pixlepix.auracascade.block.entity;

import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import org.joml.Vector3f;
import pixlepix.auracascade.block.AuraContent;
import pixlepix.auracascade.aura.AuraEnvironment;
import pixlepix.auracascade.aura.AuraPalette;
import pixlepix.auracascade.aura.AuraStorage;
import pixlepix.auracascade.data.recipe.AuraVortexRecipe;
import pixlepix.auracascade.parity.AuraColor;

public class VortexPedestalBlockEntity extends AuraNetworkBlockEntity {
    private static final String HELD_ITEM_TAG = "held_item";
    private static final String POWER_RECEIVED_TAG = "power_received";
    private static final String REQUIRED_COLOR_TAG = "required_color";
    private static final String REQUIRED_POWER_TAG = "required_power";

    private ItemStack heldItem = ItemStack.EMPTY;
    private int powerReceived;
    private AuraColor requiredColor;
    private int requiredPower;

    public VortexPedestalBlockEntity(BlockPos pos, BlockState blockState) {
        super(AuraContent.VORTEX_PEDESTAL_BLOCK_ENTITY, pos, blockState);
    }

    public static void serverTick(Level level, BlockPos pos, BlockState state, VortexPedestalBlockEntity blockEntity) {
        if (level.isClientSide()) {
            blockEntity.clientTickVisuals(level, pos);
        } else {
            blockEntity.serverTick(level, pos);
        }
    }

    private void clientTickVisuals(Level level, BlockPos pos) {
        if (heldItem.isEmpty() || powerReceived <= 0 || requiredColor == null || level.getGameTime() % 5L != 0L) {
            return;
        }
        int rgb = AuraPalette.rgb(requiredColor);
        DustParticleOptions particle = new DustParticleOptions(new Vector3f(
            ((rgb >> 16) & 255) / 255.0F, ((rgb >> 8) & 255) / 255.0F, (rgb & 255) / 255.0F
        ), 0.65F);
        double angle = (level.getGameTime() % 40L) * Math.PI / 20.0D;
        level.addParticle(particle, pos.getX() + 0.5D + Math.cos(angle) * 0.3D,
            pos.getY() + 1.0D, pos.getZ() + 0.5D + Math.sin(angle) * 0.3D, 0.0D, 0.01D, 0.0D);
    }

    private void serverTick(Level level, BlockPos pos) {
        serverTickBase(level, pos);
        if (heldItem.isEmpty() && level.getGameTime() % 5L == 0L) {
            captureNearbyItem(level, pos);
        }
        syncInspection(level, pos);
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

    public ItemStack exchangeHeldItem(ItemStack offered) {
        ItemStack previous = heldItem.copy();
        heldItem = selectedItem(offered);
        setRequirement(null, 0);
        setChanged();
        if (level != null) {
            level.sendBlockUpdated(worldPosition, getBlockState(), getBlockState(), 3);
        }
        return previous;
    }

    static ItemStack selectedItem(ItemStack offered) {
        return offered.isEmpty() ? ItemStack.EMPTY : offered.copyWithCount(1);
    }

    public Receipt inspectionSnapshot() {
        return new Receipt(powerReceived, requiredPower, requiredColor);
    }

    public void setRequirement(AuraColor color, int power) {
        if (requiredColor != color || requiredPower != power) {
            requiredColor = color;
            requiredPower = Math.max(0, power);
            powerReceived = 0;
            setChanged();
        }
    }

    // Called by AuraNetworkBlockEntity after a falling transfer credits uncolored power.
    public void receiveFallingPower(AuraStorage moved, int fallDistance, AuraEnvironment environment) {
        if (fallDistance <= 0) {
            return;
        }
        PowerDelivery delivery = powerDelivery(moved, fallDistance, environment, requiredColor);
        if (delivery.generated() > 0) {
            nodeState.setStoredPower(Math.max(0, nodeState.storedPower() - delivery.generated()));
            powerReceived += Math.min(Math.max(0, requiredPower - powerReceived), delivery.accepted());
            setChanged();
        }
    }

    static PowerDelivery powerDelivery(AuraStorage moved, int fallDistance, AuraEnvironment environment, AuraColor requiredColor) {
        int generated = 0;
        int accepted = 0;
        for (AuraColor color : AuraColor.values()) {
            if (!color.generatesFallingPower()) {
                continue;
            }
            int amount = fallDistance <= 0 ? 0 : (int) Math.floor(fallDistance * moved.get(color) * color.relativeMass(environment));
            generated += Math.max(0, amount);
            if (requiredColor == AuraColor.WHITE || requiredColor == color) {
                accepted += Math.max(0, amount);
            }
        }
        return new PowerDelivery(generated, accepted);
    }

    public boolean canCraft(AuraVortexRecipe.Component component) {
        AuraColor color = component.requiredColor();
        return heldItem.is(component.item()) && heldItem.getCount() >= component.count()
            && requiredColor == color && requiredPower == component.requiredPower() && powerReceived >= requiredPower;
    }

    public void consumeForRecipe(AuraVortexRecipe.Component component) {
        if (!canCraft(component)) {
            return;
        }

        heldItem.shrink(component.count());
        if (heldItem.isEmpty()) {
            heldItem = ItemStack.EMPTY;
        }
        powerReceived = 0;
        setChanged();
    }

    public void dropHeldItem(Level level, BlockPos pos) {
        if (!heldItem.isEmpty() && !level.isClientSide()) {
            net.minecraft.world.level.block.Block.popResource(level, pos.above(), heldItem.copy());
            heldItem = ItemStack.EMPTY;
            setRequirement(null, 0);
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
            setRequirement(null, 0);
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
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        heldItem = tag.contains(HELD_ITEM_TAG, Tag.TAG_COMPOUND)
            ? ItemStack.parseOptional(registries, tag.getCompound(HELD_ITEM_TAG))
            : ItemStack.EMPTY;
        Receipt receipt = readReceipt(tag);
        powerReceived = receipt.received();
        requiredPower = receipt.required();
        requiredColor = receipt.color();
    }

    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        if (!heldItem.isEmpty()) {
            tag.put(HELD_ITEM_TAG, heldItem.saveOptional(registries));
        }
        writeReceipt(tag, inspectionSnapshot());
    }

    static Receipt readReceipt(CompoundTag tag) {
        int required = Math.max(0, pixlepix.auracascade.util.NbtCompat.getIntOr(tag, REQUIRED_POWER_TAG, 0));
        int received = Math.min(required, Math.max(0, pixlepix.auracascade.util.NbtCompat.getIntOr(tag, POWER_RECEIVED_TAG, 0)));
        String color = pixlepix.auracascade.util.NbtCompat.getStringOr(tag, REQUIRED_COLOR_TAG, "");
        return new Receipt(received, required, color.isEmpty() ? null : AuraColor.byId(color));
    }

    static void writeReceipt(CompoundTag tag, Receipt receipt) {
        tag.putInt(POWER_RECEIVED_TAG, receipt.received());
        tag.putInt(REQUIRED_POWER_TAG, receipt.required());
        if (receipt.color() != null) {
            tag.putString(REQUIRED_COLOR_TAG, receipt.color().id());
        } else {
            tag.remove(REQUIRED_COLOR_TAG);
        }
    }

    @Override
    public net.minecraft.nbt.CompoundTag getUpdateTag(HolderLookup.Provider registries) {
        return saveWithoutMetadata(registries);
    }

    @Override
    public Packet<ClientGamePacketListener> getUpdatePacket() {
        return ClientboundBlockEntityDataPacket.create(this);
    }

    public record Receipt(int received, int required, AuraColor color) {
    }

    record PowerDelivery(int generated, int accepted) {
    }
}
