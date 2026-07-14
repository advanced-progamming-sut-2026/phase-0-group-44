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
