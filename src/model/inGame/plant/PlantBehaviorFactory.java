package model.inGame.plant;

import model.enums.PlantType;

import java.util.Collections;
import java.util.EnumMap;
import java.util.Map;
import java.util.function.Function;

public final class PlantBehaviorFactory {
    private final Map<PlantType, Function<PlantDefinition, PlantBehavior>> builders =
            new EnumMap<>(PlantType.class);

    public PlantBehaviorFactory() {
        registerSunProducers();
        registerShooters();
        registerPiercingAndLobbers();
        registerExplosives();
        registerMeleeAndDefenders();
        registerModifiers();
        registerMints();
    }

    private void registerSunProducers() {
        register(PlantType.SUNFLOWER, d -> new SunProducerBehavior(SunProducerBehavior.Mode.NORMAL, 50, 150));
        register(PlantType.TWIN_SUNFLOWER, d -> new SunProducerBehavior(SunProducerBehavior.Mode.NORMAL, 100, 250));
        register(PlantType.SUN_SHROOM, d -> new SunProducerBehavior(SunProducerBehavior.Mode.RAMP_UP, 25, 225));
        register(PlantType.PRIMAL_SUNFLOWER, d -> new SunProducerBehavior(SunProducerBehavior.Mode.NORMAL, 75, 225));
        register(PlantType.GOLD_BLOOM, d -> new SunProducerBehavior(SunProducerBehavior.Mode.INSTANT, 375, 0));
    }

    private void registerShooters() {
        register(PlantType.PEASHOOTER, d -> new ShooterBehavior(ShooterBehavior.Mode.FORWARD));
        register(PlantType.REPEATER, d -> new ShooterBehavior(ShooterBehavior.Mode.REPEATER));
        register(PlantType.THREEPEATER, d -> new ShooterBehavior(ShooterBehavior.Mode.THREE_LANES));
        register(PlantType.SNOW_PEA, d -> new ShooterBehavior(ShooterBehavior.Mode.FORWARD));
        register(PlantType.ROTOBAGA, d -> new ShooterBehavior(ShooterBehavior.Mode.DIAGONAL));
        register(PlantType.SPLIT_PEA, d -> new ShooterBehavior(ShooterBehavior.Mode.SPLIT));
        register(PlantType.CITRON, d -> new ShooterBehavior(ShooterBehavior.Mode.CHARGED));
        register(PlantType.BOWLING_BULB, d -> new ShooterBehavior(ShooterBehavior.Mode.BOWLING));
        register(PlantType.FIRE_PEASHOOTER, d -> new ShooterBehavior(ShooterBehavior.Mode.FORWARD));
        register(PlantType.MEGA_GATLING_PEA, d -> new ShooterBehavior(ShooterBehavior.Mode.GATLING));
        register(PlantType.SEA_SHROOM, d -> new ShooterBehavior(ShooterBehavior.Mode.SHORT_RANGE));
        register(PlantType.PUFF_SHROOM, d -> new ShooterBehavior(ShooterBehavior.Mode.SHORT_RANGE));
    }

    private void registerPiercingAndLobbers() {
        register(PlantType.CACTUS, d -> new PiercingBehavior(PiercingBehavior.Mode.CACTUS));
        register(PlantType.FUME_SHROOM, d -> new PiercingBehavior(PiercingBehavior.Mode.FUME));
        register(PlantType.CABBAGE_PULT, d -> new LobberBehavior(LobberBehavior.Mode.NORMAL));
        register(PlantType.KERNEL_PULT, d -> new LobberBehavior(LobberBehavior.Mode.KERNEL));
        register(PlantType.MELON_PULT, d -> new LobberBehavior(LobberBehavior.Mode.SPLASH));
        register(PlantType.WINTER_MELON, d -> new LobberBehavior(LobberBehavior.Mode.ICE_SPLASH));
        register(PlantType.PEPPER_PULT, d -> new LobberBehavior(LobberBehavior.Mode.FIRE_SPLASH));
    }

