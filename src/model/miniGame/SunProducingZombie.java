package model.miniGame;

import model.sim.zombie.ZombieInstance;

/** One non-replaceable stationary sun producer belonging to a single row. */
public final class SunProducingZombie {
    private final ZombieInstance zombie;
    private final IZombieLevelRules rules;
    private long nextProductionTick;
    private int productionCount;

    public SunProducingZombie(ZombieInstance zombie, IZombieLevelRules rules) {
        if (zombie == null || rules == null) {
            throw new IllegalArgumentException("A sun-producing zombie requires runtime rules.");
        }
        this.zombie = zombie;
        this.rules = rules;
        this.nextProductionTick = rules.getProducerBaseIntervalTicks();
    }

    public ZombieInstance getZombie() { return zombie; }
    public int getProductionCount() { return productionCount; }
    public long getNextProductionTick() { return nextProductionTick; }

    /**
     * interval(t) = max(minInterval,
     * baseInterval - floor(t / rampPeriod) * intervalStep).
     */
    public int intervalAt(long elapsedTicks) {
        long stages = Math.max(0L, elapsedTicks) / rules.getProducerRampPeriodTicks();
        long reduced = (long) rules.getProducerBaseIntervalTicks()
                - stages * rules.getProducerIntervalStepTicks();
        return (int) Math.max(rules.getProducerMinimumIntervalTicks(), reduced);
    }

    public int produceIfDue(long completedTick) {
        if (zombie.isDead() || completedTick < nextProductionTick) {
            return 0;
        }
        productionCount++;
        nextProductionTick = completedTick + intervalAt(completedTick);
        return rules.getProducerSunAmount();
    }
}
