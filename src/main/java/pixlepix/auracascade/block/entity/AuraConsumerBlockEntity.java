package pixlepix.auracascade.block.entity;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.Holder;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.animal.sheep.Sheep;
import net.minecraft.world.item.DyeColor;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.alchemy.Potion;
import net.minecraft.world.item.alchemy.PotionContents;
import net.minecraft.world.item.alchemy.Potions;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.item.crafting.SingleRecipeInput;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BonemealableBlock;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.minecraft.world.phys.AABB;
import pixlepix.auracascade.block.AuraContent;
import pixlepix.auracascade.data.recipe.AuraWorldRecipe;
import pixlepix.auracascade.data.recipe.AuraWorldRecipeCatalog;
import pixlepix.auracascade.enchantment.AuraEnchantments;
import pixlepix.auracascade.item.AuraItems;
import pixlepix.auracascade.parity.AuraColor;

public class AuraConsumerBlockEntity extends BlockEntity implements AuraSignalSource {
    private static final String STORED_POWER_TAG = "stored_power";
    private static final String PROGRESS_TAG = "progress";
    private static final List<Holder<Potion>> BREW_RESULTS = List.of(
        Potions.AWKWARD,
        Potions.SWIFTNESS,
        Potions.FIRE_RESISTANCE,
        Potions.WATER_BREATHING,
        Potions.NIGHT_VISION,
        Potions.HEALING,
        Potions.STRENGTH,
        Potions.SLOW_FALLING
    );

    private int storedPower;
    private int progress;

    public AuraConsumerBlockEntity(BlockPos pos, BlockState blockState) {
        super(AuraContent.AURA_CONSUMER_BLOCK_ENTITY, pos, blockState);
    }

    public static void serverTick(Level level, BlockPos pos, BlockState state, AuraConsumerBlockEntity blockEntity) {
        if (!level.isClientSide()) {
            blockEntity.serverTick(level, pos);
        }
    }

    private void serverTick(Level level, BlockPos pos) {
        int previousPower = storedPower;
        int previousProgress = progress;

        if (level.getGameTime() % 20L == 18L) {
            storedPower = AuraConsumerLogic.bleedStoredPower(storedPower);
        }

        storedPower += collectAdjacentPower(level, pos);

        AuraConsumerVariant variant = variant();
        if (hasWork(level, pos, variant) && storedPower >= variant.powerPerProgress()) {
            int steps = AuraConsumerLogic.progressStepsForTick(storedPower, variant.powerPerProgress());
            for (int step = 0; step < steps; step++) {
                int stepCost = AuraConsumerLogic.powerCostForStep(variant.powerPerProgress(), step);
                if (stepCost <= 0 || storedPower < stepCost) {
                    break;
                }

                storedPower -= stepCost;
                progress++;
                if (progress > variant.maxProgress()) {
                    if (performWork(level, pos, variant)) {
                        progress = 0;
                    } else {
                        progress = variant.maxProgress();
                        break;
                    }
                }
            }
        }

        if (storedPower != previousPower || progress != previousProgress) {
            setChanged();
            level.sendBlockUpdated(pos, getBlockState(), getBlockState(), 3);
        }
    }

    @Override
    public int auraSignal() {
        int minimumPower = Math.max(1, variant().powerPerProgress());
        return Math.min(15, (int) Math.ceil(storedPower / (double) minimumPower));
    }

    @Override
    protected void loadAdditional(ValueInput input) {
        super.loadAdditional(input);
        storedPower = input.getIntOr(STORED_POWER_TAG, 0);
        progress = input.getIntOr(PROGRESS_TAG, 0);
    }

