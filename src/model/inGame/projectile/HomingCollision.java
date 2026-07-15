package model.inGame.projectile;

import model.GameEngine;

public class HomingCollision implements CollisionLogic {
    @Override
    public void advance(Projectile projectile, GameEngine engine, double deltaSeconds) {
        projectile.advanceHoming(engine, deltaSeconds);
    }
}
