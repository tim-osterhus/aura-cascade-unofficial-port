package pixlepix.auracascade.item;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;
import net.fabricmc.fabric.api.event.player.UseBlockCallback;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.Container;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.TooltipDisplay;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;

public final class PrismaticWandItem extends Item {
    static final int MAX_REGION_VOLUME = 512;
    private static boolean blockCallbackRegistered;

    public PrismaticWandItem(Properties properties) {
        super(properties.stacksTo(1));
        if (!blockCallbackRegistered) {
            UseBlockCallback.EVENT.register((player, level, hand, hit) -> {
                ItemStack held = player.getItemInHand(hand);
                return held.getItem() instanceof PrismaticWandItem wand
                    ? wand.handleUse(player, level, held, hit.getBlockPos()) : InteractionResult.PASS;
            });
            blockCallbackRegistered = true;
        }
    }

    @Override
    public InteractionResult useOn(UseOnContext context) {
        Player player = context.getPlayer();
        return player == null ? InteractionResult.PASS
            : handleUse(player, context.getLevel(), context.getItemInHand(), context.getClickedPos());
    }

    @Override
    public InteractionResult use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        InteractionResult result = handleUse(player, level, stack, null);
        return result == InteractionResult.PASS ? InteractionResult.PASS
            : level.isClientSide() ? InteractionResult.SUCCESS : InteractionResult.CONSUME;
    }

    private InteractionResult handleUse(Player player, Level level, ItemStack stack, BlockPos clicked) {
        if (player.isSpectator() || !player.isAlive()) {
            return InteractionResult.PASS;
        }
        if (!player.isShiftKeyDown() && clicked == null && PrismaticWandState.mode(stack) == PrismaticWandState.Mode.SELECTION) {
            return InteractionResult.PASS;
        }
        if (level.isClientSide()) {
            return InteractionResult.SUCCESS;
        }
        if (player.isShiftKeyDown()) {
            PrismaticWandState.Mode mode = PrismaticWandState.cycleMode(stack);
            status(player, Component.translatable("message.aura.prismatic_wand_switched", mode.displayComponent()));
            return InteractionResult.SUCCESS;
        }
        if (clicked != null && PrismaticWandState.mode(stack) == PrismaticWandState.Mode.SELECTION) {
            PrismaticWandState.setSelectionPoint(stack, clicked);
            status(player, Component.translatable("message.aura.prismatic_wand_position_set"));
            return InteractionResult.SUCCESS;
        }
        WorldAccess world = live((ServerLevel) level, player);
        if (PrismaticWandState.mode(stack) == PrismaticWandState.Mode.COPY) {
            PrismaticWandState.Selection selection = PrismaticWandState.selection(stack);
            if (selection == null || !sourceRegionIsSafe(world, selection.min(), selection.max())) {
                status(player, Component.translatable("message.aura.prismatic_wand_invalid_selection"));
                return InteractionResult.SUCCESS;
            }
            PrismaticWandState.copySelection(stack, playerPosition(player));
            status(player, Component.translatable("message.aura.prismatic_wand_copied"));
            return InteractionResult.SUCCESS;
        }
        PrismaticWandState.CopiedRegion copied = PrismaticWandState.copiedRegion(stack);
        if (copied == null) {
            status(player, Component.translatable("message.aura.prismatic_wand_nothing_copied"));
            return InteractionResult.SUCCESS;
        }
        PasteResult result = paste(world, copied, playerPosition(player), player.getInventory(),
            player.getInventory().getNonEquipmentItems().size(), player.getAbilities().instabuild);
        if (!result.safe()) {
            status(player, Component.translatable("message.aura.prismatic_wand_invalid_selection"));
        } else if (result.placed() == 0 && result.missingMaterial()) {
            status(player, Component.translatable("message.aura.prismatic_wand_not_enough_materials"));
        } else {
            status(player, Component.translatable("message.aura.prismatic_wand_pasted", result.placed()));
        }
        return InteractionResult.SUCCESS;
    }

    private static void status(Player player, Component message) {
        player.displayClientMessage(message, true);
    }

    private static BlockPos playerPosition(Player player) {
        return new BlockPos((int) player.getX(), (int) player.getY(), (int) player.getZ());
    }

    static int regionVolume(BlockPos min, BlockPos max) {
        long width = (long) max.getX() - min.getX() + 1L;
        long height = (long) max.getY() - min.getY() + 1L;
        long depth = (long) max.getZ() - min.getZ() + 1L;
        if (width < 1 || height < 1 || depth < 1
            || width > MAX_REGION_VOLUME || height > MAX_REGION_VOLUME || depth > MAX_REGION_VOLUME
            || width * height * depth > MAX_REGION_VOLUME) {
            return -1;
        }
        return (int) (width * height * depth);
    }

    static boolean sourceRegionIsSafe(WorldAccess world, BlockPos min, BlockPos max) {
        if (regionVolume(min, max) < 0) {
            return false;
        }
        for (long x = min.getX(); x <= max.getX(); x++) {
            for (long y = min.getY(); y <= max.getY(); y++) {
                for (long z = min.getZ(); z <= max.getZ(); z++) {
                    if (!world.isSafe(new BlockPos((int) x, (int) y, (int) z))) {
                        return false;
                    }
                }
            }
        }
        return true;
    }

    static PasteResult paste(WorldAccess world, PrismaticWandState.CopiedRegion copied, BlockPos playerPosition,
                             Container inventory, int materialSlots, boolean creative) {
        if (!world.mayBuild() || regionVolume(copied.min(), copied.max()) < 0) {
            return new PasteResult(false, 0, false);
        }
        List<Placement> plan = new ArrayList<>();
        for (long x = copied.min().getX(); x <= copied.max().getX(); x++) {
            for (long y = copied.min().getY(); y <= copied.max().getY(); y++) {
                for (long z = copied.min().getZ(); z <= copied.max().getZ(); z++) {
                    BlockPos source = new BlockPos((int) x, (int) y, (int) z);
                    BlockPos target = destination(copied, playerPosition, source);
                    if (target == null || !world.isSafe(source) || !world.isSafe(target)
                        || !world.mayInteract(target)) {
                        return new PasteResult(false, 0, false);
                    }
                    plan.add(new Placement(source, target));
                }
            }
        }
        int placed = 0;
        boolean missingMaterial = false;
        for (Placement placement : plan) {
            if (!world.getBlockState(placement.target()).isAir()) {
                continue;
            }
            BlockState state = world.getBlockState(placement.source());
            if (state.isAir()) {
                continue;
            }
            int slot = -1;
            if (!creative) {
                Item item = state.getBlock().asItem();
                if (!(item instanceof BlockItem) || !state.getFluidState().isEmpty()) {
                    continue;
                }
                slot = findMaterial(inventory, materialSlots, item);
                if (slot < 0) {
                    missingMaterial = true;
                    continue;
                }
            }
            if (world.setBlockState(placement.target(), state)
                && world.getBlockState(placement.target()).equals(state)) {
                placed++;
                if (!creative) {
                    inventory.removeItem(slot, 1);
                    inventory.setChanged();
                }
            }
        }
        return new PasteResult(true, placed, missingMaterial);
    }

    private static BlockPos destination(PrismaticWandState.CopiedRegion copied, BlockPos playerPosition,
                                        BlockPos source) {
        long x = (long) playerPosition.getX() + copied.playerOffset().getX() + source.getX() - copied.min().getX();
        long y = (long) playerPosition.getY() + copied.playerOffset().getY() + source.getY() - copied.min().getY();
        long z = (long) playerPosition.getZ() + copied.playerOffset().getZ() + source.getZ() - copied.min().getZ();
        if (x < Integer.MIN_VALUE || x > Integer.MAX_VALUE || y < Integer.MIN_VALUE || y > Integer.MAX_VALUE
            || z < Integer.MIN_VALUE || z > Integer.MAX_VALUE) {
            return null;
        }
        return new BlockPos((int) x, (int) y, (int) z);
    }

    private static int findMaterial(Container inventory, int materialSlots, Item item) {
        ItemStack plainBlockItem = new ItemStack(item);
        for (int slot = 0; slot < Math.min(materialSlots, inventory.getContainerSize()); slot++) {
            ItemStack candidate = inventory.getItem(slot);
            if (ItemStack.isSameItemSameComponents(plainBlockItem, candidate)) {
                return slot;
            }
        }
        return -1;
    }

    private static WorldAccess live(ServerLevel level, Player player) {
        return new WorldAccess() {
            @Override
            public boolean mayBuild() {
                return player.getAbilities().mayBuild;
            }

            @Override
            public boolean mayInteract(BlockPos pos) {
                return player.mayInteract(level, pos);
            }

            @Override
            public boolean isSafe(BlockPos pos) {
                return !level.isOutsideBuildHeight(pos) && level.getWorldBorder().isWithinBounds(pos)
                    && level.hasChunkAt(pos);
            }

            @Override
            public BlockState getBlockState(BlockPos pos) {
                return level.getBlockState(pos);
            }

            @Override
            public boolean setBlockState(BlockPos pos, BlockState state) {
                return level.setBlock(pos, state, 3);
            }
        };
    }

    interface WorldAccess {
        boolean mayBuild();

        boolean mayInteract(BlockPos pos);

        boolean isSafe(BlockPos pos);

        BlockState getBlockState(BlockPos pos);

        boolean setBlockState(BlockPos pos, BlockState state);
    }

    record PasteResult(boolean safe, int placed, boolean missingMaterial) {
    }

    private record Placement(BlockPos source, BlockPos target) {
    }

    @Override
    public void appendHoverText(
        ItemStack stack,
        TooltipContext context,
        TooltipDisplay tooltipDisplay,
        Consumer<Component> tooltip,
        TooltipFlag flag
    ) {
        tooltip.accept(PrismaticWandState.mode(stack).displayComponent().copy().withStyle(ChatFormatting.GRAY));
        tooltip.accept(Component.translatable("tooltip.aura.prismatic_wand.copy_limits").withStyle(ChatFormatting.DARK_GRAY));
        tooltip.accept(Component.translatable("tooltip.aura.prismatic_wand.material_limits").withStyle(ChatFormatting.DARK_GRAY));
    }
}
