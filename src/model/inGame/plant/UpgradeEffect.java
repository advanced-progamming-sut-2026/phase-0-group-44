package model.inGame.plant;

public record UpgradeEffect(String key, double amount, boolean percentage, boolean flag) {
    public static UpgradeEffect numeric(String key, double amount) {
        return new UpgradeEffect(key, amount, false, false);
    }

    public static UpgradeEffect percent(String key, double amount) {
        return new UpgradeEffect(key, amount, true, false);
    }

    public static UpgradeEffect flag(String key) {
        return new UpgradeEffect(key, 1.0, false, true);
    }
}
