package pixlepix.auracascade.item;

import java.util.EnumMap;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.OptionalInt;
import java.util.UUID;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.fabricmc.fabric.api.entity.event.v1.ServerLivingEntityEvents;
import net.fabricmc.fabric.api.event.player.AttackEntityCallback;
import net.fabricmc.fabric.api.event.player.UseBlockCallback;
import net.fabricmc.fabric.api.event.player.UseItemCallback;
import net.minecraft.core.Registry;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.damagesource.DamageTypes;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.npc.villager.AbstractVillager;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.hurtingprojectile.AbstractHurtingProjectile;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.trading.MerchantOffer;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.ItemLike;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import pixlepix.auracascade.AuraCascadeMod;
import pixlepix.auracascade.compat.AuraAccessoryBridgeRegistry;
import pixlepix.auracascade.compat.AuraAccessorySlot;
import pixlepix.auracascade.fairy.FairyRole;
import pixlepix.auracascade.fairy.FairySystem;
import pixlepix.auracascade.item.books.StorageBookItem;
import pixlepix.auracascade.item.books.StorageBookVariant;
import pixlepix.auracascade.parity.AuraColor;

public final class AuraItems {
    private static final int AURA_CRYSTAL_CHARGE = 300;
    private static boolean angelsteelTickHookRegistered;
    private static boolean utilityHooksRegistered;
    private static final int PRISMATIC_WAND_BLOCK_LIMIT = 512;
    private static final Map<UUID, Integer> ANGEL_SASH_COLLISION_CHARGE = new HashMap<>();

    private static final EnumMap<AuraColor, Item> AURA_CRYSTALS = new EnumMap<>(AuraColor.class);
    private static final EnumMap<AuraColor, Item> ARCANE_INGOTS = new EnumMap<>(AuraColor.class);
    private static final EnumMap<AuraColor, Item> ARCANE_GEMS = new EnumMap<>(AuraColor.class);
    private static final EnumMap<StorageBookVariant, Item> STORAGE_BOOKS = new EnumMap<>(StorageBookVariant.class);
    private static final Item[] ANGELSTEEL_INGOTS = new Item[AngelsteelToolHelper.MAX_DEGREE];
    private static final EnumMap<AngelsteelToolKind, Item[]> ANGELSTEEL_TOOLS = new EnumMap<>(AngelsteelToolKind.class);

    public static final Item ARCANE_PRISM = register("arcane_prism");
    public static final Item ENCYCLOPEDIA_AURA = register("encyclopedia_aura", new EncyclopediaAuraItem(itemProperties("encyclopedia_aura")));
    public static final Item FAIRY_CHARM = register("fairy_charm", new FairyCharmItem(itemProperties("fairy_charm")));
    public static final Item RING_OF_BINDING = register("ring_of_binding", new RingOfBindingItem(itemProperties("ring_of_binding")));
    public static final Item RING_OF_SHATTERED_STONE = register("ring_of_shattered_stone", new RingOfShatteredStoneItem(itemProperties("ring_of_shattered_stone")));
    public static final Item AMULET_OF_THE_ANGELS_WING = register("amulet_of_the_angels_wing", new AuraAccessoryItem(AuraAccessorySlot.AMULET, itemProperties("amulet_of_the_angels_wing")));
    public static final Item SASH_OF_THE_ANGELS_HEELS = register("sash_of_the_angels_heels", new AuraAccessoryItem(AuraAccessorySlot.BELT, itemProperties("sash_of_the_angels_heels")));
    public static final Item AMULET_OF_THE_FORBIDDEN_FRUIT = register("amulet_of_the_forbidden_fruit", new AuraAccessoryItem(AuraAccessorySlot.AMULET, itemProperties("amulet_of_the_forbidden_fruit")));
    public static final Item RED_PROTECTION_AMULET = register("red_protection_amulet", new AuraAccessoryItem(AuraAccessorySlot.AMULET, itemProperties("red_protection_amulet")));
    public static final Item ORANGE_PROTECTION_AMULET = register("orange_protection_amulet", new AuraAccessoryItem(AuraAccessorySlot.AMULET, itemProperties("orange_protection_amulet")));
    public static final Item YELLOW_PROTECTION_AMULET = register("yellow_protection_amulet", new AuraAccessoryItem(AuraAccessorySlot.AMULET, itemProperties("yellow_protection_amulet")));
    public static final Item GREEN_PROTECTION_AMULET = register("green_protection_amulet", new AuraAccessoryItem(AuraAccessorySlot.AMULET, itemProperties("green_protection_amulet")));
    public static final Item BLUE_PROTECTION_AMULET = register("blue_protection_amulet", new AuraAccessoryItem(AuraAccessorySlot.AMULET, itemProperties("blue_protection_amulet")));
    public static final Item VIOLET_PROTECTION_AMULET = register("violet_protection_amulet", new AuraAccessoryItem(AuraAccessorySlot.AMULET, itemProperties("violet_protection_amulet")));
    public static final Item MIRROR_OF_THE_ANGEL = register("mirror_of_the_angel", new Item(itemProperties("mirror_of_the_angel").stacksTo(1)));
    public static final Item PORTABLE_RED_HOLE = register("portable_red_hole", new Item(itemProperties("portable_red_hole").stacksTo(1)));
    public static final Item PORTABLE_BLACK_HOLE = register("portable_black_hole", new Item(itemProperties("portable_black_hole").stacksTo(1)));
    public static final Item PRISMATIC_WAND = register("prismatic_wand", new PrismaticWandItem(itemProperties("prismatic_wand")));
    public static final Item TRANSMUTING_SWORD = register(
        "transmuting_sword",
        new TransmutingSwordItem(new Item.Properties().stacksTo(1).sword(AuraUtilityToolMaterials.ARCANE_SWORD, 3.0F, -2.4F).setId(itemKey("transmuting_sword")))
    );
    public static final Item SWORD_OF_THE_THIEF = register(
        "sword_of_the_thief",
        new Item(new Item.Properties().stacksTo(1).sword(AuraUtilityToolMaterials.ARCANE_SWORD, 2.0F, -2.4F).setId(itemKey("sword_of_the_thief")))
    );
    public static final Item SWORD_OF_THE_BARBARIAN = register(
        "sword_of_the_barbarian",
        new SwordOfBarbarianItem(new Item.Properties().stacksTo(1).sword(AuraUtilityToolMaterials.ARCANE_SWORD, 3.0F, -2.4F).setId(itemKey("sword_of_the_barbarian")))
    );

