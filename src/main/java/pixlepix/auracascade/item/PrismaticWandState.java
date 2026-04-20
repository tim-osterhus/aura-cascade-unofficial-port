package pixlepix.auracascade.item;

import java.util.ArrayList;
import java.util.List;
import net.minecraft.core.component.DataComponents;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.core.BlockPos;

public final class PrismaticWandState {
    private static final String MODE_TAG = "mode";
    private static final String FIRST_POS_TAG = "firstPos";
    private static final String SECOND_POS_TAG = "secondPos";
    private static final String CLIPBOARD_TAG = "clipboard";

    private PrismaticWandState() {
    }

    public static Mode mode(ItemStack stack) {
        CustomData customData = stack.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY);
        CompoundTag tag = customData.copyTag();
        return Mode.byId(tag.getString(MODE_TAG).orElse(Mode.SELECTION.id()));
    }

    public static Mode cycleMode(ItemStack stack) {
        Mode nextMode = mode(stack).next();
        CustomData.update(DataComponents.CUSTOM_DATA, stack, tag -> tag.putString(MODE_TAG, nextMode.id()));
        return nextMode;
    }

    public static boolean setSelectionPoint(ItemStack stack, BlockPos pos) {
        boolean settingFirst = firstPosition(stack) == null || secondPosition(stack) != null;
        CustomData.update(DataComponents.CUSTOM_DATA, stack, tag -> {
            if (settingFirst) {
                tag.putString(FIRST_POS_TAG, serializePos(pos));
                tag.remove(SECOND_POS_TAG);
            } else {
                tag.putString(SECOND_POS_TAG, serializePos(pos));
            }
        });
        return settingFirst;
    }

    public static Selection selection(ItemStack stack) {
        BlockPos first = firstPosition(stack);
        BlockPos second = secondPosition(stack);
        if (first == null || second == null) {
            return null;
        }
        return new Selection(first, second);
    }

    public static void storeClipboard(ItemStack stack, List<ClipboardBlock> blocks) {
        String serialized = blocks.stream().map(ClipboardBlock::serialize).reduce((left, right) -> left + "\n" + right).orElse("");
        CustomData.update(DataComponents.CUSTOM_DATA, stack, tag -> tag.putString(CLIPBOARD_TAG, serialized));
    }

    public static List<ClipboardBlock> clipboard(ItemStack stack) {
        CustomData customData = stack.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY);
        CompoundTag tag = customData.copyTag();
        String serialized = tag.getString(CLIPBOARD_TAG).orElse("");
        ArrayList<ClipboardBlock> blocks = new ArrayList<>();
        if (serialized.isEmpty()) {
            return blocks;
        }
        for (String line : serialized.split("\n")) {
            if (!line.isEmpty()) {
                blocks.add(ClipboardBlock.deserialize(line));
            }
        }
        return blocks;
    }

    private static String serializePos(BlockPos pos) {
        return pos.getX() + "," + pos.getY() + "," + pos.getZ();
    }

    private static BlockPos firstPosition(ItemStack stack) {
        return readPosition(stack, FIRST_POS_TAG);
    }

    private static BlockPos secondPosition(ItemStack stack) {
        return readPosition(stack, SECOND_POS_TAG);
    }

    private static BlockPos readPosition(ItemStack stack, String key) {
        CustomData customData = stack.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY);
        CompoundTag tag = customData.copyTag();
        String raw = tag.getString(key).orElse("");
        return raw.isEmpty() ? null : deserializePos(raw);
    }

    private static BlockPos deserializePos(String raw) {
        String[] pieces = raw.split(",", 3);
        return new BlockPos(Integer.parseInt(pieces[0]), Integer.parseInt(pieces[1]), Integer.parseInt(pieces[2]));
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

        public String translationKey() {
            return translationKey;
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

    public record ClipboardBlock(int dx, int dy, int dz, int stateId, String itemId) {
        public String serialize() {
            return dx + "|" + dy + "|" + dz + "|" + stateId + "|" + itemId;
        }

        public static ClipboardBlock deserialize(String raw) {
            String[] pieces = raw.split("\\|", 5);
            return new ClipboardBlock(
                Integer.parseInt(pieces[0]),
                Integer.parseInt(pieces[1]),
                Integer.parseInt(pieces[2]),
                Integer.parseInt(pieces[3]),
                pieces[4]
            );
        }
    }
}
