package util;

import java.util.Random;

/** Adapts a java.util.Random to the RandomSource seam used across the simulation. */
public final class RandomSourceAdapter {
    private RandomSourceAdapter() {
    }

    public static RandomSource wrap(Random random) {
        return new RandomSource() {
            @Override
            public int nextInt(int bound) {
                return random.nextInt(bound);
            }

            @Override
            public double nextDouble() {
                return random.nextDouble();
            }
        };
    }
}