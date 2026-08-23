package screen.gameplay;

import com.badlogic.gdx.math.Rectangle;
import com.badlogic.gdx.scenes.scene2d.Group;
import com.badlogic.gdx.utils.Disposable;
import pvz.libpvz.pam.PamPlayer;
import pvz.libpvz.textures.TextureBank;
import model.GameEngine;
import model.inGame.projectile.Projectile;
import model.inGame.projectile.ProjectileImpact;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

/** Mirrors authoritative model projectiles and impact markers into Scene2D. */
public final class ProjectileActorManager implements Disposable {
    private final Group entityLayer;
    private final BattlefieldLayout layout;
    private final PamPlayer pamPlayer;
    private final TextureBank textureBank;
    private final Map<Integer, ProjectileVisualActor> projectileActors = new HashMap<>();
    private final Set<Long> visibleImpactIds = new HashSet<>();

    public ProjectileActorManager(
            Group entityLayer,
            BattlefieldLayout layout,
            PamPlayer pamPlayer,
            TextureBank textureBank
    ) {
        this.entityLayer = entityLayer;
        this.layout = layout;
        this.pamPlayer = pamPlayer;
        this.textureBank = textureBank;
    }

    public void sync(GameEngine engine) {
        if (engine == null) {
            clear();
            return;
        }
        syncProjectiles(engine);
        syncProjectileImpacts(engine);
    }

    private void syncProjectiles(GameEngine engine) {
        Set<Integer> liveIds = new HashSet<>();
        for (Projectile projectile : engine.getProjectiles()) {
            if (projectile == null || !projectile.isActive()) {
                continue;
            }
            liveIds.add(projectile.getId());
            ProjectileVisualActor actor = projectileActors.computeIfAbsent(
                    projectile.getId(),
                    ignored -> {
                        ProjectileVisualActor created = new ProjectileVisualActor(pamPlayer, textureBank);
                        entityLayer.addActor(created);
                        return created;
                    }
            );
            actor.updateStyle(projectile);
            updatePosition(actor, projectile);
            actor.toFront();
        }

        projectileActors.entrySet().removeIf(entry -> {
            if (liveIds.contains(entry.getKey())) {
                return false;
            }
            entry.getValue().remove();
            return true;
        });
    }


    private void syncProjectileImpacts(GameEngine engine) {
        Set<Long> liveImpactIds = new HashSet<>();
        for (ProjectileImpact impact : engine.getProjectileImpacts()) {
            if (impact == null) {
                continue;
            }
            ProjectileAssetCatalog.ImpactSpec spec =
                    ProjectileAssetCatalog.forImpact(impact);
            if (spec == null) {
                continue;
            }
            liveImpactIds.add(impact.getId());
            if (!visibleImpactIds.add(impact.getId()) || pamPlayer == null) {
                continue;
            }

            Rectangle board = layout.boardBounds();
            float centerX = board.x + (float) impact.getX() * layout.cellWidth();
            float centerY = rowCenter(impact.getRow()) + layout.cellHeight() * 0.08f;

            PamTransientEffectActor splat = new PamTransientEffectActor(
                    pamPlayer, spec.pamPath(), spec.clip(), spec.scale(),
                    0f, 0f, spec.durationSeconds());
            splat.setBounds(
                    centerX - layout.cellWidth() * 0.5f,
                    centerY - layout.cellHeight() * 0.5f,
                    layout.cellWidth(),
                    layout.cellHeight());
            entityLayer.addActor(splat);
            splat.toFront();
        }
        visibleImpactIds.retainAll(liveImpactIds);
    }

    private void updatePosition(ProjectileVisualActor actor, Projectile projectile) {
        Rectangle board = layout.boardBounds();
        float centerX = board.x + (float) projectile.getVisualX() * layout.cellWidth();
        float centerY = rowCenter(projectile.getVisualRow()) + layout.cellHeight() * 0.10f;
        centerY += (float) projectile.getVisualArc() * layout.cellHeight() * 1.35f;

        // Start at the shooter's head/muzzle rather than the tile centre.
        centerX += projectile.getDirection() * layout.cellWidth() * 0.08f;

        float size = Math.min(layout.cellWidth(), layout.cellHeight())
                * actor.getProjectileSizeTiles();
        actor.setTarget(centerX, centerY, size);
    }

    private float rowCenter(double row) {
        Rectangle board = layout.boardBounds();
        return board.y
                + (float) (BattlefieldLayout.ROWS - 0.5 - row) * layout.cellHeight();
    }

    public void clear() {
        for (ProjectileVisualActor actor : projectileActors.values()) {
            actor.remove();
        }
        projectileActors.clear();
        visibleImpactIds.clear();
    }

    @Override
    public void dispose() {
        clear();
    }
}
