package model.inGame.plant;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public final class DamageProfile {
    private static final Pattern NUMBER = Pattern.compile("\\d+");

    private final String raw;
    private final List<Integer> values;
    private final int projectileCount;
    private final boolean instantKill;

    private DamageProfile(String raw, List<Integer> values, int projectileCount, boolean instantKill) {
        this.raw = raw;
        this.values = List.copyOf(values);
        this.projectileCount = projectileCount;
        this.instantKill = instantKill;
    }

    public static DamageProfile parse(String rawValue) {
        String raw = rawValue == null ? "0" : rawValue.trim();
        if (raw.equalsIgnoreCase("Insta-kill")) {
            return new DamageProfile(raw, List.of(99_999), 1, true);
        }
        if (raw.isBlank() || raw.equals("-")) {
            return new DamageProfile(raw, List.of(0), 1, false);
        }

        List<Integer> numbers = new ArrayList<>();
        Matcher matcher = NUMBER.matcher(raw);
        while (matcher.find()) {
            numbers.add(Integer.parseInt(matcher.group()));
        }
        if (numbers.isEmpty()) {
            numbers.add(0);
        }

        int count = 1;
        int multiplierIndex = raw.toLowerCase().indexOf('x');
        if (multiplierIndex >= 0 && numbers.size() >= 2) {
            count = numbers.get(1);
            numbers = new ArrayList<>(List.of(numbers.get(0)));
        }
        return new DamageProfile(raw, numbers, Math.max(1, count), false);
    }

    public String getRaw() {
        return raw;
    }

    public List<Integer> getValues() {
        return Collections.unmodifiableList(values);
    }

    public int getPrimaryDamage() {
        return values.get(0);
    }

    public int getProjectileCount() {
        return projectileCount;
    }

    public boolean isInstantKill() {
        return instantKill;
    }
}
