package pixlepix.auracascade.item;

import java.util.EnumMap;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.OptionalInt;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.fabricmc.fabric.api.entity.event.v1.ServerLivingEntityEvents;
import net.fabricmc.fabric.api.event.player.AttackEntityCallback;
import net.fabricmc.fabric.api.event.player.UseBlockCallback;
import net.fabricmc.fabric.api.event.player.UseItemCallback;
import net.minecraft.core.Registry;
import net.minecraft.core.BlockPos;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.core.registries.Registries;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.util.ARGB;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.damagesource.DamageTypes;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.npc.villager.Villager;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.hurtingprojectile.AbstractHurtingProjectile;
import net.minecraft.world.entity.projectile.hurtingprojectile.Fireball;
import net.minecraft.world.entity.projectile.hurtingprojectile.WitherSkull;
import net.minecraft.world.entity.monster.Blaze;
import net.minecraft.world.entity.monster.Ghast;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.ItemLike;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import pixlepix.auracascade.AuraCascadeMod;
import pixlepix.auracascade.aura.WorldInteractionVisuals;
import pixlepix.auracascade.block.entity.AuraNetworkBlockEntity;
import pixlepix.auracascade.enchantment.KaleidoscopicOriginalEffects;
import pixlepix.auracascade.compat.AuraAccessoryBridgeRegistry;
import pixlepix.auracascade.compat.AuraAccessorySlot;
import pixlepix.auracascade.compat.AuraAccessoryInventory;
import pixlepix.auracascade.fairy.FairyRole;
import pixlepix.auracascade.fairy.FairySystem;
import pixlepix.auracascade.item.books.StorageBookItem;
import pixlepix.auracascade.item.books.StorageBookVariant;
import pixlepix.auracascade.parity.AuraColor;
import pixlepix.auracascade.util.ToolPropertiesCompat;

