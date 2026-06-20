package model;

import model.user.User;

import java.util.ArrayList;

public class Store {
    private static ArrayList<User> users;
    private static ArrayList<User> beforeTheQuestionUser;
    private static ArrayList<String> listOfQuestions;
    private static User loggedInUser;
    public static ArrayList<User> getUsers() {
        return users;
    }

    public static ArrayList<User> getBeforeTheQuestionUser() {
        return beforeTheQuestionUser;
    }

    public static ArrayList<String> getListOfQuestions() {
        return listOfQuestions;
    }

    public static User getLoggedInUser() {
        return loggedInUser;
    }

    public static void setLoggedInUser(User loggedInUser) {
        Store.loggedInUser = loggedInUser;
    }
}
