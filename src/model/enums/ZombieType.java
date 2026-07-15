package model.enums;

import java.util.Locale;

public enum ZombieType {
    NORMAL,
    CONEHEAD,
    BUCKETHEAD,
    KNIGHT,
    BLOCKHEAD,
    GARGANTUAR,
    IMP,
    ALL_STAR,
    ARCADE_ZOMBIE,
    PARASOL_ZOMBIE,
    TURQUOISE_ZOMBIE,
    PROSPECTOR,
    PIANIST,
    NEWSPAPER_ZOMBIE,
    BARREL_ROLLER,
    RA_ZOMBIE,
    EXPLORER,
    TOMBRAISER,
    DODO_RIDER,
    HUNTER,
    TROGLOBITE,
    FISHERMAN,
    SNORKEL,
    OCTOPUS_ZOMBIE,
    JESTER,
    WIZARD,
    KING,
    DRAGON_IMP,

    /** Legacy save/token aliases retained for backward compatibility. */
    BASIC,
    POLE_VAULTING,
    FOOTBALL,
    SCREEN_DOOR,
    NEWSPAPER,
    DANCING,
    BALLOON;

    public static ZombieType fromToken(String token) {
        if (token == null || token.isBlank()) {
            return null;
        }
        String normalized = token.trim().toUpperCase(Locale.ROOT)
                .replace('-', '_')
                .replace(' ', '_')
                .replace("/", "_");
        return switch (normalized) {
            case "BASIC", "ZOMBIE", "NORMAL_ZOMBIE", "ZOMBIEDEFAULT" -> NORMAL;
            case "FOOTBALL", "ALLSTAR", "ALL_STAR_ZOMBIE" -> ALL_STAR;
            case "NEWSPAPER" -> NEWSPAPER_ZOMBIE;
            case "RA", "ZOMBIE_RA" -> RA_ZOMBIE;
            case "TOMB_RAISER", "TOMB_RAISER_ZOMBIE" -> TOMBRAISER;
            case "DODO", "DODO_RIDER_ZOMBIE" -> DODO_RIDER;
            case "OCTOPUS", "OCTOPUS_THROWER" -> OCTOPUS_ZOMBIE;
            case "PARASOL" -> PARASOL_ZOMBIE;
            case "TURQUOISE" -> TURQUOISE_ZOMBIE;
            case "BARREL", "BARREL_ROLLER_ZOMBIE" -> BARREL_ROLLER;
            case "DRAGON", "DRAGONIMP" -> DRAGON_IMP;
            case "JUGGLER", "JUGGLER_JESTER", "JESTER_JUGGLER" -> JESTER;
            default -> {
                try {
                    yield valueOf(normalized);
                } catch (IllegalArgumentException ignored) {
                    yield null;
                }
            }
        };
    }
}
