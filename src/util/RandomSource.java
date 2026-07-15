package util;

/**
 * The single randomness seam for the simulation, so probability can be made
 * deterministic in tests by injecting a scripted or seeded source.
 */
public interface RandomSource {

    /** A value in {@code [0, bound)}. */
    int nextInt(int bound);

    /** A value in {@code [0.0, 1.0)}. */
    double nextDouble();
}
