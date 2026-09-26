package pixlepix.auracascade.qa.multiplayer;

import com.google.gson.GsonBuilder;
import com.google.gson.JsonObject;
import java.lang.reflect.Method;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.security.MessageDigest;
import java.util.HexFormat;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.fabricmc.loader.api.FabricLoader;
import net.fabricmc.loader.api.metadata.ModOrigin;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import pixlepix.auracascade.item.ConsumerItemKeepAlive;
import pixlepix.auracascade.item.ConsumerItemLifetimeAccess;

/** Opt-in checks on detached entities in a real, transformed server runtime. */
public final class ConsumerLifetimeProbe implements ModInitializer {
    private static final String PREFIX = "aura.qa.consumerLifetimeProbe";

    @Override
    public void onInitialize() {
        if (!Boolean.getBoolean(PREFIX)) return;
        Path output = Path.of(System.getProperty(PREFIX + ".output")).toAbsolutePath();
        if (Files.exists(output)) throw new IllegalStateException("Use a fresh lifetime probe report path");
        ServerLifecycleEvents.SERVER_STARTED.register(server -> {
            JsonObject report = new JsonObject();
            report.addProperty("success", false);
            try {
                packagedOrigin(report);
                ServerLevel level = server.overworld();
                require(item(level, 1) instanceof ConsumerItemLifetimeAccess, "ItemEntity is not transformed");
                report.addProperty("itemEntityTransformed", true);
                boolean passed = check(report, "ageAndPersistence", () -> ageAndPersistence(level));
                passed &= check(report, "merge", () -> merge(level));
                passed &= check(report, "oldMergeEligibility", () -> oldMergeEligibility(level));
                report.addProperty("success", passed);
            } catch (Exception | AssertionError error) {
                report.addProperty("error", error.toString());
            }
            try {
                Files.createDirectories(output.getParent());
                Files.writeString(output, new GsonBuilder().setPrettyPrinting().create().toJson(report),
                    StandardOpenOption.CREATE_NEW, StandardOpenOption.WRITE);
            } catch (Exception error) {
                throw new IllegalStateException("Cannot write lifetime probe result", error);
            }
        });
    }

    private static void packagedOrigin(JsonObject report) throws Exception {
        var loader = FabricLoader.getInstance();
        String namespace = loader.getMappingResolver().getCurrentRuntimeNamespace();
        report.addProperty("runtimeNamespace", namespace);
        require("intermediary".equals(namespace), "Packaged intermediary runtime required");
        var origin = loader.getModContainer("aura").orElseThrow().getOrigin();
        require(origin.getKind() == ModOrigin.Kind.PATH && origin.getPaths().size() == 1,
            "Expected one Aura origin JAR");
        Path jar = origin.getPaths().getFirst().toAbsolutePath();
        require(Files.isRegularFile(jar) && jar.toString().endsWith(".jar"), "Aura is not a JAR");
        MessageDigest digest = MessageDigest.getInstance("SHA-256");
        try (var input = Files.newInputStream(jar)) {
            byte[] buffer = new byte[8192];
            int length;
            while ((length = input.read(buffer)) != -1) digest.update(buffer, 0, length);
        }
        String hash = HexFormat.of().formatHex(digest.digest());
        report.addProperty("auraOrigin", jar.toString());
        report.addProperty("auraSha256", hash);
        require(hash.equalsIgnoreCase(System.getProperty(PREFIX + ".sha256", "")), "Candidate hash mismatch");
    }

    private static ItemEntity item(ServerLevel level, int count) {
        return new ItemEntity(level, 0, 0, 0, new ItemStack(Items.STONE, count));
    }

    private static ConsumerItemLifetimeAccess lifetime(ItemEntity item) {
        return (ConsumerItemLifetimeAccess) item;
    }

    private static CompoundTag save(ItemEntity item) {
        CompoundTag tag = new CompoundTag();
        item.addAdditionalSaveData(tag);
        return tag;
    }