    static {
        for (AuraColor color : AuraColor.values()) {
            AURA_CRYSTALS.put(color, register("aura_crystal_" + color.id()));
            ARCANE_INGOTS.put(color, register("arcane_ingot_" + color.id()));
            ARCANE_GEMS.put(color, register("arcane_gem_" + color.id()));
        }

        for (int degreeIndex = 0; degreeIndex < AngelsteelToolHelper.MAX_DEGREE; degreeIndex++) {
            String path = "angelsteel_ingot_" + (degreeIndex + 1);
            ANGELSTEEL_INGOTS[degreeIndex] = register(path, new AngelsteelIngotItem(degreeIndex, itemProperties(path)));
        }

        for (AngelsteelToolKind kind : AngelsteelToolKind.values()) {
            Item[] degreeItems = new Item[AngelsteelToolHelper.MAX_DEGREE];
            for (int degreeIndex = 0; degreeIndex < AngelsteelToolHelper.MAX_DEGREE; degreeIndex++) {
                String path = kind.registryPath(degreeIndex);
                degreeItems[degreeIndex] = register(
                    path,
                    kind == AngelsteelToolKind.SWORD
                        ? new AngelsteelSwordItem(
                            degreeIndex,
                            kind.createProperties(AngelsteelToolHelper.material(degreeIndex)).setId(itemKey(path))
                        )
                        : new AngelsteelToolItem(
                            kind,
                            degreeIndex,
                            kind.createProperties(AngelsteelToolHelper.material(degreeIndex)).setId(itemKey(path))
                        )
                );
            }
            ANGELSTEEL_TOOLS.put(kind, degreeItems);
        }

        for (StorageBookVariant variant : StorageBookVariant.values()) {
            STORAGE_BOOKS.put(variant, register(variant.registryPath(), new StorageBookItem(variant, itemProperties(variant.registryPath()))));
        }
    }

    private AuraItems() {
    }

    public static void bootstrap() {
        if (!angelsteelTickHookRegistered) {
            ServerTickEvents.END_WORLD_TICK.register(AuraItems::tickAngelsteelIngots);
            angelsteelTickHookRegistered = true;
        }
        if (!utilityHooksRegistered) {
            ServerTickEvents.END_WORLD_TICK.register(AuraItems::tickWorldUtilityItems);
            ServerTickEvents.END_SERVER_TICK.register(AuraItems::tickPlayerAccessories);
            ServerLivingEntityEvents.ALLOW_DAMAGE.register(AuraItems::allowAccessoryDamage);
            ServerLivingEntityEvents.AFTER_DAMAGE.register(AuraItems::afterAccessoryDamage);
            ServerLivingEntityEvents.AFTER_DEATH.register(AuraItems::afterAccessoryDeath);
            UseItemCallback.EVENT.register(AuraItems::handleUseItem);
            UseBlockCallback.EVENT.register(AuraItems::handleUseBlock);
            AttackEntityCallback.EVENT.register(AuraItems::handleAttackEntity);
            utilityHooksRegistered = true;
        }
        AuraCascadeMod.LOGGER.info("Registered aura progression materials, accessory compatibility hooks, and utility gear runtime.");
    }

    public static Item crystal(AuraColor color) {
        return AURA_CRYSTALS.get(color);
    }

    public static Item arcaneIngot(AuraColor color) {
        return ARCANE_INGOTS.get(color);
    }

    public static Item arcaneGem(AuraColor color) {
        return ARCANE_GEMS.get(color);
    }

    public static Map<AuraColor, Item> crystals() {
        return Map.copyOf(AURA_CRYSTALS);
    }

    public static Map<AuraColor, Item> arcaneIngots() {
        return Map.copyOf(ARCANE_INGOTS);
    }

    public static Map<AuraColor, Item> arcaneGems() {
        return Map.copyOf(ARCANE_GEMS);
    }

    public static Optional<AuraColor> auraCrystalColor(ItemStack stack) {
        for (Map.Entry<AuraColor, Item> entry : AURA_CRYSTALS.entrySet()) {
            if (stack.is(entry.getValue())) {
                return Optional.of(entry.getKey());
            }
        }
        return Optional.empty();
    }

