package model.inGame.zombie;

import model.GameEngine;

public final class StationaryZombieMovement implements ZombieMovementComponent {
    @Override
    public void move(Zombie zombie, GameEngine engine, double deltaSeconds) {
        // Intentionally stationary (for right-edge support zombies).
    }
}
