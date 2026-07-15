package model.inGame.plant;

import model.GameEngine;
import model.enums.DamageType;
import model.enums.PlantType;
import model.inGame.zombie.Zombie;

public class DefenderBehavior implements PlantBehavior {
    public enum Mode {
        WALL_NUT,
        TALL_NUT,
        ENDURIAN,
        GARLIC,
        EXPLODE_O_NUT,
        PUMPKIN,
        SUN_BEAN
    }

    private final Mode mode;

    public DefenderBehavior(Mode mode) {
        this.mode = mode;
    }

    @Override
    public void onDamaged(Plant plant, GameEngine engine, Zombie attacker, int damageTaken) {
        switch (mode) {
            case ENDURIAN -> {
                if (attacker != null && !attacker.isDead()) {
                    int reflect = plant.getStats().getDamage()
                            + (int) plant.getStats().getSpecial("REFLECT_DAMAGE", 0);
                    if (plant.getBooleanState("PLANT_FOOD_REFLECT")) {
                        reflect *= 2;
                    }
                    attacker.takeDamage(reflect, DamageType.NORMAL);
                }
            }
            case GARLIC -> engine.moveZombieToAdjacentLane(attacker);
            case SUN_BEAN -> engine.addSun(5 + (int) plant.getStats().getSpecial("SUN_DROP", 0));
            default -> {
            }
        }
    }

    @Override
    public void onArmorBroken(Plant plant, GameEngine engine) {
        if (mode == Mode.EXPLODE_O_NUT) {
            engine.damageArea(plant.getPosition(), 1, 1,
                    plant.getStats().getDamage(), DamageType.NORMAL);
        }
    }

    @Override
    public void onDeath(Plant plant, GameEngine engine) {
        if (mode == Mode.EXPLODE_O_NUT) {
            engine.damageArea(plant.getPosition(), 1, 1,
                    plant.getStats().getDamage(), DamageType.NORMAL);
        }
    }

    @Override
    public void onPlantFood(Plant plant, GameEngine engine) {
        if (mode == Mode.GARLIC) {
            for (Zombie zombie : engine.getZombiesInLane(plant.getPosition().getRow())) {
                engine.moveZombieToAdjacentLane(zombie);
            }
            return;
        }
        int armor = switch (mode) {
            case TALL_NUT -> 8000;
            case ENDURIAN -> 3000;
            case EXPLODE_O_NUT, PUMPKIN, WALL_NUT -> 4000;
            case SUN_BEAN -> 1000;
            case GARLIC -> 0;
        };
        plant.addArmor(armor);
        if (mode == Mode.ENDURIAN) {
            plant.putState("PLANT_FOOD_REFLECT", true);
        }
    }
}