public final class AuraItems {
    private static final int AURA_CRYSTAL_CHARGE = 1_000;
    private static boolean angelsteelTickHookRegistered;
    private static boolean utilityHooksRegistered;

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
    public static final Item PORTABLE_RED_HOLE = register("portable_red_hole", new PortableRedHoleItem(itemProperties("portable_red_hole").stacksTo(1)));
    public static final Item PORTABLE_BLACK_HOLE = register("portable_black_hole", new Item(itemProperties("portable_black_hole").stacksTo(1)));
    public static final Item PRISMATIC_WAND = register("prismatic_wand", new PrismaticWandItem(itemProperties("prismatic_wand")));
    public static final Item TRANSMUTING_SWORD = register(
        "transmuting_sword",
        new TransmutingSwordItem(ToolPropertiesCompat.sword(itemProperties("transmuting_sword").stacksTo(1), AuraUtilityToolMaterials.ARCANE_SWORD, 3, -2.4F))
    );
    public static final Item SWORD_OF_THE_THIEF = register(
        "sword_of_the_thief",
        new Item(ToolPropertiesCompat.sword(itemProperties("sword_of_the_thief").stacksTo(1), AuraUtilityToolMaterials.ARCANE_SWORD, 2, -2.4F))
    );
    public static final Item SWORD_OF_THE_BARBARIAN = register(
        "sword_of_the_barbarian",
        new SwordOfBarbarianItem(ToolPropertiesCompat.sword(itemProperties("sword_of_the_barbarian").stacksTo(1), AuraUtilityToolMaterials.ARCANE_SWORD, 3, -2.4F))
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
                            kind.createProperties(AngelsteelToolHelper.material(degreeIndex), itemProperties(path))
                        )
                        : new AngelsteelToolItem(
                            kind,
                            degreeIndex,
                            kind.createProperties(AngelsteelToolHelper.material(degreeIndex), itemProperties(path))
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
        AngelsteelCurseEffects.bootstrap();
        if (!angelsteelTickHookRegistered) {
            ServerTickEvents.END_WORLD_TICK.register(AuraItems::tickAngelsteelIngots);
            angelsteelTickHookRegistered = true;
        }
        if (!utilityHooksRegistered) {
            ServerTickEvents.END_SERVER_TICK.register(AuraItems::tickPlayerAccessories);
            ServerLivingEntityEvents.ALLOW_DAMAGE.register(AuraItems::allowAccessoryDamage);
            ServerLivingEntityEvents.AFTER_DEATH.register(AuraItems::afterAccessoryDeath);
            UseItemCallback.EVENT.register(AuraItems::handleUseItem);
            UseBlockCallback.EVENT.register(AuraItems::handleUseBlock);
            AttackEntityCallback.EVENT.register(AuraItems::handleAttackEntity);
            KaleidoscopicOriginalEffects.bootstrap();
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

    private static Item.Properties itemProperties(String path) {
        return new Item.Properties().setId(ResourceKey.create(Registries.ITEM, id(path)));
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
            if (level.addFreshEntity(upgraded)) {
                emitAngelsteelGroundCraft(level, upgraded.position());
            }
        }
    }

    private static void emitAngelsteelGroundCraft(ServerLevel level, Vec3 center) {
        DustParticleOptions particle = new DustParticleOptions(ARGB.colorFromFloat(1.0F, 1.0F, 0.72F, 0.28F) & 0xFFFFFF, 1.1F);
        for (WorldInteractionVisuals.BurstSample sample : WorldInteractionVisuals.groundCraftBurst(center)) {
            Vec3 point = sample.position();
            Vec3 velocity = sample.velocity();
            level.sendParticles(particle, point.x, point.y, point.z,
                0, velocity.x, velocity.y, velocity.z, 1.0D);
        }
    }

    private static void tickPlayerAccessories(MinecraftServer server) {
        for (var player : server.getPlayerList().getPlayers()) {
            updateAngelHeelsStepHeight(player, isAccessoryEquipped(player, SASH_OF_THE_ANGELS_HEELS));

            if (player.level().getGameTime() % 100L == 0L && inventoryContains(player.getInventory(), PORTABLE_BLACK_HOLE)) {
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
            boolean redAmuletInFire = profile == ProtectionAmuletProfile.RED && source.is(DamageTypes.IN_FIRE);
            if (profile.healFraction() > 0.0F && !redAmuletInFire) {
                player.heal(amount * profile.healFraction());
            }
            return false;
        }

        if (shouldBlockShatteredStoneExplosionDamage(family, isAccessoryEquipped(player, RING_OF_SHATTERED_STONE))) {
            player.heal(amount * 0.25F);
            return false;
        }

        return true;
    }

    public static float modifyPreMitigationDamage(
        net.minecraft.world.entity.LivingEntity victim,
        DamageSource source,
        float original
    ) {
        if (victim.level().isClientSide()) {
            return original;
        }

        float amount = original;
        if (victim instanceof Player player
            && classifyDamage(source) == ProtectionAmuletProfile.DamageFamily.PROJECTILE
            && isAccessoryEquipped(player, YELLOW_PROTECTION_AMULET)) {
            amount *= ProtectionAmuletProfile.YELLOW.incomingDamageMultiplier();
        }
        if (source.getEntity() instanceof Player attacker
            && attacker.getMainHandItem().is(SWORD_OF_THE_BARBARIAN)) {
            amount = SwordOfBarbarianItem.modifyIncomingDamage(attacker, attacker.getMainHandItem(), amount);
        }
        return amount;
    }

    private static void updateAngelHeelsStepHeight(Player player, boolean equipped) {
        AngelHeelsRuntime.update(player.getAttribute(Attributes.STEP_HEIGHT), equipped, player.horizontalCollision);
    }

    private static void afterAccessoryDeath(net.minecraft.world.entity.LivingEntity entity, DamageSource damageSource) {
        if (!(entity instanceof Villager villager) || !(entity.level() instanceof ServerLevel level)) {
            return;
        }
        if (!(damageSource.getEntity() instanceof Player player) || !player.getMainHandItem().is(SWORD_OF_THE_THIEF)) {
            return;
        }
        if (level.getRandom().nextInt(4) != 0) {
            return;
        }

        if (villager.getOffers().isEmpty()) {
            return;
        }

        ItemStack droppedTrade = villager.getOffers().get(0).getResult().copy();
        level.addFreshEntity(new ItemEntity(level, entity.getX(), entity.getY(), entity.getZ(), droppedTrade));
    }

    private static InteractionResult handleUseItem(Player player, Level world, InteractionHand hand) {
        if (player.isSpectator()) {
            return InteractionResult.PASS;
        }
        ItemStack stack = player.getItemInHand(hand);

        if (stack.is(FAIRY_CHARM)) {
            return useItemResult(handleFairyCharmUse(player, world, stack), world);
        }
        if (stack.is(RING_OF_BINDING)) {
            return useItemResult(handleRingOfBindingUse(player, world, hand, stack), world);
        }
        if (stack.is(AMULET_OF_THE_ANGELS_WING)) {
            return useItemResult(handleAngelWingUse(player, world, stack), world);
        }
        if (stack.is(MIRROR_OF_THE_ANGEL)) {
            return useItemResult(handleMirrorUse(player, world), world);
        }
        if (stack.getItem() instanceof AuraAccessoryItem accessoryItem) {
            return useItemResult(handlePassiveAccessoryUse(player, world, stack, accessoryItem.slot()), world);
        }

        return InteractionResult.PASS;
    }

    public static void onFoodFinished(
        ServerPlayer player,
        ItemStack usedFood,
        net.minecraft.world.item.ItemUseAnimation useAnimation
    ) {
        if (ForbiddenFruitEffects.acceptsCompletedUse(
            useAnimation,
            player.isAlive(),
            player.isSpectator(),
            isAccessoryEquipped(player, AMULET_OF_THE_FORBIDDEN_FRUIT)
        )) {
            ForbiddenFruitEffects.apply(player, usedFood);
        }
    }

    private static InteractionResult useItemResult(InteractionResult result, Level level) {
        if (result == InteractionResult.PASS) {
            return InteractionResult.PASS;
        }
        if (result == InteractionResult.FAIL) {
            return InteractionResult.FAIL;
        }
        return level.isClientSide() ? InteractionResult.SUCCESS : InteractionResult.CONSUME;
    }

    private static InteractionResult handleFairyCharmUse(Player player, Level world, ItemStack stack) {
        if (world.isClientSide()) {
            return InteractionResult.SUCCESS;
        }
        ItemStack ring = AuraAccessoryBridgeRegistry.equipped(player, AuraAccessorySlot.RING).stream()
            .filter(equipped -> equipped.is(RING_OF_BINDING)).findFirst().orElse(ItemStack.EMPTY);
        if (ring.isEmpty()) {
            showStatus(player, Component.translatable("message.aura.fairy_charm.no_ring"));
            return InteractionResult.FAIL;
        }
        FairyRole role = FairyCharmItem.role(stack);
        if (!RingOfBindingItem.bindCharm(ring, role)) {
            showStatus(player, Component.translatable("message.aura.ring_of_binding_full"));
            return InteractionResult.FAIL;
        }
        stack.shrink(1);
        AuraAccessoryInventory.touch(player);
        showStatus(player, Component.translatable("message.aura.ring_of_binding_bound",
            role.displayComponent(), RingOfBindingItem.boundFairyCount(ring), RingOfBindingItem.MAX_BOUND_FAIRIES));
        return InteractionResult.SUCCESS;
    }

    private static InteractionResult handleUseBlock(Player player, Level world, InteractionHand hand, net.minecraft.world.phys.BlockHitResult hitResult) {
        if (player.isSpectator()) {
            return InteractionResult.PASS;
        }
        ItemStack stack = player.getItemInHand(hand);
        Optional<AuraColor> crystalColor = auraCrystalColor(stack);
        if (crystalColor.isPresent()) {
            BlockEntity blockEntity = world.getBlockEntity(hitResult.getBlockPos());
            if (!(blockEntity instanceof AuraNetworkBlockEntity network)) {
                return InteractionResult.PASS;
            }
            if (world.isClientSide()) {
                return InteractionResult.SUCCESS;
            }

            network.feedCrystal(crystalColor.get(), auraCrystalCharge(stack));
            if (!player.getAbilities().instabuild) {
                stack.shrink(1);
            }
            return InteractionResult.SUCCESS;
        }
        return InteractionResult.PASS;
    }

    private static InteractionResult handleAttackEntity(
        Player player,
        Level world,
        InteractionHand hand,
        Entity entity,
        net.minecraft.world.phys.EntityHitResult hitResult
    ) {
        if (player.isSpectator()) {
            return InteractionResult.PASS;
        }
        if (!player.getItemInHand(hand).is(MIRROR_OF_THE_ANGEL)
            || (!(entity instanceof Fireball) && !(entity instanceof WitherSkull))) {
            return InteractionResult.PASS;
        }
        if (world.isClientSide()) {
            return InteractionResult.SUCCESS;
        }
        deflectProjectile((AbstractHurtingProjectile) entity);
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
            boolean equipped = AuraAccessoryBridgeRegistry.equip(player, stack, AuraAccessorySlot.RING);
            showStatus(player, Component.translatable(equipped
                ? "message.aura.ring_of_binding_equipped" : "message.aura.accessory.full"));
            return InteractionResult.SUCCESS;
        }

        showStatus(player, Component.translatable("message.aura.ring_of_binding_count", RingOfBindingItem.boundFairyCount(stack)));
        return InteractionResult.SUCCESS;
    }

    private static InteractionResult handleAngelWingUse(Player player, Level world, ItemStack stack) {
        return handlePassiveAccessoryUse(player, world, stack, AuraAccessorySlot.AMULET);
    }

    public static void activateAngelWing(ServerPlayer player) {
        if (!player.isAlive() || player.isSpectator() || !isAccessoryEquipped(player, AMULET_OF_THE_ANGELS_WING)) {
            return;
        }

        BlockPos standingSpot = findStandingSpot(player, player.getXRot() > 45.0F ? -1 : 1);
        if (standingSpot == null) {
            showStatus(player, "No clear space found");
            return;
        }

        player.teleportTo(standingSpot.getX() + 0.5D, standingSpot.getY(), standingSpot.getZ() + 0.5D);
        player.fallDistance = 0.0F;
        showStatus(player, player.getXRot() > 45.0F ? "Descended" : "Ascended");
    }

    private static InteractionResult handleMirrorUse(Player player, Level world) {
        List<AbstractHurtingProjectile> projectiles = world.getEntitiesOfClass(
            AbstractHurtingProjectile.class,
            player.getBoundingBox().inflate(6.0D),
            projectile -> projectile instanceof Fireball || projectile instanceof WitherSkull
        );
        if (projectiles.isEmpty()) {
            return InteractionResult.PASS;
        }
        if (world.isClientSide()) {
            return InteractionResult.SUCCESS;
        }

        for (AbstractHurtingProjectile projectile : projectiles) {
            if (projectile.distanceToSqr(player) <= 25.0D) {
                deflectProjectile(projectile);
            }
        }
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
            boolean placed = AuraAccessoryBridgeRegistry.equip(player, stack, slot);
            showStatus(player, Component.translatable(placed
                ? "message.aura.accessory.equipped" : "message.aura.accessory.full"));
            return InteractionResult.SUCCESS;
        }
        return InteractionResult.PASS;
    }

