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
    private final Set<TileCoordinate> pendingLowTideSpawns = new LinkedHashSet<>();
    private int pendingLowTideSpawnDelayTicks;
    private final Set<TileCoordinate> pendingNecromancySpawns = new LinkedHashSet<>();
    private int pendingNecromancySpawnDelayTicks;


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

    public void queueLowTideSpawn(TileCoordinate coordinate) {
        if (coordinate != null) {
            pendingLowTideSpawns.add(coordinate);
            // Gameplay can advance up to four model ticks in one render frame.
            // Five ticks guarantees the warning event reaches the UI before the
            // queued zombies are allowed to spawn.
            pendingLowTideSpawnDelayTicks = Math.max(pendingLowTideSpawnDelayTicks, 5);
        }
    }

    public boolean tickLowTideSpawnDelay() {
        if (pendingLowTideSpawns.isEmpty()) {
            pendingLowTideSpawnDelayTicks = 0;
            return false;
        }
        if (pendingLowTideSpawnDelayTicks > 0) {
            pendingLowTideSpawnDelayTicks--;
        }
        return pendingLowTideSpawnDelayTicks <= 0;
    }

    public Set<TileCoordinate> drainLowTideSpawns() {
        if (pendingLowTideSpawns.isEmpty()) {
            return Set.of();
        }
        Set<TileCoordinate> result = new LinkedHashSet<>(pendingLowTideSpawns);
        pendingLowTideSpawns.clear();
        return result;
    }

    public void queueNecromancySpawn(TileCoordinate coordinate) {
        if (coordinate != null) {
            pendingNecromancySpawns.add(coordinate);
            // As with low tide, leave enough model ticks for the render/UI layer
            // to consume the warning before anything appears on the lawn.
            pendingNecromancySpawnDelayTicks = Math.max(pendingNecromancySpawnDelayTicks, 5);
        }
    }

    public boolean tickNecromancySpawnDelay() {
        if (pendingNecromancySpawns.isEmpty()) {
            pendingNecromancySpawnDelayTicks = 0;
            return false;
        }
        if (pendingNecromancySpawnDelayTicks > 0) {
            pendingNecromancySpawnDelayTicks--;
        }
        return pendingNecromancySpawnDelayTicks <= 0;
    }

    public Set<TileCoordinate> drainNecromancySpawns() {
        if (pendingNecromancySpawns.isEmpty()) {
            return Set.of();
        }
        Set<TileCoordinate> result = new LinkedHashSet<>(pendingNecromancySpawns);
        pendingNecromancySpawns.clear();
        return result;
    }


}
