package screen;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input;
import com.badlogic.gdx.InputAdapter;
import com.badlogic.gdx.InputMultiplexer;
import com.badlogic.gdx.Screen;
import com.badlogic.gdx.files.FileHandle;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.Pixmap;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.math.Rectangle;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.scenes.scene2d.Actor;
import com.badlogic.gdx.scenes.scene2d.Group;
import com.badlogic.gdx.scenes.scene2d.Stage;
import com.badlogic.gdx.scenes.scene2d.ui.Image;
import com.badlogic.gdx.scenes.scene2d.ui.Label;
import com.badlogic.gdx.scenes.scene2d.ui.Skin;
import com.badlogic.gdx.scenes.scene2d.ui.Table;
import com.badlogic.gdx.scenes.scene2d.ui.TextButton;
import com.badlogic.gdx.scenes.scene2d.utils.ChangeListener;
import com.badlogic.gdx.utils.Align;
import com.badlogic.gdx.utils.Scaling;
import com.badlogic.gdx.utils.viewport.FitViewport;
import controller.App;
import model.GameEngine;
import model.Store;
import model.config.GameWorld;
import model.inGame.GameSession;
import model.sim.Simulation;
import pvz.libpvz.pam.PamPlayer;
import pvz.libpvz.textures.TextureBank;
import pvz.skin.PvzSkin;
import screen.gameplay.BattlefieldEnvironmentLayer;
import screen.gameplay.BattlefieldLayout;
import screen.gameplay.BattlefieldTheme;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Phase-2 battlefield foundation.
 *
 * <p>This screen renders the official world background, the live environment
 * state from {@link GameEngine}, the five mower slots, a shared 5x9 coordinate
 * contract and the basic gameplay HUD. Plants/zombies intentionally belong to
 * {@link #entityLayer}; teammates can attach their actors there without changing
 * the environment renderer.</p>
 *
 * <p>When opened from the developer Cheat screen without an active session, it
 * enters a visual-only preview. Keys 1-4 switch worlds and never create or write
 * a fake {@link GameSession} into {@link Store}.</p>
 */
public final class GameplayScreen implements Screen {
    public static final float VIRTUAL_WIDTH = 1280f;
    public static final float VIRTUAL_HEIGHT = 720f;

    private final PvzGame game;
    @SuppressWarnings("unused")
    private final App app;
    private final Map<String, Texture> textures = new LinkedHashMap<>();

    private Stage stage;
    private Skin skin;
    private Texture whiteTexture;
    private Texture runeTexture;
    private TextureBank pamTextures;
    private PamPlayer pamPlayer;
    private FileHandle pamRoot;

    private BattlefieldTheme theme;
    private BattlefieldLayout layout;
    private BattlefieldEnvironmentLayer environmentLayer;
    private Group entityLayer;
    private Group interactionLayer;
    private Image hoverHighlight;
    private Image selectedHighlight;
    private Label sunLabel;
    private Label plantFoodLabel;
    private Label waveLabel;
    private Label cellLabel;
    private Group pauseOverlay;

    private boolean previewMode;
    private boolean paused;
    private int selectedRow = -1;
    private int selectedColumn = -1;

    public GameplayScreen(PvzGame game, App app) {
        this.game = game;
        this.app = app;
    }

    @Override
    public void show() {
        stage = new Stage(new FitViewport(VIRTUAL_WIDTH, VIRTUAL_HEIGHT));
        skin = PvzSkin.get();
        whiteTexture = makeWhiteTexture();
        runeTexture = makeRuneTexture();
        initializePam();
        resolveInitialTheme();
        rebuildScene();

        InputMultiplexer input = new InputMultiplexer();
        input.addProcessor(stage);
        input.addProcessor(new BattlefieldKeys());
        Gdx.input.setInputProcessor(input);
    }

    private void resolveInitialTheme() {
        GameSession session = Store.getActiveSession();
        Simulation simulation = Store.getActiveSimulation();
        GameWorld world = session == null || session.getLevel() == null
                ? null : session.getLevel().getWorld();
        previewMode = simulation == null || session == null || world == null;
        theme = BattlefieldTheme.forWorld(world);
    }

    private void rebuildScene() {
        stage.clear();
        selectedRow = -1;
        selectedColumn = -1;
        paused = false;

        layout = new BattlefieldLayout(theme, VIRTUAL_WIDTH, VIRTUAL_HEIGHT);
        stage.addActor(buildBackground());

        environmentLayer = new BattlefieldEnvironmentLayer(
                theme,
                layout,
                whiteTexture,
                runeTexture,
                skin,
                pamPlayer,
                pamRoot
        );
        environmentLayer.sync(engine(), previewMode);
        stage.addActor(environmentLayer);

        // Shared hook for teammate plant/zombie/projectile actors.
        entityLayer = new Group();
        entityLayer.setSize(VIRTUAL_WIDTH, VIRTUAL_HEIGHT);
        stage.addActor(entityLayer);

        interactionLayer = buildInteractionLayer();
        stage.addActor(interactionLayer);
        stage.addActor(buildHud());
        pauseOverlay = buildPauseOverlay();
        pauseOverlay.setVisible(false);
        stage.addActor(pauseOverlay);
    }

    /** Public integration hook: plant/zombie renderers should attach actors here. */
    public Group getEntityLayer() {
        return entityLayer;
    }

    /** Public integration hook: all gameplay actors should use this same mapping. */
    public BattlefieldLayout getBattlefieldLayout() {
        return layout;
    }

    private Actor buildBackground() {
        Image background = new Image(loadTexture(theme.backgroundAsset()));
        background.setScaling(Scaling.stretch);
        background.setBounds(0f, 0f, VIRTUAL_WIDTH, VIRTUAL_HEIGHT);
        return background;
    }

    private Group buildInteractionLayer() {
        Group group = new Group();
        group.setSize(VIRTUAL_WIDTH, VIRTUAL_HEIGHT);

        hoverHighlight = new Image(whiteTexture);
        hoverHighlight.setColor(1f, 1f, 1f, 0.18f);
        hoverHighlight.setVisible(false);
        group.addActor(hoverHighlight);

        selectedHighlight = new Image(whiteTexture);
        selectedHighlight.setColor(1f, 0.86f, 0.22f, 0.22f);
        selectedHighlight.setVisible(false);
        group.addActor(selectedHighlight);
        return group;
    }

    private Table buildHud() {
        Table hud = new Table();
        hud.setFillParent(true);
        hud.top().pad(10f, 14f, 0f, 14f);

        Table resourcePanel = new Table();
        resourcePanel.setBackground(new com.badlogic.gdx.scenes.scene2d.utils.TextureRegionDrawable(
                new com.badlogic.gdx.graphics.g2d.TextureRegion(whiteTexture)));
        resourcePanel.setColor(0.04f, 0.06f, 0.07f, 0.64f);

        sunLabel = new Label("SUN 0", skin, "medium_outline");
        plantFoodLabel = new Label("PLANT FOOD 0 / 3", skin);
        resourcePanel.add(sunLabel).left().pad(5f, 10f, 2f, 10f).row();
        resourcePanel.add(plantFoodLabel).left().pad(0f, 10f, 5f, 10f);
        hud.add(resourcePanel).width(210f).height(58f).left().top();

        Table center = new Table();
        Label world = new Label(theme.world().getDisplayName().toUpperCase(), skin, "medium_outline");
        world.setAlignment(Align.center);
        center.add(world).row();
        waveLabel = new Label("WAVE", skin);
        waveLabel.setAlignment(Align.center);
        center.add(waveLabel).padTop(1f).row();
        cellLabel = new Label("", skin);
        cellLabel.setColor(0.92f, 0.96f, 1f, 0.86f);
        cellLabel.setAlignment(Align.center);
        center.add(cellLabel).padTop(1f);
        hud.add(center).expandX().top();

        TextButton pause = new TextButton("II", skin, "purple");
        pause.addListener(new ChangeListener() {
            @Override
            public void changed(ChangeEvent event, Actor actor) {
                togglePause();
            }
        });
        hud.add(pause).size(58f, 48f).right().top();
        return hud;
    }

    private Group buildPauseOverlay() {
        Group group = new Group();
        group.setSize(VIRTUAL_WIDTH, VIRTUAL_HEIGHT);

        Image shade = new Image(whiteTexture);
        shade.setColor(0f, 0f, 0f, 0.48f);
        shade.setBounds(0f, 0f, VIRTUAL_WIDTH, VIRTUAL_HEIGHT);
        group.addActor(shade);

        Table panel = new Table();
        panel.setBounds(430f, 245f, 420f, 230f);
        panel.setBackground(new com.badlogic.gdx.scenes.scene2d.utils.TextureRegionDrawable(
                new com.badlogic.gdx.graphics.g2d.TextureRegion(whiteTexture)));
        panel.setColor(0.08f, 0.10f, 0.11f, 0.92f);

        Label title = new Label("PAUSED", skin, "medium_outline");
        panel.add(title).padBottom(18f).row();

        TextButton resume = new TextButton("RESUME", skin, "purple");
        resume.addListener(new ChangeListener() {
            @Override
            public void changed(ChangeEvent event, Actor actor) {
                togglePause();
            }
        });
        panel.add(resume).width(190f).height(48f).row();

        if (previewMode) {
            Label hint = new Label("DEV PREVIEW: press 1, 2, 3 or 4 to switch world", skin);
            hint.setWrap(true);
            hint.setAlignment(Align.center);
            panel.add(hint).width(350f).padTop(16f);
        }
        group.addActor(panel);
        return group;
    }

    private void togglePause() {
        paused = !paused;
        if (pauseOverlay != null) {
            pauseOverlay.setVisible(paused);
        }
    }

    private void updateHud() {
        GameEngine engine = engine();
        if (previewMode || engine == null) {
            sunLabel.setText("SUN 50");
            plantFoodLabel.setText("PLANT FOOD 0 / 3");
            waveLabel.setText("DEV PREVIEW");
            return;
        }

        sunLabel.setText("SUN " + engine.getSun());
        plantFoodLabel.setText("PLANT FOOD " + engine.getPlantFood()
                + " / " + GameEngine.MAX_PLANT_FOOD);

        GameSession session = Store.getActiveSession();
        int totalWaves = session == null || session.getLevel() == null
                || session.getLevel().getWaveConfig() == null
                ? 0 : session.getLevel().getWaveConfig().getWaveCount();
        if (totalWaves <= 0) {
            waveLabel.setText("WAVE " + engine.getCurrentWave());
        } else {
            waveLabel.setText("WAVE " + engine.getCurrentWave() + " / " + totalWaves);
        }
    }

    private void updatePointerHighlights() {
        if (paused) {
            hoverHighlight.setVisible(false);
            return;
        }
        Vector2 pointer = stage.getViewport().unproject(
                new Vector2(Gdx.input.getX(), Gdx.input.getY()));
        int[] cell = layout.screenToCell(pointer.x, pointer.y);
        if (cell[0] < 0) {
            hoverHighlight.setVisible(false);
            cellLabel.setText("");
        } else {
            Rectangle bounds = layout.cellBounds(cell[0], cell[1]);
            hoverHighlight.setBounds(bounds.x + 2f, bounds.y + 2f,
                    Math.max(0f, bounds.width - 4f), Math.max(0f, bounds.height - 4f));
            hoverHighlight.setVisible(true);
            cellLabel.setText("ROW " + (cell[0] + 1) + "   COL " + (cell[1] + 1));
        }

        if (Gdx.input.justTouched() && cell[0] >= 0) {
            selectedRow = cell[0];
            selectedColumn = cell[1];
            Rectangle bounds = layout.cellBounds(selectedRow, selectedColumn);
            selectedHighlight.setBounds(bounds.x + 3f, bounds.y + 3f,
                    Math.max(0f, bounds.width - 6f), Math.max(0f, bounds.height - 6f));
            selectedHighlight.setVisible(true);
        }
    }

    private GameEngine engine() {
        Simulation simulation = Store.getActiveSimulation();
        return simulation == null ? null : simulation.getWorld();
    }

    private void initializePam() {
        try {
            FileHandle assets = Gdx.files.internal("pvz-asset-browser/pvz-assets");
            if (!assets.exists()) {
                assets = Gdx.files.local("pvz-asset-browser/pvz-assets");
            }
            if (!assets.exists()) {
                pamRoot = null;
                pamPlayer = null;
                return;
            }
            pamRoot = assets;
            pamTextures = new TextureBank("768", assets);
            pamPlayer = new PamPlayer(pamTextures, assets);
        } catch (RuntimeException exception) {
            pamRoot = null;
            pamPlayer = null;
            Gdx.app.error("GameplayScreen", "Could not initialize environment PAM assets", exception);
        }
    }

    private Texture loadTexture(String path) {
        Texture cached = textures.get(path);
        if (cached != null) {
            return cached;
        }
        Texture texture = new Texture(Gdx.files.internal(path));
        textures.put(path, texture);
        return texture;
    }

    private Texture makeWhiteTexture() {
        Pixmap pixel = new Pixmap(1, 1, Pixmap.Format.RGBA8888);
        pixel.setColor(Color.WHITE);
        pixel.fill();
        Texture texture = new Texture(pixel);
        pixel.dispose();
        return texture;
    }

    /** Simple procedural rune; no generated/external art is needed for necromancy tiles. */
    private Texture makeRuneTexture() {
        Pixmap pixmap = new Pixmap(128, 128, Pixmap.Format.RGBA8888);
        pixmap.setColor(0f, 0f, 0f, 0f);
        pixmap.fill();
        pixmap.setColor(Color.WHITE);
        pixmap.drawCircle(64, 64, 52);
        pixmap.drawCircle(64, 64, 38);
        pixmap.drawLine(64, 10, 64, 118);
        pixmap.drawLine(10, 64, 118, 64);
        pixmap.drawLine(25, 25, 103, 103);
        pixmap.drawLine(103, 25, 25, 103);
        Texture texture = new Texture(pixmap);
        pixmap.dispose();
        return texture;
    }

    private void switchPreview(BattlefieldTheme next) {
        if (!previewMode || next == null || next == theme) {
            return;
        }
        theme = next;
        rebuildScene();
    }

    @Override
    public void render(float delta) {
        Gdx.gl.glClearColor(0f, 0f, 0f, 1f);
        Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT);

        if (pamTextures != null) {
            pamTextures.update();
        }

        environmentLayer.sync(engine(), previewMode);
        updateHud();
        updatePointerHighlights();
        stage.act(delta);
        stage.draw();
    }

    @Override
    public void resize(int width, int height) {
        stage.getViewport().update(width, height, true);
    }

    @Override public void pause() { }
    @Override public void resume() { }
    @Override public void hide() { }

    @Override
    public void dispose() {
        if (stage != null) {
            stage.dispose();
        }
        for (Texture texture : textures.values()) {
            texture.dispose();
        }
        textures.clear();
        if (whiteTexture != null) {
            whiteTexture.dispose();
            whiteTexture = null;
        }
        if (runeTexture != null) {
            runeTexture.dispose();
            runeTexture = null;
        }
        if (pamTextures != null) {
            pamTextures.dispose();
            pamTextures = null;
        }
    }

    private final class BattlefieldKeys extends InputAdapter {
        @Override
        public boolean keyDown(int keycode) {
            if (keycode == Input.Keys.ESCAPE) {
                togglePause();
                return true;
            }
            if (!previewMode) {
                return false;
            }
            if (keycode == Input.Keys.NUM_1) {
                switchPreview(BattlefieldTheme.ANCIENT_EGYPT);
                return true;
            }
            if (keycode == Input.Keys.NUM_2) {
                switchPreview(BattlefieldTheme.FROSTBITE_CAVES);
                return true;
            }
            if (keycode == Input.Keys.NUM_3) {
                switchPreview(BattlefieldTheme.BIG_WAVE_BEACH);
                return true;
            }
            if (keycode == Input.Keys.NUM_4) {
                switchPreview(BattlefieldTheme.DARK_AGES);
                return true;
            }
            return false;
        }
    }
}
