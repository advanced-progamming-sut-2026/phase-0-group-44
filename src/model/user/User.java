package model.user;

import model.News;
import model.Result;
import model.enums.PlantType;
import model.miniGame.GreenHouse;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.List;

public class User {
    private String username;
    private String hashOfPassword;
    private String nickname;
    private String email;
    private String gender;

    private int progress;
    private int gamesPlayed;
    private Collection collection;
    //can be done through the collection class
    private ArrayList<PlantType> upgradedPlants;
    private Settings settings;
    private int coins;
    private int gems;
    private int mioPoint;
    private GreenHouse greenHouse;
    private String question;
    private String answerToQuestion;
    private boolean stayLoggedIn;


    //constructor


    public User(String username, String hashOfPassword, String nickname,
                String email, String gender, int progress,
                int gamesPlayed, Collection collection,
                ArrayList<PlantType> upgradedPlants, Settings settings, int coins,
                int gems, int mioPoint, GreenHouse greenHouse, String question,
                String answerToQuestion) {
        this.username = username;
        this.hashOfPassword = hashOfPassword;
        this.nickname = nickname;
        this.email = email;
        this.gender = gender;
        this.progress = progress;
        this.gamesPlayed = gamesPlayed;
        this.collection = collection;
        this.upgradedPlants = upgradedPlants;
        this.settings = settings;
        this.coins = coins;
        this.gems = gems;
        this.mioPoint = mioPoint;
        this.greenHouse = greenHouse;
        this.question = question;
        this.answerToQuestion = answerToQuestion;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getHashOfPassword() {
        return hashOfPassword;
    }

    public void setHashOfPassword(String hashOfPassword) {
        this.hashOfPassword = hashOfPassword;
    }

    public String getNickname() {
        return nickname;
    }

    public void setNickname(String nickname) {
        this.nickname = nickname;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getGender() {
        return gender;
    }

    public void setGender(String gender) {
        this.gender = gender;
    }

    public int getProgress() {
        return progress;
    }

    public void setProgress(int progress) {
        this.progress = progress;
    }

    public int getGamesPlayed() {
        return gamesPlayed;
    }

    public void setGamesPlayed(int gamesPlayed) {
        this.gamesPlayed = gamesPlayed;
    }

    public Collection getCollection() {
        return collection;
    }

    public void setCollection(Collection collection) {
        this.collection = collection;
    }

    public ArrayList<PlantType> getUpgradedPlants() {
        return upgradedPlants;
    }

    public void setUpgradedPlants(ArrayList<PlantType> upgradedPlants) {
        this.upgradedPlants = upgradedPlants;
    }

    public Settings getSettings() {
        return settings;
    }

    public void setSettings(Settings settings) {
        this.settings = settings;
    }

    public int getCoins() {
        return coins;
    }

    public void setCoins(int coins) {
        this.coins = coins;
    }

    public int getGems() {
        return gems;
    }

    public void setGems(int gems) {
        this.gems = gems;
    }

    public int getMioPoint() {
        return mioPoint;
    }

    public void setMioPoint(int mioPoint) {
        this.mioPoint = mioPoint;
    }

    public GreenHouse getGreenHouse() {
        return greenHouse;
    }

    public void setGreenHouse(GreenHouse greenHouse) {
        this.greenHouse = greenHouse;
    }

    public String getQuestion() {
        return question;
    }

    public void setQuestion(String question) {
        this.question = question;
    }

    public String getAnswerToQuestion() {
        return answerToQuestion;
    }

    public void setAnswerToQuestion(String answerToQuestion) {
        this.answerToQuestion = answerToQuestion;
    }

    public boolean isStayLoggedIn() {
        return stayLoggedIn;
    }

    public void setStayLoggedIn(boolean stayLoggedIn) {
        this.stayLoggedIn = stayLoggedIn;
    }

    //methods
    public static String hashPassword(String password)  {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] encodedHash = digest.digest(password.getBytes(StandardCharsets.UTF_8));
            StringBuilder hexString = new StringBuilder();
            for (byte b : encodedHash) {
                String hex = String.format("%02x", b);
                hexString.append(hex);
            }
            return hexString.toString();
        }catch (NoSuchAlgorithmException e){
            return null;
        }

    }


