package model.inGame.plant;

import model.GameEngine;
import model.enums.PlantCategory;

public class MintBehavior implements PlantBehavior {
    private final PlantCategory family;

    public MintBehavior(PlantCategory family) {
        this.family = family;
    }

    @Override
    public void onPlant(Plant plant, GameEngine engine) {
        double duration = 5.0 + plant.getStats().getSpecial("MINT_DURATION", 0);
        engine.addFamilyBoost(family, duration);
        engine.applyPlantFoodToFamily(family, plant);
        if (plant.getStats().hasFlag("RESET_FAMILY_COOLDOWNS")) {
            engine.resetFamilyCooldowns(family);
        }
        plant.expire(engine);
    }
}
