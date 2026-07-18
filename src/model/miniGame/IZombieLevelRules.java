package model.miniGame;

import model.enums.PlantType;
import model.enums.ZombieType;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/** Explicit board, economy, roster, and sun-production balance for I, Zombie. */
public final class IZombieLevelRules {
    private final int level;
    private final int redLineColumn;
    private final int preplacedPlantCount;
    private final int plantPressure;
    private final int timeLimitTicks;
    private final int producerBaseIntervalTicks;
    private final int producerMinimumIntervalTicks;
    private final int producerRampPeriodTicks;
    private final int producerIntervalStepTicks;
    private final int producerSunAmount;
    private final List<PlantType> plantPool;
    private final Map<ZombieType, Integer> zombiePrices;

    public IZombieLevelRules(
            int level,
            int redLineColumn,
            int preplacedPlantCount,
            int plantPressure,
            int timeLimitTicks,
            int producerBaseIntervalTicks,
            int producerMinimumIntervalTicks,
            int producerRampPeriodTicks,
            int producerIntervalStepTicks,
            int producerSunAmount,
            List<PlantType> plantPool,
            Map<ZombieType, Integer> zombiePrices
    ) {
        if (level < 1 || level > 3 || redLineColumn < 1
                || preplacedPlantCount < 5 || plantPressure <= 0 || timeLimitTicks <= 0
                || producerBaseIntervalTicks <= 0 || producerMinimumIntervalTicks <= 0
                || producerMinimumIntervalTicks > producerBaseIntervalTicks
                || producerRampPeriodTicks <= 0 || producerIntervalStepTicks <= 0
                || producerSunAmount <= 0 || plantPool == null || plantPool.isEmpty()
                || zombiePrices == null || zombiePrices.size() != 5
                || zombiePrices.values().stream().anyMatch(price -> price == null || price <= 0)) {
            throw new IllegalArgumentException("Invalid I, Zombie level rules.");
        }
        this.level = level;
        this.redLineColumn = redLineColumn;
        this.preplacedPlantCount = preplacedPlantCount;
        this.plantPressure = plantPressure;
        this.timeLimitTicks = timeLimitTicks;
        this.producerBaseIntervalTicks = producerBaseIntervalTicks;
        this.producerMinimumIntervalTicks = producerMinimumIntervalTicks;
        this.producerRampPeriodTicks = producerRampPeriodTicks;
        this.producerIntervalStepTicks = producerIntervalStepTicks;
        this.producerSunAmount = producerSunAmount;
        this.plantPool = List.copyOf(plantPool);
        this.zombiePrices = Collections.unmodifiableMap(new LinkedHashMap<>(zombiePrices));
    }

    public int getLevel() { return level; }
    public int getRedLineColumn() { return redLineColumn; }
    public int getPreplacedPlantCount() { return preplacedPlantCount; }
    public int getPlantPressure() { return plantPressure; }
    public int getTimeLimitTicks() { return timeLimitTicks; }
    public int getProducerBaseIntervalTicks() { return producerBaseIntervalTicks; }
    public int getProducerMinimumIntervalTicks() { return producerMinimumIntervalTicks; }
    public int getProducerRampPeriodTicks() { return producerRampPeriodTicks; }
    public int getProducerIntervalStepTicks() { return producerIntervalStepTicks; }
    public int getProducerSunAmount() { return producerSunAmount; }
    public List<PlantType> getPlantPool() { return plantPool; }
    public Map<ZombieType, Integer> getZombiePrices() { return zombiePrices; }

    public int priceOf(ZombieType type) {
        return zombiePrices.getOrDefault(type, -1);
    }

    public int minimumZombiePrice() {
        return zombiePrices.values().stream().mapToInt(Integer::intValue).min().orElseThrow();
    }

    public boolean isHarderThan(IZombieLevelRules previous) {
        return previous != null
                && level == previous.level + 1
                && redLineColumn > previous.redLineColumn
                && preplacedPlantCount > previous.preplacedPlantCount
                && plantPressure > previous.plantPressure
                && timeLimitTicks < previous.timeLimitTicks
                && producerBaseIntervalTicks > previous.producerBaseIntervalTicks;
    }
}
