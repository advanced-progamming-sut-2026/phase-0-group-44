package util;

import java.util.Random;

/** Production {@link RandomSource} backed by a seedable {@link Random}. */
public class SeededRandomSource implements RandomSource {

    private final Random random;

    public SeededRandomSource() {
        this.random = new Random();
    }

    public SeededRandomSource(long seed) {
        this.random = new Random(seed);
    }

    @Override
    public int nextInt(int bound) {
        return random.nextInt(bound);
    }

    @Override
    public double nextDouble() {
        return random.nextDouble();
    }
}
