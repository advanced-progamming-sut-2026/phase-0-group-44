package model.inGame.plant;

import model.enums.PlantType;

public class PlantFactory {
    private final PlantRegistry registry;
    private final PlantBehaviorFactory behaviorFactory;

    public PlantFactory(PlantRegistry registry) {
        this(registry, new PlantBehaviorFactory());
    }

    public PlantFactory(PlantRegistry registry, PlantBehaviorFactory behaviorFactory) {
        this.registry = registry;
        this.behaviorFactory = behaviorFactory;
        validateCoverage();
    }

    public Plant create(PlantType type) {
        return create(type, 1);
    }

    public Plant create(PlantType type, int level) {
        PlantDefinition definition = registry.require(type);
        if (type == PlantType.IMITATER) {
            throw new IllegalArgumentException("Imitater requires the plant type it should copy.");
        }
        PlantBehavior behavior = behaviorFactory.create(definition);
        return new Plant(definition, definition, level, definition.statsAtLevel(level), behavior);
    }

    public Plant createImitater(PlantType copiedType, int imitaterLevel) {
        if (copiedType == null || copiedType == PlantType.IMITATER) {
            throw new IllegalArgumentException("Imitater must copy another plant.");
        }
        PlantDefinition imitater = registry.require(PlantType.IMITATER);
        PlantDefinition copied = registry.require(copiedType);
        PlantBehavior copiedBehavior = behaviorFactory.create(copied);
        PlantBehavior wrapper = new ModifierBehavior(ModifierBehavior.Mode.IMITATER_WRAPPER, copiedBehavior);
        PlantStats stats = PlantStats.imitate(copied, imitater, imitaterLevel);
        return new Plant(imitater, copied, imitaterLevel, stats, wrapper);
    }

    public boolean supports(PlantType type) {
        PlantDefinition definition = registry.findByType(type);
        return definition != null
                && (type == PlantType.IMITATER || behaviorFactory.supports(type));
    }

    private void validateCoverage() {
        for (PlantDefinition definition : registry.findAll()) {
            if (definition.getType() != PlantType.IMITATER && !behaviorFactory.supports(definition.getType())) {
                throw new IllegalStateException("Plant has no behavior: " + definition.getName());
            }
        }
    }
}
