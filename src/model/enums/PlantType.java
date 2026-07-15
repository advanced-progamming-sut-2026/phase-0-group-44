package model.enums;

import java.util.Locale;

public enum PlantType {
    SUNFLOWER,
    TWIN_SUNFLOWER,
    SUN_SHROOM,
    PRIMAL_SUNFLOWER,
    GOLD_BLOOM,
    PEASHOOTER,
    REPEATER,
    THREEPEATER,
    SNOW_PEA,
    ROTOBAGA,
    PEA_POD,
    SPLIT_PEA,
    CITRON,
    CAULIPOWER,
    ELECTRIC_BLUEBERRY,
    BOWLING_BULB,
    CACTUS,
    FIRE_PEASHOOTER,
    STARFRUIT,
    GOO_PEASHOOTER,
    MEGA_GATLING_PEA,
    SEA_SHROOM,
    PUFF_SHROOM,
    FUME_SHROOM,
    CABBAGE_PULT,
    KERNEL_PULT,
    MELON_PULT,
    WINTER_MELON,
    PEPPER_PULT,
    POTATO_MINE,
    PRIMAL_POTATO_MINE,
    CHERRY_BOMB,
    SQUASH,
    GRAPESHOT,
    JALAPENO,
    DOOM_SHROOM,
    TANGLE_KELP,
    ICEBERG_LETTUCE,
    BONK_CHOY,
    PHAT_BEET,
    CHOMPER,
    WASABI_WHIP,
    KIWIBEAST,
    WALL_NUT,
    TALL_NUT,
    ENDURIAN,
    GARLIC,
    SWEET_POTATO,
    EXPLODE_O_NUT,
    PUMPKIN,
    SUN_BEAN,
    TORCHWOOD,
    MAGNET_SHROOM,
    HYPNO_SHROOM,
    CAT_TAIL,
    IMITATER,
    ICE_SHROOM,
    LILY_PAD,
    HOT_POTATO,
    GRAVE_BUSTER,
    ENLIGHTEN_MINT,
    APPEASE_MINT,
    ARMA_MINT,
    BOMBARD_MINT,
    ENFORCE_MINT,
    REINFORCE_MINT,
    ENCHANT_MINT,
    PIERCE_MINT,
    CATTAIL_MINT;

    public static PlantType fromCanonicalName(String name) {
        if (name == null || name.isBlank()) {
            throw new IllegalArgumentException("Plant name is blank.");
        }
        String normalized = name.trim()
                .toUpperCase(Locale.ROOT)
                .replace('–', '-')
                .replaceAll("[^A-Z0-9]+", "_")
                .replaceAll("^_+|_+$", "");
        if (normalized.equals("CAT_TAIL_MINT")) {
            normalized = "CATTAIL_MINT";
        }
        return PlantType.valueOf(normalized);
    }
}
