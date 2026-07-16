package model.miniGame;

import model.enums.PlantType;

public final class SeedPacket {
    private final String id;
    private final PlantType plantType;
    private final long droppedAtTick;
    private final long expiresAtTick;
    private boolean used;

    public SeedPacket(String id, PlantType plantType, long droppedAtTick, long lifetimeTicks) {
        if (lifetimeTicks < 1) throw new IllegalArgumentException("Packet lifetime must be positive.");
        this.id = id; this.plantType = plantType; this.droppedAtTick = droppedAtTick;
        this.expiresAtTick = droppedAtTick + lifetimeTicks;
    }
    public String getId() { return id; }
    public PlantType getPlantType() { return plantType; }
    public long getDroppedAtTick() { return droppedAtTick; }
    public long getExpiresAtTick() { return expiresAtTick; }
    public boolean isUsed() { return used; }
    public boolean isExpired(long tick) { return !used && tick >= expiresAtTick; }
    public boolean isAvailable(long tick) { return !used && tick < expiresAtTick; }
    public void use(long tick) {
        if (!isAvailable(tick)) throw new IllegalStateException("Seed packet is expired or already used.");
        used = true;
    }
}
