package model.inGame.zombie;

import model.enums.ZombieType;

import java.util.ArrayList;

public interface ZombieRepository {
    void load();
    ArrayList<ZombieDefinition> findAll();

    ZombieDefinition findByType(ZombieType type);

    ZombieDefinition findByName(String name);
}

