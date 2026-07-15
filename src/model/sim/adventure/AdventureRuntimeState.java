package model.sim.adventure;

import model.enums.PlantType;
import model.level.AdventureLevelConfig;
import model.level.TileCoordinate;
import util.RandomSource;

import java.util.Collections;
import java.util.EnumMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/** Mutable per-session state for chapter and special-level rules. */
public final class AdventureRuntimeState {
    private final AdventureLevelConfig config;
    private final Set<TileCoordinate> protectedTiles = new LinkedHashSet<>();
    private final Map<PlantType, Integer> conveyorPackets = new EnumMap<>(PlantType.class);
    private final List<PlantType> conveyorPool;
    private int lastProcessedWave;
    private long elapsedTicks;
    private int conveyorTicks;
    private int waterColumns;

    public AdventureRuntimeState(
            AdventureLevelConfig config,
            Set<TileCoordinate> protectedTiles,
            List<PlantType> conveyorPool
    ) {
        this.config = config;
        if (protectedTiles != null) {
            this.protectedTiles.addAll(protectedTiles);
        }
        this.conveyorPool = conveyorPool == null ? List.of() : List.copyOf(conveyorPool);
    }

    public AdventureLevelConfig getConfig() {
        return config;
    }

    public long getElapsedTicks() {
        return elapsedTicks;
    }

    public void tickElapsed() {
        elapsedTicks++;
    }

    public int getLastProcessedWave() {
        return lastProcessedWave;
    }

    public void setLastProcessedWave(int wave) {
        lastProcessedWave = wave;
    }

    public Set<TileCoordinate> getProtectedTiles() {
        return Collections.unmodifiableSet(protectedTiles);
    }

    public boolean isProtected(int x, int y) {
        return protectedTiles.contains(new TileCoordinate(x, y));
    }

    public int getConveyorPacketCount(PlantType type) {
        return conveyorPackets.getOrDefault(type, 0);
    }

    public Map<PlantType, Integer> getConveyorPackets() {
        return Collections.unmodifiableMap(conveyorPackets);
    }

    public PlantType issueConveyorPacket(RandomSource random) {
        if (conveyorPool.isEmpty()) {
            return null;
        }
        PlantType type = conveyorPool.get(random.nextInt(conveyorPool.size()));
        conveyorPackets.put(type, getConveyorPacketCount(type) + 1);
        return type;
    }

    public boolean consumeConveyorPacket(PlantType type) {
        int count = getConveyorPacketCount(type);
        if (count <= 0) {
            return false;
        }
        if (count == 1) {
            conveyorPackets.remove(type);
        } else {
            conveyorPackets.put(type, count - 1);
        }
        return true;
    }

    public boolean conveyorPacketDue() {
        conveyorTicks++;
        if (conveyorTicks >= config.getConveyorIntervalTicks()) {
            conveyorTicks = 0;
            return true;
        }
        return false;
    }

    public int getWaterColumns() {
        return waterColumns;
    }

    public void setWaterColumns(int waterColumns) {
        this.waterColumns = waterColumns;
    }
}
