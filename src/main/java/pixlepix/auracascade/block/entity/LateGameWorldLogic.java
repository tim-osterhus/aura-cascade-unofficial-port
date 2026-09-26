package pixlepix.auracascade.block.entity;

import java.util.List;
import java.util.LinkedHashSet;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;
import net.minecraft.util.RandomSource;
import net.minecraft.util.random.WeightedRandomList;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.level.biome.MobSpawnSettings;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import pixlepix.auracascade.block.FortifiedBlockVariant;
import pixlepix.auracascade.block.LateGameVariant;

public final class LateGameWorldLogic {
    private static final TagKey<Item> ITEM_ORES = TagKey.create(Registries.ITEM,
        ResourceLocation.fromNamespaceAndPath("c", "ores"));
    private static final TagKey<Block> BLOCK_ORES = TagKey.create(Registries.BLOCK,
        ResourceLocation.fromNamespaceAndPath("c", "ores"));
    private static final int TAGGED_ORE_WEIGHT = 1_000;
    private static final List<WeightedItem> ORE_TABLE = List.of(
        weighted(Blocks.COAL_ORE, 46_525),
        weighted(Blocks.IRON_ORE, 20_665),
        weighted(Blocks.COPPER_ORE, 8_325),
        weighted(Blocks.REDSTONE_ORE, 6_885),
        weighted(Blocks.GOLD_ORE, 2_970),
        weighted(Blocks.LAPIS_ORE, 1_285),
        weighted(Blocks.DIAMOND_ORE, 1_265),
        weighted(Blocks.EMERALD_ORE, 780),
        weighted(Blocks.NETHER_QUARTZ_ORE, 19_600)
    );

    private LateGameWorldLogic() {
    }

    public static int powerCostForStep(int basePower, int stepIndex) {
        return AuraConsumerLogic.powerCostForStep(basePower, stepIndex);
    }

    public static int progressStepsForTick(int storedPower, int basePower) {
        return AuraConsumerLogic.progressStepsForTick(storedPower, basePower);
    }

    public static ItemStack chooseOre(RandomSource random) {
        return chooseOre(random, List.of());
    }

    public static ItemStack chooseOre(RandomSource random, List<Item> taggedOres) {
        List<WeightedItem> options = new java.util.ArrayList<>(ORE_TABLE);
        LinkedHashSet<Item> extras = new LinkedHashSet<>(taggedOres);
        ORE_TABLE.forEach(option -> extras.remove(option.stack().getItem()));
        extras.remove(Items.AIR);
        extras.forEach(item -> options.add(new WeightedItem(new ItemStack(item), TAGGED_ORE_WEIGHT)));
        int total = options.stream().mapToInt(WeightedItem::weight).sum();
        int roll = random.nextInt(total);
        for (WeightedItem option : options) {
            roll -= option.weight();
            if (roll < 0) {
                return option.stack().copy();
            }
        }
        throw new IllegalStateException("Ore table has no positive weight");
    }

    public static List<Item> taggedOres() {
        LinkedHashSet<Item> ores = new LinkedHashSet<>();
        BuiltInRegistries.ITEM.getTag(ITEM_ORES).ifPresent(tag ->
            tag.forEach(holder -> ores.add(holder.value())));
        BuiltInRegistries.BLOCK.getTag(BLOCK_ORES).ifPresent(tag ->
            tag.forEach(holder -> ores.add(holder.value().asItem())));
        ores.remove(Items.AIR);
        return List.copyOf(ores);
    }

    public static ItemStack chooseGeneratedLoot(List<ItemStack> generated, RandomSource random) {
        List<ItemStack> nonempty = generated.stream().filter(stack -> !stack.isEmpty()).toList();
        return nonempty.isEmpty() ? ItemStack.EMPTY : nonempty.get(random.nextInt(nonempty.size())).copy();
    }

    public static int minerOreYield(int charge) {
        if (charge <= 20) {
            return 0;
        }
        return (int) Math.floor(Math.pow(charge, 1.5D) / 50.0D);
    }

    public static boolean minerContainmentFails(int fortifiedBlockCount) {
        return fortifiedBlockCount <= 0;
    }

    public static int minerExplosionCooldown(int charge) {
        return (int) Math.max(0.0D, 100.0D - 20.0D * Math.log10(Math.max(1, charge)));
    }

    public static double minerBounceAmplitude(int charge) {
        return 0.25D * (1.0D + Math.log10(Math.max(1, charge)));
    }

