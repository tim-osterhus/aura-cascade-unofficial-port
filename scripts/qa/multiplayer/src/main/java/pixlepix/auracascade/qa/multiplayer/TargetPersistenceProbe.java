package pixlepix.auracascade.qa.multiplayer;

import com.google.gson.GsonBuilder;
import com.google.gson.JsonObject;
import com.mojang.authlib.GameProfile;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.security.MessageDigest;
import java.util.HexFormat;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.fabricmc.loader.api.FabricLoader;
import net.fabricmc.loader.api.metadata.ModOrigin;
import net.minecraft.core.BlockPos;
import net.minecraft.core.component.DataComponents;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ClientInformation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.tags.BlockTags;
import net.minecraft.util.ProblemReporter;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EquipmentSlotGroup;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.ToolMaterial;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.item.component.ItemAttributeModifiers;
import net.minecraft.world.item.component.Tool;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.storage.TagValueInput;
import net.minecraft.world.level.storage.TagValueOutput;
import pixlepix.auracascade.aura.AuraEnvironment;
import pixlepix.auracascade.aura.AuraStorage;
import pixlepix.auracascade.block.AuraContent;
import pixlepix.auracascade.block.entity.MinerExplosionEntities;
import pixlepix.auracascade.block.entity.MinerExplosionEntity;
import pixlepix.auracascade.block.entity.AuraNodeBlockEntity;
import pixlepix.auracascade.block.entity.StorageBookshelfBlockEntity;
import pixlepix.auracascade.block.entity.VortexPedestalBlockEntity;
import pixlepix.auracascade.compat.AuraAccessoryInventory;
import pixlepix.auracascade.fairy.AuraFairyEntity;
import pixlepix.auracascade.fairy.AuraFairyEntityRegistry;
import pixlepix.auracascade.fairy.FairyRole;
import pixlepix.auracascade.item.AuraItems;
import pixlepix.auracascade.item.AngelsteelToolHelper;
import pixlepix.auracascade.item.AngelsteelToolKind;
import pixlepix.auracascade.item.RingOfBindingItem;
import pixlepix.auracascade.item.books.StorageBookData;
import pixlepix.auracascade.item.books.StorageBookVariant;
import pixlepix.auracascade.parity.AuraColor;

/** Native persistence and registered-item/entity contracts on the candidate packaged dedicated server. */
public final class TargetPersistenceProbe implements ModInitializer {
    private static final String PREFIX = "aura.qa.targetPersistence";
    private static final String SHELF_BOOK_TAG = "stored_book";
    private static final String BOOK_NAME = "target persistence book";
    private static final String BOOK_MARKER = "book-probe";
    private static final String PAYLOAD_NAME = "component-rich payload";
    private static final String PAYLOAD_MARKER = "payload-probe";
    private static final UUID PLAYER_ID = UUID.fromString("12345678-1234-5678-9abc-def012345678");
    private static final UUID FAIRY_OWNER = UUID.fromString("abcdef01-2345-6789-abcd-ef0123456789");

    @Override
    public void onInitialize() {
        if (!Boolean.getBoolean(PREFIX)) {
            return;
        }

        String configuredOutput = System.getProperty(PREFIX + ".output", "");
        if (configuredOutput.isBlank()) {
            failToConsole("reportPath: property " + PREFIX + ".output must name a fresh report file");
            return;
        }

        Path output;
        try {
            output = Path.of(configuredOutput).toAbsolutePath().normalize();
        } catch (RuntimeException error) {
            failToConsole("reportPath: invalid " + PREFIX + ".output: " + error);
            return;
        }

        ServerLifecycleEvents.SERVER_STARTED.register(server -> run(server, output));
    }

