package pixlepix.auracascade.block.entity;

import java.util.List;
import net.minecraft.resources.ResourceKey;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import pixlepix.auracascade.block.FortifiedBlockVariant;
import pixlepix.auracascade.block.LateGameVariant;

public final class LateGameWorldLogic {
    private static final List<WeightedItem> LOOT_TABLE = List.of(
        weighted(Items.IRON_INGOT, 4, 8),
        weighted(Items.GOLD_INGOT, 2, 6),
        weighted(Items.ENDER_PEARL, 1, 3),
        weighted(Items.BLAZE_POWDER, 2, 3),
        weighted(Items.EXPERIENCE_BOTTLE, 1, 2),
        weighted(Items.GOLDEN_APPLE, 1, 1),
        weighted(Items.DIAMOND, 1, 1),
        weighted(Items.ENCHANTED_BOOK, 1, 1)
    );
    private static final List<WeightedItem> ORE_TABLE = List.of(
        weighted(Items.COAL, 2, 8),
        weighted(Items.RAW_IRON, 1, 4),
        weighted(Items.RAW_COPPER, 1, 6),
        weighted(Items.REDSTONE, 4, 10),
        weighted(Items.LAPIS_LAZULI, 2, 5),
        weighted(Items.RAW_GOLD, 1, 3),
        weighted(Items.DIAMOND, 1, 1),
        weighted(Items.EMERALD, 1, 1)
    );
    private static final List<EntityType<? extends Mob>> OVERWORLD_HOSTILES = List.of(
        EntityType.ZOMBIE,
        EntityType.SKELETON,
        EntityType.SPIDER,
        EntityType.CREEPER
    );
    private static final List<EntityType<? extends Mob>> NETHER_HOSTILES = List.of(
        EntityType.BLAZE,
        EntityType.WITHER_SKELETON,
        EntityType.MAGMA_CUBE,
        EntityType.ZOMBIFIED_PIGLIN
    );
    private static final List<EntityType<? extends Mob>> END_HOSTILES = List.of(
        EntityType.ENDERMAN,
        EntityType.ENDERMITE,
        EntityType.SHULKER
    );

    private LateGameWorldLogic() {
    }

    public static int powerCostForStep(int basePower, int stepIndex) {
        return AuraConsumerLogic.powerCostForStep(basePower, stepIndex);
    }

    public static int progressStepsForTick(int storedPower, int basePower) {
        return AuraConsumerLogic.progressStepsForTick(storedPower, basePower);
    }

    public static ItemStack chooseLoot(RandomSource random) {
        return chooseWeighted(random, LOOT_TABLE);
    }

    public static ItemStack chooseOre(RandomSource random) {
        return chooseWeighted(random, ORE_TABLE);
    }

    public static int minerOreYield(int charge) {
        if (charge <= 20) {
            return 0;
        }
        return Math.max(1, (int) Math.floor(Math.pow(charge, 1.5D) / 50.0D));
    }

    public static boolean minerContainmentFails(int fortifiedBlockCount) {
        return fortifiedBlockCount <= 0;
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
            if (block == Blocks.OAK_LOG || block == Blocks.SPRUCE_LOG || block == Blocks.BIRCH_LOG || block == Blocks.JUNGLE_LOG) {
                return Blocks.NETHER_WART_BLOCK;
            }
            if (block == Blocks.OAK_LEAVES || block == Blocks.SPRUCE_LEAVES || block == Blocks.BIRCH_LEAVES || block == Blocks.JUNGLE_LEAVES) {
                return Blocks.CRIMSON_ROOTS;
            }
            if (block == Blocks.SAND || block == Blocks.SANDSTONE) {
                return Blocks.MAGMA_BLOCK;
            }
            if (block == Blocks.WATER) {
                return Blocks.LAVA;
            }
            if (block == Blocks.ICE || block == Blocks.SNOW_BLOCK) {
                return Blocks.AIR;
            }
            return null;
        }

        if (block == Blocks.STONE || block == Blocks.DIRT || block == Blocks.GRASS_BLOCK) {
            return Blocks.END_STONE;
        }
        if (block == Blocks.OAK_LOG || block == Blocks.SPRUCE_LOG || block == Blocks.BIRCH_LOG || block == Blocks.JUNGLE_LOG) {
            return Blocks.OBSIDIAN;
        }
        if (block == Blocks.OAK_LEAVES || block == Blocks.SPRUCE_LEAVES || block == Blocks.BIRCH_LEAVES || block == Blocks.JUNGLE_LEAVES) {
            return Blocks.AIR;
        }
        if (block == Blocks.SAND || block == Blocks.SANDSTONE) {
            return Blocks.PURPUR_BLOCK;
        }
        if (block == Blocks.WATER) {
            return Blocks.OBSIDIAN;
        }
        if (block == Blocks.LAVA) {
            return Blocks.AIR;
        }
        return null;
    }

    public static EntityType<? extends Mob> chooseSpawnType(ResourceKey<Level> dimension, RandomSource random) {
        List<EntityType<? extends Mob>> pool = dimension == Level.NETHER
            ? NETHER_HOSTILES
            : dimension == Level.END ? END_HOSTILES : OVERWORLD_HOSTILES;
        return pool.get(random.nextInt(pool.size()));
    }

    public static RitualDanger ritualDanger(LateGameVariant variant) {
        return switch (variant) {
            case RITUAL_NETHER -> new RitualDanger(2.0F, 4, true);
            case RITUAL_END -> new RitualDanger(1.0F, 0, false);
            default -> new RitualDanger(0.0F, 0, false);
        };
    }

    private static WeightedItem weighted(net.minecraft.world.item.Item item, int min, int max) {
        return new WeightedItem(new ItemStack(item, min), max);
    }

    private static ItemStack chooseWeighted(RandomSource random, List<WeightedItem> options) {
        WeightedItem option = options.get(random.nextInt(options.size()));
        ItemStack stack = option.stack().copy();
        if (option.maxCount() > stack.getCount()) {
            stack.setCount(stack.getCount() + random.nextInt(option.maxCount() - stack.getCount() + 1));
        }
        return stack;
    }

    public record RitualDanger(float damage, int fireSeconds, boolean blazeBurst) {
    }

    private record WeightedItem(ItemStack stack, int maxCount) {
    }
}