    private static void deflectProjectile(AbstractHurtingProjectile projectile) {
        if (projectile.level().isClientSide() || projectile instanceof WitherSkull) {
            return;
        }

        Fireball target = projectile.level().getEntitiesOfClass(
            Fireball.class,
            projectile.getBoundingBox().inflate(100.0D),
            candidate -> candidate.getOwner() instanceof Blaze || candidate.getOwner() instanceof Ghast
        ).stream().findFirst().orElse(null);
        if (target == null) {
            return;
        }

        projectile.setDeltaMovement(mirrorRedirectVelocity(projectile.position(), target.position()));
        projectile.accelerationPower = 0.3D;
    }

    static Vec3 mirrorRedirectVelocity(Vec3 projectilePosition, Vec3 targetPosition) {
        return targetPosition.subtract(projectilePosition).scale(1.0D / 15.0D);
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
        if (source.is(DamageTypes.IN_FIRE) || source.is(DamageTypes.LAVA) || source.is(DamageTypes.ON_FIRE)) {
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

    static boolean shouldBlockShatteredStoneExplosionDamage(ProtectionAmuletProfile.DamageFamily family, boolean ringEquipped) {
        return RingOfShatteredStoneRuntime.blocksExplosionDamage(family, ringEquipped);
    }

    public static List<BlockPos> filterShatteredStoneExplosionBlocks(ServerLevel level, Vec3 center, List<BlockPos> affectedBlocks) {
        return ShatteredStoneExplosionSeam.filterBlocks(liveShatteredStoneExplosionAccess(level), center, affectedBlocks);
    }

    private static ShatteredStoneExplosionAccess liveShatteredStoneExplosionAccess(ServerLevel level) {
        return new ShatteredStoneExplosionAccess() {
            @Override
            public BlockState getBlockState(BlockPos pos) {
                return level.getBlockState(pos);
            }

            @Override
            public List<Vec3> protectedWearerPositions(AABB searchBounds) {
                return level.getEntitiesOfClass(
                    Player.class,
                    searchBounds,
                    player -> isAccessoryEquipped(player, RING_OF_SHATTERED_STONE)
                ).stream().map(Player::position).toList();
            }
        };
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

    private static void showStatus(Player player, String text) {
        showStatus(player, Component.literal(text));
    }

    private static void showStatus(Player player, Component message) {
        player.displayClientMessage(message, true);
    }

}

final class ShatteredStoneExplosionFilter {
    private ShatteredStoneExplosionFilter() {
    }

    static List<BlockPos> filterBlocks(
        List<Vec3> protectedWearerPositions,
        List<BlockPos> affectedBlocks,
        java.util.function.Function<BlockPos, BlockState> stateLookup
    ) {
        return RingOfShatteredStoneRuntime.filterProtectedExplosionBlocks(protectedWearerPositions, affectedBlocks, stateLookup);
    }
}

interface ShatteredStoneExplosionAccess {
    BlockState getBlockState(BlockPos pos);

    List<Vec3> protectedWearerPositions(AABB searchBounds);
}

final class ShatteredStoneExplosionSeam {
    private ShatteredStoneExplosionSeam() {
    }

    static List<BlockPos> filterBlocks(
        ShatteredStoneExplosionAccess access,
        Vec3 center,
        List<BlockPos> affectedBlocks
    ) {
        return ShatteredStoneExplosionFilter.filterBlocks(
            access.protectedWearerPositions(RingOfShatteredStoneItem.explosionSearchBounds(center)),
            affectedBlocks,
            access::getBlockState
        );
    }
}