    private static void run(MinecraftServer server, Path output) {
        if (Files.exists(output)) {
            failToConsole("freshReportPath: expected an absent path, found " + output);
            return;
        }

        JsonObject report = new JsonObject();
        report.addProperty("probe", "targetPersistence");
        report.addProperty("reportPath", output.toString());
        report.addProperty("success", false);
        report.addProperty("scope",
            "Detached ValueOutput/ValueInput round-trips and real target item/entity contracts; no blocks, chunks, spawned entities, or user inventories are changed.");

        try {
            require(server.isDedicatedServer(), "dedicatedServer: expected a dedicated server callback");
            report.addProperty("serverKind", "dedicated");
            packagedOrigin(report);

            JsonObject assertions = new JsonObject();
            ServerLevel level = server.overworld();
            boolean passed = true;
            passed &= check(assertions, "auraNodeStorage", () -> auraNodeStorage(level));
            passed &= check(assertions, "auraNodeLinksAndFlags", () -> auraNodeLinksAndFlags(level));
            passed &= check(assertions, "bookshelfBookComponents", () -> bookshelfBookComponents(level));
            passed &= check(assertions, "bookshelfNestedInventory", () -> bookshelfNestedInventory(level));
            passed &= check(assertions, "vortexPartialReceipt", () -> vortexPartialReceipt(level));
            passed &= check(assertions, "accessoryAttachment", () -> accessoryAttachment(server, level));
            passed &= check(assertions, "ringBoundRoles", () -> ringBoundRoles(server, level));
            passed &= check(assertions, "fairyOwnerSlotRole", () -> fairyOwnerSlotRole(level));
            report.add("assertions", assertions);

            JsonObject targetContracts = new JsonObject();
            boolean contractsPassed = true;
            contractsPassed &= check(targetContracts, "minerRestoreNeedsSync", () -> minerRestoreNeedsSync(level));
            contractsPassed &= check(targetContracts, "registeredAngelsteelPickaxe", TargetPersistenceProbe::registeredAngelsteelPickaxe);
            report.add("targetContracts", targetContracts);
            report.addProperty("success", passed && contractsPassed);
        } catch (Exception | AssertionError error) {
            report.addProperty("probeFailure", describe(error));
        }

        try {
            Files.createDirectories(output.getParent());
            Files.writeString(output, new GsonBuilder().setPrettyPrinting().create().toJson(report),
                StandardOpenOption.CREATE_NEW, StandardOpenOption.WRITE);
        } catch (Exception error) {
            failToConsole("reportWrite: could not create " + output + ": " + error);
        }
    }

    private static void packagedOrigin(JsonObject report) throws Exception {
        var loader = FabricLoader.getInstance();
        String namespace = loader.getMappingResolver().getCurrentRuntimeNamespace();
        require("intermediary".equals(namespace),
            "runtimeNamespace: expected intermediary packaged runtime, got " + namespace);
        var aura = loader.getModContainer("aura").orElseThrow(
            () -> new AssertionError("candidateOrigin: Aura mod container is absent"));
        ModOrigin origin = aura.getOrigin();
        require(origin.getKind() == ModOrigin.Kind.PATH && origin.getPaths().size() == 1,
            "candidateOrigin: expected exactly one path origin, got " + origin.getKind() + " / " + origin.getPaths().size());
        Path jar = origin.getPaths().getFirst().toAbsolutePath().normalize();
        require(Files.isRegularFile(jar) && jar.toString().endsWith(".jar"),
            "candidateOrigin: expected one Aura JAR, got " + jar);

        MessageDigest digest = MessageDigest.getInstance("SHA-256");
        try (var input = Files.newInputStream(jar)) {
            byte[] buffer = new byte[8192];
            int length;
            while ((length = input.read(buffer)) != -1) {
                digest.update(buffer, 0, length);
            }
        }
        String hash = HexFormat.of().formatHex(digest.digest());
        String version = aura.getMetadata().getVersion().getFriendlyString();
        report.addProperty("runtimeNamespace", namespace);
        report.addProperty("auraOrigin", jar.toString());
        report.addProperty("auraSha256", hash);
        report.addProperty("auraVersion", version);
        require(hash.equalsIgnoreCase(System.getProperty(PREFIX + ".sha256", "")),
            "candidateSha256: loaded " + hash + " does not match " + PREFIX + ".sha256");
        String expectedVersion = System.getProperty(PREFIX + ".version", "");
        if (!expectedVersion.isBlank()) {
            report.addProperty("expectedAuraVersion", expectedVersion);
            require(expectedVersion.equals(version),
                "candidateVersion: expected " + expectedVersion + ", loaded " + version);
        }
    }

