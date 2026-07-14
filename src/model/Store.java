package model;

import model.user.AuthFlowState;
import model.user.User;
import model.enums.MenuName;

import java.util.ArrayList;

public class Store {
    private static ArrayList<User> users = new ArrayList<>();
    private static ArrayList<User> beforeTheQuestionUser = new ArrayList<>();
    private static ArrayList<String> listOfQuestions = new ArrayList<>();
    private static User loggedInUser;
    private static AuthFlowState authFlow = new AuthFlowState();
    private static MenuName currentMenu = MenuName.REGISTER;
    private static boolean running = true;

    public static ArrayList<User> getUsers() {
        return users;
    }

    public static void setUsers(ArrayList<User> users) {
        Store.users = users == null ? new ArrayList<>() : users;
    }

    public static User findUser(String username) {
        if (username == null) {
            return null;
        }

        for (User user : users) {
            if (username.equals(user.getUsername())) {
                return user;
            }
        }

        return null;
    }

    public static ArrayList<User> getBeforeTheQuestionUser() {
        return beforeTheQuestionUser;
    }

    public static ArrayList<String> getListOfQuestions() {
        return listOfQuestions;
    }

    public static AuthFlowState getAuthFlow() {
        return authFlow;
    }

    public static void resetAuthFlow() {
        authFlow = new AuthFlowState();
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
