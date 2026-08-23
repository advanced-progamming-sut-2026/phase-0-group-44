package model.inGame.plant;

import model.GameEngine;
import model.enums.PlantCategory;

public class MintBehavior implements PlantBehavior {
    private final PlantCategory family;

    private static final double MINT_INTRO_SECONDS = 0.5; // guessed — verify
    private static final double MINT_OUTRO_SECONDS = 0.5; // guessed — verify

    public MintBehavior(PlantCategory family) {
        this.family = family;
    }

    @Override
    public void onPlant(Plant plant, GameEngine engine) {
        double duration = 5.0 + plant.getStats().getSpecial("MINT_DURATION", 0);
        engine.addFamilyBoost(family, duration);
        engine.applyPlantFoodToFamily(family, plant);
        if (plant.getStats().hasFlag("RESET_FAMILY_COOLDOWNS")) {
            engine.resetFamilyCooldowns(family, plant);
        }
        engine.recordEvent(plant.getEffectiveType() + " activated: boosted the " + family
                + " family for " + duration + "s and applied its instant effect, then was consumed.");

        // Gameplay effect (family boost) still applies instantly above. What
        // changes: the plant now lingers on the field for intro -> loop -> outro
        // so its PAM animation is actually visible, instead of expire()'ing in
        // the same tick it's planted (same fix as Gold Bloom's ACTIVE_ZERO_HP).
        plant.putState("ACTIVE_ZERO_HP", true);
        plant.putState("MINT_BOOST_DURATION", duration);
        plant.putState("MINT_CLIP", "intro");
    }

    @Override
    public void tick(Plant plant, GameEngine engine, double deltaSeconds) {
        double duration = plant.getState("MINT_BOOST_DURATION", Double.class, 5.0);
        double loopEndsAt = MINT_INTRO_SECONDS + duration;
        double outroEndsAt = loopEndsAt + MINT_OUTRO_SECONDS;
        double age = plant.getAgeSeconds();

        if (age < MINT_INTRO_SECONDS) {
            plant.putState("MINT_CLIP", "intro");
        } else if (age < loopEndsAt) {
            plant.putState("MINT_CLIP", "loop");
        } else if (age < outroEndsAt) {
            plant.putState("MINT_CLIP", "outro");
        } else {
            plant.expire(engine); // expired=true overrides ACTIVE_ZERO_HP, same as Gold Bloom/Grapeshot
        }
    }
}