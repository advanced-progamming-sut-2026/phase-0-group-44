package model.enums;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

public enum Command {
    // Menu framework (available in every menu)
    MENU_ENTER("^menu\\s+enter\\s+(\\S+(?:\\s+\\S+)*)$"),
    MENU_SHOW_CURRENT("^menu\\s+show\\s+current$"),
    MENU_EXIT("^menu\\s+exit$"),

    // Register Menu
    REGISTER("^register\\s+-u\\s+(\\S+)\\s+-p\\s+(\\S+)\\s+(\\S+)\\s+-n\\s+(.+?)\\s+-e\\s+(\\S+)\\s+-g\\s+(\\S+)$"),
    PICK_QUESTION("^pick\\s+question\\s+-q\\s+(\\d+)\\s+-a\\s+(.+?)\\s+-c\\s+(.+)$"),

    // Login Menu
    LOGIN("^login\\s+-u\\s+(\\S+)\\s+-p\\s+(\\S+)(\\s+-stay-logged-in)?$"),
    FORGET_PASSWORD("^forget\\s+password\\s+-u\\s+(\\S+)\\s+-e\\s+(\\S+)$"),
    ANSWER("^answer\\s+-a\\s+(.+)$"),
    NEW_PASSWORD("^new\\s+password\\s+-p\\s+(\\S+)$"),

    // Main Menu
    MENU_LOGOUT("^menu\\s+logout$"),

    // Game Menu
    MENU_ENTER_CHAPTER("^menu\\s+enter\\s+chapter\\s+-c\\s+(.+)$"),
    MENU_GREENHOUSE("^menu\\s+greenhouse$"),
    MENU_TRAVEL_LOG("^menu\\s+travel-log$"),
    MENU_LEADERBOARD("^menu\\s+leaderboard$"),
    MENU_COIN_WALLET("^menu\\s+coin-wallet$"),
    MENU_GEM_WALLET("^menu\\s+gem-wallet$"),
    MENU_CHEAT_ADD("^menu\\s+cheat\\s+add\\s+(-?\\d+)\\s+(\\S+)$"),

    // Greenhouse
    SHOW_GREENHOUSE("^show\\s+greenhouse$"),
    PLANT_POT("^plant\\s+pot\\s+at\\s+\\(\\s*(-?\\d+)\\s*,\\s*(-?\\d+)\\s*\\)$"),
    COLLECT_GREENHOUSE("^collect\\s+\\(\\s*(-?\\d+)\\s*,\\s*(-?\\d+)\\s*\\)$"),
    GROW_GREENHOUSE("^grow\\s+\\(\\s*(-?\\d+)\\s*,\\s*(-?\\d+)\\s*\\)$"),
    ENTER_SHOP("^enter\\s+shop$"),

    // Shop
    SHOP_LIST("^shop\\s+list$"),
    SHOP_DAILY("^shop\\s+daily$"),
    SHOP_BUY("^shop\\s+buy\\s+-i\\s+(\\S+)\\s+-n\\s+(-?\\d+)(?:\\s+-t\\s+(.+))?$"),

    // Settings Menu
    MENU_SETTINGS_CHANGE_DIFFICULTY(
            "^menu\\s+settings\\s+change-difficulty\\s+-l\\s+(-?\\d+)$"),

    // Profile Menu
    MENU_PROFILE_CHANGE_USERNAME("^menu\\s+profile\\s+change-username\\s+-u\\s+(\\S+)$"),
    MENU_PROFILE_CHANGE_NICKNAME("^menu\\s+profile\\s+change-nickname\\s+-u\\s+(.+)$"),
    MENU_PROFILE_CHANGE_EMAIL("^menu\\s+profile\\s+change-email\\s+-e\\s+(\\S+)$"),
    MENU_PROFILE_CHANGE_PASSWORD(
            "^menu\\s+profile\\s+change-password\\s+-p\\s+(\\S+)\\s+-o\\s+(\\S+)$"),
    MENU_PROFILE_SHOW_INFO("^menu\\s+profile\\s+show-info$"),