    public static int auraCrystalCharge(ItemStack stack) {
        return auraCrystalColor(stack).isPresent() ? AURA_CRYSTAL_CHARGE : 0;
    }

    public static Optional<AuraColor> arcaneIngotColor(ItemStack stack) {
        for (Map.Entry<AuraColor, Item> entry : ARCANE_INGOTS.entrySet()) {
            if (stack.is(entry.getValue())) {
                return Optional.of(entry.getKey());
            }
        }
        return Optional.empty();
    }

    public static Item angelsteelIngot(int degreeIndex) {
        return ANGELSTEEL_INGOTS[AngelsteelToolHelper.clampDegree(degreeIndex)];
    }

    public static Item angelsteelTool(AngelsteelToolKind kind, int degreeIndex) {
        return ANGELSTEEL_TOOLS.get(kind)[AngelsteelToolHelper.clampDegree(degreeIndex)];
    }

    public static Item storageBook(StorageBookVariant variant) {
        return STORAGE_BOOKS.get(variant);
    }

    public static void addDiscoverableItems(java.util.function.Consumer<ItemLike> consumer) {
        consumer.accept(ENCYCLOPEDIA_AURA);

        for (AuraColor color : AuraColor.values()) {
            consumer.accept(crystal(color));
        }
        for (AuraColor color : AuraColor.values()) {
            consumer.accept(arcaneIngot(color));
        }
        for (AuraColor color : AuraColor.values()) {
            consumer.accept(arcaneGem(color));
        }

        consumer.accept(ARCANE_PRISM);

        for (StorageBookVariant variant : StorageBookVariant.values()) {
            consumer.accept(storageBook(variant));
        }

        consumer.accept(FAIRY_CHARM);
        consumer.accept(RING_OF_BINDING);
        consumer.accept(RING_OF_SHATTERED_STONE);
        consumer.accept(AMULET_OF_THE_ANGELS_WING);
        consumer.accept(SASH_OF_THE_ANGELS_HEELS);
        consumer.accept(AMULET_OF_THE_FORBIDDEN_FRUIT);
        consumer.accept(RED_PROTECTION_AMULET);
        consumer.accept(ORANGE_PROTECTION_AMULET);
        consumer.accept(YELLOW_PROTECTION_AMULET);
        consumer.accept(GREEN_PROTECTION_AMULET);
        consumer.accept(BLUE_PROTECTION_AMULET);
        consumer.accept(VIOLET_PROTECTION_AMULET);
        consumer.accept(MIRROR_OF_THE_ANGEL);
        consumer.accept(PORTABLE_RED_HOLE);
        consumer.accept(PORTABLE_BLACK_HOLE);
        consumer.accept(PRISMATIC_WAND);
        consumer.accept(TRANSMUTING_SWORD);
        consumer.accept(SWORD_OF_THE_THIEF);
        consumer.accept(SWORD_OF_THE_BARBARIAN);

        for (int degreeIndex = 0; degreeIndex < AngelsteelToolHelper.MAX_DEGREE; degreeIndex++) {
            consumer.accept(angelsteelIngot(degreeIndex));
        }
        for (AngelsteelToolKind kind : AngelsteelToolKind.values()) {
            for (int degreeIndex = 0; degreeIndex < AngelsteelToolHelper.MAX_DEGREE; degreeIndex++) {
                consumer.accept(angelsteelTool(kind, degreeIndex));
            }
        }
    }

    public static OptionalInt angelsteelDegree(ItemStack stack) {
        for (int degreeIndex = 0; degreeIndex < ANGELSTEEL_INGOTS.length; degreeIndex++) {
            if (stack.is(ANGELSTEEL_INGOTS[degreeIndex])) {
                return OptionalInt.of(degreeIndex);
            }
        }
        return OptionalInt.empty();
    }

    public static boolean isAngelsteelIngot(ItemStack stack) {
        return angelsteelDegree(stack).isPresent();
    }

    private static Item register(String path) {
        return register(path, new Item(itemProperties(path)));
    }

    private static Item register(String path, Item item) {
        return Registry.register(BuiltInRegistries.ITEM, id(path), item);
    }

    private static Identifier id(String path) {
        return Identifier.fromNamespaceAndPath(AuraCascadeMod.MOD_ID, path);
    }

    private static ResourceKey<Item> itemKey(String path) {
        return ResourceKey.create(Registries.ITEM, id(path));
    }

    private static Item.Properties itemProperties(String path) {
        return new Item.Properties().setId(itemKey(path));
    }

