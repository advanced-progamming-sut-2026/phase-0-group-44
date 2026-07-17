package model.miniGame;

import model.enums.PlantType;
import model.enums.ZombieType;

import java.util.List;

/** Explicit Vasebreaker-only composition and packet timing for one level. */
public final class VaseBreakerLevelRules {
    private final int level;
    private final int emptyVases;
    private final int zombieVases;
    private final int packetVases;
    private final int plantVases;
    private final int gargantuarVases;
    private final int packetLifetimeTicks;
    private final List<PlantType> plantPool;
    private final List<ZombieType> zombiePool;

    public VaseBreakerLevelRules(
            int level,
            int emptyVases,
            int zombieVases,
            int packetVases,
            int plantVases,
            int gargantuarVases,
            int packetLifetimeTicks,
            List<PlantType> plantPool,
            List<ZombieType> zombiePool
    ) {
        if (level < 1 || level > 3 || emptyVases < 0 || zombieVases < 0
                || packetVases < 0 || plantVases < 0 || gargantuarVases < 0
                || getTotal(emptyVases, zombieVases, packetVases,
                plantVases, gargantuarVases) <= 0
                || packetLifetimeTicks <= 0 || plantPool == null || plantPool.isEmpty()
                || zombiePool == null || zombiePool.isEmpty()) {
            throw new IllegalArgumentException("Invalid Vasebreaker level rules.");
        }
        this.level = level;
        this.emptyVases = emptyVases;
        this.zombieVases = zombieVases;
        this.packetVases = packetVases;
        this.plantVases = plantVases;
        this.gargantuarVases = gargantuarVases;
        this.packetLifetimeTicks = packetLifetimeTicks;
        this.plantPool = List.copyOf(plantPool);
        this.zombiePool = List.copyOf(zombiePool);
    }

    private static int getTotal(int empty, int zombie, int packet, int plant, int gargantuar) {
        return empty + zombie + packet + plant + gargantuar;
    }

    public int getLevel() {
        return level;
    }

    public int getEmptyVases() {
        return emptyVases;
    }

    public int getZombieVases() {
        return zombieVases;
    }

    public int getPacketVases() {
        return packetVases;
    }

    public int getPlantVases() {
        return plantVases;
    }

    public int getGargantuarVases() {
        return gargantuarVases;
    }

    public int getPacketLifetimeTicks() {
        return packetLifetimeTicks;
    }

    public List<PlantType> getPlantPool() {
        return plantPool;
    }

    public List<ZombieType> getZombiePool() {
        return zombiePool;
    }

    public int getVaseCount() {
        return getTotal(emptyVases, zombieVases, packetVases, plantVases, gargantuarVases);
    }

    public int getDangerScore() {
        return zombieVases + gargantuarVases * 5;
    }

    public boolean isHarderThan(VaseBreakerLevelRules previous) {
        return previous != null
                && level == previous.level + 1
                && getVaseCount() > previous.getVaseCount()
                && getDangerScore() > previous.getDangerScore()
                && packetLifetimeTicks < previous.packetLifetimeTicks
                && zombiePool.size() >= previous.zombiePool.size();
    }
}
