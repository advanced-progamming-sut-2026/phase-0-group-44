package model.sim.zombie;

import java.util.List;

/** Supplies the zombie kinds a level can spawn, for wave composition. */
public interface ZombieSpecSource {

    /** The specs available to compose waves from; each must have a positive wave cost. */
    List<ZombieSpec> availableSpecs();
}
