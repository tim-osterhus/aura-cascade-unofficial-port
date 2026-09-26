package pixlepix.auracascade.block.entity;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.core.Holder;
import net.minecraft.core.HolderSet;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.TagKey;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.animal.Sheep;
import net.minecraft.world.entity.projectile.FishingHook;
import net.minecraft.world.item.DyeColor;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.item.crafting.SingleRecipeInput;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.loot.BuiltInLootTables;
import net.minecraft.world.level.storage.loot.LootParams;
import net.minecraft.world.level.storage.loot.parameters.LootContextParamSets;
import net.minecraft.world.level.storage.loot.parameters.LootContextParams;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.joml.Vector3f;
import pixlepix.auracascade.aura.AuraConsumerInspectionState;
import pixlepix.auracascade.aura.WorldInteractionVisuals;
import pixlepix.auracascade.block.AuraContent;
import pixlepix.auracascade.data.recipe.AuraWorldRecipe;
import pixlepix.auracascade.data.recipe.AuraWorldRecipeCatalog;
import pixlepix.auracascade.enchantment.AuraEnchantments;
import pixlepix.auracascade.item.AuraItems;
import pixlepix.auracascade.parity.AuraColor;
import pixlepix.auracascade.util.NbtCompat;

public class AuraConsumerBlockEntity extends BlockEntity implements AuraSignalSource {
    private static final String STORED_POWER_TAG = "stored_power";
    private static final String PROGRESS_TAG = "progress";
    private static final String LAST_RECEIVED_POWER_TAG = "last_received_power";
    private int storedPower;
    private int progress;
    private int lastReceivedPower;
    private Vec3 craftAnimationCenter;
    private int craftAnimationTick;

    public AuraConsumerBlockEntity(BlockPos pos, BlockState blockState) {
        super(AuraContent.AURA_CONSUMER_BLOCK_ENTITY, pos, blockState);
    }

    public static void serverTick(Level level, BlockPos pos, BlockState state, AuraConsumerBlockEntity blockEntity) {
        if (!level.isClientSide()) {
            blockEntity.serverTick(level, pos);
        }
    }

    private void serverTick(Level level, BlockPos pos) {
        pixlepix.auracascade.item.ConsumerItemKeepAlive.tick(level, pos);
        int previousPower = storedPower;
        int previousProgress = progress;
        int previousLastReceivedPower = lastReceivedPower;
        long gameTime = level.getGameTime();

        if (gameTime % 20L == 18L) {
            storedPower = AuraConsumerLogic.bleedStoredPower(storedPower);
        }

        int receivedPower = collectAdjacentPower(level, pos);
        storedPower += receivedPower;
        if (gameTime % 20L == 0L) {
            lastReceivedPower = 0;
        }
        if (receivedPower > 0) {
            // The legacy "Last Power" readout snapshots the stored total after a transfer.
            lastReceivedPower = storedPower;
        }

        AuraConsumerVariant variant = variant();
        if (AuraConsumerLogic.shouldAdvanceProgress(gameTime)) {
            AuraConsumerLogic.ProgressState next = AuraConsumerLogic.advanceProgress(
                progress,
                storedPower,
                variant,
                () -> performWork(level, pos, variant)
            );
            progress = next.progress();
            storedPower = next.storedPower();
        }
        tickCraftAnimation(level, variant);

        if (storedPower != previousPower || progress != previousProgress || lastReceivedPower != previousLastReceivedPower) {
            setChanged();
            level.sendBlockUpdated(pos, getBlockState(), getBlockState(), 3);
        }
    }

    public AuraConsumerInspectionState inspectionState() {
        AuraConsumerVariant variant = variant();
        return new AuraConsumerInspectionState(
            progress,
            variant.maxProgress(),
            variant.powerPerProgress(),
            lastReceivedPower,
            storedPower
        );
    }

    public boolean hasValidWork() {
        Level currentLevel = level;
        return currentLevel != null
            && !currentLevel.isClientSide()
            && hasWork(currentLevel, worldPosition, variant());
    }

