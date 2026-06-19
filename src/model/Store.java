package model;

import model.user.User;

import java.util.ArrayList;

public class Store {
    private static ArrayList<User> users;

    public static ArrayList<User> getUsers() {
        return users;
    }
}
