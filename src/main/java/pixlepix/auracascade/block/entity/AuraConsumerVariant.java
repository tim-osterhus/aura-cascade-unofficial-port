package pixlepix.auracascade.block.entity;

public enum AuraConsumerVariant {
    PROCESSOR("consumer_block_ore", "block.aura.consumer_block_ore", 150, 8, ConsumerFamily.PROCESSOR, false),
    PRISMATIC_PROCESSOR("consumer_block_ore_adv", "block.aura.consumer_block_ore_adv", 300, 10, ConsumerFamily.PROCESSOR, true),
    SMELTER("consumer_block_furnace", "block.aura.consumer_block_furnace", 190, 3, ConsumerFamily.SMELTER, false),
    GROWER("consumer_block_plant", "block.aura.consumer_block_plant", 50, 2, ConsumerFamily.GROWER, false),
    FISHER("consumer_block_fish", "block.aura.consumer_block_fish", 200, 8, ConsumerFamily.FISHER, false),
    BREWER("consumer_block_potion", "block.aura.consumer_block_potion", 500, 5, ConsumerFamily.BREWER, false),
    COLORER("consumer_block_dye", "block.aura.consumer_block_dye", 50, 2, ConsumerFamily.COLORER, false),
    SYNTHESIZER("consumer_block_angel", "block.aura.consumer_block_angel", 10_000, 49, ConsumerFamily.SYNTHESIZER, false),
    ENCHANTER("consumer_block_enchant", "block.aura.consumer_block_enchant", 500, 999, ConsumerFamily.ENCHANTER, false);

    private final String registryPath;
    private final String translationKey;
    private final int powerPerProgress;
    private final int maxProgress;
    private final ConsumerFamily family;
    private final boolean prismatic;

    AuraConsumerVariant(
        String registryPath,
        String translationKey,
        int powerPerProgress,
        int maxProgress,
        ConsumerFamily family,
        boolean prismatic
    ) {
        this.registryPath = registryPath;
        this.translationKey = translationKey;
        this.powerPerProgress = powerPerProgress;
        this.maxProgress = maxProgress;
        this.family = family;
        this.prismatic = prismatic;
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

    public ConsumerFamily family() {
        return family;
    }

    public boolean prismatic() {
        return prismatic;
    }

    public enum ConsumerFamily {
        PROCESSOR,
        SMELTER,
        GROWER,
        FISHER,
        BREWER,
        COLORER,
        SYNTHESIZER,
        ENCHANTER
    }
}
