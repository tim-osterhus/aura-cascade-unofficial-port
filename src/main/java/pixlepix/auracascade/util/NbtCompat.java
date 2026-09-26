package pixlepix.auracascade.util;

import com.mojang.serialization.Codec;
import java.util.Optional;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;

public final class NbtCompat {
    private NbtCompat() {
    }

    public static String getStringOr(CompoundTag tag, String key, String fallback) {
        return tag.getStringOr(key, fallback);
    }

    public static int getIntOr(CompoundTag tag, String key, int fallback) {
        return tag.getIntOr(key, fallback);
    }

    public static short getShortOr(CompoundTag tag, String key, short fallback) {
        return tag.getShortOr(key, fallback);
    }

    public static long getLongOr(CompoundTag tag, String key, long fallback) {
        return tag.getLongOr(key, fallback);
    }

    public static boolean getBooleanOr(CompoundTag tag, String key, boolean fallback) {
        return tag.getBooleanOr(key, fallback);
    }

    public static int[] getIntArrayOr(CompoundTag tag, String key, int[] fallback) {
        return tag.getIntArray(key).orElse(fallback);
    }

    public static CompoundTag getCompoundOrEmpty(CompoundTag tag, String key) {
        return tag.getCompoundOrEmpty(key);
    }

    public static ListTag getListOrEmpty(CompoundTag tag, String key, int elementType) {
        return tag.getList(key)
            .filter(list -> list.isEmpty() || list.get(0).getId() == elementType)
            .orElseGet(ListTag::new);
    }

    public static String getStringOr(ValueInput input, String key, String fallback) {
        return input.getStringOr(key, fallback);
    }

    public static int getIntOr(ValueInput input, String key, int fallback) {
        return input.getIntOr(key, fallback);
    }

    public static long getLongOr(ValueInput input, String key, long fallback) {
        return input.getLongOr(key, fallback);
    }

    public static boolean getBooleanOr(ValueInput input, String key, boolean fallback) {
        return input.getBooleanOr(key, fallback);
    }

    public static short getShortOr(ValueInput input, String key, short fallback) {
        return (short) input.getShortOr(key, fallback);
    }

    public static CompoundTag getCompoundOrEmpty(ValueInput input, String key) {
        return input.read(key, CompoundTag.CODEC).orElseGet(CompoundTag::new);
    }

    public static <T> Optional<T> read(CompoundTag tag, String key, Codec<T> codec) {
        return tag.read(key, codec);
    }

    public static <T> Optional<T> read(ValueInput input, String key, Codec<T> codec) {
        return input.read(key, codec);
    }

    public static <T> void store(CompoundTag tag, String key, Codec<T> codec, T value) {
        tag.store(key, codec, value);
    }

    public static <T> void store(ValueOutput output, String key, Codec<T> codec, T value) {
        output.store(key, codec, value);
    }
}
