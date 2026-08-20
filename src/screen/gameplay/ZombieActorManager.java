package screen.gameplay;

import com.badlogic.gdx.math.Rectangle;
import com.badlogic.gdx.scenes.scene2d.Group;

import model.GameEngine;
import model.enums.ZombieType;
import model.inGame.zombie.Zombie;


import pvz.libpvz.pam.PamPlayer;

import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static model.enums.ZombieType.GARGANTUAR;
import static model.enums.ZombieType.IMP;
import static model.inGame.zombie.ZombieBehaviorKind.DRAGON_IMP;

public final class ZombieActorManager {

    private final Group entityLayer;
    private final BattlefieldLayout layout;
    private final PamPlayer pamPlayer;

    private final List<ZombieAnimationInfo> animationInfos;

    private final Map<Long, PamZombieActor> actors =
            new HashMap<>();

    public ZombieActorManager(
            Group entityLayer,
            BattlefieldLayout layout,
            PamPlayer pamPlayer
    ) {
        this.entityLayer = entityLayer;
        this.layout = layout;
        this.pamPlayer = pamPlayer;

        this.animationInfos =
                ZombieAnimationCatalog.load();
    }

    public void sync(GameEngine engine) {

        if (engine == null || pamPlayer == null) {
            clear();
            return;
        }

        Set<Long> aliveIds =
                new HashSet<>();

        for (Zombie zombie : engine.getZombies()) {

            if (zombie == null || zombie.isDead()) {
                continue;
            }

            long zombieId =
                    zombie.getId();

            aliveIds.add(zombieId);

            PamZombieActor actor =
                    actors.get(zombieId);

            /*
             * Create visual actor when we encounter
             * this zombie for the first time.
             */
            if (actor == null) {

                actor =
                        createActor(zombie);

                if (actor == null) {
                    continue;
                }

                actors.put(
                        zombieId,
                        actor
                );

                entityLayer.addActor(
                        actor
                );
            }

            /*
             * Synchronize graphical position with
             * the real simulation zombie.
             */
            updatePosition(
                    actor,
                    zombie
            );
        }

        /*
         * Remove graphical actors whose model
         * zombies no longer exist.
         */
        actors.entrySet().removeIf(
                entry -> {

                    if (!aliveIds.contains(
                            entry.getKey()
                    )) {

                        entry.getValue().remove();

                        return true;
                    }

                    return false;
                }
        );
    }

    private PamZombieActor createActor(
            Zombie zombie
    ) {

        ZombieAnimationInfo info =
                ZombieAnimationCatalog.findForZombie(
                        zombie,
                        animationInfos
                );

        if (info == null) {

            System.err.println(
                    "No PAM animation found for zombie: "
                            + zombie.getType()
                            + " / "
                            + zombie.getName()
            );

            return null;
        }

        PamZombieActor actor =
                new PamZombieActor(
                        pamPlayer,
                        zombie,
                        info
                );

        actor.setScale(
                defaultScale(zombie)
        );

        return actor;
    }

    private void updatePosition(
            PamZombieActor actor,
            Zombie zombie
    ) {

        /*
         * Clamp row to the 5 PvZ lanes.
         */
        int row =
                Math.max(
                        0,
                        Math.min(
                                4,
                                zombie.getRow()
                        )
                );

        /*
         * Zombie X is continuous.
         *
         * Example:
         *
         * 6.0 = exactly column 6
         * 5.5 = halfway between columns
         */
        double zombieX =
                zombie.getX();

        int column =
                (int) Math.floor(
                        zombieX
                );

        /*
         * Clamp to visible lawn columns.
         */
        column =
                Math.max(
                        0,
                        Math.min(
                                8,
                                column
                        )
                );

        Rectangle cell =
                layout.cellBounds(
                        row,
                        column
                );

        /*
         * Fraction inside the current tile.
         */
        double fraction =
                zombieX
                        - Math.floor(
                        zombieX
                );

        float screenX =
                cell.x
                        + (float) fraction
                        * cell.width;

        /*
         * Zombies are taller than a lawn cell,
         * so give the actor a larger drawing area.
         */
        actor.setBounds(
                screenX - cell.width * 0.60f,
                cell.y - cell.height * 0.08f,
                cell.width * 1.45f,
                cell.height * 1.85f
        );
    }

    private float defaultScale(
            Zombie zombie
    ) {

        ZombieType type =
                zombie.getType();

        return switch (type) {

            case GARGANTUAR ->
                    0.68f;

            case IMP, DRAGON_IMP ->
                    0.42f;

            default ->
                    0.52f;
        };
    }

    public void clear() {

        for (
                PamZombieActor actor
                : actors.values()
        ) {

            actor.remove();
        }

        actors.clear();
    }
}