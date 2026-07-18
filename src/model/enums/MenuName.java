package model.enums;

public enum MenuName {
    REGISTER("register"),
    LOGIN("login"),
    MAIN("main"),
    GAME("game"),
    SETTINGS("settings"),
    NETWORK("network"),
    NEWS("news"),
    PROFILE("profile"),
    COLLECTION("collection"),
    GREENHOUSE("greenhouse"),
    TRAVEL_LOG("travel-log"),
    MINIGAME("minigame"),
    SCORED_GAME("scored-game"),
    SHOP("shop"),
    PLANT_SELECTION("plant-selection"),
    GAMEPLAY("gameplay");

    private final String token;

    MenuName(String token) {
        this.token = token;
    }

    /** The word the player types in {@code menu enter <menu_name>}. */
    public String getToken() {
        return token;
    }

    public String getDisplayName() {
        return token + " menu";
    }

    /**
     * Resolves a typed menu name, tolerating case, surrounding spaces and an
     * optional trailing "menu" word.
     *
     * @return the menu, or {@code null} when the name is unknown
     */
    public static MenuName fromToken(String input) {
        if (input == null) {
            return null;
        }

        String normalized = input.trim()
                .toLowerCase()
                .replace("-", "")
                .replace("_", "")
                .replace(" ", "");

        if (normalized.endsWith("menu") && normalized.length() > 4) {
            normalized = normalized.substring(0, normalized.length() - 4);
        }

        for (MenuName menu : values()) {
            String normalizedToken = menu.token
                    .replace("-", "")
                    .replace("_", "")
                    .replace(" ", "");
            if (normalizedToken.equals(normalized)) {
                return menu;
            }
        }

        return null;
    }
}