    private static void tickAngelsteelIngots(net.minecraft.server.level.ServerLevel level) {
        if (level.getGameTime() % 4L != 0L) {
            return;
        }

        java.util.HashSet<java.util.UUID> consumedEntities = new java.util.HashSet<>();
        @SuppressWarnings("unchecked")
        java.util.List<ItemEntity> itemEntities = (java.util.List<ItemEntity>) level.getEntities(
            net.minecraft.world.level.entity.EntityTypeTest.forClass(ItemEntity.class),
            entity -> !entity.isRemoved() && entity.onGround() && isAngelsteelIngot(entity.getItem())
        );

        for (ItemEntity itemEntity : itemEntities) {
            if (itemEntity.isRemoved() || consumedEntities.contains(itemEntity.getUUID())) {
                continue;
            }

            OptionalInt degree = angelsteelDegree(itemEntity.getItem());
            if (degree.isEmpty() || degree.getAsInt() >= AngelsteelToolHelper.MAX_DEGREE - 1) {
                continue;
            }

            java.util.ArrayList<ItemEntity> matching = new java.util.ArrayList<>();
            matching.add(itemEntity);
            matching.addAll(level.getEntitiesOfClass(
                ItemEntity.class,
                itemEntity.getBoundingBox().inflate(3.0D),
                other -> other != itemEntity
                    && !other.isRemoved()
                    && other.onGround()
                    && angelsteelDegree(other.getItem()).orElse(-1) == degree.getAsInt()
            ));

            int available = matching.stream().mapToInt(entity -> entity.getItem().getCount()).sum();
            if (available < 3) {
                continue;
            }

            int remaining = 3;
            for (ItemEntity candidate : matching) {
                ItemStack stack = candidate.getItem();
                int taken = Math.min(remaining, stack.getCount());
                stack.shrink(taken);
                remaining -= taken;
                consumedEntities.add(candidate.getUUID());
                if (stack.isEmpty()) {
                    candidate.discard();
                }
                if (remaining == 0) {
                    break;
                }
            }

            ItemEntity upgraded = new ItemEntity(
                level,
                itemEntity.getX(),
                itemEntity.getY(),
                itemEntity.getZ(),
                new ItemStack(angelsteelIngot(degree.getAsInt() + 1))
            );
            upgraded.setDeltaMovement(0.0D, 0.05D, 0.0D);
            level.addFreshEntity(upgraded);
        }
    }

    private static void tickWorldUtilityItems(ServerLevel level) {
        if (level.getGameTime() % 20L != 0L) {
            return;
        }

        @SuppressWarnings("unchecked")
        List<ItemEntity> itemEntities = (List<ItemEntity>) level.getEntities(
            net.minecraft.world.level.entity.EntityTypeTest.forClass(ItemEntity.class),
            entity -> entity.getItem().is(PORTABLE_RED_HOLE) || entity.getItem().is(PORTABLE_BLACK_HOLE)
        );

        for (ItemEntity itemEntity : itemEntities) {
            itemEntity.setUnlimitedLifetime();
            if (itemEntity.getItem().is(PORTABLE_RED_HOLE) && level.getGameTime() % 100L == 0L) {
                level.explode(itemEntity, itemEntity.getX(), itemEntity.getY(), itemEntity.getZ(), 8.0F, Level.ExplosionInteraction.BLOCK);
            }
        }
    }

    private static void tickPlayerAccessories(MinecraftServer server) {
        for (var player : server.getPlayerList().getPlayers()) {
            if (isAccessoryEquipped(player, RED_PROTECTION_AMULET) && player.isOnFire()) {
                player.clearFire();
                player.heal(0.5F);
            }
            if (isAccessoryEquipped(player, BLUE_PROTECTION_AMULET)) {
                player.setAirSupply(player.getMaxAirSupply());
            }
            if (isAccessoryEquipped(player, VIOLET_PROTECTION_AMULET)) {
                player.removeEffect(MobEffects.WITHER);
            }
            if (isAccessoryEquipped(player, GREEN_PROTECTION_AMULET)) {
                player.fallDistance = 0.0F;
            }
            if (isAccessoryEquipped(player, SASH_OF_THE_ANGELS_HEELS)) {
                player.addEffect(new MobEffectInstance(MobEffects.JUMP_BOOST, 10, 1, true, false, false));
                int charge = player.horizontalCollision ? ANGEL_SASH_COLLISION_CHARGE.getOrDefault(player.getUUID(), 0) + 1 : 0;
                if (player.horizontalCollision && player.onGround() && charge >= 10) {
                    Vec3 motion = player.getDeltaMovement();
                    player.setDeltaMovement(motion.x, Math.max(motion.y, 0.65D), motion.z);
                    player.fallDistance = 0.0F;
                    charge = 0;
                }
                ANGEL_SASH_COLLISION_CHARGE.put(player.getUUID(), charge);
            } else {
                ANGEL_SASH_COLLISION_CHARGE.remove(player.getUUID());
            }

            if (player.level().getGameTime() % 20L == 0L && inventoryContains(player.getInventory(), PORTABLE_BLACK_HOLE)) {
                consumeInventoryItem(player.getInventory(), Blocks.COBBLESTONE.asItem(), Integer.MAX_VALUE);
            }

            if (player instanceof ServerPlayer serverPlayer) {
                FairySystem.syncPlayerFairies(serverPlayer);
            }
        }
    }

    private static boolean allowAccessoryDamage(net.minecraft.world.entity.LivingEntity entity, DamageSource source, float amount) {
        if (!(entity instanceof Player player)) {
            return true;
        }

        ProtectionAmuletProfile.DamageFamily family = classifyDamage(source);
        ProtectionAmuletProfile profile = equippedProtectionProfile(player, family);
        if (profile != null && profile.blocksDamage()) {
            if (profile.healFraction() > 0.0F) {
                player.heal(amount * profile.healFraction());
            }
            if (profile == ProtectionAmuletProfile.RED) {
                player.clearFire();
            }
            if (profile == ProtectionAmuletProfile.VIOLET) {
                player.removeEffect(MobEffects.WITHER);
            }
            return false;
        }

        if (family == ProtectionAmuletProfile.DamageFamily.EXPLOSION && isAccessoryEquipped(player, RING_OF_SHATTERED_STONE)) {
            player.heal(amount * 0.25F);
            return false;
        }

        return true;
    }

