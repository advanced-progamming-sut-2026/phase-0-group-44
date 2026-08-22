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

    // Mirrors BonusPlantBehavior's Grapeshot idle->attack timing: the model owns
    // the pose schedule via a one-shot "started" flag, and markAttacked() is only
    // called once that idle window has elapsed. GameplayScreen's generic
    // isAttackingWithin() branch then shows idle, then attack, with no bespoke
    // per-type animation code needed.
    private static final double GOLD_BLOOM_IDLE_SECONDS = 0.5;
    private static final double GOLD_BLOOM_ATTACK_SECONDS = 0.6; // matches GameplayScreen's default ATTACK_WINDOW_SECONDS fallback

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
            plant.putState("ACTIVE_ZERO_HP", true);
            engine.addSun(amount);
        }
    }

    @Override
    public void tick(Plant plant, GameEngine engine, double deltaSeconds) {
        if (mode == Mode.INSTANT) {
            tickInstantPose(plant, engine);
            return;
        }
        if (engine.plantHasUncollectedSun(plant)) {
            return; // طبق SunProducer دنیای B: تا جمع نشه،
            // چرخه‌ی بعدی شروع نمی‌شه (تایمر هم پیش نمی‌ره)
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
        plant.markAttacked(); // signals GameplayScreen to play the "special" production clip once per cycle
        engine.spawnPlantSun(plant, amount);
        for (int i = 1; i < sunCount; i++) {
            engine.spawnPlantSun(plant, amount);
        }
        if (plant.getStats().hasFlag("DOUBLE_SUN_CHANCE") && engine.getRandom().nextDouble() < 0.25) {
            engine.spawnPlantSun(plant, amount);
        }
    }

    private void tickInstantPose(Plant plant, GameEngine engine) {
        double age = plant.getAgeSeconds();
        boolean justStartedAttack = false;

        if (age >= GOLD_BLOOM_IDLE_SECONDS && !plant.getBooleanState("INSTANT_ATTACK_STARTED")) {
            plant.markAttacked();
            plant.putState("INSTANT_ATTACK_STARTED", true);
            justStartedAttack = true;
            System.out.println("[GoldBloom] attack started at age=" + age);
        }

        if (!justStartedAttack && age >= GOLD_BLOOM_IDLE_SECONDS + GOLD_BLOOM_ATTACK_SECONDS) {
            System.out.println("[GoldBloom] expiring at age=" + age);
            plant.expire(engine);
        }
    }

    @Override
    public void onPlantFood(Plant plant, GameEngine engine) {
        if (mode == Mode.RAMP_UP) {
            plant.putState("MAX_GROWTH", true);
        }
        plant.markAttacked(); // plant food burst also plays the production pose
        int perSun = plantFoodAmount / sunCount;
        int remainder = plantFoodAmount - perSun * sunCount;
        for (int i = 0; i < sunCount; i++) {
            engine.spawnPlantSun(plant, perSun + (i == 0 ? remainder : 0));
        }
    }
}