    private static void auraNodeStorage(ServerLevel level) throws Exception {
        ProbeNode restored = roundTripNode(level);
        AuraStorage expected = new AuraStorage();
        expected.set(AuraColor.RED, 417);
        expected.set(AuraColor.BLUE, 29);
        requireEquals(expected, restored.storageForProbe(), "node storage by color");
    }

    private static void auraNodeLinksAndFlags(ServerLevel level) throws Exception {
        ProbeNode restored = roundTripNode(level);
        Set<BlockPos> expected = Set.of(new BlockPos(12, 64, -7), new BlockPos(-3, 80, 21));
        String actual = "links=" + restored.linksForProbe() + ", scanned=" + restored.scannedForProbe()
            + ", storedPower=" + restored.powerForProbe();
        require(expected.equals(restored.linksForProbe()) && restored.scannedForProbe() && restored.powerForProbe() == 91,
            "node link state: expected links=" + expected + ", scanned=true, storedPower=91; got " + actual);
    }

    private static ProbeNode roundTripNode(ServerLevel level) throws Exception {
        ProbeNode original = new ProbeNode();
        original.seed();
        CompoundTag saved = original.saveWithoutMetadata(level.registryAccess());
        ProbeNode restored = new ProbeNode();
        loadBlockEntity(restored, saved, level);
        return restored;
    }

    private static void bookshelfBookComponents(ServerLevel level) throws Exception {
        ItemStack book = roundTripBookshelfBook(level);
        String actual = "item=" + book.getItem() + ", name=" + componentString(book, DataComponents.CUSTOM_NAME)
            + ", marker=" + customString(book, BOOK_MARKER);
        require(book.getItem() == AuraItems.storageBook(StorageBookVariant.BASIC)
                && BOOK_NAME.equals(componentString(book, DataComponents.CUSTOM_NAME))
                && "outer-state".equals(customString(book, BOOK_MARKER)),
            "bookshelf book components: expected registered basic book with its custom name/data; got " + actual);
    }

    private static void bookshelfNestedInventory(ServerLevel level) throws Exception {
        ItemStack book = roundTripBookshelfBook(level);
        List<StorageBookData.Entry> entries = StorageBookData.detailedEntries(book);
        String actual = entries.isEmpty() ? "entries=[]" : describeEntry(entries.get(0));
        require(entries.size() == 1 && entries.get(0).count() == 1
                && entries.get(0).stack().is(Items.DIAMOND_SWORD)
                && PAYLOAD_NAME.equals(componentString(entries.get(0).stack(), DataComponents.CUSTOM_NAME))
                && entries.get(0).stack().getOrDefault(DataComponents.DAMAGE, 0) == 13
                && "nested-state".equals(customString(entries.get(0).stack(), PAYLOAD_MARKER)),
            "bookshelf nested inventory: expected one damaged, named sword with custom data; got " + actual);
    }

    private static ItemStack roundTripBookshelfBook(ServerLevel level) throws Exception {
        StorageBookVariant variant = StorageBookVariant.BASIC;
        ItemStack book = new ItemStack(AuraItems.storageBook(variant));
        book.set(DataComponents.CUSTOM_NAME, Component.literal(BOOK_NAME));
        CustomData.update(DataComponents.CUSTOM_DATA, book, tag -> tag.putString(BOOK_MARKER, "outer-state"));

        ItemStack payload = new ItemStack(Items.DIAMOND_SWORD);
        payload.set(DataComponents.CUSTOM_NAME, Component.literal(PAYLOAD_NAME));
        payload.set(DataComponents.DAMAGE, 13);
        CustomData.update(DataComponents.CUSTOM_DATA, payload, tag -> tag.putString(PAYLOAD_MARKER, "nested-state"));
        requireEquals(1, StorageBookData.insert(book, variant, payload), "seed one nested sword");

        TagValueOutput seed = TagValueOutput.createWithContext(ProblemReporter.DISCARDING, level.registryAccess());
        seed.store(SHELF_BOOK_TAG, ItemStack.OPTIONAL_CODEC, book);
        StorageBookshelfBlockEntity source = new StorageBookshelfBlockEntity(
            BlockPos.ZERO, AuraContent.STORAGE_BOOKSHELF.defaultBlockState());
        loadBlockEntity(source, seed.buildResult(), level);

        CompoundTag saved = source.saveWithoutMetadata(level.registryAccess());
        StorageBookshelfBlockEntity restored = new StorageBookshelfBlockEntity(
            BlockPos.ZERO, AuraContent.STORAGE_BOOKSHELF.defaultBlockState());
        loadBlockEntity(restored, saved, level);
        return restored.storedBook();
    }

