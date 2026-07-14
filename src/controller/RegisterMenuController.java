package controller;

import model.News;
import model.Result;
import model.Store;
import model.enums.PlantType;
import model.miniGame.GreenHouse;
import model.user.Collection;
import model.user.Settings;
import model.user.User;

import java.util.ArrayList;

public class RegisterMenuController {
    public Result<String> register(String username, String password,
                                   String repeatPass,String nickname,String email,
                                   String gender) {
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
                0,0,0,new GreenHouse(),"a","a");
        Store.getUsers().add(newUser);
        result.appendToMessage("which question you want to answer?");
        return result;
    }
    public Result<String> question(String question,String answer) {
        Result<String> result = new Result<>();
        for (String s:Store.getListOfQuestions()){
            if (s.equals(question)){
                if (answer==null){
                    result.appendToMessage("the answer should at least have 1 character");
                    return result;
                }
                Store.getBeforeTheQuestionUser().get(0).setQuestion(question);
                Store.getBeforeTheQuestionUser().get(0).setAnswerToQuestion(answer);
                Store.getUsers().add(Store.getBeforeTheQuestionUser().get(0));
                Store.getBeforeTheQuestionUser().clear();
                result.appendToMessage("your account has been created");

                return result;
            }
        }
        result.appendToMessage("choose one of the given questions");
        return result;
    }


}
