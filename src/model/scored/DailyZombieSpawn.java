package model.scored;

import model.enums.ZombieType;

/** One immutable entry in the shared daily scored-game schedule. */
public record DailyZombieSpawn(long spawnTick, int row, ZombieType type) {
    public DailyZombieSpawn {
        if (spawnTick < 0 || row < 0 || row >= 5 || type == null) {
            throw new IllegalArgumentException("A daily zombie spawn requires tick, row, and type.");
        }
    }
}