    private static void vortexPartialReceipt(ServerLevel level) throws Exception {
        VortexPedestalBlockEntity original = new VortexPedestalBlockEntity(
            BlockPos.ZERO, AuraContent.VORTEX_PEDESTAL.defaultBlockState());
        original.exchangeHeldItem(new ItemStack(Items.EMERALD));
        original.setRequirement(AuraColor.RED, 20);
        original.receiveFallingPower(AuraStorage.of(AuraColor.RED, 3), 2, AuraEnvironment.CLEAR_DAY);

        CompoundTag saved = original.saveWithoutMetadata(level.registryAccess());
        VortexPedestalBlockEntity restored = new VortexPedestalBlockEntity(
            BlockPos.ZERO, AuraContent.VORTEX_PEDESTAL.defaultBlockState());
        loadBlockEntity(restored, saved, level);

        VortexPedestalBlockEntity.Receipt expected = new VortexPedestalBlockEntity.Receipt(6, 20, AuraColor.RED);
        VortexPedestalBlockEntity.Receipt actual = restored.inspectionSnapshot();
        requireEquals(expected, actual, "vortex partial receipt");
        require(restored.heldItem().is(Items.EMERALD),
            "vortex held item: expected emerald alongside partial receipt, got " + restored.heldItem());
    }

    private static void accessoryAttachment(MinecraftServer server, ServerLevel level) throws Exception {
        ServerPlayer restored = roundTripPlayer(server, level);
        ItemStack ring = AuraAccessoryInventory.get(restored, AuraAccessoryInventory.FIRST_RING);
        require(ring.is(AuraItems.RING_OF_BINDING)
                && AuraAccessoryInventory.get(restored, AuraAccessoryInventory.SECOND_RING).isEmpty(),
            "persistent accessory attachment: expected binding ring in first ring slot only; got first="
                + ring + ", second=" + AuraAccessoryInventory.get(restored, AuraAccessoryInventory.SECOND_RING));
    }

    private static void ringBoundRoles(MinecraftServer server, ServerLevel level) throws Exception {
        ServerPlayer restored = roundTripPlayer(server, level);
        ItemStack ring = AuraAccessoryInventory.get(restored, AuraAccessoryInventory.FIRST_RING);
        List<FairyRole> expected = List.of(FairyRole.LIGHTER, FairyRole.SHOOTER);
        requireEquals(expected, RingOfBindingItem.boundFairies(ring), "ring's persisted bound fairy roles");
    }

    private static ServerPlayer roundTripPlayer(MinecraftServer server, ServerLevel level) throws Exception {
        GameProfile profile = new GameProfile(PLAYER_ID, "AuraPersistQA");
        ServerPlayer original = new ServerPlayer(server, level, profile, ClientInformation.createDefault());
        ItemStack ring = new ItemStack(AuraItems.RING_OF_BINDING);
        RingOfBindingItem.bindCharm(ring, FairyRole.LIGHTER);
        RingOfBindingItem.bindCharm(ring, FairyRole.SHOOTER);
        AuraAccessoryInventory.set(original, AuraAccessoryInventory.FIRST_RING, ring);

        CompoundTag saved = saveEntity(original, level);
        ServerPlayer restored = new ServerPlayer(server, level, profile, ClientInformation.createDefault());
        restored.load(TagValueInput.create(ProblemReporter.DISCARDING, level.registryAccess(), saved));
        return restored;
    }

