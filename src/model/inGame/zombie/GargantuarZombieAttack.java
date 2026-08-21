package model.inGame.zombie;

import model.GameEngine;
import model.inGame.plant.Plant;

/**
 * Runs one complete Gargantuar smash before another attack may start.
 * Damage lands once near the middle of the PAM clip instead of being applied
 * every simulation tick or instantly deleting the target.
 */
final class GargantuarZombieAttack implements ZombieAttackComponent {
    static final double SMASH_DURATION_SECONDS = 1.7667;
    private static final double IMPACT_TIME_SECONDS = 0.85;
    private static final int SMASH_DAMAGE = 150;

    private static final String ELAPSED_STATE = "GARGANTUAR_SMASH_ELAPSED";
    private static final String IMPACTED_STATE = "GARGANTUAR_SMASH_IMPACTED";
    private static final String SEQUENCE_STATE = "GARGANTUAR_SMASH_SEQUENCE";

    @Override
    public boolean attack(
            Zombie zombie,
            GameEngine engine,
            double deltaSeconds
    ) {
        double elapsed =
                zombie.getDoubleState(
                        ELAPSED_STATE,
                        -1.0
                );

        if (elapsed >= 0.0) {
            elapsed += deltaSeconds;

            if (!zombie.getBooleanState(IMPACTED_STATE)
                    && elapsed >= IMPACT_TIME_SECONDS) {
                Plant target = engine.findCollidingPlant(zombie);
                if (target != null
                        && !target.getBooleanState("TRANSFORMED")) {
                    target.receiveDamage(
                            SMASH_DAMAGE,
                            engine,
                            zombie
                    );
                    engine.recordEvent(
                            "Gargantuar smashed "
                                    + target.getEffectiveType()
                                    + " for "
                                    + SMASH_DAMAGE
                                    + " damage."
                    );
                }
                zombie.putState(IMPACTED_STATE, true);
            }

            if (elapsed >= SMASH_DURATION_SECONDS) {
                zombie.putState(ELAPSED_STATE, null);
                zombie.putState(IMPACTED_STATE, null);
            } else {
                zombie.putState(ELAPSED_STATE, elapsed);
            }

            return true;
        }

        Plant target = engine.findCollidingPlant(zombie);
        if (target == null
                || target.getBooleanState("TRANSFORMED")) {
            return false;
        }

        zombie.putState(ELAPSED_STATE, 0.0);
        zombie.putState(IMPACTED_STATE, false);
        zombie.putState(
                SEQUENCE_STATE,
                zombie.getIntState(SEQUENCE_STATE, 0) + 1
        );
        return true;
    }
}
