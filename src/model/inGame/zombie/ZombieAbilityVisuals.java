package model.inGame.zombie;

/** Keeps short-lived ability animation flags synchronized with model time. */
final class ZombieAbilityVisuals {
    private ZombieAbilityVisuals() {
    }

    static void tick(
            Zombie zombie,
            String timerState,
            String booleanState,
            double deltaSeconds
    ) {
        double remaining = Math.max(
                0.0,
                zombie.getDoubleState(timerState, 0.0) - deltaSeconds
        );
        zombie.putState(timerState, remaining);
        zombie.putState(booleanState, remaining > 0.0);
    }

    static void start(
            Zombie zombie,
            String timerState,
            String booleanState,
            double seconds
    ) {
        zombie.putState(timerState, seconds);
        zombie.putState(booleanState, true);
    }
}
