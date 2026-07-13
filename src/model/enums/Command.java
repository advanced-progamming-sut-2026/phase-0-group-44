package model.enums;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

public enum Command {
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