    public static boolean shouldDamageContainment(FortifiedBlockVariant variant, RandomSource random) {
        return random.nextDouble() > variant.resistChance();
    }

    public static Block ritualMapping(LateGameVariant variant, Block block, RandomSource random) {
        if (variant == LateGameVariant.RITUAL_NETHER) {
            if (block == Blocks.STONE) {
                return Blocks.NETHERRACK;
            }
            if (block == Blocks.DIRT || block == Blocks.GRASS_BLOCK) {
                return random.nextInt(3) == 0 ? Blocks.SOUL_SAND : Blocks.NETHERRACK;
            }
            if (block == Blocks.OAK_LOG || block == Blocks.SPRUCE_LOG || block == Blocks.BIRCH_LOG
                || block == Blocks.JUNGLE_LOG || block == Blocks.ACACIA_LOG || block == Blocks.DARK_OAK_LOG) {
                return Blocks.GLOWSTONE;
            }
            if (block == Blocks.OAK_LEAVES || block == Blocks.SPRUCE_LEAVES || block == Blocks.BIRCH_LEAVES
                || block == Blocks.JUNGLE_LEAVES || block == Blocks.ACACIA_LEAVES || block == Blocks.DARK_OAK_LEAVES) {
                return Blocks.GLOWSTONE;
            }
            if (block == Blocks.SHORT_GRASS || block == Blocks.FERN || block == Blocks.DEAD_BUSH) {
                return Blocks.NETHER_WART;
            }
            if (block == Blocks.SAND || block == Blocks.GRAVEL) {
                return Blocks.SOUL_SAND;
            }
            if (block == Blocks.WATER) {
                return Blocks.LAVA;
            }
            if (block == Blocks.ICE || block == Blocks.SNOW) {
                return Blocks.AIR;
            }
            return null;
        }

        if (variant != LateGameVariant.RITUAL_END) {
            return null;
        }
        if (block == Blocks.STONE || block == Blocks.DIRT || block == Blocks.GRASS_BLOCK) {
            return Blocks.END_STONE;
        }
        if (block == Blocks.OAK_LOG || block == Blocks.SPRUCE_LOG || block == Blocks.BIRCH_LOG
            || block == Blocks.JUNGLE_LOG || block == Blocks.ACACIA_LOG || block == Blocks.DARK_OAK_LOG) {
            return Blocks.OBSIDIAN;
        }
        if (block == Blocks.OAK_LEAVES || block == Blocks.SPRUCE_LEAVES || block == Blocks.BIRCH_LEAVES
            || block == Blocks.JUNGLE_LEAVES || block == Blocks.ACACIA_LEAVES || block == Blocks.DARK_OAK_LEAVES) {
            return Blocks.OBSIDIAN;
        }
        if (block == Blocks.SHORT_GRASS || block == Blocks.FERN || block == Blocks.DEAD_BUSH) {
            return Blocks.AIR;
        }
        if (block == Blocks.SAND || block == Blocks.GRAVEL) {
            return Blocks.END_STONE;
        }
        if (block == Blocks.ICE || block == Blocks.SNOW) {
            return Blocks.AIR;
        }
        return null;
    }

    public static EntityType<?> chooseSpawnType(
        WeightedRandomList<MobSpawnSettings.SpawnerData> naturalSpawns,
        RandomSource random
    ) {
        return naturalSpawns.getRandom(random).map(entry -> entry.type).orElse(null);
    }

    public static boolean withinRitualRadius(int deltaX, int deltaZ) {
        return (long) deltaX * deltaX + (long) deltaZ * deltaZ <= 22_500L;
    }

    public static BlockPos ritualCellOrigin(BlockPos pos) {
        return new BlockPos(pos.getX() & ~3, pos.getY(), pos.getZ() & ~3);
    }

    public static boolean ritualCellIntersectsRadius(BlockPos cell, BlockPos origin) {
        int nearestX = Math.max(cell.getX(), Math.min(origin.getX(), cell.getX() + 3));
        int nearestZ = Math.max(cell.getZ(), Math.min(origin.getZ(), cell.getZ() + 3));
        return withinRitualRadius(nearestX - origin.getX(), nearestZ - origin.getZ());
    }

    private static WeightedItem weighted(Block block, int weight) {
        return new WeightedItem(new ItemStack(block), weight);
    }

    private record WeightedItem(ItemStack stack, int weight) {
    }
}
