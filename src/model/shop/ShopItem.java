package model.shop;

import java.util.Locale;

/**
 * The fixed shop catalog: every permanent item plus the rotating daily offer.
 * Prices are per unit; grant sizes and caps live here so the controller,
 * listing, and tests read the same numbers.
 */
public enum ShopItem {
    POT("pot", 2_000, Currency.COINS,
            "permanently unlocks one greenhouse slot (max 20 pots total)"),
    PLANT_FOOD("plant-food", 3, Currency.DIAMONDS,
            "one plant food for the next level (max 3 stored)"),
    RANDOM_SEEDS("random-seeds", 1_000, Currency.COINS,
            "5 seed packets for one random unlocked plant"),
    SELECTED_SEEDS("selected-seeds", 5, Currency.DIAMONDS,
            "10 seed packets for a selected unlocked plant (requires -t)"),
    COIN_PACK("coin-pack", 5, Currency.DIAMONDS,
            "500 coins"),
    DAILY("daily", 1_600, Currency.COINS,
            "daily offer: 10 seed packets for today's plant (20% off 2000)");

    /** How many packets one random-seed bundle grants. */
    public static final int RANDOM_BUNDLE_PACKETS = 5;
    /** How many packets one selected-seed bundle grants. */
    public static final int SELECTED_BUNDLE_PACKETS = 10;
    /** Coins granted by one currency conversion. */
    public static final int COIN_PACK_COINS = 500;
    /** Packets granted by the daily offer. */
    public static final int DAILY_PACKETS = 10;
    /** Undiscounted daily price, shown in listings. */
    public static final int DAILY_BASE_PRICE = 2_000;
    /** Maximum plant food that can be stored for the next level. */
    public static final int MAX_STORED_PLANT_FOOD = 3;

    public enum Currency {
        COINS("coins"),
        DIAMONDS("diamonds");

        private final String label;

        Currency(String label) {
            this.label = label;
        }

        public String getLabel() {
            return label;
        }
    }

    private final String id;
    private final int unitPrice;
    private final Currency currency;
    private final String description;

    ShopItem(String id, int unitPrice, Currency currency, String description) {
        this.id = id;
        this.unitPrice = unitPrice;
        this.currency = currency;
        this.description = description;
    }

    public String getId() {
        return id;
    }

    public int getUnitPrice() {
        return unitPrice;
    }

    public Currency getCurrency() {
        return currency;
    }

    public String getDescription() {
        return description;
    }

    /** Resolves a typed item id, tolerating case; {@code null} when unknown. */
    public static ShopItem fromId(String token) {
        if (token == null) {
            return null;
        }
        String normalized = token.trim().toLowerCase(Locale.ROOT);
        for (ShopItem item : values()) {
            if (item.id.equals(normalized)) {
                return item;
            }
        }
        return null;
    }
}