    @Override
    public int auraSignal() {
        int minimumPower = Math.max(1, variant().powerPerProgress());
        return Math.min(15, (int) Math.ceil(storedPower / (double) minimumPower));
    }

    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        storedPower = NbtCompat.getIntOr(tag, STORED_POWER_TAG, NbtCompat.getIntOr(tag, "storedPower", 0));
        progress = NbtCompat.getIntOr(tag, PROGRESS_TAG, 0);
        lastReceivedPower = NbtCompat.getIntOr(tag, LAST_RECEIVED_POWER_TAG, NbtCompat.getIntOr(tag, "lastPower", 0));
    }

    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        tag.putInt(STORED_POWER_TAG, storedPower);
        tag.putInt(PROGRESS_TAG, progress);
        tag.putInt(LAST_RECEIVED_POWER_TAG, lastReceivedPower);
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
            BlockPos neighbor = pos.relative(direction);
            if (!level.hasChunkAt(neighbor)) {
                continue;
            }
            BlockEntity blockEntity = level.getBlockEntity(neighbor);
            if (blockEntity instanceof AuraNetworkBlockEntity auraNetworkBlockEntity) {
                collected += auraNetworkBlockEntity.extractStoredPower(Integer.MAX_VALUE);
            }
        }
        return collected;
    }

    private boolean hasWork(Level level, BlockPos pos, AuraConsumerVariant variant) {
        return switch (variant.family()) {
            case PROCESSOR -> {
                List<ItemEntity> items = nearbyItems(level, pos);
                yield findMatchingRecipe(items, variant.prismatic()).isPresent()
                    || findCommonDustConversion(items, level).isPresent();
            }
            case SMELTER -> findSmeltResult(level, nearbyItems(level, pos)).isPresent();
            case GROWER -> true;
            case FISHER -> hasWaterPool(level, pos);
            case BREWER -> nearbyItems(level, pos).stream().anyMatch(item -> AuraConsumerBrewLogic.canBrew(item.getItem()));
            case COLORER -> !nearbySheep(level, pos).isEmpty();
            // The legacy AngelSteelTile produces its first-tier ingot from power alone.
            case SYNTHESIZER -> true;
            case ENCHANTER -> findEnchantInput(level, nearbyItems(level, pos)).isPresent();
        };
    }

    private boolean performWork(Level level, BlockPos pos, AuraConsumerVariant variant) {
        return switch (variant.family()) {
            case PROCESSOR -> processRecipe(level, pos, nearbyItems(level, pos), variant.prismatic(), AuraWorldRecipe.RecipeKind.PROCESSOR);
            case SMELTER -> smeltNearbyItem(level, nearbyItems(level, pos));
            case GROWER -> boostGrowth(level, pos);
            case FISHER -> spawnFish(level, pos);
            case BREWER -> brewPotions(level, nearbyItems(level, pos));
            case COLORER -> recolorSheep(level, pos);
            case SYNTHESIZER -> synthesizeAngelsteel(level, pos);
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
        Optional<DustConversion> dustConversion = kind == AuraWorldRecipe.RecipeKind.PROCESSOR
            ? findCommonDustConversion(items, level)
            : Optional.empty();
        Optional<AuraWorldRecipe> recipe = findMatchingRecipe(items, allowPrismaticRecipes, kind);
        AuraConsumerLogic.ProcessorRoute route = AuraConsumerLogic.processorRoute(
            dustConversion.isPresent(),
            recipe.isPresent()
        );
        if (kind == AuraWorldRecipe.RecipeKind.PROCESSOR
            && route == AuraConsumerLogic.ProcessorRoute.DUST) {
            return processCommonDustConversion(level, dustConversion.orElseThrow(), allowPrismaticRecipes);
        }
        if (recipe.isEmpty()) {
            return false;
        }

        Map<ItemEntity, Integer> reserved = reserveIngredients(items, recipe.get());
        if (reserved.isEmpty()) {
            return false;
        }

        ItemEntity anchor = lastReservedItem(items, reserved);
        if (anchor == null) {
            return false;
        }
        ItemEntity output = replacementDrop(level, anchor, recipe.get().result());
        consumeReservedItems(reserved);
        level.addFreshEntity(output);
        beginCraftAnimation(output.position());
        return true;
    }

    private boolean processCommonDustConversion(Level level, DustConversion conversion, boolean prismatic) {
        ItemEntity output = replacementDrop(
            level,
            conversion.oreEntity(),
            new ItemStack(conversion.dustItem(), prismatic ? 3 : 2)
        );
        consumeReservedItems(Map.of(conversion.oreEntity(), 1));
        level.addFreshEntity(output);
        beginCraftAnimation(output.position());
        return true;
    }

    private ItemEntity lastReservedItem(List<ItemEntity> items, Map<ItemEntity, Integer> reserved) {
        ItemEntity last = null;
        for (ItemEntity item : items) {
            if (reserved.containsKey(item)) {
                last = item;
            }
        }
        return last;
    }

    private Optional<DustConversion> findCommonDustConversion(List<ItemEntity> items, Level level) {
        HolderLookup<Item> itemLookup = level.registryAccess().lookupOrThrow(Registries.ITEM);
        List<TagKey<Item>> oreTags = itemLookup.listTagIds()
            .filter(tag -> AuraConsumerLogic.commonDustTagForOre(tag).isPresent())
            .sorted(Comparator.comparing(tag -> tag.location().toString()))
            .toList();

        for (ItemEntity oreEntity : items) {
            ItemStack oreStack = oreEntity.getItem();
            for (TagKey<Item> oreTag : oreTags) {
                if (!oreStack.is(oreTag)) {
                    continue;
                }

                Optional<TagKey<Item>> maybeDustTag = AuraConsumerLogic.commonDustTagForOre(oreTag);
                if (maybeDustTag.isEmpty()) {
                    continue;
                }
                Optional<HolderSet.Named<Item>> maybeDusts = itemLookup.get(maybeDustTag.get());
                if (maybeDusts.isEmpty()) {
                    continue;
                }

                Optional<Item> maybeDust = maybeDusts.get().stream()
                    .map(Holder::value)
                    .filter(item -> item != Items.AIR)
                    .findFirst();
                if (maybeDust.isPresent()) {
                    return Optional.of(new DustConversion(oreEntity, maybeDust.get()));
                }
            }
        }
        return Optional.empty();
    }

    private Optional<AuraWorldRecipe> findMatchingRecipe(List<ItemEntity> items, boolean allowPrismaticRecipes) {
        return findMatchingRecipe(items, allowPrismaticRecipes, AuraWorldRecipe.RecipeKind.PROCESSOR);
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

    private Optional<EnchantInput> findEnchantInput(Level level, List<ItemEntity> items) {
        for (ItemEntity target : items) {
            ItemStack targetStack = target.getItem();
            if (!KaleidoscopicEnchanterLogic.isValidTarget(targetStack)) {
                continue;
            }

            for (ItemEntity ingot : items) {
                Optional<AuraColor> maybeColor = AuraItems.arcaneIngotColor(ingot.getItem());
                if (maybeColor.isEmpty() || AuraEnchantments.kaleidoscopic(maybeColor.get()).isEmpty()) {
                    continue;
                }

                AuraColor color = maybeColor.get();
                if (KaleidoscopicEnchanterLogic.canApply(targetStack, color, level.registryAccess())) {
                    return Optional.of(new EnchantInput(target, ingot, color));
                }
            }
        }
        return Optional.empty();
    }

    private boolean synthesizeAngelsteel(Level level, BlockPos pos) {
        spawnOutput(level, pos, new ItemStack(AuraItems.angelsteelIngot(0)));
        beginCraftAnimation(Vec3.atCenterOf(pos).add(0.0D, 1.0D, 0.0D));
        return true;
    }

    private boolean enchantNearbyItem(Level level, BlockPos pos, List<ItemEntity> items) {
        Optional<EnchantInput> maybeInput = findEnchantInput(level, items);
        if (maybeInput.isEmpty()) {
            return false;
        }

        EnchantInput input = maybeInput.get();
        ItemStack targetStack = input.target().getItem();
        ItemStack ingotStack = input.ingot().getItem();
        AuraColor color = input.color();

        Map<AuraColor, Integer> levels = KaleidoscopicEnchanterLogic.levels(targetStack, level.registryAccess());
        double successRate = KaleidoscopicEnchanterLogic.successRate(
            KaleidoscopicEnchanterLogic.totalLevel(levels),
            KaleidoscopicEnchanterLogic.maxLevel(levels)
        );

        // The original consumes one Arcane Ingot even when the enchantment roll fails.
        ingotStack.shrink(1);
        if (ingotStack.isEmpty()) {
            input.ingot().discard();
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

        beginCraftAnimation(input.target().position());

        return true;
    }

    private boolean brewPotions(Level level, List<ItemEntity> items) {
        boolean brewedAny = false;
        for (ItemEntity potionEntity : items) {
            Optional<ItemStack> maybeResult = AuraConsumerBrewLogic.result(potionEntity.getItem(), level.getRandom());
            if (maybeResult.isEmpty()) {
                continue;
            }

            double x = potionEntity.getX();
            double y = potionEntity.getY();
            double z = potionEntity.getZ();
            Vec3 velocity = potionEntity.getDeltaMovement();
            CompoundTag entityData = new CompoundTag();
            potionEntity.addAdditionalSaveData(entityData);
            int pickupDelay = entityData.getShort("PickupDelay");
            ItemStack input = potionEntity.getItem();
            input.shrink(1);
            if (input.isEmpty()) {
                potionEntity.discard();
            }

            ItemEntity resultEntity = new ItemEntity(
                level,
                x,
                y,
                z,
                maybeResult.get()
            );
            resultEntity.setDeltaMovement(velocity);
            resultEntity.setPickUpDelay(pickupDelay);
            level.addFreshEntity(resultEntity);
            beginCraftAnimation(resultEntity.position());
            brewedAny = true;
        }
        return brewedAny;
    }

    private boolean smeltNearbyItem(Level level, List<ItemEntity> items) {
        Optional<SmeltCandidate> candidate = findSmeltResult(level, items);
        if (candidate.isEmpty()) {
            return false;
        }

        ItemEntity source = candidate.get().itemEntity();
        ItemEntity output = replacementDrop(level, source, candidate.get().result());
        ItemStack input = source.getItem();
        input.shrink(1);
        if (input.isEmpty()) {
            source.discard();
        }

        level.addFreshEntity(output);
        beginCraftAnimation(output.position());
        return true;
    }

    private void beginCraftAnimation(Vec3 center) {
        craftAnimationCenter = center;
        craftAnimationTick = 0;
    }

    private void tickCraftAnimation(Level level, AuraConsumerVariant variant) {
        if (craftAnimationCenter == null || !(level instanceof ServerLevel serverLevel)) {
            return;
        }
        Vector3f color = switch (variant.family()) {
            case SYNTHESIZER -> new Vector3f(1.0F, 0.78F, 0.3F);
            case ENCHANTER -> new Vector3f(0.75F, 0.45F, 1.0F);
            default -> new Vector3f(0.4F, 0.9F, 1.0F);
        };
        DustParticleOptions particle = new DustParticleOptions(color, 1.0F);
        for (Vec3 point : WorldInteractionVisuals.craftSamples(craftAnimationCenter, craftAnimationTick)) {
            serverLevel.sendParticles(particle, point.x, point.y, point.z, 1, 0.0D, 0.0D, 0.0D, 0.0D);
        }
        if (++craftAnimationTick >= WorldInteractionVisuals.CRAFT_TICKS) {
            craftAnimationCenter = null;
        }
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

        BlockPos targetPos = pos.above(2);
        RandomSource random = serverLevel.getRandom();
        BlockState initialState = serverLevel.getBlockState(targetPos);
        Block initialBlock = initialState.getBlock();
        for (int tick = 0; tick < AuraConsumerLogic.GROWER_RANDOM_TICKS; tick++) {
            BlockState currentState = serverLevel.getBlockState(targetPos);
            if (!AuraConsumerLogic.shouldContinueGrowerBatch(initialBlock, currentState)) {
                break;
            }
            currentState.randomTick(serverLevel, targetPos, random);
        }
        return true;
    }

    private boolean spawnFish(Level level, BlockPos pos) {
        if (!hasWaterPool(level, pos)) {
            return false;
        }

        if (!(level instanceof ServerLevel serverLevel)) {
            return false;
        }

        Vec3 origin = Vec3.atCenterOf(pos.above());
        FishingHook hook = new FishingHook(EntityType.FISHING_BOBBER, serverLevel);
        hook.setPos(origin);
        LootParams lootParams = new LootParams.Builder(serverLevel)
            .withParameter(LootContextParams.ORIGIN, origin)
            .withParameter(LootContextParams.TOOL, new ItemStack(Items.FISHING_ROD))
            .withParameter(LootContextParams.THIS_ENTITY, hook)
            .withLuck(0.0F)
            .create(LootContextParamSets.FISHING);
        List<ItemStack> catches = serverLevel.getServer()
            .reloadableRegistries()
            .getLootTable(BuiltInLootTables.FISHING)
            .getRandomItems(lootParams);
        for (ItemStack caught : catches) {
            spawnOutput(level, pos, caught);
        }
        return !catches.isEmpty();
    }

    private boolean hasWaterPool(Level level, BlockPos pos) {
        BlockPos min = pos.offset(-1, -1, -1);
        BlockPos max = pos.offset(2, -1, 2);
        for (BlockPos cursor : BlockPos.betweenClosed(min, max)) {
            BlockState state = level.getBlockState(cursor);
            if (!AuraConsumerLogic.isFisherWater(state)) {
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

    static ItemEntity replacementDrop(Level level, ItemEntity source, ItemStack stack) {
        CompoundTag sourceData = new CompoundTag();
        source.addAdditionalSaveData(sourceData);
        Vec3 motion = source.getDeltaMovement();
        ItemEntity output = new ItemEntity(level, source.getX(), source.getY(), source.getZ(),
            stack.copy(), motion.x, motion.y, motion.z);
        output.setPickUpDelay(sourceData.getShort("PickupDelay"));
        return output;
    }

    private AuraConsumerVariant variant() {
        AuraConsumerVariant variant = AuraContent.consumerVariant(getBlockState().getBlock());
        return variant != null ? variant : AuraConsumerVariant.PROCESSOR;
    }

    private record SmeltCandidate(ItemEntity itemEntity, ItemStack result) {
    }

    private record EnchantInput(ItemEntity target, ItemEntity ingot, AuraColor color) {
    }

    private record DustConversion(ItemEntity oreEntity, Item dustItem) {
    }

    private static final class BuiltInItemSort {
        private BuiltInItemSort() {
        }

        private static String key(ItemStack stack) {
            return stack.getItem().toString();
        }
    }
}
