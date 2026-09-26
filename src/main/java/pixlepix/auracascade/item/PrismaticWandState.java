package pixlepix.auracascade.item;

import net.minecraft.core.BlockPos;
import net.minecraft.core.component.DataComponents;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.CustomData;
import pixlepix.auracascade.util.NbtCompat;

public final class PrismaticWandState {
    private static final String MODE_TAG = "mode";
    private static final String FIRST_POS_TAG = "firstPos";
    private static final String SECOND_POS_TAG = "secondPos";
    private static final String SOURCE_MIN_TAG = "sourceMin";
    private static final String SOURCE_MAX_TAG = "sourceMax";
    private static final String PLAYER_OFFSET_TAG = "playerOffset";
    private static final String OLD_CLIPBOARD_TAG = "clipboard";

    private PrismaticWandState() {
    }

    public static Mode mode(ItemStack stack) {
        return Mode.byId(NbtCompat.getStringOr(data(stack), MODE_TAG, Mode.SELECTION.id()));
    }

    public static Mode cycleMode(ItemStack stack) {
        Mode next = mode(stack).next();
        CustomData.update(DataComponents.CUSTOM_DATA, stack, tag -> {
            tag.putString(MODE_TAG, next.id());
            tag.remove(OLD_CLIPBOARD_TAG);
        });
        return next;
    }

    public static boolean setSelectionPoint(ItemStack stack, BlockPos pos) {
        BlockPos previous = readPosition(data(stack), FIRST_POS_TAG);
        CustomData.update(DataComponents.CUSTOM_DATA, stack, tag -> {
            if (previous != null) {
                tag.putString(SECOND_POS_TAG, encode(previous));
            } else {
                tag.remove(SECOND_POS_TAG);
            }
            tag.putString(FIRST_POS_TAG, encode(pos));
        });
        return previous == null;
    }

    public static Selection selection(ItemStack stack) {
        CompoundTag tag = data(stack);
        BlockPos first = readPosition(tag, FIRST_POS_TAG);
        BlockPos second = readPosition(tag, SECOND_POS_TAG);
        return first == null || second == null ? null : new Selection(first, second);
    }

    public static boolean copySelection(ItemStack stack, BlockPos playerPosition) {
        Selection selected = selection(stack);
        if (selected == null) {
            return false;
        }
        BlockPos min = selected.min();
        BlockPos max = selected.max();
        BlockPos offset = min.subtract(playerPosition);
        CustomData.update(DataComponents.CUSTOM_DATA, stack, tag -> {
            tag.putString(SOURCE_MIN_TAG, encode(min));
            tag.putString(SOURCE_MAX_TAG, encode(max));
            tag.putString(PLAYER_OFFSET_TAG, encode(offset));
            tag.remove(OLD_CLIPBOARD_TAG);
        });
        return true;
    }

    public static CopiedRegion copiedRegion(ItemStack stack) {
        CompoundTag tag = data(stack);
        BlockPos min = readPosition(tag, SOURCE_MIN_TAG);
        BlockPos max = readPosition(tag, SOURCE_MAX_TAG);
        BlockPos offset = readPosition(tag, PLAYER_OFFSET_TAG);
        if (min == null || max == null || offset == null
            || min.getX() > max.getX() || min.getY() > max.getY() || min.getZ() > max.getZ()) {
            return null;
        }
        return new CopiedRegion(min, max, offset);
    }

    private static CompoundTag data(ItemStack stack) {
        return stack.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY).copyTag();
    }

    private static String encode(BlockPos pos) {
        return pos.getX() + "," + pos.getY() + "," + pos.getZ();
    }

    private static BlockPos readPosition(CompoundTag tag, String key) {
        String raw = NbtCompat.getStringOr(tag, key, "");
        String[] parts = raw.split(",", -1);
        if (parts.length != 3) {
            return null;
        }
        try {
            return new BlockPos(Integer.parseInt(parts[0]), Integer.parseInt(parts[1]), Integer.parseInt(parts[2]));
        } catch (NumberFormatException ignored) {
            return null;
        }
    }

    public enum Mode {
        SELECTION("selection", "Selection", "text.aura.prismatic_wand_mode.selection"),
        COPY("copy", "Copy", "text.aura.prismatic_wand_mode.copy"),
        PASTE("paste", "Paste", "text.aura.prismatic_wand_mode.paste");

        private final String id;
        private final String displayName;
        private final String translationKey;

        Mode(String id, String displayName, String translationKey) {
            this.id = id;
            this.displayName = displayName;
            this.translationKey = translationKey;
        }

        public String id() {
            return id;
        }

        public String displayName() {
            return displayName;
        }

        public Component displayComponent() {
            return Component.translatable(translationKey);
        }

        public Mode next() {
            return switch (this) {
                case SELECTION -> COPY;
                case COPY -> PASTE;
                case PASTE -> SELECTION;
            };
        }

        public static Mode byId(String id) {
            for (Mode mode : values()) {
                if (mode.id.equals(id)) {
                    return mode;
                }
            }
            return SELECTION;
        }
    }

    public record Selection(BlockPos first, BlockPos second) {
        public BlockPos min() {
            return new BlockPos(
                Math.min(first.getX(), second.getX()),
                Math.min(first.getY(), second.getY()),
                Math.min(first.getZ(), second.getZ())
            );
        }

        public BlockPos max() {
            return new BlockPos(
                Math.max(first.getX(), second.getX()),
                Math.max(first.getY(), second.getY()),
                Math.max(first.getZ(), second.getZ())
            );
        }
    }

    public record CopiedRegion(BlockPos min, BlockPos max, BlockPos playerOffset) {
    }
}
