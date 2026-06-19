package model.user;

import model.News;
import model.Result;
import model.enums.PlantType;
import model.miniGame.GreenHouse;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;

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


    //constructor


    public User(String username, String hashOfPassword, String nickname,
                String email, String gender, int progress, int gamesPlayed,
                Collection collection, ArrayList<PlantType> upgradedPlants,
                Settings settings, int coins, int gems, int mioPoint, GreenHouse greenHouse) {
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
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public int getHashOfPassword() {
        return hashOfPassword;
    }

    public void setHashOfPassword(int hashOfPassword) {
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
        }

    }
    public static boolean isUserNameValid(String name){

    }
    public static Result<String> isPasswordValid(String pass){

    }
    public static boolean isNickNameValid(String name){

    }
    public static Result<String> isEmailNameValid(String name){

    }
    public static boolean isGenderValid(String name){

    }


}