    private void registerExplosives() {
        register(PlantType.POTATO_MINE, d -> new ExplosiveBehavior(ExplosiveBehavior.Mode.POTATO_MINE));
        register(PlantType.PRIMAL_POTATO_MINE, d -> new ExplosiveBehavior(ExplosiveBehavior.Mode.PRIMAL_MINE));
        register(PlantType.CHERRY_BOMB, d -> new ExplosiveBehavior(ExplosiveBehavior.Mode.CHERRY));
        register(PlantType.SQUASH, d -> new ExplosiveBehavior(ExplosiveBehavior.Mode.SQUASH));
        register(PlantType.JALAPENO, d -> new ExplosiveBehavior(ExplosiveBehavior.Mode.JALAPENO));
        register(PlantType.DOOM_SHROOM, d -> new ExplosiveBehavior(ExplosiveBehavior.Mode.DOOM));
        register(PlantType.TANGLE_KELP, d -> new ExplosiveBehavior(ExplosiveBehavior.Mode.TANGLE_KELP));
        register(PlantType.ICEBERG_LETTUCE, d -> new ExplosiveBehavior(ExplosiveBehavior.Mode.ICEBERG));
        register(PlantType.ICE_SHROOM, d -> new ExplosiveBehavior(ExplosiveBehavior.Mode.ICE_SHROOM));
        register(PlantType.HOT_POTATO, d -> new ExplosiveBehavior(ExplosiveBehavior.Mode.HOT_POTATO));
        register(PlantType.GRAVE_BUSTER, d -> new ExplosiveBehavior(ExplosiveBehavior.Mode.GRAVE_BUSTER));
    }

    private void registerMeleeAndDefenders() {
        register(PlantType.BONK_CHOY, d -> new MeleeBehavior(MeleeBehavior.Mode.BONK_CHOY));
        register(PlantType.PHAT_BEET, d -> new MeleeBehavior(MeleeBehavior.Mode.PHAT_BEET));
        register(PlantType.WALL_NUT, d -> new DefenderBehavior(DefenderBehavior.Mode.WALL_NUT));
        register(PlantType.TALL_NUT, d -> new DefenderBehavior(DefenderBehavior.Mode.TALL_NUT));
        register(PlantType.ENDURIAN, d -> new DefenderBehavior(DefenderBehavior.Mode.ENDURIAN));
        register(PlantType.GARLIC, d -> new DefenderBehavior(DefenderBehavior.Mode.GARLIC));
        register(PlantType.EXPLODE_O_NUT, d -> new DefenderBehavior(DefenderBehavior.Mode.EXPLODE_O_NUT));
        register(PlantType.PUMPKIN, d -> new DefenderBehavior(DefenderBehavior.Mode.PUMPKIN));
        register(PlantType.SUN_BEAN, d -> new DefenderBehavior(DefenderBehavior.Mode.SUN_BEAN));
    }

    private void registerModifiers() {
        register(PlantType.TORCHWOOD, d -> new ModifierBehavior(ModifierBehavior.Mode.TORCHWOOD));
        register(PlantType.MAGNET_SHROOM, d -> new ModifierBehavior(ModifierBehavior.Mode.MAGNET_SHROOM));
        register(PlantType.LILY_PAD, d -> new ModifierBehavior(ModifierBehavior.Mode.LILY_PAD));
    }

    private void registerMints() {
        register(PlantType.ENLIGHTEN_MINT, d -> new MintBehavior(d.getCategory()));
        register(PlantType.APPEASE_MINT, d -> new MintBehavior(d.getCategory()));
        register(PlantType.ARMA_MINT, d -> new MintBehavior(d.getCategory()));
        register(PlantType.BOMBARD_MINT, d -> new MintBehavior(d.getCategory()));
        register(PlantType.ENFORCE_MINT, d -> new MintBehavior(d.getCategory()));
        register(PlantType.REINFORCE_MINT, d -> new MintBehavior(d.getCategory()));
        register(PlantType.ENCHANT_MINT, d -> new MintBehavior(d.getCategory()));
        register(PlantType.PIERCE_MINT, d -> new MintBehavior(d.getCategory()));
        register(PlantType.CATTAIL_MINT, d -> new MintBehavior(d.getCategory()));
    }

    private void register(PlantType type, Function<PlantDefinition, PlantBehavior> builder) {
        if (builders.put(type, builder) != null) {
            throw new IllegalStateException("Duplicate behavior registration: " + type);
        }
    }

    public PlantBehavior create(PlantDefinition definition) {
        Function<PlantDefinition, PlantBehavior> builder = builders.get(definition.getType());
        if (builder == null) {
            throw new UnsupportedOperationException("No mandatory behavior registered for " + definition.getName());
        }
        return builder.apply(definition);
    }

    public boolean supports(PlantType type) {
        return builders.containsKey(type);
    }

    public Map<PlantType, Function<PlantDefinition, PlantBehavior>> registrations() {
        return Collections.unmodifiableMap(builders);
    }
}
