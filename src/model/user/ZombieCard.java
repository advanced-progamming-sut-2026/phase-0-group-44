package model.user;

import model.enums.ZombieType;

public class ZombieCard {
    private ZombieType type;
    private boolean seen;
    private boolean unlocked;

    public ZombieCard() {
    }

    public ZombieCard(
            ZombieType type,
            boolean seen,
            boolean unlocked
    ) {
        this.type = type;
        this.seen = seen;
        this.unlocked = unlocked;
    }

    public ZombieType getType() {
        return type;
    }

    public boolean isUnlocked() {
        return unlocked;
    }

}

