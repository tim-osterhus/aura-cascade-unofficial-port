package pixlepix.auracascade.aura;

import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.Set;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import pixlepix.auracascade.util.NbtCompat;

public final class AuraNodeState {
    private static final String STORAGE_TAG = "storage";
    private static final String LINKED_NODES_TAG = "linked_nodes";
    private static final String HAS_SCANNED_LINKS_TAG = "has_scanned_links";
    private static final String STORED_POWER_TAG = "stored_power";
    private static final String X_TAG = "x";
    private static final String Y_TAG = "y";
    private static final String Z_TAG = "z";

    private final AuraStorage storage;
    private final LinkedHashSet<BlockPos> linkedNodes;
    private boolean hasScannedLinks;
    private int storedPower;

    public AuraNodeState() {
        this(new AuraStorage(), Set.of(), false, 0);
    }

    public AuraNodeState(AuraStorage storage) {
        this(storage, Set.of(), false, 0);
    }

    private AuraNodeState(
        AuraStorage storage,
        Collection<BlockPos> linkedNodes,
        boolean hasScannedLinks,
        int storedPower
    ) {
        this.storage = storage.copy();
        this.linkedNodes = new LinkedHashSet<>(linkedNodes);
        this.hasScannedLinks = hasScannedLinks;
        this.storedPower = Math.max(0, storedPower);
    }

    public AuraStorage storage() {
        return storage;
    }

    public Set<BlockPos> linkedNodes() {
        return Collections.unmodifiableSet(linkedNodes);
    }

    public void replaceLinkedNodes(Collection<BlockPos> links) {
        linkedNodes.clear();
        linkedNodes.addAll(links);
    }

    public boolean hasScannedLinks() {
        return hasScannedLinks;
    }

    public void setHasScannedLinks(boolean hasScannedLinks) {
        this.hasScannedLinks = hasScannedLinks;
    }

    public int storedPower() {
        return storedPower;
    }

    public void setStoredPower(int storedPower) {
        this.storedPower = Math.max(0, storedPower);
    }

    public void receivePower(int amount) {
        if (amount > 0) {
            storedPower += amount;
        }
    }

    public AuraInspectionState inspectionState() {
        return new AuraInspectionState(storage.copy(), linkedNodes.size(), storedPower, hasScannedLinks);
    }

    public CompoundTag toTag() {
        CompoundTag tag = new CompoundTag();
        tag.put(STORAGE_TAG, storage.toTag());

        ListTag links = new ListTag();
        for (BlockPos pos : linkedNodes) {
            CompoundTag entry = new CompoundTag();
            entry.putInt(X_TAG, pos.getX());
            entry.putInt(Y_TAG, pos.getY());
            entry.putInt(Z_TAG, pos.getZ());
            links.add(entry);
        }
        tag.put(LINKED_NODES_TAG, links);
        tag.putBoolean(HAS_SCANNED_LINKS_TAG, hasScannedLinks);
        tag.putInt(STORED_POWER_TAG, storedPower);
        return tag;
    }

    public static AuraNodeState fromTag(CompoundTag tag) {
        AuraStorage storage = AuraStorage.fromTag(NbtCompat.getCompoundOrEmpty(tag, STORAGE_TAG));
        ListTag links = NbtCompat.getListOrEmpty(tag, LINKED_NODES_TAG, Tag.TAG_COMPOUND);
        LinkedHashSet<BlockPos> linkedNodes = new LinkedHashSet<>();
        for (int index = 0; index < links.size(); index++) {
            CompoundTag entry = links.getCompound(index);
            linkedNodes.add(new BlockPos(
                NbtCompat.getIntOr(entry, X_TAG, 0),
                NbtCompat.getIntOr(entry, Y_TAG, 0),
                NbtCompat.getIntOr(entry, Z_TAG, 0)
            ));
        }
        return new AuraNodeState(
            storage,
            linkedNodes,
            NbtCompat.getBooleanOr(tag, HAS_SCANNED_LINKS_TAG, false),
            NbtCompat.getIntOr(tag, STORED_POWER_TAG, 0)
        );
    }
}
