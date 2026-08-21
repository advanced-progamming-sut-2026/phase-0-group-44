package model.inGame.zombie;

import model.GameEngine;
import model.inGame.plant.Plant;

public final class InstantDestroyZombieAttack implements ZombieAttackComponent {
    private final String eventPrefix;
    private final String recoveryStateKey;
    private final double recoverySeconds;

    public InstantDestroyZombieAttack(String eventPrefix) {
        this(eventPrefix, 0.0);
    }

    public InstantDestroyZombieAttack(
            String eventPrefix,
            double recoverySeconds
    ) {
        this.eventPrefix = eventPrefix;
        this.recoverySeconds = Math.max(0.0, recoverySeconds);
        this.recoveryStateKey =
                "INSTANT_ATTACK_RECOVERY_"
                        + eventPrefix
                        .toUpperCase()
                        .replace(' ', '_');
    }

    @Override
    public boolean attack(Zombie zombie, GameEngine engine, double deltaSeconds) {
        double remaining =
                zombie.getDoubleState(
                        recoveryStateKey,
                        0.0
                );

        if (remaining > 0.0) {
            zombie.putState(
                    recoveryStateKey,
                    Math.max(0.0, remaining - deltaSeconds)
            );
            return true;
        }

        Plant target = engine.findCollidingPlant(zombie);
        if (target == null || target.getBooleanState("TRANSFORMED")) {
            return false;
        }

        target.receiveDamage(Integer.MAX_VALUE, engine, zombie);
        engine.recordEvent(eventPrefix + " destroyed " + target.getEffectiveType() + ".");

        if (recoverySeconds > 0.0) {
            zombie.putState(
                    recoveryStateKey,
                    recoverySeconds
            );
        }

        return true;
    }
}