        public static boolean isUserNameValid(String name) {
            if (name == null) return false;
            return name.matches("^[a-zA-Z0-9-]+$");
        }

        public static Result<String> isPasswordValid(String pass) {
            Result<String> result = new Result<>();
            if (pass == null) {
                result.appendToMessage("Password cannot be null.");
                return result;
            }

            String allowedSpecialChars = "?><,'\";:\\/|\\[\\]\\}{+=()*&^%$#!";
            if (!pass.matches("^[a-zA-Z0-9" + allowedSpecialChars + "]+$")) {
                result.appendToMessage("Password contains invalid characters. Only letters, digits, and specific special characters are allowed.");
                return result;
            }

            List<String> errors = new ArrayList<>();
            if (pass.length() < 8) {
                errors.add("Minimum length is 8 characters.");
            }
            if (!pass.matches(".*[A-Z].*")) {
                errors.add("Must contain at least one uppercase letter (A-Z).");
            }
            if (!pass.matches(".*[a-z].*")) {
                errors.add("Must contain at least one lowercase letter (a-z).");
            }
            if (!pass.matches(".*[0-9].*")) {
                errors.add("Must contain at least one number (0-9).");
            }
            if (!pass.matches(".*[" + allowedSpecialChars + "].*")) {
                errors.add("Must contain at least one special character (" + allowedSpecialChars + ").");
            }

            if (errors.isEmpty()) {
                return null;
            } else {
                String errorMessage = String.join(" ", errors);
                result.appendToMessage("Weak password: " + errorMessage);
                return result;
            }
        }

        public static boolean isNickNameValid(String name) {
            if (name == null) return false;
            return name.matches("^.{3,30}$");
        }

        public static Result<String> isEmailNameValid(String email) {
            Result<String> result = new Result<>();
            if (email == null || email.isEmpty()) {
                result.appendToMessage("Email cannot be empty.");
                return result;
            }

            String forbiddenChars = "[?><,'\";:\\/\\[\\]\\}{+=()*&^%$#!]";
            if (email.matches(".*" + forbiddenChars + ".*")) {
                result.appendToMessage("Email contains invalid special characters.");
                return result;
            }

            int atIndex = email.indexOf('@');
            if (atIndex == -1) {
                result.appendToMessage("Email must contain '@'.");
                return result;
            }
            if (email.indexOf('@', atIndex + 1) != -1) {
                result.appendToMessage("Email must contain exactly one '@'.");
                return result;
            }

            String localPart = email.substring(0, atIndex);
            String domainPart = email.substring(atIndex + 1);

            if (localPart.isEmpty()) {
                result.appendToMessage("Local part (before @) cannot be empty.");
                return result;
            }
            if (!localPart.matches("^[A-Za-z0-9][A-Za-z0-9._-]*[A-Za-z0-9]$")) {
                result.appendToMessage("Local part must start and end with a letter/digit, and only contain letters, digits, dot (.), dash (-), or underscore (_).");
                return result;
            }
            if (localPart.contains("..")) {
                result.appendToMessage("Local part cannot contain consecutive dots (..).");
                return result;
            }

            if (domainPart.isEmpty()) {
                result.appendToMessage("Domain part (after @) cannot be empty.");
                return result;
            }
            if (!domainPart.contains(".")) {
                result.appendToMessage("Domain must contain at least one dot (.) for TLD.");
                return result;
            }
            if (domainPart.contains("..")) {
                result.appendToMessage("Domain cannot contain consecutive dots (..).");
                return result;
            }

            String[] domainParts = domainPart.split("\\.");
            for (String part : domainParts) {
                if (part.isEmpty()) {
                    result.appendToMessage("Domain cannot have empty parts (e.g., trailing or leading dots).");
                    return result;
                }
                if (!part.matches("^[A-Za-z0-9][A-Za-z0-9-]*[A-Za-z0-9]$")) {
                    result.appendToMessage("Each domain part must start and end with a letter/digit, and only contain letters, digits, or hyphens (-).");
                    return result;
                }
            }

            String tld = domainParts[domainParts.length - 1];
            if (tld.length() < 2) {
                result.appendToMessage("Domain  must be at least 2 characters long (e.g., .com, .ir).");
                return result;
            }

            return null;
        }

        public static boolean isGenderValid(String gender) {
            if (gender == null) return false;
            return gender.matches("^(male|female)$");
        }



}
