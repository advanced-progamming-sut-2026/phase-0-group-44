package model.inGame.zombie;

import model.enums.ZombieType;

import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.function.Supplier;

public final class ZombieBehaviorFactory {
    private final Map<ZombieType, Supplier<CompositeZombieBehavior>> registrations =
            new EnumMap<>(ZombieType.class);

    public ZombieBehaviorFactory() {
        registerShared();
        registerSpecials();
    }

    private void registerShared() {
        register(ZombieType.NORMAL, normal());
        register(ZombieType.CONEHEAD, normal());
        register(ZombieType.BUCKETHEAD, normal());
        register(ZombieType.KNIGHT, normal());
        register(ZombieType.BLOCKHEAD, normal());
        register(ZombieType.IMP, () -> new CompositeZombieBehavior(
                new NormalZombieMovement(1.5), new NormalZombieAttack(1.5), List.of()));
        register(ZombieType.DRAGON_IMP, () -> new CompositeZombieBehavior(
                new NormalZombieMovement(1.5), new NormalZombieAttack(1.5), List.of()));
    }

    private void registerSpecials() {
        register(ZombieType.GARGANTUAR, () -> new CompositeZombieBehavior(
                new NormalZombieMovement(0.35),
                new GargantuarZombieAttack(),
                List.of(new GargantuarAbility())));
        register(ZombieType.ALL_STAR, () -> new CompositeZombieBehavior(
                new NormalZombieMovement(), new AllStarZombieAttack(), List.of(new AllStarAbility())));
        register(ZombieType.PARASOL_ZOMBIE, withAbility(new ParasolAbility()));
        register(ZombieType.TURQUOISE_ZOMBIE, withAbility(new TurquoiseAbility()));
        register(ZombieType.PROSPECTOR, withAbility(new ProspectorAbility()));
        register(ZombieType.PIANIST, () -> new CompositeZombieBehavior(
                new NormalZombieMovement(0.5),
                new InstantDestroyZombieAttack("Pianist piano"),
                List.of(new PianistAbility())));
        register(ZombieType.NEWSPAPER_ZOMBIE, withAbility(new NewspaperAbility()));
        register(ZombieType.BARREL_ROLLER, withAbility(new BarrelRollerAbility()));
        register(ZombieType.RA_ZOMBIE, withAbility(new RaAbility()));
        register(ZombieType.EXPLORER, () -> new CompositeZombieBehavior(
                new NormalZombieMovement(), new ExplorerZombieAttack(), List.of(new ExplorerAbility())));
        register(ZombieType.TOMBRAISER, withAbility(new TombraiserAbility()));
        register(ZombieType.DODO_RIDER, () -> new CompositeZombieBehavior(
                new DodoZombieMovement(), new DodoZombieAttack(), List.of()));
        register(ZombieType.HUNTER, withAbility(new HunterAbility()));
        register(ZombieType.SNORKEL, withAbility(new SnorkelAbility()));
        register(ZombieType.OCTOPUS_ZOMBIE, withAbility(new OctopusAbility()));
        register(ZombieType.ARCADE_ZOMBIE, () -> new CompositeZombieBehavior(
                new NormalZombieMovement(),
                new ArmoredRamZombieAttack("arcadeMachine"),
                List.of(new ArcadeAbility())));
        register(ZombieType.TROGLOBITE, () -> new CompositeZombieBehavior(
                new NormalZombieMovement(),
                new ArmoredRamZombieAttack("groundIce"),
                List.of()));
        register(ZombieType.FISHERMAN, () -> new CompositeZombieBehavior(
                new StationaryZombieMovement(),
                new NormalZombieAttack(),
                List.of(new BonusZombieAbilities.FishermanAbility())));
        register(ZombieType.JESTER, withAbility(new BonusZombieAbilities.JesterAbility()));
        register(ZombieType.WIZARD, () -> new CompositeZombieBehavior(
                new NormalZombieMovement(0.6),
                new WizardZombieAttack(),
                List.of(new BonusZombieAbilities.WizardAbility())));
        register(ZombieType.KING, () -> new CompositeZombieBehavior(
                new StationaryZombieMovement(),
                new NormalZombieAttack(),
                List.of(new BonusZombieAbilities.KingAbility())));
    }

    private Supplier<CompositeZombieBehavior> normal() {
        return () -> new CompositeZombieBehavior(
                new NormalZombieMovement(), new NormalZombieAttack(), List.of());
    }

    private Supplier<CompositeZombieBehavior> withAbility(ZombieSpecialAbility ability) {
        return () -> new CompositeZombieBehavior(
                new NormalZombieMovement(), new NormalZombieAttack(), List.of(ability));
    }

    private void register(ZombieType type, Supplier<CompositeZombieBehavior> supplier) {
        registrations.put(type, supplier);
    }

    public boolean supports(ZombieType type) {
        return registrations.containsKey(type);
    }

    public CompositeZombieBehavior create(ZombieDefinition definition) {
        if (definition == null) {
            throw new IllegalArgumentException("Zombie definition is null.");
        }
        Supplier<CompositeZombieBehavior> supplier = registrations.get(definition.getType());
        if (supplier == null) {
            throw new IllegalStateException("No zombie behavior registered for " + definition.getType());
        }
        return supplier.get();
    }
}