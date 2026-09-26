package pixlepix.auracascade.item;

public interface ConsumerItemLifetimeAccess {
    boolean aura$isConsumerKeptAlive();

    void aura$extendConsumerLifetime();

    void aura$keepAlive();
}
