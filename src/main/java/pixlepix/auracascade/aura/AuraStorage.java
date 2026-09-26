package pixlepix.auracascade.aura;

import java.util.Collections;
import java.util.EnumMap;
import java.util.Map;
import java.util.Objects;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import pixlepix.auracascade.parity.AuraColor;
import pixlepix.auracascade.util.NbtCompat;

public final class AuraStorage {
    private static final String ENTRIES_TAG = "entries";
    private static final String COLOR_TAG = "color";
    private static final String AMOUNT_TAG = "amount";

    private final EnumMap<AuraColor, Integer> amounts;

    public AuraStorage() {
        this.amounts = new EnumMap<>(AuraColor.class);
        for (AuraColor color : AuraColor.values()) {
            amounts.put(color, 0);
        }
    }

    public AuraStorage(AuraStorage source) {
        this();
        addAll(source);
    }

    public static AuraStorage of(AuraColor color, int amount) {
        AuraStorage storage = new AuraStorage();
        storage.set(color, amount);
        return storage;
    }

    public int get(AuraColor color) {
        return amounts.get(color);
    }

    public void set(AuraColor color, int amount) {
        Objects.requireNonNull(color, "color");
        amounts.put(color, Math.max(0, amount));
    }

    public void add(AuraColor color, int amount) {
        if (amount < 0) {
            throw new IllegalArgumentException("amount must be non-negative");
        }
        if (amount == 0) {
            return;
        }
        set(color, get(color) + amount);
    }

    public int removeUpTo(AuraColor color, int amount) {
        if (amount < 0) {
            throw new IllegalArgumentException("amount must be non-negative");
        }
        int removed = Math.min(get(color), amount);
        set(color, get(color) - removed);
        return removed;
    }

    public void addAll(AuraStorage other) {
        for (AuraColor color : AuraColor.values()) {
            add(color, other.get(color));
        }
    }

    public void subtractAll(AuraStorage other) {
        for (AuraColor color : AuraColor.values()) {
            removeUpTo(color, other.get(color));
        }
    }

    public AuraStorage copy() {
        return new AuraStorage(this);
    }

    public int total() {
        int total = 0;
        for (AuraColor color : AuraColor.values()) {
            total += get(color);
        }
        return total;
    }

    public double composition(AuraColor color) {
        int total = total();
        if (total == 0) {
            return 0.0D;
        }
        return (double) get(color) / total;
    }

    public boolean covers(AuraStorage other) {
        for (AuraColor color : AuraColor.values()) {
            if (get(color) < other.get(color)) {
                return false;
            }
        }
        return true;
    }

    public boolean isEmpty() {
        return total() == 0;
    }

    public AuraStorage scaled(double factor) {
        if (factor < 0.0D) {
            throw new IllegalArgumentException("factor must be non-negative");
        }
        AuraStorage scaled = new AuraStorage();
        for (AuraColor color : AuraColor.values()) {
            scaled.set(color, (int) Math.floor(get(color) * factor));
        }
        return scaled;
    }

    public AuraStorage min(AuraStorage other) {
        AuraStorage minimum = new AuraStorage();
        for (AuraColor color : AuraColor.values()) {
            minimum.set(color, Math.min(get(color), other.get(color)));
        }
        return minimum;
    }

    public Map<AuraColor, Integer> asMap() {
        return Collections.unmodifiableMap(new EnumMap<>(amounts));
    }

    public CompoundTag toTag() {
        CompoundTag tag = new CompoundTag();
        ListTag entries = new ListTag();
        for (AuraColor color : AuraColor.values()) {
            CompoundTag entry = new CompoundTag();
            entry.putString(COLOR_TAG, color.id());
            entry.putInt(AMOUNT_TAG, get(color));
            entries.add(entry);
        }
        tag.put(ENTRIES_TAG, entries);
        return tag;
    }

    public static AuraStorage fromTag(CompoundTag tag) {
        AuraStorage storage = new AuraStorage();
        ListTag entries = NbtCompat.getListOrEmpty(tag, ENTRIES_TAG, Tag.TAG_COMPOUND);
        for (int index = 0; index < entries.size(); index++) {
            CompoundTag entry = entries.getCompoundOrEmpty(index);
            String colorId = NbtCompat.getStringOr(entry, COLOR_TAG, AuraColor.WHITE.id());
            AuraColor color = AuraColor.byId(colorId);
            storage.set(color, NbtCompat.getIntOr(entry, AMOUNT_TAG, 0));
        }
        return storage;
    }

    @Override
    public boolean equals(Object other) {
        if (this == other) {
            return true;
        }
        if (!(other instanceof AuraStorage storage)) {
            return false;
        }
        return amounts.equals(storage.amounts);
    }

    @Override
    public int hashCode() {
        return amounts.hashCode();
    }

    @Override
    public String toString() {
        return amounts.toString();
    }
}
