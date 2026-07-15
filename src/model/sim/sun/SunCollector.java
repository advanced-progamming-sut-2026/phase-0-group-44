package model.sim.sun;

import model.sim.Damageable;
import model.sim.SimulationWorld;

/**
 * Collecting a sun at a tile, and the radioactive explosion that happens when a
 * radioactive sun is collected while still falling.
 */
public final class SunCollector {

    /** 150 damage to zombies in a 5x5 area centred on the sun. */
    public static final int RADIOACTIVE_ZOMBIE_DAMAGE = 150;
    public static final int RADIOACTIVE_ZOMBIE_RADIUS = 2;

    /** 80 damage to plants in a 3x3 area centred on the sun. */
    public static final int RADIOACTIVE_PLANT_DAMAGE = 80;
    public static final int RADIOACTIVE_PLANT_RADIUS = 1;

    /** The outcome of a collection attempt. */
    public static final class Outcome {
        private final boolean collected;
        private final boolean exploded;
        private final int gained;

        private Outcome(boolean collected, boolean exploded, int gained) {
            this.collected = collected;
            this.exploded = exploded;
            this.gained = gained;
        }

        public boolean isCollected() {
            return collected;
        }

        public boolean isExploded() {
            return exploded;
        }

        public int getGained() {
            return gained;
        }

        static Outcome nothing() {
            return new Outcome(false, false, 0);
        }

        static Outcome value(int gained) {
            return new Outcome(true, false, gained);
        }

        static Outcome explosion() {
            return new Outcome(true, true, 0);
        }
    }

    private final SimulationWorld world;

    public SunCollector(SimulationWorld world) {
        this.world = world;
    }

    /**
     * Collects the sun at the given tile. Absent sun or off-board coordinates
     * leave the balance unchanged.
     */
    public Outcome collectAt(int x, int y) {
        if (!world.isInsideBoard(x, y)) {
            return Outcome.nothing();
        }

        Sun target = null;

        for (Sun sun : world.getSuns()) {
            if (sun.isAt(x, y)) {
                target = sun;
                break;
            }
        }

        if (target == null) {
            return Outcome.nothing();
        }

        if (target.getType() == SunType.RADIOACTIVE && target.isFalling()) {
            world.getSuns().remove(target);
            detachFromProducer(target);
            explode(x, y);

            return Outcome.explosion();
        }

        int gained = target.getValue();
        world.addSun(gained);
        world.getSuns().remove(target);
        detachFromProducer(target);

        return Outcome.value(gained);
    }

    private void detachFromProducer(Sun sun) {
        for (SunProducer producer : world.getProducers()) {
            producer.onSunCollected(sun);
        }
    }

    private void explode(int x, int y) {
        damageInArea(world.getZombies(), x, y, RADIOACTIVE_ZOMBIE_RADIUS, RADIOACTIVE_ZOMBIE_DAMAGE);
        damageInArea(world.getPlants(), x, y, RADIOACTIVE_PLANT_RADIUS, RADIOACTIVE_PLANT_DAMAGE);
    }

    private void damageInArea(
            Iterable<Damageable> targets,
            int centerX,
            int centerY,
            int radius,
            int damage
    ) {
        for (Damageable target : targets) {
            int dx = Math.abs(target.getTileX() - centerX);
            int dy = Math.abs(target.getTileY() - centerY);

            if (dx <= radius && dy <= radius) {
                target.takeDamage(damage);
            }
        }
    }
}
