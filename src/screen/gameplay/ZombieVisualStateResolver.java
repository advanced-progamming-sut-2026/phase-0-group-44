package screen.gameplay;

import model.inGame.zombie.Zombie;

public final class ZombieVisualStateResolver {

    private ZombieVisualStateResolver() {
    }

    public static ZombieVisualState resolve(Zombie zombie) {
        if (zombie == null || zombie.isDead()) {
            return ZombieVisualState.DEAD;
        }

        if (zombie.isFrozen()) {
            return ZombieVisualState.FROZEN;
        }

        if (zombie.isStunned()) {
            return ZombieVisualState.STUNNED;
        }

        if (zombie.getBooleanState("SPINNING")) {
            return ZombieVisualState.SPINNING;
        }

        if (zombie.getBooleanState("CHARGING")) {
            return ZombieVisualState.CHARGING;
        }

        if (zombie.getBooleanState("EATING")) {
            return ZombieVisualState.EAT;
        }

        if (zombie.getSpeedTilesPerSecond() <= 0.0) {
            return ZombieVisualState.IDLE;
        }

        return ZombieVisualState.WALK;
    }
}
