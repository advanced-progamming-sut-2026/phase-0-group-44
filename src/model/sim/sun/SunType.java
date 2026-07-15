package model.sim.sun;

import util.RandomSource;

/**
 * The three kinds of falling sun, with their spawn probabilities and values.
 *
 * <p>normal: 80%, worth 25. special: 15%, worth 100. radioactive: 5% — worth
 * nothing while falling (it explodes if collected mid-air) and becomes a normal
 * sun worth 25 if it reaches the ground.</p>
 */
public enum SunType {
    NORMAL("normal", 25),
    SPECIAL("special", 100),
    RADIOACTIVE("radioactive", 0);

    private final String label;
    private final int value;

    SunType(String label, int value) {
        this.label = label;
        this.value = value;
    }

    public String getLabel() {
        return label;
    }

    public int getValue() {
        return value;
    }

    /**
     * Rolls a falling-sun type: normal 80%, special 15%, radioactive 5%. The
     * single {@code nextDouble()} call keeps the sequence predictable for tests.
     */
    public static SunType roll(RandomSource random) {
        double r = random.nextDouble();

        if (r < 0.80) {
            return NORMAL;
        }

        if (r < 0.95) {
            return SPECIAL;
        }

        return RADIOACTIVE;
    }
}
