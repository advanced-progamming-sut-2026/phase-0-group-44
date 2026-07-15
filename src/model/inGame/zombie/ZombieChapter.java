package model.inGame.zombie;

import java.util.Locale;

public enum ZombieChapter {
    COMMON,
    ANCIENT_EGYPT,
    FROSTBITE_CAVES,
    BIG_WAVE_BEACH,
    DARK_AGES;

    public boolean isAllowedIn(String levelName) {
        if (this == COMMON) {
            return true;
        }
        String normalized = levelName == null ? "" : levelName.toLowerCase(Locale.ROOT);
        return switch (this) {
            case ANCIENT_EGYPT -> normalized.contains("egypt");
            case FROSTBITE_CAVES -> normalized.contains("frost") || normalized.contains("ice");
            case BIG_WAVE_BEACH -> normalized.contains("beach") || normalized.contains("wave");
            case DARK_AGES -> normalized.contains("dark") || normalized.contains("medieval");
            case COMMON -> true;
        };
    }
}
