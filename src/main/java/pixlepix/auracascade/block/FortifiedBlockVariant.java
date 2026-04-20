package pixlepix.auracascade.block;

import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;

public enum FortifiedBlockVariant {
    COBBLESTONE("fortified_cobblestone", "block.aura.fortified_cobblestone", Blocks.COBBLESTONE, 75, 30, 8.0F, 14.0F, false),
    STONE("fortified_stone", "block.aura.fortified_stone", Blocks.STONE, 100, 60, 9.0F, 18.0F, false),
    PLANKS("fortified_planks", "block.aura.fortified_planks", Blocks.OAK_PLANKS, 30, 5, 4.0F, 9.0F, false),
    GLASS("fortified_glass", "block.aura.fortified_glass", Blocks.GLASS, 16, 120, 2.5F, 6.0F, true),
    OBSIDIAN("fortified_obsidian", "block.aura.fortified_obsidian", Blocks.OBSIDIAN, 1600, 6000, 50.0F, 1_200.0F, false),
    DIRT("fortified_dirt", "block.aura.fortified_dirt", Blocks.DIRT, 50, 120, 3.0F, 8.0F, false);

    private final String registryPath;
    private final String translationKey;
    private final Block baseBlock;
    private final int virtualHealth;
    private final int repairSeconds;
    private final float destroyTime;
    private final float explosionResistance;
    private final boolean translucent;

    FortifiedBlockVariant(
        String registryPath,
        String translationKey,
        Block baseBlock,
        int virtualHealth,
        int repairSeconds,
        float destroyTime,
        float explosionResistance,
        boolean translucent
    ) {
        this.registryPath = registryPath;
        this.translationKey = translationKey;
        this.baseBlock = baseBlock;
        this.virtualHealth = virtualHealth;
        this.repairSeconds = repairSeconds;
        this.destroyTime = destroyTime;
        this.explosionResistance = explosionResistance;
        this.translucent = translucent;
    }

    public String registryPath() {
        return registryPath;
    }

    public String translationKey() {
        return translationKey;
    }

    public Block baseBlock() {
        return baseBlock;
    }

    public int virtualHealth() {
        return virtualHealth;
    }

    public int repairSeconds() {
        return repairSeconds;
    }

    public float destroyTime() {
        return destroyTime;
    }

    public float explosionResistance() {
        return explosionResistance;
    }

    public boolean translucent() {
        return translucent;
    }

    public double repairChance() {
        return 1.0D / (repairSeconds / 5.0D);
    }

    public double resistChance() {
        return 1.0D - 1.0D / (virtualHealth / 16.0D);
    }
}
