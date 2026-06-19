package controller;

import model.News;
import model.Result;
import model.Store;
import model.enums.PlantType;
import model.inGame.plant.Plant;
import model.miniGame.GreenHouse;
import model.user.Collection;
import model.user.Settings;
import model.user.User;

import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;

public class RegisterMenuController {
    public Result<String> register(String username, String password,
                                   String repeatPass,String nickname,String email,
                                   String gender) throws NoSuchAlgorithmException {
        Result<String> result = new Result<>();
        if (!User.isUserNameValid(username)){
            result.appendToMessage("incorrect username format");
            return  result;
        }
        for (User u: Store.getUsers()){
            if(u.getUsername().equals(username)){
                result.appendToMessage("username already taken");
                return  result;
            }
        }
        if (User.isPasswordValid(password)!=null){
            return User.isPasswordValid(password);
        }
        if (!password.equals(repeatPass)){
            result.appendToMessage("wrong , you shall not pass," +
                    "repeat the password correctly or go back");
            return  result;
        }
        if (!User.isNickNameValid(nickname)){
            result.appendToMessage("make sure it's between 3 and 30");
            return  result;
        }
        if (User.isEmailNameValid(email)!=null){
            return User.isEmailNameValid(email);
        }
        if (!User.isGenderValid(gender)){
            result.appendToMessage("hmm,Not under this roof");
            return  result;
        }
        String hashOfPassword=User.hashPassword(password);
        User newUser=new User(username,hashOfPassword,nickname,email,gender,0,
                0,new Collection(),new ArrayList<PlantType>(),new Settings(),
                0,0,0,new GreenHouse());
        Store.getUsers().add(newUser);
        result.appendToMessage("your account has been created");
        return result;
    }


}