    private static void fairyOwnerSlotRole(ServerLevel level) throws Exception {
        AuraFairyEntity original = AuraFairyEntityRegistry.entityType().create(level, EntitySpawnReason.LOAD);
        require(original != null, "fairy factory returned null for source");
        original.setFairyData(FAIRY_OWNER, 14, FairyRole.LIGHTER);
        CompoundTag saved = saveEntity(original, level);

        AuraFairyEntity restored = AuraFairyEntityRegistry.entityType().create(level, EntitySpawnReason.LOAD);
        require(restored != null, "fairy factory returned null for restored entity");
        restored.load(TagValueInput.create(ProblemReporter.DISCARDING, level.registryAccess(), saved));
        String actual = "owner=" + restored.ownerId() + ", slot=" + restored.slot() + ", role=" + restored.role();
        require(FAIRY_OWNER.equals(restored.ownerId()) && restored.slot() == 14 && restored.role() == FairyRole.LIGHTER,
            "fairy persisted state: expected owner=" + FAIRY_OWNER + ", slot=14, role=LIGHTER; got " + actual);
    }

    private static void minerRestoreNeedsSync(ServerLevel level) {
        MinerExplosionEntity miner = MinerExplosionEntities.type().create(level, EntitySpawnReason.LOAD);
        require(miner != null, "miner factory returned null");
        miner.needsSync = false;
        miner.restore(new BlockPos(5, 70, -9), 4, 123L, 45L);
        require(miner.needsSync,
            "MinerExplosionEntity.restore must mark its changed motion for synchronization; needsSync=" + miner.needsSync);
    }

    private static void registeredAngelsteelPickaxe() {
        int degreeIndex = 3;
        ItemStack stack = new ItemStack(AuraItems.angelsteelTool(AngelsteelToolKind.PICKAXE, degreeIndex));
        Tool tool = stack.get(DataComponents.TOOL);
        require(tool != null, "registered Angelsteel pickaxe has no native TOOL component");

        ToolMaterial material = AngelsteelToolHelper.material(degreeIndex);
        var stone = Blocks.STONE.defaultBlockState();
        var oakLog = Blocks.OAK_LOG.defaultBlockState();
        ItemAttributeModifiers attributes = stack.getOrDefault(
            DataComponents.ATTRIBUTE_MODIFIERS, ItemAttributeModifiers.EMPTY);
        double attackDamage = mainHandModifier(attributes, Attributes.ATTACK_DAMAGE);
        double attackSpeed = mainHandModifier(attributes, Attributes.ATTACK_SPEED);

        String actual = "item=" + net.minecraft.core.registries.BuiltInRegistries.ITEM.getKey(stack.getItem())
            + ", maxStack=" + stack.getMaxStackSize() + ", maxDamage=" + stack.getMaxDamage()
            + ", tool(defaultSpeed=" + tool.defaultMiningSpeed() + ", damagePerBlock=" + tool.damagePerBlock()
            + ", creative=" + tool.canDestroyBlocksInCreative() + ", stoneSpeed=" + tool.getMiningSpeed(stone)
            + ", stoneTagged=" + stone.is(BlockTags.MINEABLE_WITH_PICKAXE)
            + ", stoneCorrect=" + stack.isCorrectToolForDrops(stone)
            + ", oakLogTagged=" + oakLog.is(BlockTags.MINEABLE_WITH_PICKAXE)
            + ", oakLogCorrect=" + stack.isCorrectToolForDrops(oakLog) + "), expectedSpeed=" + material.speed()
            + ", attackDamageModifier=" + attackDamage + " (expected " + (material.attackDamageBonus() + 1.0D) + ")"
            + ", attackSpeedModifier=" + attackSpeed + " (expected -2.8)";
        require(stack.getMaxStackSize() == 1 && stack.getMaxDamage() == material.durability()
                && tool.defaultMiningSpeed() == 1.0F && tool.damagePerBlock() == 1
                && tool.canDestroyBlocksInCreative()
                && stone.is(BlockTags.MINEABLE_WITH_PICKAXE)
                && !oakLog.is(BlockTags.MINEABLE_WITH_PICKAXE)
                && Math.abs(tool.getMiningSpeed(stone) - material.speed()) < 0.0001F
                && stack.isCorrectToolForDrops(stone) && !stack.isCorrectToolForDrops(oakLog)
                && Math.abs(attackDamage - (material.attackDamageBonus() + 1.0D)) < 0.0001D
                && Math.abs(attackSpeed - (-2.8D)) < 0.0001D,
            "native Angelsteel pickaxe defaults/attributes/tag behavior mismatch: " + actual);
    }

