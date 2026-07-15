package model.sim.sun;

import model.sim.SimulationSystem;
import model.sim.TickContext;

/**
 * A planted sun-producing plant. It produces one sun every production interval,
 * but only while its previously produced sun has been collected: an uncollected
 * sun blocks the next cycle, and the produced sun stays on the plant until then.
 */
public class SunProducer {

    private final String plantTypeLabel;
    private final int tileX;
    private final int tileY;
    private final int productionIntervalTicks;

    private int ticksSinceProduction;
    private Sun uncollectedSun;

    public SunProducer(
            String plantTypeLabel,
            int tileX,
            int tileY,
            int productionIntervalTicks
    ) {
        this.plantTypeLabel = plantTypeLabel;
        this.tileX = tileX;
        this.tileY = tileY;
        this.productionIntervalTicks = productionIntervalTicks;
    }

    public int getTileX() {
        return tileX;
    }

    public int getTileY() {
        return tileY;
    }

    public boolean hasUncollectedSun() {
        return uncollectedSun != null;
    }

    public Sun getUncollectedSun() {
        return uncollectedSun;
    }

    /**
     * Advances one tick. When the interval elapses and no produced sun is
     * waiting, it produces a sun, emits the exact message, and holds the sun.
     */
    public void tick(TickContext context) {
        if (uncollectedSun != null) {
            return;
        }

        ticksSinceProduction++;

        if (ticksSinceProduction >= productionIntervalTicks) {
            produce(context);
        }
    }

    private void produce(TickContext context) {
        uncollectedSun = Sun.onPlant(SunType.NORMAL, tileX, tileY);
        context.getWorld().getSuns().add(uncollectedSun);
        context.getWorld().recordProducedSun(uncollectedSun.getValue());
        ticksSinceProduction = 0;

        context.emit("plant " + plantTypeLabel + " produced a sun at ("
                + tileX + ", " + tileY + ")");
    }

    /** Called when this producer's sun is collected, so the next cycle can start. */
    public void onSunCollected(Sun sun) {
        if (uncollectedSun == sun) {
            uncollectedSun = null;
            ticksSinceProduction = 0;
        }
    }

    /** Ticks every producer in the world. */
    public static final class System implements SimulationSystem {
        @Override
        public void tick(TickContext context) {
            for (SunProducer producer : context.getWorld().getProducers()) {
                producer.tick(context);
            }
        }
    }
}
