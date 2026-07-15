package model.sim.board;

import model.enums.PlantType;

/** Supplies the {@link PlantSpec} for a plant type. */
public interface PlantSpecSource {

    /** @return the spec, or {@code null} if this source does not know the type */
    PlantSpec specOf(PlantType type);
}