    private static void ageAndPersistence(ServerLevel level) {
        ItemEntity item = item(level, 1);
        require(!lifetime(item).aura$isConsumerKeptAlive(), "New entity already protected");
        item.setExtendedLifetime();
        require(item.getAge() == -6_000, "Unexpected vanilla extended age");
        lifetime(item).aura$keepAlive();
        require(lifetime(item).aura$isConsumerKeptAlive() && item.getAge() == 0, "Keepalive did not reset age");
        CompoundTag saved = save(item);
        require(saved.getBoolean(ConsumerItemKeepAlive.PROTECTED_TAG), "Protection not saved");
        require(saved.getInt(ConsumerItemKeepAlive.AGE_TAG) == 0, "Reset age not saved");
        saved.putInt(ConsumerItemKeepAlive.AGE_TAG, 70_000);
        ItemEntity restored = item(level, 1);
        restored.readAdditionalSaveData(saved);
        require(lifetime(restored).aura$isConsumerKeptAlive() && restored.getAge() == 70_000,
            "Full-width age/protection not restored");
        require(save(restored).getInt(ConsumerItemKeepAlive.AGE_TAG) == 70_000, "Full-width age not resaved");
        require(ConsumerItemKeepAlive.effectiveLifetime(restored, 30_000) == Integer.MAX_VALUE,
            "Protection did not override Red Hole lifetime");
        CompoundTag ordinary = save(item(level, 1));
        ordinary.putShort("Age", (short) 123);
        restored.readAdditionalSaveData(ordinary);
        require(!lifetime(restored).aura$isConsumerKeptAlive() && restored.getAge() == 123,
            "Ordinary NBT retained stale protection or age");
        require(ConsumerItemKeepAlive.effectiveLifetime(restored, 6_000) == 6_000, "Ordinary lifetime changed");
    }

    private static void merge(ServerLevel level) throws Exception {
        ItemEntity target = item(level, 2);
        ItemEntity source = item(level, 1);
        target.setExtendedLifetime();
        lifetime(source).aura$keepAlive();
        Method merge = method("method_18006", "(Lnet/minecraft/class_1542;Lnet/minecraft/class_1799;"
            + "Lnet/minecraft/class_1542;Lnet/minecraft/class_1799;)V",
            ItemEntity.class, ItemStack.class, ItemEntity.class, ItemStack.class);
        merge.invoke(null, target, target.getItem(), source, source.getItem());
        require(target.getItem().getCount() == 3 && source.isRemoved(), "Vanilla merge/counts failed");
        require(lifetime(target).aura$isConsumerKeptAlive(), "Merge lost protection");
        require(target.getAge() == -6_000, "Merge reset vanilla minimum age");
    }

    private static void oldMergeEligibility(ServerLevel level) throws Exception {
        ItemEntity item = item(level, 1);
        CompoundTag saved = save(item);
        saved.putShort("Age", (short) 6_000);
        item.readAdditionalSaveData(saved);
        Method mergable = method("method_20397", "()Z");
        require(Boolean.FALSE.equals(mergable.invoke(item)), "Ordinary old item unexpectedly mergeable");
        lifetime(item).aura$extendConsumerLifetime();
        require(Boolean.TRUE.equals(mergable.invoke(item)), "Protected old item not mergeable");
        require(item.getAge() == 6_000, "Extending protection reset age");
    }

    // 1.21.1 intermediary mappings, not named-dev reflection strings.
    private static Method method(String intermediary, String descriptor, Class<?>... parameters) throws Exception {
        String name = FabricLoader.getInstance().getMappingResolver().mapMethodName("intermediary",
            "net.minecraft.class_1542", intermediary, descriptor);
        Method method = ItemEntity.class.getDeclaredMethod(name, parameters);
        method.setAccessible(true);
        return method;
    }

    private static boolean check(JsonObject report, String name, Check check) {
        try {
            check.run();
            report.addProperty(name, "PASS");
            return true;
        } catch (Exception | AssertionError error) {
            report.addProperty(name, "FAIL: " + error);
            return false;
        }
    }

    private static void require(boolean condition, String message) {
        if (!condition) throw new AssertionError(message);
    }

    @FunctionalInterface
    private interface Check {
        void run() throws Exception;
    }
}
