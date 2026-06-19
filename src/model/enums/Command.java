package model.enums;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

public enum Command {
    // News Menu

    MENU_NEWS_SHOW_UNREAD("^menu\\s+news\\s+show-unread$"),
    MENU_NEWS_SHOW_ALL("^menu\\s+news\\s+show-all$");

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