    private static void afterAccessoryDamage(
        net.minecraft.world.entity.LivingEntity entity,
        DamageSource source,
        float baseDamageTaken,
        float damageTaken,
        boolean blocked
    ) {
        if (!(entity instanceof Player player)) {
            return;
        }

        if (classifyDamage(source) == ProtectionAmuletProfile.DamageFamily.PROJECTILE && isAccessoryEquipped(player, YELLOW_PROTECTION_AMULET)) {
            player.heal(Math.max(0.0F, damageTaken * ProtectionAmuletProfile.YELLOW.healFraction()));
        }
    }

    private static void afterAccessoryDeath(net.minecraft.world.entity.LivingEntity entity, DamageSource damageSource) {
        if (!(entity instanceof AbstractVillager villager) || !(entity.level() instanceof ServerLevel level)) {
            return;
        }
        if (!(damageSource.getEntity() instanceof Player player) || !player.getMainHandItem().is(SWORD_OF_THE_THIEF)) {
            return;
        }
        if (level.getRandom().nextFloat() >= 0.25F) {
            return;
        }

        ArrayList<ItemStack> possibleDrops = new ArrayList<>();
        for (MerchantOffer offer : villager.getOffers()) {
            if (!offer.isOutOfStock()) {
                possibleDrops.add(offer.getResult().copy());
            }
        }
        if (possibleDrops.isEmpty()) {
            for (MerchantOffer offer : villager.getOffers()) {
                possibleDrops.add(offer.getResult().copy());
            }
        }
        if (possibleDrops.isEmpty()) {
            return;
        }

        ItemStack droppedTrade = possibleDrops.get(level.getRandom().nextInt(possibleDrops.size()));
        level.addFreshEntity(new ItemEntity(level, entity.getX(), entity.getY(), entity.getZ(), droppedTrade));
    }

    private static InteractionResult handleUseItem(Player player, Level world, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);

        if (!world.isClientSide() && stack.hasNonDefault(net.minecraft.core.component.DataComponents.FOOD) && isAccessoryEquipped(player, AMULET_OF_THE_FORBIDDEN_FRUIT)) {
            ForbiddenFruitEffects.apply(player, stack);
        }

        if (stack.is(FAIRY_CHARM)) {
            return handleFairyCharmUse(player, world, stack);
        }
        if (stack.is(RING_OF_BINDING)) {
            return handleRingOfBindingUse(player, world, hand, stack);
        }
        if (stack.is(AMULET_OF_THE_ANGELS_WING)) {
            return handleAngelWingUse(player, world, stack);
        }
        if (stack.is(MIRROR_OF_THE_ANGEL)) {
            return handleMirrorUse(player, world);
        }
        if (stack.is(PRISMATIC_WAND)) {
            return handlePrismaticWandUse(player, world, stack);
        }
        if (stack.getItem() instanceof AuraAccessoryItem accessoryItem) {
            return handlePassiveAccessoryUse(player, world, stack, accessoryItem.slot());
        }

