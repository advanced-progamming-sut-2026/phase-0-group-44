package model;

import model.user.User;
import model.enums.MenuName;

import java.util.ArrayList;

public class Store {
    private static ArrayList<User> users;
    private static ArrayList<User> beforeTheQuestionUser;
    private static ArrayList<String> listOfQuestions;
    private static User loggedInUser;
    private static MenuName currentMenu = MenuName.REGISTER;
    private static boolean running = true;

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

    public static MenuName getCurrentMenu() { return currentMenu; }

    public static void setCurrentMenu(MenuName currentMenu) { Store.currentMenu = currentMenu; }

    public static boolean isRunning() { return running; }

    public static void setRunning(boolean running ) { Store.running = running; }
}
