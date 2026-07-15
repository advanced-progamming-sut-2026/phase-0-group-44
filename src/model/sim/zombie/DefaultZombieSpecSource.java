package model.sim.zombie;

import model.inGame.zombie.ZombieDefinition;
import model.inGame.zombie.ZombieRepository;

import java.util.ArrayList;
import java.util.List;

/** Adapts the canonical mandatory zombie registry to wave composition. */
public class DefaultZombieSpecSource implements ZombieSpecSource {

    private final ZombieRepository zombieRepository;

    public DefaultZombieSpecSource(ZombieRepository zombieRepository) {
        this.zombieRepository = zombieRepository;
    }

    @Override
    public List<ZombieSpec> availableSpecs() {
        List<ZombieSpec> specs = new ArrayList<>();
        for (ZombieDefinition definition : zombieRepository.findAll()) {
            if (definition.isMandatory()) {
                specs.add(ZombieSpec.fromDefinition(definition));
            }
        }
        return specs;
    }
}
