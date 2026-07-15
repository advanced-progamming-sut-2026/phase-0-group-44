package model.inGame.zombie;

import model.GameEngine;

public interface ZombieAttackComponent {
    /** @return true when a target blocked movement this tick. */
    boolean attack(Zombie zombie, GameEngine engine, double deltaSeconds);
}
