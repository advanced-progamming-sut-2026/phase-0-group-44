package model.sim.board;

import model.enums.PlantType;
import model.inGame.plant.PlantDefinition;
import model.inGame.plant.PlantRepository;
import model.sim.TickContext;

/**
 * Production {@link PlantSpecSource} that adapts a {@link PlantDefinition} from
 * the plant repository into a {@link PlantSpec}.
 *
 * <p>Cost, recharge and health come straight from the definition. Capability
 * flags (water-capable, support, fire, plant-food effect) are not yet present in
 * the plant data, so they default to false until the plant data layer carries
 * them. When the repository has no definition for a type (as while the data load
 * is unresolved), this returns {@code null} and planting reports missing data
 * rather than inventing values.</p>
 */
public class DefaultPlantSpecSource implements PlantSpecSource {

    private final PlantRepository plantRepository;

    public DefaultPlantSpecSource(PlantRepository plantRepository) {
        this.plantRepository = plantRepository;
    }

    @Override
    public PlantSpec specOf(PlantType type) {
        PlantDefinition definition = plantRepository.findByType(type);

        if (definition == null) {
            return null;
        }

        return PlantSpec.builder(type)
                .sunCost(definition.getCost())
                .rechargeTicks(definition.getRechargeTime() * TickContext.TICKS_PER_SECOND)
                .hp(definition.getHp())
                .build();
    }
}
