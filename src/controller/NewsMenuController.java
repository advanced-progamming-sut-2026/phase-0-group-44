package controller;

import model.News;
import model.Result;
import model.Store;
import model.user.User;
import service.UserService;

import java.util.ArrayList;

/** The news menu: listing all news, and listing unread news (which marks them read). */
public class NewsMenuController {

    private final UserService userService;

    public NewsMenuController(UserService userService) {
        this.userService = userService;
    }

    /** Handles {@code menu news show-all}. */
    public Result<ArrayList<News>> showAllNews() {
        Result<ArrayList<News>> result = new Result<>();
        User user = requireUser(result);

        if (user == null) {
            return result;
        }

        ArrayList<News> newsList = user.getNewsList();

        result.setStatus(true);
        result.setData(newsList);

        if (newsList.isEmpty()) {
            result.appendToMessage("no news");
            return result;
        }

        appendAll(result, newsList);

        return result;
    }

    /**
     * Handles {@code menu news show-unread}. Every entry returned is marked read
     * and the change is persisted, so the next call will not return it again.
     */
    public Result<ArrayList<News>> showUnreadNews() {
        Result<ArrayList<News>> result = new Result<>();
        User user = requireUser(result);

        if (user == null) {
            return result;
        }

        ArrayList<News> unread = new ArrayList<>();

        for (News news : user.getNewsList()) {
            if (!news.isRead()) {
                unread.add(news);
            }
        }

        result.setStatus(true);
        result.setData(unread);

        if (unread.isEmpty()) {
            result.appendToMessage("no unread news");
            return result;
        }

        appendAll(result, unread);

        for (News news : unread) {
            news.markAsRead();
        }

        userService.updateUser(user);

        return result;
    }

    private void appendAll(Result<ArrayList<News>> result, ArrayList<News> list) {
        for (int i = 0; i < list.size(); i++) {
            result.appendToMessage(list.get(i).getDisplayText());

            if (i < list.size() - 1) {
                result.appendToMessage("\n\n");
            }
        }
    }

    private User requireUser(Result<ArrayList<News>> result) {
        User user = Store.getLoggedInUser();

        if (user == null) {
            result.appendToMessage("no user is logged in");
        }

        return user;
    }
}