    private static double mainHandModifier(
        ItemAttributeModifiers attributes,
        net.minecraft.core.Holder<net.minecraft.world.entity.ai.attributes.Attribute> attribute
    ) {
        return attributes.modifiers().stream()
            .filter(entry -> entry.attribute().equals(attribute) && entry.slot().equals(EquipmentSlotGroup.MAINHAND))
            .mapToDouble(entry -> entry.modifier().amount())
            .findFirst()
            .orElse(Double.NaN);
    }

    private static CompoundTag saveEntity(Entity entity, ServerLevel level) {
        TagValueOutput output = TagValueOutput.createWithContext(ProblemReporter.DISCARDING, level.registryAccess());
        entity.saveWithoutId(output);
        return output.buildResult();
    }

    private static void loadBlockEntity(BlockEntity entity, CompoundTag saved, ServerLevel level) throws Exception {
        entity.loadWithComponents(TagValueInput.create(ProblemReporter.DISCARDING, level.registryAccess(), saved));
    }

    private static String componentString(ItemStack stack, net.minecraft.core.component.DataComponentType<Component> type) {
        Component value = stack.get(type);
        return value == null ? "" : value.getString();
    }

    private static String customString(ItemStack stack, String key) {
        CustomData data = stack.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY);
        return data.copyTag().getString(key).orElse("");
    }

    private static String describeEntry(StorageBookData.Entry entry) {
        ItemStack stack = entry.stack();
        return "count=" + entry.count() + ", item=" + stack.getItem() + ", name="
            + componentString(stack, DataComponents.CUSTOM_NAME) + ", damage="
            + stack.getOrDefault(DataComponents.DAMAGE, 0) + ", marker=" + customString(stack, PAYLOAD_MARKER);
    }

    private static boolean check(JsonObject assertions, String name, Check check) {
        try {
            check.run();
            assertions.addProperty(name, "PASS");
            return true;
        } catch (Exception | AssertionError error) {
            assertions.addProperty(name, "FAIL: " + describe(error));
            return false;
        }
    }

    private static void require(boolean condition, String message) {
        if (!condition) {
            throw new AssertionError(message);
        }
    }

    private static void requireEquals(Object expected, Object actual, String assertion) {
        if (!Objects.equals(expected, actual)) {
            throw new AssertionError(assertion + ": expected <" + expected + ">, got <" + actual + ">");
        }
    }

    private static String describe(Throwable error) {
        String message = error.getMessage();
        return error.getClass().getSimpleName() + (message == null ? "" : ": " + message);
    }

    private static void failToConsole(String assertion) {
        System.err.println("[Aura Target Persistence QA] FAIL: " + assertion);
    }

    @FunctionalInterface
    private interface Check {
        void run() throws Exception;
    }

    private static final class ProbeNode extends AuraNodeBlockEntity {
        private ProbeNode() {
            super(BlockPos.ZERO, AuraContent.AURA_NODE.defaultBlockState());
        }

        private void seed() {
            nodeState.storage().set(AuraColor.RED, 417);
            nodeState.storage().set(AuraColor.BLUE, 29);
            nodeState.replaceLinkedNodes(List.of(new BlockPos(12, 64, -7), new BlockPos(-3, 80, 21)));
            nodeState.setHasScannedLinks(true);
            nodeState.setStoredPower(91);
        }

        private AuraStorage storageForProbe() {
            return nodeState.storage().copy();
        }

        private Set<BlockPos> linksForProbe() {
            return nodeState.linkedNodes();
        }

        private boolean scannedForProbe() {
            return nodeState.hasScannedLinks();
        }

        private int powerForProbe() {
            return nodeState.storedPower();
        }
    }
}
