package model.inGame.zombie;

import model.GameEngine;
import model.Position;
import model.enums.DamageType;
import model.enums.ZombieType;
import model.inGame.plant.Plant;
import model.inGame.projectile.FireEffect;
import model.inGame.projectile.IceEffect;
import model.inGame.projectile.Projectile;
import model.inGame.projectile.ProjectileType;

import java.util.ArrayList;
import java.util.List;

/** Namespace for the six canonical blue-row zombie abilities. */
public final class BonusZombieAbilities {
    private BonusZombieAbilities() {
    }

    public static final class FishermanAbility implements ZombieSpecialAbility {
        private static final double HOOK_INTERVAL_SECONDS = 5.0;

        @Override
        public void tickAfterMovement(Zombie zombie, GameEngine engine, double deltaSeconds) {
            int activations = zombie.advanceTimerCount("FISHERMAN_HOOK", deltaSeconds, HOOK_INTERVAL_SECONDS);
            for (int i = 0; i < activations; i++) {
                hook(zombie, engine);
            }
        }

        private void hook(Zombie zombie, GameEngine engine) {
            Plant target = engine.findNearestPlantInLane(zombie.getRow(), zombie.getX());
            if (target == null || target.getPosition() == null) {
                return;
            }
            int destinationColumn = target.getPosition().getColumn() + 1;
            if (destinationColumn >= engine.getGameMap().getColumns()) {
                target.receiveDamage(Integer.MAX_VALUE, engine, zombie);
                engine.recordEvent("Fisherman threw and destroyed " + target.getEffectiveType() + ".");
                return;
            }
            if (Math.abs(zombie.getX() - (target.getPosition().getColumn() + 0.5)) <= 1.1) {
                target.receiveDamage(Integer.MAX_VALUE, engine, zombie);
                engine.recordEvent("Fisherman threw and destroyed " + target.getEffectiveType() + ".");
                return;
            }
            Position destination = new Position(zombie.getRow(), destinationColumn);
            if (engine.movePlant(target, destination)) {
                engine.recordEvent("Fisherman hooked " + target.getEffectiveType()
                        + " to " + destination + ".");
            }
        }
    }

    public static final class JesterAbility implements ZombieSpecialAbility {
        private static final double SPIN_GRACE_SECONDS = 1.5;

        @Override
        public void tickBeforeMovement(Zombie zombie, GameEngine engine, double deltaSeconds) {
            double until = zombie.getDoubleState("JESTER_SPIN_UNTIL", -1.0);
            boolean spinning = engine.getElapsedSeconds() <= until;
            zombie.putState("SPINNING", spinning);
            zombie.setRuntimeSpeedMultiplier(spinning ? 1.8 : 1.0);
        }

        @Override
        public ZombieProjectileDisposition projectileDisposition(
                Zombie zombie, Projectile projectile, GameEngine engine
        ) {
            if (projectile.getType() != ProjectileType.DIRECT) {
                return ZombieProjectileDisposition.ACCEPT;
            }
            zombie.putState("JESTER_SPIN_UNTIL", engine.getElapsedSeconds() + SPIN_GRACE_SECONDS);
            zombie.putState("SPINNING", true);
            zombie.setRuntimeSpeedMultiplier(1.8);
            Plant target = engine.findNearestPlantInLane(zombie.getRow(), zombie.getX());
            if (target != null) {
                target.receiveDamage(projectile.getDamage(), engine, zombie);
                if (projectile.getEffect() instanceof IceEffect) {
                    engine.addIceHit(target);
                } else if (projectile.getEffect() instanceof FireEffect && target.getPosition() != null) {
                    engine.damageObstacleAt(target.getPosition().getRow(),
                            target.getPosition().getColumn(), Integer.MAX_VALUE, DamageType.FIRE);
                }
                engine.recordEvent("Jester reflected a direct projectile into "
                        + target.getEffectiveType() + ".");
            }
            return ZombieProjectileDisposition.BLOCK;
        }
    }

    public static final class WizardAbility implements ZombieSpecialAbility {
        private static final double TRANSFORM_INTERVAL_SECONDS = 5.0;

        @Override
        public void tickAfterMovement(Zombie zombie, GameEngine engine, double deltaSeconds) {
            int activations = zombie.advanceTimerCount(
                    "WIZARD_TRANSFORM", deltaSeconds, TRANSFORM_INTERVAL_SECONDS);
            for (int i = 0; i < activations; i++) {
                List<Plant> candidates = new ArrayList<>();
                for (Plant plant : engine.getGameMap().getPlants()) {
                    if (!plant.isDead() && !plant.getBooleanState("TRANSFORMED")) {
                        candidates.add(plant);
                    }
                }
                if (!candidates.isEmpty()) {
                    Plant target = candidates.get(engine.getRandom().nextInt(candidates.size()));
                    engine.transformPlantToCat(target, zombie);
                }
            }
        }

        @Override
        public void onDeath(Zombie zombie, GameEngine engine) {
            engine.restoreWizardTransformations(zombie.getId());
        }
    }

    public static final class KingAbility implements ZombieSpecialAbility {
        private static final double PROMOTION_INTERVAL_SECONDS = 5.0;
        private static final double PROMOTION_RANGE = 4.0;

        @Override
        public void tickAfterMovement(Zombie zombie, GameEngine engine, double deltaSeconds) {
            int activations = zombie.advanceTimerCount("KING_PROMOTE", deltaSeconds, PROMOTION_INTERVAL_SECONDS);
            for (int i = 0; i < activations; i++) {
                promote(zombie, engine);
            }
        }

        private void promote(Zombie king, GameEngine engine) {
            List<Zombie> candidates = new ArrayList<>();
            for (Zombie zombie : engine.getZombies()) {
                if (zombie != king && !zombie.isDead() && !zombie.isHypnotized()
                        && zombie.getType() == ZombieType.NORMAL
                        && !zombie.getBooleanState("KING_PROMOTED")
                        && Math.abs(zombie.getX() - king.getX()) <= PROMOTION_RANGE
                        && Math.abs(zombie.getRow() - king.getRow()) <= 1) {
                    candidates.add(zombie);
                }
            }
            if (candidates.isEmpty()) {
                return;
            }
            Zombie target = candidates.get(engine.getRandom().nextInt(candidates.size()));
            target.addArmorPart(new ZombieArmorPart("helmet", 1600, true));
            target.addArmorPart(new ZombieArmorPart("shoulderArmor", 1600, false));
            target.putState("KING_PROMOTED", true);
            engine.recordEvent("King promoted " + target.getName() + " to Knight armor.");
        }
    }
}
