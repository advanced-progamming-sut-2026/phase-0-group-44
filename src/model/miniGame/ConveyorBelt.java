package model.miniGame;

import model.sim.TickContext;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Attempt-local conveyor. It supplies configured bowling plants on the shared
 * simulation clock and never creates sun or normal seed packets.
 */
public final class ConveyorBelt {
    private final int capacity;
    private final int arrivalIntervalTicks;
    private final List<BowlingPlantType> pattern;
    private final List<ConveyorPacket> packets = new ArrayList<>();
    private int nextPacketId = 1;
    private int nextPatternIndex;
    private long nextArrivalTick;

    public ConveyorBelt(
            int capacity,
            int initialPacketCount,
            int arrivalIntervalTicks,
            List<BowlingPlantType> pattern
    ) {
        if (capacity <= 0 || initialPacketCount < 0 || initialPacketCount > capacity
                || arrivalIntervalTicks <= 0 || pattern == null || pattern.isEmpty()) {
            throw new IllegalArgumentException("Invalid conveyor configuration.");
        }
        this.capacity = capacity;
        this.arrivalIntervalTicks = arrivalIntervalTicks;
        this.pattern = List.copyOf(pattern);
        for (int index = 0; index < initialPacketCount; index++) {
            addNextPacket();
        }
        this.nextArrivalTick = arrivalIntervalTicks;
    }

    public List<ConveyorPacket> getPackets() {
        return Collections.unmodifiableList(packets);
    }

    public List<ConveyorPacket> getAvailablePackets() {
        return packets.stream().filter(ConveyorPacket::isAvailable).toList();
    }

    public int getAvailableCount() {
        return (int) packets.stream().filter(ConveyorPacket::isAvailable).count();
    }

    public ConveyorPacket find(int id) {
        for (ConveyorPacket packet : packets) {
            if (packet.getId() == id) {
                return packet;
            }
        }
        return null;
    }

    /** Returns and consumes an available item, or null without mutation. */
    public ConveyorPacket take(int id) {
        ConveyorPacket packet = find(id);
        return packet != null && packet.use() ? packet : null;
    }

    public void tick(TickContext context) {
        long completedTick = context.getCurrentTick() + 1;
        if (completedTick < nextArrivalTick || getAvailableCount() >= capacity) {
            return;
        }
        ConveyorPacket packet = addNextPacket();
        nextArrivalTick = completedTick + arrivalIntervalTicks;
        context.emit("conveyor delivered packet " + packet.getId() + " ("
                + packet.getPlantType().getDisplayName() + ")");
    }

    private ConveyorPacket addNextPacket() {
        BowlingPlantType type = pattern.get(nextPatternIndex);
        nextPatternIndex = (nextPatternIndex + 1) % pattern.size();
        ConveyorPacket packet = new ConveyorPacket(nextPacketId++, type);
        packets.add(packet);
        return packet;
    }
}
