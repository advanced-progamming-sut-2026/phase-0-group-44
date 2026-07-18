package model.inGame.plant;

import model.GameEngine;
import model.Position;
import model.enums.DamageType;
import model.enums.PlantType;
import model.inGame.zombie.Zombie;

public class ModifierBehavior extends AbstractTimedBehavior {
    public enum Mode {
        TORCHWOOD,
        MAGNET_SHROOM,
        LILY_PAD,
        IMITATER_WRAPPER
    }

    private final Mode mode;
    private final PlantBehavior delegate;

    public ModifierBehavior(Mode mode) {
        this(mode, null);
    }

    public ModifierBehavior(Mode mode, PlantBehavior delegate) {
        this.mode = mode;
        this.delegate = delegate;
    }

    @Override
    public void onPlant(Plant plant, GameEngine engine) {
        if (delegate != null) {
            delegate.onPlant(plant, engine);
        }
        if (mode == Mode.IMITATER_WRAPPER && plant.getStats().hasFlag("AUTO_PLANT_FOOD_ON_ENTER")) {
            onPlantFood(plant, engine);
        }
    }

    @Override
    public void tick(Plant plant, GameEngine engine, double deltaSeconds) {
        if (delegate != null) {
            delegate.tick(plant, engine, deltaSeconds);
            return;
        }
        if (mode == Mode.MAGNET_SHROOM
                && ready(plant, deltaSeconds, plant.getStats().getActionInterval())) {
            double range = 5.0 + plant.getStats().getSpecial("RANGE", 0);
            Zombie target = engine.findNearestMetalZombie(plant.getPosition(), range);
            if (target != null && target.removeMetalArmor()) {
                engine.recordEvent("Magnet-shroom removed metal armor from " + target.getName() + ".");
            }
        }
    }

    @Override
    public void onDamaged(Plant plant, GameEngine engine, Zombie attacker, int damageTaken) {
        if (delegate != null) {
            delegate.onDamaged(plant, engine, attacker, damageTaken);
        }
    }

    @Override
    public void onDeath(Plant plant, GameEngine engine) {
        if (delegate != null) {
            delegate.onDeath(plant, engine);
        }
        if (mode == Mode.TORCHWOOD && plant.getStats().hasFlag("DEATH_EXPLOSION_AOE")) {
            engine.damageArea(plant.getPosition(), 1, 1, 300, DamageType.FIRE);
        }
    }

    @Override
    public void onPlantFood(Plant plant, GameEngine engine) {
        if (delegate != null) {
            delegate.onPlantFood(plant, engine);
            return;
        }
        switch (mode) {
            case TORCHWOOD -> plant.putState("BLUE_FLAME", true);
            case MAGNET_SHROOM -> {
                int removed = 0;
                for (Zombie zombie : engine.getZombies()) {
                    if (removed >= 5) {
                        break;
                    }
                    if (zombie.removeMetalArmor()) {
                        removed++;
                    }
                }
            }
            case LILY_PAD -> {
                int spawned = 0;
                for (Position position : engine.emptyPositions(true)) {
                    if (spawned >= 4) {
                        break;
                    }
                    try {
                        engine.placePlantForFree(
                                engine.getPlantFactory().create(
                                        PlantType.LILY_PAD, plant.getLevel()),
                                position);
                        spawned++;
                    } catch (IllegalStateException ignored) {
                        // Another layer or terrain rule rejected this tile.
                    }
                }
            }
            case IMITATER_WRAPPER -> {
                // Delegate handled above.
            }
        }
    }
}
