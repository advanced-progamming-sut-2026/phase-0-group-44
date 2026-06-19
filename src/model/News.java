package model;

import model.enums.NewsType;

public class News {
    private String title;
    private String description;
    private NewsType type;
    private boolean read;

    public News(String title, String description, NewsType type) {
        this.title = title;
        this.description = description;
        this.type = type;
        this.read = false;
    }

    public boolean isRead() {
        return read;
    }

    public void markAsRead() {
        this.read = true;
    }

    public String getDisplayText() {
        return "[" + type + "] " + title + "\n" + description;
    }
}
