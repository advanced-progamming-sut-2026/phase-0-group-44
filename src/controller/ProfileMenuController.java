package controller;

import model.Result;
import model.Store;
import model.user.User;

public class ProfileMenuController {
    public Result<String> changeUsername(String username) {
        Result<String> result = new Result<>();
        if (Store.getLoggedInUser().getUsername().equals(username)){
            result.appendToMessage("you can not change it to what it already is");
            return result;
        }
        for (User u:Store.getUsers()){
            if (u.getUsername().equals(username)){
                result.appendToMessage("username is already taken");
                return result;
            }
        }
        if (!User.isUserNameValid(username)){
            result.appendToMessage("wrong username format");
            return result;
        }
        Store.getLoggedInUser().setUsername(username);
        result.appendToMessage("username Changed!!!");
        return result;
    }
    public Result<String> changeNickname(String nickname) {
        Result<String> result = new Result<>();
        if (Store.getLoggedInUser().getNickname().equals(nickname)){
            result.appendToMessage("you can not change it to what it already is");
            return result;
        }

        if (!User.isNickNameValid(nickname)){
            result.appendToMessage("wrong nickname format");
            return result;
        }
        Store.getLoggedInUser().setNickname(nickname);
        result.appendToMessage("nickname Changed!!!");
        return result;
    }
    public Result<String> changeEmail(String email) {
        Result<String> result = new Result<>();
        if (Store.getLoggedInUser().getEmail().equals(email)){
            result.appendToMessage("you can not change it to what it already is");
            return result;
        }

        if (User.isEmailNameValid(email)!=null){

            return User.isEmailNameValid(email);
        }
        Store.getLoggedInUser().setEmail(email);
        result.appendToMessage("email Changed!!!");
        return result;
    }
    public Result<String> newPassword(String newPassword,String oldPassword) {
        Result<String> result = new Result<>();
        if (!User.hashPassword(oldPassword).equals(Store.getLoggedInUser())){
            result.appendToMessage("incorrect old password");
            return result;
        }
        if (User.isPasswordValid(newPassword)!=null){
            return User.isPasswordValid(newPassword);
        }

        String hashOfPassword=User.hashPassword(newPassword);
        Store.getLoggedInUser().setHashOfPassword(hashOfPassword);
        result.appendToMessage("password Changed!!!");
        return result;
    }
    public Result<String> showDetails() {
        Result<String> result = new Result<>();
        User u=Store.getLoggedInUser();
        String text = String.format(
                "Username: %s\nNickname: %s\nGames Played: %d\nCoins Earned: %d\nGems Earned: %d\nLevels Passed: %d\nHighest MioPoint: %d",
                u.getUsername(),
                u.getNickname(),
                u.getGamesPlayed(),
                u.getCoins(),
                u.getGems(),
                u.getProgress(),
                u.getMioPoint()
        );
        result.appendToMessage(text);
        return result;
    }
}
