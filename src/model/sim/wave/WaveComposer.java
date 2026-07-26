package model.sim.wave;

import model.sim.zombie.ZombieSpec;
import util.RandomSource;

import java.util.ArrayList;
import java.util.List;

/**
 * Builds a multiset of zombies whose wave costs sum to exactly the target.
 *
 * <p>Uses a bounded dynamic-programming reachability pass and then reconstructs
 * one exact composition, choosing among the specs that keep the remainder
 * reachable using the injected randomness. It never loops forever: if no exact
 * composition exists, it returns {@code null} so the caller can reject the
 * configuration rather than spin.</p>
 */
public final class WaveComposer {

    private WaveComposer() {
    }

    /**
     * @return true if {@code targetCost} can be composed exactly from the wave
     *         costs of {@code specs} (a non-negative combination summing to it)
     */
    public static boolean isComposable(int targetCost, List<ZombieSpec> specs) {
        if (targetCost < 0) {
            return false;
        }
        if (targetCost == 0) {
            return true;
        }

        List<ZombieSpec> usable = new ArrayList<>();
        for (ZombieSpec spec : specs) {
            if (spec.getWaveCost() > 0) {
                usable.add(spec);
            }
        }
        if (usable.isEmpty()) {
            return false;
        }

        return reachability(targetCost, usable)[targetCost];
    }

    /**
     * @return a list of specs summing to {@code targetCost}, or {@code null} if
     *         the target cannot be composed from the given specs
     */
    public static List<ZombieSpec> compose(
            int targetCost,
            List<ZombieSpec> specs,
            RandomSource random
    ) {
        if (targetCost < 0) {
            return null;
        }

        if (targetCost == 0) {
            return new ArrayList<>();
        }

        List<ZombieSpec> usable = new ArrayList<>();

        for (ZombieSpec spec : specs) {
            if (spec.getWaveCost() > 0) {
                usable.add(spec);
            }
        }

        if (usable.isEmpty()) {
            return null;
        }

        boolean[] reachable = reachability(targetCost, usable);

        if (!reachable[targetCost]) {
            return null;
        }

        return reconstruct(targetCost, usable, reachable, random);
    }

    private static boolean[] reachability(int target, List<ZombieSpec> specs) {
        boolean[] reachable = new boolean[target + 1];
        reachable[0] = true;

        for (int amount = 1; amount <= target; amount++) {
            for (ZombieSpec spec : specs) {
                int cost = spec.getWaveCost();

                if (cost <= amount && reachable[amount - cost]) {
                    reachable[amount] = true;
                    break;
                }
            }
        }

        return reachable;
    }

    private static List<ZombieSpec> reconstruct(
            int target,
            List<ZombieSpec> specs,
            boolean[] reachable,
            RandomSource random
    ) {
        List<ZombieSpec> chosen = new ArrayList<>();
        int remaining = target;

        while (remaining > 0) {
            List<ZombieSpec> viable = new ArrayList<>();

            for (ZombieSpec spec : specs) {
                int cost = spec.getWaveCost();

                if (cost <= remaining && reachable[remaining - cost]) {
                    viable.add(spec);
                }
            }

            // viable is non-empty because remaining is reachable.
            ZombieSpec pick = viable.get(random.nextInt(viable.size()));
            chosen.add(pick);
            remaining -= pick.getWaveCost();
        }

        return chosen;
    }
}
