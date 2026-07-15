package model.sim.zombie;

import model.inGame.zombie.ZombieDefinition;
import model.inGame.zombie.ZombieRepository;
import model.enums.ZombieType;

import java.util.ArrayList;
import java.util.List;

/**
 * Production {@link ZombieSpecSource} adapting {@link ZombieDefinition}s from the
 * zombie repository.
 *
 * <p>Wave cost and health come from the definition; speed and eat rate default
 * until the zombie data carries them. While the zombie data layer is unresolved
 * the repository yields nothing, so this returns an empty list and wave spawning
 * reports an uncomposable configuration rather than inventing zombies.</p>
 */
public class DefaultZombieSpecSource implements ZombieSpecSource {

    private final ZombieRepository zombieRepository;

    public DefaultZombieSpecSource(ZombieRepository zombieRepository) {
        this.zombieRepository = zombieRepository;
    }

    @Override
    public List<ZombieSpec> availableSpecs() {
        List<ZombieSpec> specs = new ArrayList<>();

        for (ZombieDefinition definition : zombieRepository.findAll()) {
            ZombieType type = definition.getType();

            specs.add(ZombieSpec.builder(type == null ? "zombie" : type.name())
                    .waveCost(definition.getWaveCost())
                    .health(definition.getHealth())
                    .build());
        }

        return specs;
    }
}
