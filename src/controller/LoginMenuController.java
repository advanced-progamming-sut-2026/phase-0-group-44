package controller;

import model.Result;
import model.Store;
import model.user.User;

public class LoginMenuController {
    public Result<String> login(String username, String password,Boolean stayLoggedIn) {
        Result<String> result = new Result<>();
        for (User u: Store.getUsers()){
           if (u.getUsername().equals(username)){
               if (!u.getHashOfPassword().equals(User.hashPassword(password))){
                   result.appendToMessage("wrong password");
                   return result;
               }
               Store.setLoggedInUser(u);
               u.setStayLoggedIn(stayLoggedIn);
               result.appendToMessage("welcome");
               return result;
           }
        }
        result.appendToMessage("username not found");
        return result;
    }
    public Result<String> forgotPassword(String username, String email,String answer) {
        Result<String> result = new Result<>();
        for (User u: Store.getUsers()){
            if (u.getUsername().equals(username)){
                if (!u.getEmail().equals(email)){
                    result.appendToMessage("wrong email");
                    return result;
                }
                if (!u.getAnswerToQuestion().equals(answer)){
                    //TODO change menu
                    result.appendToMessage("answer");
                    return result;
                }
                Store.getBeforeTheQuestionUser().add(u);
                result.appendToMessage("give the answer to this question"+u.getQuestion());
                return result;
            }
        }
        result.appendToMessage("username not found");
        return result;
    }
    public Result<String> checkTheAnswer(String answer) {
        Result<String> result = new Result<>();

        if (!Store.getBeforeTheQuestionUser().get(0).getAnswerToQuestion().equals(answer)){
            //TODO change menu
            result.appendToMessage("answer is wrong");
            Store.getBeforeTheQuestionUser().clear();
            return result;
        }

        result.appendToMessage("give the new password");
        return result;
    }
    public Result<String> newPassword(String password) {
        Result<String> result = new Result<>();
        if (User.isPasswordValid(password)!=null){
            return User.isPasswordValid(password);
        }
        String hashOfPassword=User.hashPassword(password);
        Store.getBeforeTheQuestionUser().get(0).setHashOfPassword(hashOfPassword);
        result.appendToMessage("password Changed!!!");
        return result;
    }
}

