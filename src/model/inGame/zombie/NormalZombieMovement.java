package model.inGame.zombie;

import model.GameEngine;
import model.Position;
import model.enums.TerrainType;

public class NormalZombieMovement implements ZombieMovementComponent {
    private final double multiplier;

    public NormalZombieMovement() {
        this(1.0);
    }

    public NormalZombieMovement(double multiplier) {
        this.multiplier = multiplier;
    }

    @Override
    public void move(Zombie zombie, GameEngine engine, double deltaSeconds) {
        double distance = zombie.getSpeedTilesPerSecond() * multiplier * deltaSeconds;
        zombie.moveBy(zombie.getDirection() * distance);
        int column = Math.max(0, Math.min(engine.getGameMap().getColumns() - 1, zombie.getColumn()));
        TerrainType terrain = engine.getGameMap().getTile(zombie.getRow(), column).getTerrain();
        if (terrain.isSlippery()) {
            int nextRow = zombie.getRow() + terrain.slipperyRowDelta();
            if (nextRow >= 0 && nextRow < engine.getGameMap().getRows()) {
                zombie.moveToRow(nextRow);
            }
        }
        if (zombie.getX() <= 0.0) {
            engine.recordEvent(zombie.getName() + " reached the house in row " + zombie.getRow() + ".");
        }
    }
}