        return InteractionResult.PASS;
    }

    private static InteractionResult handleFairyCharmUse(Player player, Level world, ItemStack stack) {
        if (!player.isShiftKeyDown()) {
            return InteractionResult.PASS;
        }
        if (world.isClientSide()) {
            return InteractionResult.SUCCESS;
        }
        FairyRole nextRole = FairyCharmItem.cycleRole(stack);
        showStatus(player, Component.translatable("message.aura.fairy_charm_attuned", nextRole.displayComponent()));
        return InteractionResult.SUCCESS;
    }

    private static InteractionResult handleUseBlock(Player player, Level world, InteractionHand hand, net.minecraft.world.phys.BlockHitResult hitResult) {
        ItemStack stack = player.getItemInHand(hand);
        if (!stack.is(PRISMATIC_WAND)) {
            return InteractionResult.PASS;
        }
        if (player.isShiftKeyDown()) {
            return handlePrismaticWandUse(player, world, stack);
        }
        if (world.isClientSide()) {
            return InteractionResult.SUCCESS;
        }

        switch (PrismaticWandState.mode(stack)) {
            case SELECTION -> {
                PrismaticWandState.setSelectionPoint(stack, hitResult.getBlockPos());
                showStatus(player, Component.translatable("message.aura.prismatic_wand_position_set"));
            }
            case COPY -> {
                PrismaticWandState.Selection selection = PrismaticWandState.selection(stack);
                if (selection == null) {
                    showStatus(player, Component.translatable("message.aura.prismatic_wand_invalid_selection"));
                    return InteractionResult.SUCCESS;
                }
                List<PrismaticWandState.ClipboardBlock> blocks = copySelection(world, selection);
                PrismaticWandState.storeClipboard(stack, blocks);
                showStatus(
                    player,
                    blocks.isEmpty()
                        ? Component.translatable("message.aura.prismatic_wand_nothing_copied")
                        : Component.translatable("message.aura.prismatic_wand_copied", blocks.size())
                );
            }
            case PASTE -> {
                List<PrismaticWandState.ClipboardBlock> blocks = PrismaticWandState.clipboard(stack);
                if (blocks.isEmpty()) {
                    showStatus(player, Component.translatable("message.aura.prismatic_wand_nothing_copied"));
                    return InteractionResult.SUCCESS;
                }
                if (!player.getAbilities().instabuild) {
                    Map<String, Integer> requiredItems = requiredClipboardItems(blocks);
                    if (!hasInventoryItems(player.getInventory(), requiredItems)) {
                        showStatus(player, Component.translatable("message.aura.prismatic_wand_not_enough_materials"));
                        return InteractionResult.SUCCESS;
                    }
                    consumeInventoryItems(player.getInventory(), requiredItems);
                }
                pasteClipboard(world, hitResult.getBlockPos(), blocks);
                showStatus(player, Component.translatable("message.aura.prismatic_wand_pasted"));
            }
        }

        return InteractionResult.SUCCESS;
    }

    private static InteractionResult handleAttackEntity(
        Player player,
        Level world,
        InteractionHand hand,
        Entity entity,
        net.minecraft.world.phys.EntityHitResult hitResult
    ) {
        if (!player.getItemInHand(hand).is(MIRROR_OF_THE_ANGEL) || !(entity instanceof AbstractHurtingProjectile projectile)) {
            return InteractionResult.PASS;
        }
        if (world.isClientSide()) {
            return InteractionResult.SUCCESS;
        }
        deflectProjectile(player, projectile);
        return InteractionResult.SUCCESS;
    }

    private static InteractionResult handleRingOfBindingUse(Player player, Level world, InteractionHand hand, ItemStack stack) {
        ItemStack offhand = player.getOffhandItem();
        if (world.isClientSide()) {
            return InteractionResult.SUCCESS;
        }

        if (player.isShiftKeyDown()) {
            List<FairyRole> released = RingOfBindingItem.releaseBoundFairies(stack);
            for (FairyRole role : released) {
                ItemStack charm = FairyCharmItem.withRole(new ItemStack(FAIRY_CHARM), role);
                if (!player.addItem(charm)) {
                    player.drop(charm, false);
                }
            }
            showStatus(
                player,
                released.isEmpty()
                    ? Component.translatable("message.aura.ring_of_binding_empty")
                    : Component.translatable("message.aura.ring_of_binding_released", released.size())
            );
            return InteractionResult.SUCCESS;
        }

        if (hand != InteractionHand.OFF_HAND && offhand.is(FAIRY_CHARM)) {
            FairyRole role = FairyCharmItem.role(offhand);
            if (!RingOfBindingItem.bindCharm(stack, role)) {
                showStatus(player, Component.translatable("message.aura.ring_of_binding_full"));
                return InteractionResult.SUCCESS;
            }
            offhand.shrink(1);
            showStatus(
                player,
                Component.translatable(
                    "message.aura.ring_of_binding_bound",
                    role.displayComponent(),
                    RingOfBindingItem.boundFairyCount(stack),
                    RingOfBindingItem.MAX_BOUND_FAIRIES
                )
            );
            return InteractionResult.SUCCESS;
        }

        if (!AuraAccessoryBridgeRegistry.isEquipped(player, stack, AuraAccessorySlot.RING)) {
            AuraAccessoryBridgeRegistry.equip(player, stack, AuraAccessorySlot.RING);
            showStatus(player, Component.translatable("message.aura.ring_of_binding_equipped"));
            return InteractionResult.SUCCESS;
        }

        showStatus(player, Component.translatable("message.aura.ring_of_binding_count", RingOfBindingItem.boundFairyCount(stack)));
        return InteractionResult.SUCCESS;
    }

    private static InteractionResult handleAngelWingUse(Player player, Level world, ItemStack stack) {
        if (world.isClientSide()) {
            return InteractionResult.SUCCESS;
        }

        boolean equipped = AuraAccessoryBridgeRegistry.isEquipped(player, stack, AuraAccessorySlot.AMULET);
        if (player.isShiftKeyDown() && equipped) {
            AuraAccessoryBridgeRegistry.unequip(player, stack, AuraAccessorySlot.AMULET);
            showStatus(player, "Amulet of the Angel's Wing unequipped");
            return InteractionResult.SUCCESS;
        }
        if (!equipped) {
            AuraAccessoryBridgeRegistry.equip(player, stack, AuraAccessorySlot.AMULET);
            showStatus(player, "Amulet of the Angel's Wing equipped");
            return InteractionResult.SUCCESS;
        }

        BlockPos standingSpot = findStandingSpot(player, player.getXRot() > 45.0F ? -1 : 1);
        if (standingSpot == null) {
            showStatus(player, "No clear space found");
            return InteractionResult.SUCCESS;
        }

        player.teleportTo(standingSpot.getX() + 0.5D, standingSpot.getY(), standingSpot.getZ() + 0.5D);
        player.fallDistance = 0.0F;
        showStatus(player, player.getXRot() > 45.0F ? "Descended" : "Ascended");
        return InteractionResult.SUCCESS;
    }

    private static InteractionResult handleMirrorUse(Player player, Level world) {
        List<AbstractHurtingProjectile> projectiles = world.getEntitiesOfClass(
            AbstractHurtingProjectile.class,
            player.getBoundingBox().inflate(5.0D)
        );
        if (projectiles.isEmpty()) {
            return InteractionResult.PASS;
        }
        if (world.isClientSide()) {
            return InteractionResult.SUCCESS;
        }

        AbstractHurtingProjectile projectile = projectiles.stream()
            .min(java.util.Comparator.comparingDouble(player::distanceToSqr))
            .orElse(null);
        if (projectile == null) {
            return InteractionResult.PASS;
        }
        deflectProjectile(player, projectile);
        return InteractionResult.SUCCESS;
    }

    private static InteractionResult handlePrismaticWandUse(Player player, Level world, ItemStack stack) {
        if (!player.isShiftKeyDown()) {
            return InteractionResult.PASS;
        }
        if (world.isClientSide()) {
            return InteractionResult.SUCCESS;
        }
        PrismaticWandState.Mode mode = PrismaticWandState.cycleMode(stack);
        showStatus(player, Component.translatable("message.aura.prismatic_wand_switched", mode.displayComponent()));
        return InteractionResult.SUCCESS;
    }

    private static InteractionResult handlePassiveAccessoryUse(Player player, Level world, ItemStack stack, AuraAccessorySlot slot) {
        boolean equipped = AuraAccessoryBridgeRegistry.isEquipped(player, stack, slot);
        if (world.isClientSide()) {
            return InteractionResult.SUCCESS;
        }

        if (player.isShiftKeyDown() && equipped) {
            AuraAccessoryBridgeRegistry.unequip(player, stack, slot);
            showStatus(player, "Accessory unequipped");
            return InteractionResult.SUCCESS;
        }
        if (!equipped) {
            AuraAccessoryBridgeRegistry.equip(player, stack, slot);
            showStatus(player, "Accessory equipped");
            return InteractionResult.SUCCESS;
        }
        return InteractionResult.PASS;
    }

    private static void deflectProjectile(Player player, AbstractHurtingProjectile projectile) {
        LivingTarget target = nearestGhastTarget(player, projectile);
        Vec3 targetVector = target != null
            ? target.position().subtract(projectile.position()).normalize().scale(1.8D)
            : player.getLookAngle().normalize().scale(1.8D);
        projectile.setOwner(player);
        projectile.setDeltaMovement(targetVector);
    }

    private static LivingTarget nearestGhastTarget(Player player, AbstractHurtingProjectile projectile) {
        List<net.minecraft.world.entity.monster.Ghast> ghasts = player.level().getEntitiesOfClass(
            net.minecraft.world.entity.monster.Ghast.class,
            projectile.getBoundingBox().inflate(24.0D)
        );
        net.minecraft.world.entity.monster.Ghast ghast = ghasts.stream()
            .min(java.util.Comparator.comparingDouble(projectile::distanceToSqr))
            .orElse(null);
        return ghast == null ? null : new LivingTarget(ghast.position());
    }

    private static ProtectionAmuletProfile equippedProtectionProfile(Player player, ProtectionAmuletProfile.DamageFamily family) {
        if (family == ProtectionAmuletProfile.DamageFamily.FIRE && isAccessoryEquipped(player, RED_PROTECTION_AMULET)) {
            return ProtectionAmuletProfile.RED;
        }
        if (family == ProtectionAmuletProfile.DamageFamily.EXPLOSION && isAccessoryEquipped(player, ORANGE_PROTECTION_AMULET)) {
            return ProtectionAmuletProfile.ORANGE;
        }
        if (family == ProtectionAmuletProfile.DamageFamily.PROJECTILE && isAccessoryEquipped(player, YELLOW_PROTECTION_AMULET)) {
            return ProtectionAmuletProfile.YELLOW;
        }
        if (family == ProtectionAmuletProfile.DamageFamily.FALL && isAccessoryEquipped(player, GREEN_PROTECTION_AMULET)) {
            return ProtectionAmuletProfile.GREEN;
        }
        if (family == ProtectionAmuletProfile.DamageFamily.DROWN && isAccessoryEquipped(player, BLUE_PROTECTION_AMULET)) {
            return ProtectionAmuletProfile.BLUE;
        }
        if (family == ProtectionAmuletProfile.DamageFamily.WITHER && isAccessoryEquipped(player, VIOLET_PROTECTION_AMULET)) {
            return ProtectionAmuletProfile.VIOLET;
        }
        return null;
    }

    private static ProtectionAmuletProfile.DamageFamily classifyDamage(DamageSource source) {
        if (source.is(DamageTypeTags.IS_FIRE)) {
            return ProtectionAmuletProfile.DamageFamily.FIRE;
        }
        if (source.is(DamageTypeTags.IS_EXPLOSION)) {
            return ProtectionAmuletProfile.DamageFamily.EXPLOSION;
        }
        if (source.is(DamageTypeTags.IS_PROJECTILE)) {
            return ProtectionAmuletProfile.DamageFamily.PROJECTILE;
        }
        if (source.is(DamageTypes.FALL)) {
            return ProtectionAmuletProfile.DamageFamily.FALL;
        }
        if (source.is(DamageTypes.DROWN)) {
            return ProtectionAmuletProfile.DamageFamily.DROWN;
        }
        if (source.is(DamageTypes.WITHER)) {
            return ProtectionAmuletProfile.DamageFamily.WITHER;
        }
        return ProtectionAmuletProfile.DamageFamily.OTHER;
    }

    private static boolean isAccessoryEquipped(Player player, Item item) {
        if (!(item instanceof AuraAccessoryItem accessoryItem)) {
            return false;
        }
        for (ItemStack equipped : AuraAccessoryBridgeRegistry.equipped(player, accessoryItem.slot())) {
            if (equipped.is(item)) {
                return true;
            }
        }
        return false;
    }

    private static boolean inventoryContains(Inventory inventory, Item item) {
        for (int slot = 0; slot < inventory.getContainerSize(); slot++) {
            if (inventory.getItem(slot).is(item)) {
                return true;
            }
        }
        return false;
    }

    private static int consumeInventoryItem(Inventory inventory, Item item, int requested) {
        int remaining = requested;
        for (int slot = 0; slot < inventory.getContainerSize() && remaining > 0; slot++) {
            ItemStack stack = inventory.getItem(slot);
            if (!stack.is(item)) {
                continue;
            }
            int consumed = Math.min(remaining, stack.getCount());
            stack.shrink(consumed);
            if (stack.isEmpty()) {
                inventory.setItem(slot, ItemStack.EMPTY);
            }
            remaining -= consumed;
        }
        return requested - remaining;
    }

    private static BlockPos findStandingSpot(Player player, int direction) {
        BlockPos origin = player.blockPosition();
        int maxDistance = 32;
        for (int step = 1; step <= maxDistance; step++) {
            int y = origin.getY() + (direction * step);
            BlockPos floor = new BlockPos(origin.getX(), y, origin.getZ());
            BlockState feet = player.level().getBlockState(floor);
            BlockState head = player.level().getBlockState(floor.above());
            BlockState below = player.level().getBlockState(floor.below());
            if (feet.isAir() && head.isAir() && !below.isAir()) {
                return floor;
            }
        }
        return null;
    }

    private static List<PrismaticWandState.ClipboardBlock> copySelection(Level world, PrismaticWandState.Selection selection) {
        BlockPos min = selection.min();
        BlockPos max = selection.max();
        ArrayList<PrismaticWandState.ClipboardBlock> blocks = new ArrayList<>();
        for (int x = min.getX(); x <= max.getX() && blocks.size() < PRISMATIC_WAND_BLOCK_LIMIT; x++) {
            for (int y = min.getY(); y <= max.getY() && blocks.size() < PRISMATIC_WAND_BLOCK_LIMIT; y++) {
                for (int z = min.getZ(); z <= max.getZ() && blocks.size() < PRISMATIC_WAND_BLOCK_LIMIT; z++) {
                    BlockPos currentPos = new BlockPos(x, y, z);
                    BlockState state = world.getBlockState(currentPos);
                    if (!PrismaticWandItem.supportsClipboardCopy(state)) {
                        continue;
                    }
                    Item item = state.getBlock().asItem();
                    blocks.add(new PrismaticWandState.ClipboardBlock(
                        x - min.getX(),
                        y - min.getY(),
                        z - min.getZ(),
                        Block.getId(state),
                        BuiltInRegistries.ITEM.getKey(item).toString()
                    ));
                }
            }
        }
        return blocks;
    }

    private static Map<String, Integer> requiredClipboardItems(List<PrismaticWandState.ClipboardBlock> blocks) {
        HashMap<String, Integer> required = new HashMap<>();
        for (PrismaticWandState.ClipboardBlock block : blocks) {
            required.merge(block.itemId(), 1, Integer::sum);
        }
        return required;
    }

    private static boolean hasInventoryItems(Inventory inventory, Map<String, Integer> required) {
        HashMap<String, Integer> available = new HashMap<>();
        for (int slot = 0; slot < inventory.getContainerSize(); slot++) {
            ItemStack stack = inventory.getItem(slot);
            if (stack.isEmpty()) {
                continue;
            }
            available.merge(BuiltInRegistries.ITEM.getKey(stack.getItem()).toString(), stack.getCount(), Integer::sum);
        }

        for (Map.Entry<String, Integer> entry : required.entrySet()) {
            if (available.getOrDefault(entry.getKey(), 0) < entry.getValue()) {
                return false;
            }
        }
        return true;
    }

    private static void consumeInventoryItems(Inventory inventory, Map<String, Integer> required) {
        HashMap<String, Integer> remaining = new HashMap<>(required);
        for (int slot = 0; slot < inventory.getContainerSize(); slot++) {
            ItemStack stack = inventory.getItem(slot);
            if (stack.isEmpty()) {
                continue;
            }
            String itemId = BuiltInRegistries.ITEM.getKey(stack.getItem()).toString();
            int stillNeeded = remaining.getOrDefault(itemId, 0);
            if (stillNeeded <= 0) {
                continue;
            }
            int consumed = Math.min(stillNeeded, stack.getCount());
            stack.shrink(consumed);
            if (stack.isEmpty()) {
                inventory.setItem(slot, ItemStack.EMPTY);
            }
            if (stillNeeded == consumed) {
                remaining.remove(itemId);
            } else {
                remaining.put(itemId, stillNeeded - consumed);
            }
            if (remaining.isEmpty()) {
                return;
            }
        }
    }

    private static void pasteClipboard(Level world, BlockPos anchor, List<PrismaticWandState.ClipboardBlock> blocks) {
        for (PrismaticWandState.ClipboardBlock block : blocks) {
            BlockState state = Block.stateById(block.stateId());
            BlockPos placePos = anchor.offset(block.dx(), block.dy(), block.dz());
            world.setBlockAndUpdate(placePos, state);
        }
    }

    private static void showStatus(Player player, String text) {
        showStatus(player, Component.literal(text));
    }

    private static void showStatus(Player player, Component message) {
        player.displayClientMessage(message, true);
    }

    private record LivingTarget(Vec3 position) {
    }
}
