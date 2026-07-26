package model.inGame.zombie;

import model.GameEngine;
import model.Position;
import model.enums.DamageType;
import model.enums.TerrainType;
import model.inGame.GameOutcome;

import java.util.ArrayList;

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
        if (zombie.getX() <= 0.0 && !zombie.getBooleanState("REACHED_HOUSE")) {
            zombie.putState("REACHED_HOUSE", true);
            reachHouse(zombie, engine);
        }
    }

    private void reachHouse(Zombie zombie, GameEngine engine) {
        int row = zombie.getRow();
        if (!engine.isLawnMowerUsed(row)) {
            engine.useLawnMower(row);
            engine.recordEvent(zombie.getName() + " reached the house in row " + row
                    + "; the lawn mower triggered.");
            for (Zombie inRow : new ArrayList<>(engine.getZombiesInLane(row))) {
                if (!inRow.isDead()) {
                    inRow.takeDamage(Integer.MAX_VALUE, DamageType.TRUE);
                }
            }
            return;
        }
        engine.recordEvent(zombie.getName() + " reached the house in row " + row + ".");
        engine.setOutcome(GameOutcome.LOST);
        engine.recordEvent("The zombie ate your brain; LOSER!!!");
    }
}
