package model.inGame.projectile;

import model.GameEngine;

public class BouncingCollision implements CollisionLogic {
    @Override
    public void advance(Projectile projectile, GameEngine engine, double deltaSeconds) {
        projectile.advanceLinear(engine, deltaSeconds, true, true);
    }
}
