package model.inGame.projectile;

import model.GameEngine;

public class LobberCollision implements CollisionLogic {
    @Override
    public void advance(Projectile projectile, GameEngine engine, double deltaSeconds) {
        projectile.advanceLobbed(engine, deltaSeconds);
    }
}
