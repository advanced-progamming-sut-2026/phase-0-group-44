package model.inGame.plant;

abstract class AbstractTimedBehavior implements PlantBehavior {
    protected boolean ready(Plant plant, double deltaSeconds, double interval) {
        return plant.advanceTimer(getClass().getName() + ".action", deltaSeconds, interval);
    }

    protected int boostedDamage(Plant plant, model.GameEngine engine) {
        return (int) Math.round(plant.getStats().getDamage()
                * engine.familyDamageMultiplier(plant.getCategory()));
    }
}
