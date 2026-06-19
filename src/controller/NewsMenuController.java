package controller;

import model.News;
import model.Result;
import model.user.User;

import java.util.ArrayList;

public class NewsMenuController {

    public Result<ArrayList<News>> showAllNews(User user) {
        Result<ArrayList<News>> result = new Result<>();

        ArrayList<News> newsList = user.getNewsList();

        result.setStatus(true);
        result.setData(newsList);

        if (newsList.isEmpty()) {
            result.appendToMessage("No news found.");
            return result;
        }

        for (News news : newsList) {
            result.appendToMessage(news.getDisplayText());
             result.appendToMessage("\n\n");
        }

        return result;
    }

    public Result<ArrayList<News>> showUnreadNews(User user) {
        Result<ArrayList<News>> result = new Result<>();

        ArrayList<News> unreadNews = new ArrayList<>();

        for (News news : user.getNewsList()) {
            if (!news.isRead()) {
                unreadNews.add(news);
                result.appendToMessage(news.getDisplayText());
                result.appendToMessage("\n\n");
            }
        }

        for (News news : unreadNews) {
            news.markAsRead();
        }

        result.setStatus(true);
        result.setData(unreadNews);

        if (unreadNews.isEmpty()) {
            result.appendToMessage("No unread news.");
        }

        return result;
    }

}
