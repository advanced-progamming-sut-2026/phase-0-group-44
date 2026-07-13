package model.inGame.zombie;

import java.util.ArrayList;

public interface ZombieRepository {
    ArrayList<ZombieDefinition> findAll();

    ZombieDefinition findByType(ZombieType type);

    ZombieDefinition findByName(String name);
}

