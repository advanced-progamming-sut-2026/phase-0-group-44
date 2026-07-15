package model.inGame.projectile;

import model.GameEngine;

public interface CollisionLogic {
    void advance(Projectile projectile, GameEngine engine, double deltaSeconds);
}
