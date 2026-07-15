package model.enums;

import java.util.Locale;

public enum PlantCategory {
    SUN_PRODUCER,
    SHOOTER,
    LOBBER,
    EXPLOSIVE,
    MELEE,
    WALL_NUT,
    MODIFIER,
    STRIKE_THROUGH,
    HOMING,
    MINT;

    public static PlantCategory fromCsv(String value) {
        if (value == null) {
            throw new IllegalArgumentException("Plant category is missing.");
        }
        String normalized = value.trim().toUpperCase(Locale.ROOT)
                .replace('-', '_')
                .replace(' ', '_');
        return PlantCategory.valueOf(normalized);
    }
}