    // Plant Selection Menu
    SHOW_ALL_PLANTS("^show\\s+all\\s+plants$"),
    SHOW_AVAILABLE_PLANTS("^show\\s+available\\s+plants$"),
    ADD_PLANT("^add\\s+plant\\s+-t\\s+(.+)$"),
    REMOVE_PLANT("^remove\\s+plant\\s+-t\\s+(.+)$"),
    BOOST_PLANT("^boost\\s+plant\\s+-t\\s+(.+)$"),
    START_GAME("^start\\s+game$"),

    // Gameplay
    ADVANCE_TIME("^advance\\s+time\\s+-t\\s+(\\d+)\\s+ticks$"),
    START_ZOMBIE_WAVES("^start\\s+zombie\\s+waves$"),
    COLLECT_SUN("^collect\\s+sun\\s+-l\\s+\\(\\s*(-?\\d+)\\s*,\\s*(-?\\d+)\\s*\\)$"),
    SHOW_SUN_AMOUNT("^show\\s+sun\\s+amount$"),
    CHEAT_ADD_SUNS("^cheat\\s+add\\s+-n\\s+(\\d+)\\s+suns$"),

    // Board / planting / plant food (gameplay context)
    PLANT_PLANT("^plant\\s+plant\\s+-t\\s+(\\S+)\\s+-l\\s+\\(\\s*(-?\\d+)\\s*,\\s*(-?\\d+)\\s*\\)$"),
    PLUCK_PLANT("^pluck\\s+plant\\s+-l\\s+\\(\\s*(-?\\d+)\\s*,\\s*(-?\\d+)\\s*\\)$"),
    CHEAT_REMOVE_COOLDOWN("^cheat\\s+remove-cooldown$"),
    FEED_PLANT("^feed\\s+plant\\s+-l\\s+\\(\\s*(-?\\d+)\\s*,\\s*(-?\\d+)\\s*\\)$"),
    CHEAT_ADD_PLANT_FOOD("^cheat\\s+add-plant-food$"),
    SHOW_MAP("^show\\s+map$"),
    SHOW_PLANTS_STATUS("^show\\s+plants\\s+status$"),
    SHOW_TILE_STATUS("^show\\s+tile\\s+status\\s+-l\\s+\\(\\s*(-?\\d+)\\s*,\\s*(-?\\d+)\\s*\\)$"),
    RELEASE_NUKE("^release\\s+the\\s+nuke$"),
    ZOMBIES_INFO("^zombies\\s+info$"),
    CHEAT_SPAWN_ZOMBIE(
            "^cheat\\s+spawn-zombie\\s+-t\\s+(.+?)\\s+-l\\s+\\(?\\s*(-?\\d+)\\s*,\\s*(-?\\d+)\\s*\\)?$"),

    // News Menu

    MENU_NEWS_SHOW_UNREAD("^menu\\s+news\\s+show-unread$"),
    MENU_NEWS_SHOW_ALL("^menu\\s+news\\s+show-all$"),

    // Collection Menu
    MENU_COLLECTION_SHOW_PLANTS("^menu\\s+collection\\s+show-plants$"),
    MENU_COLLECTION_SHOW_ALL_PLANTS("^menu\\s+collection\\s+show-all-plants$"),
    MENU_COLLECTION_SHOW_ZOMBIES("^menu\\s+collection\\s+show-zombies$"),
    MENU_COLLECTION_SHOW_ALL_ZOMBIES("^menu\\s+collection\\s+show-all-zombies$"),
    MENU_COLLECTION_SHOW_PLANT("^menu\\s+collection\\s+show-plant\\s+-p\\s+(.+)$"),
    MENU_COLLECTION_SHOW_ZOMBIE("^menu\\s+collection\\s+show-zombie\\s+-z\\s+(.+)$"),
    MENU_COLLECTION_UPGRADE_PLANT("^menu\\s+collection\\s+upgrade-plant\\s+-p\\s+(.+)$"),
    MENU_COLLECTION_PURCHASE_PLANT("^menu\\s+collection\\s+purchase-plant\\s+-p\\s+(.+)$");


    private final Pattern pattern;

    Command(String regex) {
        this.pattern = Pattern.compile(regex);
    }

    public Matcher getMatcher(String input) {
        return pattern.matcher(input.trim());
    }

    public boolean matches(String input) {
        return getMatcher(input).matches();
    }
}
