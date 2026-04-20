package pixlepix.auracascade.block;

public enum LateGameVariant {
    LOOTER("consumer_block_loot", "block.aura.consumer_block_loot", 5_000, 100),
    SPAWNER("consumer_block_spawn", "block.aura.consumer_block_spawn", 190, 15),
    MINER("consumer_block_miner", "block.aura.consumer_block_miner", 2_500, 1),
    RITUAL_NETHER("ritual_nether", "block.aura.ritual_nether", 5_000, 100),
    RITUAL_END("ritual_end", "block.aura.ritual_end", 5_000, 100);

    private final String registryPath;
    private final String translationKey;
    private final int powerPerProgress;
    private final int maxProgress;

    LateGameVariant(String registryPath, String translationKey, int powerPerProgress, int maxProgress) {
        this.registryPath = registryPath;
        this.translationKey = translationKey;
        this.powerPerProgress = powerPerProgress;
        this.maxProgress = maxProgress;
    }

    public String registryPath() {
        return registryPath;
    }

    public String translationKey() {
        return translationKey;
    }

    public int powerPerProgress() {
        return powerPerProgress;
    }

    public int maxProgress() {
        return maxProgress;
    }
}
