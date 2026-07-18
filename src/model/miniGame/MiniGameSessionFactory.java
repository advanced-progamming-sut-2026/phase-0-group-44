package model.miniGame;

import model.sim.Simulation;
import model.sim.SimulationWorld;
import util.RandomSource;

import java.util.EnumMap;
import java.util.Map;

/** Creates isolated attempts while sharing the normal board/tick implementation. */
public final class MiniGameSessionFactory {
    private final RandomSource random;
    private final Map<MiniGameId, MiniGame> strategies = new EnumMap<>(MiniGameId.class);

    public MiniGameSessionFactory(RandomSource random) {
        if (random == null) {
            throw new IllegalArgumentException("Random source is required.");
        }
        this.random = random;
        register(new VaseBreaker(random));
        register(new BowlingWallnut());
        register(new IZombie(random));
        register(new Beghouled(random));
        register(new Zombotany(random));
    }

    public void register(MiniGame strategy) {
        if (strategy == null) {
            throw new IllegalArgumentException("Minigame strategy is required.");
        }
        strategies.put(strategy.getId(), strategy);
    }

    public MiniGameSession create(
            String ownerUsername,
            MiniGameDefinition definition,
            MiniGameLevelConfig config
    ) {
        MiniGame strategy = definition == null ? null : strategies.get(definition.getId());
        if (strategy == null || config == null) {
            throw new IllegalArgumentException("No mandatory strategy exists for this minigame.");
        }
        SimulationWorld world = new SimulationWorld(config.getRows(), config.getColumns());
        Simulation simulation = new Simulation(random, world, config.isSkySunEnabled());
        MiniGameSession session = new MiniGameSession(
                ownerUsername, definition, config, strategy, simulation);
        strategy.prepare(session);
        return session;
    }
}
