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
