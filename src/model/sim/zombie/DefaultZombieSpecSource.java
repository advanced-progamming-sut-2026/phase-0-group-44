package model.sim.zombie;

import model.inGame.zombie.ZombieDefinition;
import model.inGame.zombie.ZombieRepository;

import java.util.ArrayList;
import java.util.List;

/** Adapts every implemented canonical zombie row to wave composition. */
public class DefaultZombieSpecSource implements ZombieSpecSource {

    private final ZombieRepository zombieRepository;

    public DefaultZombieSpecSource(ZombieRepository zombieRepository) {
        this.zombieRepository = zombieRepository;
    }

    @Override
    public List<ZombieSpec> availableSpecs() {
        List<ZombieSpec> specs = new ArrayList<>();
        for (ZombieDefinition definition : zombieRepository.findAll()) {
            specs.add(ZombieSpec.fromDefinition(definition));
        }
        return specs;
    }
}
