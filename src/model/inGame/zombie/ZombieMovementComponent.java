package model.inGame.zombie;

import model.GameEngine;

public interface ZombieMovementComponent {
    void move(Zombie zombie, GameEngine engine, double deltaSeconds);
}
