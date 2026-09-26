package pixlepix.auracascade.util;

import com.mojang.serialization.Codec;
import java.util.Optional;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.NbtOps;
import net.minecraft.nbt.Tag;

public final class NbtCompat {
    private NbtCompat() {
    }

    public static String getStringOr(CompoundTag tag, String key, String fallback) {
        return tag.contains(key, Tag.TAG_STRING) ? tag.getString(key) : fallback;
    }

    public static int getIntOr(CompoundTag tag, String key, int fallback) {
        return tag.contains(key, Tag.TAG_INT) ? tag.getInt(key) : fallback;
    }

    public static long getLongOr(CompoundTag tag, String key, long fallback) {
        return tag.contains(key, Tag.TAG_LONG) ? tag.getLong(key) : fallback;
    }

    public static boolean getBooleanOr(CompoundTag tag, String key, boolean fallback) {
        return tag.contains(key, Tag.TAG_BYTE) ? tag.getBoolean(key) : fallback;
    }

    public static int[] getIntArrayOr(CompoundTag tag, String key, int[] fallback) {
        return tag.contains(key, Tag.TAG_INT_ARRAY) ? tag.getIntArray(key) : fallback;
    }

    public static CompoundTag getCompoundOrEmpty(CompoundTag tag, String key) {
        return tag.contains(key, Tag.TAG_COMPOUND) ? tag.getCompound(key) : new CompoundTag();
    }

    public static ListTag getListOrEmpty(CompoundTag tag, String key, int elementType) {
        return tag.contains(key, Tag.TAG_LIST) ? tag.getList(key, elementType) : new ListTag();
    }

    public static <T> Optional<T> read(CompoundTag tag, String key, Codec<T> codec) {
        if (!tag.contains(key)) {
            return Optional.empty();
        }
        return codec.parse(NbtOps.INSTANCE, tag.get(key)).result();
    }

    public static <T> void store(CompoundTag tag, String key, Codec<T> codec, T value) {
        codec.encodeStart(NbtOps.INSTANCE, value).result().ifPresent(encoded -> tag.put(key, encoded));
    }
}
