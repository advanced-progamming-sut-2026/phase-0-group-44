package model.miniGame.framework;

public final class MinigameLevelConfig {
    private final int level;
    private final int waveCount;
    private final int spawnIntervalTicks;
    private final double zombieHealthMultiplier;
    private final int startingSun;

    public MinigameLevelConfig(int level, int waveCount, int spawnIntervalTicks,
                               double zombieHealthMultiplier, int startingSun) {
        if (level < 1 || level > 3 || waveCount < 1 || spawnIntervalTicks < 1
                || zombieHealthMultiplier <= 0 || startingSun < 0) {
            throw new IllegalArgumentException("Invalid minigame level configuration.");
        }
        this.level = level;
        this.waveCount = waveCount;
        this.spawnIntervalTicks = spawnIntervalTicks;
        this.zombieHealthMultiplier = zombieHealthMultiplier;
        this.startingSun = startingSun;
    }
    public int getLevel() { return level; }
    public int getWaveCount() { return waveCount; }
    public int getSpawnIntervalTicks() { return spawnIntervalTicks; }
    public double getZombieHealthMultiplier() { return zombieHealthMultiplier; }
    public int getStartingSun() { return startingSun; }
}