    @Override
    protected void saveAdditional(ValueOutput output) {
        super.saveAdditional(output);
        output.putInt(STORED_POWER_TAG, storedPower);
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

    private int collectAdjacentPower(Level level, BlockPos pos) {
        int collected = 0;
        for (Direction direction : Direction.values()) {
            BlockEntity blockEntity = level.getBlockEntity(pos.relative(direction));
            if (blockEntity instanceof AuraNetworkBlockEntity auraNetworkBlockEntity) {
                collected += auraNetworkBlockEntity.extractStoredPower(Integer.MAX_VALUE);
            }
        }
        return collected;
    }

    private boolean hasWork(Level level, BlockPos pos, AuraConsumerVariant variant) {
        return switch (variant.family()) {
            case PROCESSOR -> findMatchingRecipe(nearbyItems(level, pos), variant.prismatic()).isPresent();
            case SMELTER -> findSmeltResult(level, nearbyItems(level, pos)).isPresent();
            case GROWER -> findGrowTarget(level, pos).isPresent();
            case FISHER -> hasWaterPool(level, pos);
            case BREWER -> findWaterBottle(nearbyItems(level, pos)).isPresent();
            case COLORER -> !nearbySheep(level, pos).isEmpty();
            case SYNTHESIZER -> true;
            case ENCHANTER -> findEnchantTarget(nearbyItems(level, pos)).isPresent() && findArcaneIngot(nearbyItems(level, pos)).isPresent();
        };
    }

    private boolean performWork(Level level, BlockPos pos, AuraConsumerVariant variant) {
        return switch (variant.family()) {
            case PROCESSOR -> processRecipe(level, pos, nearbyItems(level, pos), variant.prismatic(), AuraWorldRecipe.RecipeKind.PROCESSOR);
            case SMELTER -> smeltNearbyItem(level, pos, nearbyItems(level, pos));
            case GROWER -> boostGrowth(level, pos);
            case FISHER -> spawnFish(level, pos);
            case BREWER -> brewPotion(level, pos, nearbyItems(level, pos));
            case COLORER -> recolorSheep(level, pos);
            case SYNTHESIZER -> processRecipe(level, pos, nearbyItems(level, pos), false, AuraWorldRecipe.RecipeKind.SYNTHESIZER) || synthesizeAngelsteel(level, pos);
            case ENCHANTER -> enchantNearbyItem(level, pos, nearbyItems(level, pos));
        };
    }

    private boolean processRecipe(
        Level level,
        BlockPos pos,
        List<ItemEntity> items,
        boolean allowPrismaticRecipes,
        AuraWorldRecipe.RecipeKind kind
    ) {
        Optional<AuraWorldRecipe> recipe = findMatchingRecipe(items, allowPrismaticRecipes, kind);
        if (recipe.isEmpty()) {
            return false;
        }

        Map<ItemEntity, Integer> reserved = reserveIngredients(items, recipe.get());
        if (reserved.isEmpty()) {
            return false;
        }

        consumeReservedItems(reserved);
        spawnOutput(level, pos, recipe.get().result());
        return true;
    }

    private Optional<AuraWorldRecipe> findMatchingRecipe(List<ItemEntity> items, boolean allowPrismaticRecipes) {
        return findMatchingRecipe(items, allowPrismaticRecipes, AuraWorldRecipe.RecipeKind.PROCESSOR);
    }

    private Optional<AuraWorldRecipe> findSynthRecipe(List<ItemEntity> items) {
        return findMatchingRecipe(items, false, AuraWorldRecipe.RecipeKind.SYNTHESIZER);
    }

    private Optional<AuraWorldRecipe> findMatchingRecipe(
        List<ItemEntity> items,
        boolean allowPrismaticRecipes,
        AuraWorldRecipe.RecipeKind kind
    ) {
        return AuraWorldRecipeCatalog.all().stream()
            .filter(recipe -> recipe.kind() == kind)
            .filter(recipe -> allowPrismaticRecipes || !recipe.prismaticOnly())
            .filter(recipe -> !reserveIngredients(items, recipe).isEmpty())
            .findFirst();
    }

    private Map<ItemEntity, Integer> reserveIngredients(List<ItemEntity> items, AuraWorldRecipe recipe) {
        ArrayList<ItemEntity> sortedItems = new ArrayList<>(items);
        sortedItems.sort(Comparator.comparing(entity -> BuiltInItemSort.key(entity.getItem())));
        LinkedHashMap<ItemEntity, Integer> reserved = new LinkedHashMap<>();

        for (AuraWorldRecipe.Ingredient ingredient : recipe.ingredients()) {
            int remaining = ingredient.count();
            for (ItemEntity itemEntity : sortedItems) {
                ItemStack stack = itemEntity.getItem();
                if (!stack.is(ingredient.item())) {
                    continue;
                }

                int alreadyReserved = reserved.getOrDefault(itemEntity, 0);
                int available = stack.getCount() - alreadyReserved;
                if (available <= 0) {
                    continue;
                }

                int taken = Math.min(remaining, available);
                reserved.put(itemEntity, alreadyReserved + taken);
                remaining -= taken;
                if (remaining == 0) {
                    break;
                }
            }

            if (remaining > 0) {
                return Map.of();
            }
        }

        return reserved;
    }

    private void consumeReservedItems(Map<ItemEntity, Integer> reserved) {
        for (Map.Entry<ItemEntity, Integer> entry : reserved.entrySet()) {
            ItemEntity itemEntity = entry.getKey();
            ItemStack stack = itemEntity.getItem();
            stack.shrink(entry.getValue());
            if (stack.isEmpty()) {
                itemEntity.discard();
            }
        }
    }

    private Optional<ItemEntity> findWaterBottle(List<ItemEntity> items) {
        return items.stream()
            .filter(itemEntity -> isWaterBottle(itemEntity.getItem()))
            .findFirst();
    }

    private Optional<ItemEntity> findEnchantTarget(List<ItemEntity> items) {
        return items.stream()
            .filter(itemEntity -> KaleidoscopicEnchanterLogic.isValidTarget(itemEntity.getItem()))
            .findFirst();
    }

    private Optional<ItemEntity> findArcaneIngot(List<ItemEntity> items) {
        return items.stream()
            .filter(itemEntity -> AuraItems.arcaneIngotColor(itemEntity.getItem()).isPresent())
            .findFirst();
    }

    private boolean synthesizeAngelsteel(Level level, BlockPos pos) {
        spawnOutput(level, pos, new ItemStack(AuraItems.angelsteelIngot(0)));
        return true;
    }

    private boolean enchantNearbyItem(Level level, BlockPos pos, List<ItemEntity> items) {
        Optional<ItemEntity> maybeTarget = findEnchantTarget(items);
        Optional<ItemEntity> maybeIngot = findArcaneIngot(items);
        if (maybeTarget.isEmpty() || maybeIngot.isEmpty()) {
            return false;
        }

        ItemStack targetStack = maybeTarget.get().getItem();
        ItemStack ingotStack = maybeIngot.get().getItem();
        AuraColor color = AuraItems.arcaneIngotColor(ingotStack).orElse(null);
        if (color == null || !KaleidoscopicEnchanterLogic.canApply(targetStack, color, level.registryAccess())) {
            return false;
        }

        Map<AuraColor, Integer> levels = KaleidoscopicEnchanterLogic.levels(targetStack, level.registryAccess());
        double successRate = KaleidoscopicEnchanterLogic.successRate(
            KaleidoscopicEnchanterLogic.totalLevel(levels),
            KaleidoscopicEnchanterLogic.maxLevel(levels)
        );

        ingotStack.shrink(1);
        if (ingotStack.isEmpty()) {
            maybeIngot.get().discard();
        }

        if (level.getRandom().nextDouble() < successRate) {
            var lookup = level.registryAccess().lookupOrThrow(Registries.ENCHANTMENT);
            ResourceKey<Enchantment> key = AuraEnchantments.kaleidoscopic(color).orElse(null);
            if (key != null) {
                var holder = lookup.getOrThrow(key);
                int nextLevel = targetStack.getEnchantments().getLevel(holder) + 1;
                targetStack.enchant(holder, nextLevel);
            }
        }

        return true;
    }

    private boolean brewPotion(Level level, BlockPos pos, List<ItemEntity> items) {
        Optional<ItemEntity> waterBottle = findWaterBottle(items);
        if (waterBottle.isEmpty()) {
            return false;
        }

        ItemStack stack = waterBottle.get().getItem();
        stack.shrink(1);
        if (stack.isEmpty()) {
            waterBottle.get().discard();
        }

        Holder<Potion> resultPotion = BREW_RESULTS.get(level.getRandom().nextInt(BREW_RESULTS.size()));
        spawnOutput(level, pos, PotionContents.createItemStack(Items.POTION, resultPotion));
        return true;
    }

    private boolean smeltNearbyItem(Level level, BlockPos pos, List<ItemEntity> items) {
        Optional<SmeltCandidate> candidate = findSmeltResult(level, items);
        if (candidate.isEmpty()) {
            return false;
        }

        ItemStack input = candidate.get().itemEntity().getItem();
        input.shrink(1);
        if (input.isEmpty()) {
            candidate.get().itemEntity().discard();
        }

        spawnOutput(level, pos, candidate.get().result());
        return true;
    }

    private Optional<SmeltCandidate> findSmeltResult(Level level, List<ItemEntity> items) {
        if (level.getServer() == null) {
            return Optional.empty();
        }

        for (ItemEntity itemEntity : items) {
            ItemStack stack = itemEntity.getItem();
            if (stack.isEmpty()) {
                continue;
            }

            SingleRecipeInput input = new SingleRecipeInput(stack.copyWithCount(1));
            Optional<RecipeHolder<net.minecraft.world.item.crafting.SmeltingRecipe>> recipe = level.getServer().getRecipeManager()
                .getRecipeFor(RecipeType.SMELTING, input, level);
            if (recipe.isPresent()) {
                ItemStack result = recipe.get().value().assemble(input, level.registryAccess());
                if (!result.isEmpty()) {
                    return Optional.of(new SmeltCandidate(itemEntity, result));
                }
            }
        }
        return Optional.empty();
    }

    private boolean boostGrowth(Level level, BlockPos pos) {
        if (!(level instanceof ServerLevel serverLevel)) {
            return false;
        }

        Optional<BlockPos> growTarget = findGrowTarget(level, pos);
        if (growTarget.isEmpty()) {
            return false;
        }

        BlockPos targetPos = growTarget.get();
        BlockState state = level.getBlockState(targetPos);
        if (!(state.getBlock() instanceof BonemealableBlock bonemealableBlock)) {
            return false;
        }
        if (!bonemealableBlock.isValidBonemealTarget(level, targetPos, state)) {
            return false;
        }

        RandomSource random = level.getRandom();
        if (!bonemealableBlock.isBonemealSuccess(level, random, targetPos, state)) {
            return false;
        }

        bonemealableBlock.performBonemeal(serverLevel, random, targetPos, state);
        return true;
    }

    private Optional<BlockPos> findGrowTarget(Level level, BlockPos pos) {
        BlockState middle = level.getBlockState(pos.above());
        if (!(middle.is(Blocks.DIRT) || middle.is(Blocks.GRASS_BLOCK) || middle.is(Blocks.FARMLAND))) {
            return Optional.empty();
        }

        BlockPos targetPos = pos.above(2);
        BlockState targetState = level.getBlockState(targetPos);
        if (targetState.getBlock() instanceof BonemealableBlock bonemealableBlock
            && bonemealableBlock.isValidBonemealTarget(level, targetPos, targetState)) {
            return Optional.of(targetPos);
        }
        return Optional.empty();
    }

    private boolean spawnFish(Level level, BlockPos pos) {
        if (!hasWaterPool(level, pos)) {
            return false;
        }

        ItemStack caughtFish = switch (level.getRandom().nextInt(8)) {
            case 0, 1, 2, 3 -> new ItemStack(Items.COD);
            case 4, 5 -> new ItemStack(Items.SALMON);
            case 6 -> new ItemStack(Items.PUFFERFISH);
            default -> new ItemStack(Items.TROPICAL_FISH);
        };
        spawnOutput(level, pos, caughtFish);
        return true;
    }

    private boolean hasWaterPool(Level level, BlockPos pos) {
        BlockPos min = pos.offset(-1, -1, -1);
        BlockPos max = pos.offset(2, -1, 2);
        for (BlockPos cursor : BlockPos.betweenClosed(min, max)) {
            BlockState state = level.getBlockState(cursor);
            if (!(state.is(Blocks.WATER))) {
                return false;
            }
        }
        return true;
    }

    private boolean recolorSheep(Level level, BlockPos pos) {
        List<Sheep> sheep = nearbySheep(level, pos);
        if (sheep.isEmpty()) {
            return false;
        }

        Sheep target = sheep.get(level.getRandom().nextInt(sheep.size()));
        target.setSheared(false);
        target.setColor(DyeColor.byId(level.getRandom().nextInt(DyeColor.values().length)));
        return true;
    }

    private List<ItemEntity> nearbyItems(Level level, BlockPos pos) {
        return level.getEntitiesOfClass(ItemEntity.class, new AABB(pos).inflate(3.0D), entity -> !entity.isRemoved());
    }

    private List<Sheep> nearbySheep(Level level, BlockPos pos) {
        return level.getEntitiesOfClass(Sheep.class, new AABB(pos).inflate(2.0D), entity -> !entity.isRemoved());
    }

    private void spawnOutput(Level level, BlockPos pos, ItemStack stack) {
        ItemEntity itemEntity = new ItemEntity(
            level,
            pos.getX() + 0.5D,
            pos.getY() + 1.5D,
            pos.getZ() + 0.5D,
            stack.copy()
        );
        itemEntity.setDeltaMovement(0.0D, 0.0D, 0.0D);
        level.addFreshEntity(itemEntity);
    }

    private boolean isWaterBottle(ItemStack stack) {
        if (!stack.is(Items.POTION)) {
            return false;
        }
        PotionContents potionContents = stack.getOrDefault(DataComponents.POTION_CONTENTS, PotionContents.EMPTY);
        return potionContents.is(Potions.WATER);
    }

    private AuraConsumerVariant variant() {
        AuraConsumerVariant variant = AuraContent.consumerVariant(getBlockState().getBlock());
        return variant != null ? variant : AuraConsumerVariant.PROCESSOR;
    }

    private record SmeltCandidate(ItemEntity itemEntity, ItemStack result) {
    }

    private static final class BuiltInItemSort {
        private BuiltInItemSort() {
        }

        private static String key(ItemStack stack) {
            return stack.getItem().toString();
        }
    }
}
