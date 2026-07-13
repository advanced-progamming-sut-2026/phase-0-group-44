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

    public boolean isSeen() {
        return seen;
    }

    public boolean isUnlocked() {
        return unlocked;
    }

    public void setSeen(boolean seen) {
        this.seen = seen;
    }

    public String showInfo() {
        return "Zombie Type: " + type + "\n"
                + "Seen: " + seen + "\n"
                + "Unlocked: " + unlocked;
    }
}

