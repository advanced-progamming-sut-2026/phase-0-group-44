package model.enums;

import java.util.EnumSet;
import java.util.Locale;
import java.util.Set;

public enum PlantTag {
    DAY,
    NIGHT,
    SHROOM,
    RAMP_UP,
    PEA,
    ICE,
    FIRE,
    STACK,
    CHARGE,
    MAGIC,
    POISON,
    WATER,
    AOE,
    TRAP,
    MOVE_ZOMBIES,
    SUN,
    EXPLOSIVE;

    public static Set<PlantTag> parseCsv(String csv) {
        EnumSet<PlantTag> result = EnumSet.noneOf(PlantTag.class);
        if (csv == null || csv.isBlank() || csv.trim().equals("-")) {
            return result;
        }
        for (String raw : csv.split(",")) {
            String normalized = raw.trim().toUpperCase(Locale.ROOT)
                    .replace("WRAMP-UP", "RAMP_UP")
                    .replace("RAMP-UP", "RAMP_UP")
                    .replace("MOVEZOMBIES", "MOVE_ZOMBIES")
                    .replace("AOE", "AOE")
                    .replace('-', '_')
                    .replace(' ', '_');
            result.add(PlantTag.valueOf(normalized));
        }
        return result;
    }
}
