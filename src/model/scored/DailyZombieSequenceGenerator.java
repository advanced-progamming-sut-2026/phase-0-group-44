package model.scored;

import model.enums.ZombieType;

import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

/** Date-only seeded generator; usernames never participate in the seed. */
public final class DailyZombieSequenceGenerator {
    public static final String RULES_VERSION = "scored-game-v1";
    public static final int ZOMBIE_COUNT = 30;
    public static final int SPAWN_INTERVAL_TICKS = 45;

    private static final List<ZombieType> ROSTER = List.of(
            ZombieType.NORMAL,
            ZombieType.CONEHEAD,
            ZombieType.BUCKETHEAD,
            ZombieType.IMP,
            ZombieType.NEWSPAPER_ZOMBIE,
            ZombieType.PROSPECTOR,
            ZombieType.EXPLORER,
            ZombieType.KNIGHT
    );

    public DailyZombieSequence generate(LocalDate date) {
        if (date == null) {
            throw new IllegalArgumentException("Date is required.");
        }
        Random random = new Random(seed(date));
        List<DailyZombieSpawn> result = new ArrayList<>();
        for (int index = 0; index < ZOMBIE_COUNT; index++) {
            long tick = 1L + (long) index * SPAWN_INTERVAL_TICKS;
            int row = random.nextInt(5);
            ZombieType type = ROSTER.get(random.nextInt(ROSTER.size()));
            result.add(new DailyZombieSpawn(tick, row, type));
        }
        return new DailyZombieSequence(date, RULES_VERSION, result);
    }

    private long seed(LocalDate date) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest((RULES_VERSION + "|" + date)
                    .getBytes(StandardCharsets.UTF_8));
            return ByteBuffer.wrap(hash, 0, Long.BYTES).getLong();
        } catch (NoSuchAlgorithmException exception) {
            throw new IllegalStateException("SHA-256 is required by the runtime.", exception);
        }
    }
}
