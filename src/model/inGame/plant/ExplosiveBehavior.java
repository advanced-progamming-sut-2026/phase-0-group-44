package model.inGame.plant;

import model.GameEngine;
import model.Position;
import model.enums.DamageType;
import model.enums.ObstacleType;
import model.enums.PlantType;
import model.inGame.zombie.Zombie;

import java.util.ArrayList;
import java.util.List;

public class ExplosiveBehavior extends AbstractTimedBehavior {
    public enum Mode {
        POTATO_MINE,
        PRIMAL_MINE,
        CHERRY,
        SQUASH,
        JALAPENO,
        DOOM,
        TANGLE_KELP,
        ICEBERG,
        ICE_SHROOM,
        HOT_POTATO,
        GRAVE_BUSTER
    }

    private final Mode mode;

    public ExplosiveBehavior(Mode mode) {
        this.mode = mode;
    }

    @Override
    public void onPlant(Plant plant, GameEngine engine) {
        switch (mode) {
            case CHERRY -> {
                engine.damageArea(plant.getPosition(), 1, 1, plant.getStats().getDamage(), DamageType.NORMAL);
                plant.expire(engine);
            }
            case JALAPENO -> {
                engine.damageLane(plant.getPosition().getRow(), plant.getStats().getDamage(), DamageType.FIRE);
                engine.clearIceInLane(plant.getPosition().getRow());
                plant.expire(engine);
            }
            case DOOM -> {
                for (Zombie zombie : new ArrayList<>(engine.getZombies())) {
                    zombie.takeDamage(plant.getStats().getDamage(), DamageType.NORMAL);
                }
                engine.getGameMap().setObstacle(plant.getPosition(), ObstacleType.CRATER);
                plant.expire(engine);
            }
            case ICE_SHROOM -> {
                engine.freezeAll(10 + plant.getStats().getSpecial("FREEZE_DURATION", 0));
                plant.expire(engine);
            }
            case HOT_POTATO -> {
                int radius = plant.getStats().hasFlag("MELT_AREA_3X3") ? 1 : 0;
                engine.clearIce(plant.getPosition(), radius);
                if (plant.getStats().hasFlag("EXPLODE_ON_FINISH")) {
                    engine.damageArea(plant.getPosition(), 1, 1,
                            plant.getStats().getDamage(), DamageType.FIRE);
                    engine.recordEvent("Hot Potato exploded after melting ice.");
                }
                plant.expire(engine);
            }
            case POTATO_MINE, PRIMAL_MINE -> {
                double baseArm = mode == Mode.POTATO_MINE ? 15.0 : 5.0;
                plant.putState("ARM_TIME", Math.max(0.0,
                        baseArm + plant.getStats().getSpecial("ARM_TIME", 0)));
                plant.putState("ARMED", false);
            }
            case SQUASH -> plant.putState("CRUSHES_LEFT",
                    1 + (int) plant.getStats().getSpecial("CRUSH_CHARGES", 0));
            case GRAVE_BUSTER -> plant.putState("ACTIVE_ZERO_HP", true);
            case TANGLE_KELP, ICEBERG -> {
                // Activated by contact in tick().
            }
        }
    }

    @Override
    public void tick(Plant plant, GameEngine engine, double deltaSeconds) {
        switch (mode) {
            case POTATO_MINE, PRIMAL_MINE -> tickMine(plant, engine, deltaSeconds);
            case SQUASH -> tickSquash(plant, engine);
            case TANGLE_KELP -> tickTangleKelp(plant, engine);
            case ICEBERG -> tickIceberg(plant, engine);
            case GRAVE_BUSTER -> tickGraveBuster(plant, engine, deltaSeconds);
            default -> {
            }
        }
    }

    private void tickMine(Plant plant, GameEngine engine, double deltaSeconds) {
        boolean armed = plant.getBooleanState("ARMED");
        double armTime = plant.getState("ARM_TIME", Double.class, mode == Mode.POTATO_MINE ? 15.0 : 5.0);
        if (!armed && plant.getAgeSeconds() >= armTime) {
            plant.putState("ARMED", true);
            armed = true;
        }
        if (!armed) {
            return;
        }
        Zombie target = contactZombie(plant, engine, 0);
        if (target != null) {
            int radius = mode == Mode.PRIMAL_MINE ? 1 : 0;
            engine.damageArea(plant.getPosition(), radius, radius,
                    boostedDamage(plant, engine), DamageType.NORMAL);
            plant.expire(engine);
        }
    }

    private void tickSquash(Plant plant, GameEngine engine) {
        Zombie target = contactZombie(plant, engine, 1);
        if (target == null) {
            return;
        }
        target.takeDamage(boostedDamage(plant, engine), DamageType.TRUE);
        int left = plant.getState("CRUSHES_LEFT", Integer.class, 1) - 1;
        plant.putState("CRUSHES_LEFT", left);
        if (left <= 0) {
            plant.expire(engine);
        }
    }

    private void tickTangleKelp(Plant plant, GameEngine engine) {
        Zombie target = contactZombie(plant, engine, 0);
        if (target != null) {
            target.takeDamage(99_999, DamageType.TRUE);
            plant.expire(engine);
        }
    }

    private void tickIceberg(Plant plant, GameEngine engine) {
        Zombie target = contactZombie(plant, engine, 0);
        if (target != null) {
            target.applyFreeze(10 + plant.getStats().getSpecial("FREEZE_DURATION", 0));
            plant.expire(engine);
        }
    }

    private void tickGraveBuster(Plant plant, GameEngine engine, double deltaSeconds) {
        double eatTime = Math.max(0.5, 4.0 + plant.getStats().getSpecial("EAT_TIME", 0));
        if (plant.getAgeSeconds() < eatTime) {
            return;
        }
        engine.getGameMap().clearObstacle(plant.getPosition());
        if (plant.getStats().hasFlag("EXPLODE_ON_FINISH")) {
            engine.damageArea(plant.getPosition(), 1, 1, plant.getStats().getDamage(), DamageType.NORMAL);
        }
        plant.expire(engine);
    }

    private Zombie contactZombie(Plant plant, GameEngine engine, int columnRadius) {
        for (Zombie zombie : engine.getZombiesInArea(plant.getPosition(), 0, columnRadius)) {
            if (zombie.getRow() == plant.getPosition().getRow()) {
                return zombie;
            }
        }
        return null;
    }

    @Override
    public void onPlantFood(Plant plant, GameEngine engine) {
        switch (mode) {
            case POTATO_MINE, PRIMAL_MINE -> {
                plant.putState("ARMED", true);
                int spawned = 0;
                for (Position position : engine.emptyPositions(false)) {
                    if (spawned >= 2) {
                        break;
                    }
                    Plant clone = engine.getPlantFactory().create(plant.getEffectiveType(), plant.getLevel());
                    engine.placePlantForFree(clone, position);
                    clone.putState("ARMED", true);
                    spawned++;
                }
            }
            case SQUASH -> {
                for (Zombie zombie : engine.getRandomZombies(2)) {
                    zombie.takeDamage(boostedDamage(plant, engine), DamageType.TRUE);
                }
            }
            case TANGLE_KELP -> {
                int count = 4 + (int) plant.getStats().getSpecial("TARGET_COUNT", 0);
                for (Zombie zombie : engine.getRandomWaterZombies(count)) {
                    zombie.takeDamage(99_999, DamageType.TRUE);
                }
            }
            case ICEBERG -> engine.freezeAll(10 + plant.getStats().getSpecial("FREEZE_DURATION", 0));
            default -> {
                // Instant-use explosives have no plant-food action by canonical data.
            }
        }
    }
}
