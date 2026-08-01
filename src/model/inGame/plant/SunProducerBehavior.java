package model.inGame.plant;

import model.GameEngine;
import model.enums.PlantType;

public class SunProducerBehavior extends AbstractTimedBehavior {
    public enum Mode {
        NORMAL,
        RAMP_UP,
        INSTANT
    }

    private final Mode mode;
    private final int baseAmount;
    private final int plantFoodAmount;
    private final int sunCount;

    public SunProducerBehavior(Mode mode, int baseAmount, int plantFoodAmount) {
        this(mode, baseAmount, plantFoodAmount, 1);
    }

    public SunProducerBehavior(Mode mode, int baseAmount, int plantFoodAmount, int sunCount) {
        this.mode = mode;
        this.baseAmount = baseAmount;
        this.plantFoodAmount = plantFoodAmount;
        this.sunCount = Math.max(1, sunCount);
    }

    @Override
    public void onPlant(Plant plant, GameEngine engine) {
        if (mode == Mode.INSTANT) {
            int amount = baseAmount + (int) plant.getStats().getSpecial("SUN_AMOUNT", 0);
            engine.addSun(amount);
            plant.expire(engine);
        }
    }

    @Override
    public void tick(Plant plant, GameEngine engine, double deltaSeconds) {
        if (mode == Mode.INSTANT) {
            return;
        }
        if (engine.plantHasUncollectedSun(plant)) {
            return; // طبق SunProducer دنیای B: تا جمع نشه، چرخه‌ی بعدی شروع نمی‌شه (تایمر هم پیش نمی‌ره)
        }
        double interval = plant.getStats().getActionInterval();
        if (!ready(plant, deltaSeconds, interval)) {
            return;
        }
        int amount = baseAmount;
        if (mode == Mode.RAMP_UP) {
            double growReduction = plant.getStats().getSpecial("GROW_TIME", 0);
            double stageTwo = Math.max(1, 24 + growReduction);
            double stageThree = Math.max(stageTwo + 1, 72 + growReduction);
            amount = plant.getBooleanState("MAX_GROWTH") || plant.getAgeSeconds() >= stageThree ? 75
                    : plant.getAgeSeconds() >= stageTwo ? 50 : 25;
        }
        if (engine.hasFamilyBoost(plant.getCategory())) {
            amount = (int) Math.round(amount * 1.5);
        }
        engine.spawnPlantSun(plant, amount);
        for (int i = 1; i < sunCount; i++) {
            engine.spawnPlantSun(plant, amount);
        }
        if (plant.getStats().hasFlag("DOUBLE_SUN_CHANCE") && engine.getRandom().nextDouble() < 0.25) {
            engine.spawnPlantSun(plant, amount);
        }
    }

    @Override
    public void onPlantFood(Plant plant, GameEngine engine) {
        if (mode == Mode.RAMP_UP) {
            plant.putState("MAX_GROWTH", true);
        }
        int perSun = plantFoodAmount / sunCount;
        int remainder = plantFoodAmount - perSun * sunCount;
        for (int i = 0; i < sunCount; i++) {
            engine.spawnPlantSun(plant, perSun + (i == 0 ? remainder : 0));
        }
    }
}
