package model.inGame.zombie;

import model.enums.ZombieType;

public final class ZombieFactory {
    private final ZombieRegistry registry;
    private final ZombieBehaviorFactory behaviorFactory;

    public ZombieFactory() {
        this(ZombieRegistry.getDefault());
    }

    public ZombieFactory(ZombieRegistry registry) {
        this.registry = registry;
        this.behaviorFactory = new ZombieBehaviorFactory();
    }

    public Zombie create(ZombieType type, int row, double x) {
        ZombieDefinition definition = registry.require(type);
        return new Zombie(definition, row, x, behaviorFactory.create(definition));
    }

    public Zombie create(String token, int row, double x) {
        ZombieDefinition definition = registry.findByName(token);
        if (definition == null) {
            throw new IllegalArgumentException("Unknown zombie type: " + token);
        }
        return create(definition.getType(), row, x);
    }

    public ZombieBehaviorFactory getBehaviorFactory() {
        return behaviorFactory;
    }
}
