package screen;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input;
import com.badlogic.gdx.Screen;
import com.badlogic.gdx.files.FileHandle;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.Pixmap;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.scenes.scene2d.Actor;
import com.badlogic.gdx.scenes.scene2d.Stage;
import com.badlogic.gdx.scenes.scene2d.ui.Label;
import com.badlogic.gdx.scenes.scene2d.ui.SelectBox;
import com.badlogic.gdx.scenes.scene2d.ui.Skin;
import com.badlogic.gdx.scenes.scene2d.ui.Table;
import com.badlogic.gdx.scenes.scene2d.ui.TextButton;
import com.badlogic.gdx.scenes.scene2d.utils.ChangeListener;
import com.badlogic.gdx.utils.Align;
import com.badlogic.gdx.utils.viewport.ScreenViewport;

import pvz.libpvz.pam.PamPlayer;
import pvz.libpvz.textures.TextureBank;
import pvz.skin.PvzSkin;

import screen.gameplay.PamZombieActor;
import screen.gameplay.ZombieAnimationCatalog;
import screen.gameplay.ZombieAnimationInfo;

import java.util.List;

public final class ZombieAnimationTestScreen implements Screen {

    private final PvzGame game;
    private final Screen returnScreen;

    private Stage stage;
    private Skin skin;

    private TextureBank textureBank;
    private PamPlayer pamPlayer;
    private FileHandle pamRoot;

    private List<ZombieAnimationInfo> zombies;

    private SelectBox<ZombieAnimationInfo> zombieBox;
    private SelectBox<String> clipBox;

    private PamZombieActor zombieActor;

    private Label infoLabel;

    private boolean loadingControls;

    public ZombieAnimationTestScreen(
            PvzGame game,
            Screen returnScreen
    ) {
        this.game = game;
        this.returnScreen = returnScreen;
    }

    @Override
    public void show() {

        stage =
                new Stage(
                        new ScreenViewport()
                );

        skin =
                PvzSkin.get();

        initializePam();

        zombies =
                ZombieAnimationCatalog.load();

        System.out.println(
                "Zombie lab animations found: "
                        + zombies.size()
        );

        buildScreen();

        Gdx.input.setInputProcessor(stage);

        if (!zombies.isEmpty()) {

            zombieBox.setSelectedIndex(0);

            selectZombie(
                    zombies.get(0)
            );

        } else {

            infoLabel.setText(
                    "NO ZOMBIE PAM ANIMATIONS FOUND"
            );

            infoLabel.setColor(
                    1f,
                    0.3f,
                    0.2f,
                    1f
            );
        }
    }

    private void initializePam() {

        FileHandle assets =
                Gdx.files.internal(
                        "pvz-asset-browser/pvz-assets"
                );

        if (!assets.exists()) {
            assets =
                    Gdx.files.local(
                            "pvz-asset-browser/pvz-assets"
                    );
        }

        if (!assets.exists()) {

            throw new IllegalStateException(
                    "PVZ asset folder was not found."
            );
        }

        pamRoot = assets;

        textureBank =
                new TextureBank(
                        "768",
                        assets
                );

        pamPlayer =
                new PamPlayer(
                        textureBank,
                        assets
                );
    }

    private void buildScreen() {

        Table root =
                new Table();

        root.setFillParent(true);
        root.pad(25f);

        stage.addActor(root);

        /*
         * TITLE
         */

        Label title =
                new Label(
                        "ZOMBIE ANIMATION LAB",
                        skin
                );

        title.setFontScale(1.5f);

        root.add(title)
                .colspan(4)
                .padBottom(20f)
                .row();

        /*
         * CONTROLS
         */

        root.add(
                new Label(
                        "Zombie",
                        skin
                )
        ).padRight(8f);

        zombieBox =
                new SelectBox<>(skin);

        zombieBox.setItems(
                zombies.toArray(
                        new ZombieAnimationInfo[0]
                )
        );

        zombieBox.addListener(
                new ChangeListener() {

                    @Override
                    public void changed(
                            ChangeEvent event,
                            Actor actor
                    ) {

                        if (loadingControls) {
                            return;
                        }

                        selectZombie(
                                zombieBox.getSelected()
                        );
                    }
                }
        );

        root.add(zombieBox)
                .width(330f)
                .height(45f)
                .padRight(25f);

        root.add(
                new Label(
                        "Animation",
                        skin
                )
        ).padRight(8f);

        clipBox =
                new SelectBox<>(skin);

        clipBox.addListener(
                new ChangeListener() {

                    @Override
                    public void changed(
                            ChangeEvent event,
                            Actor actor
                    ) {

                        if (loadingControls) {
                            return;
                        }

                        selectClip(
                                clipBox.getSelected()
                        );
                    }
                }
        );

        root.add(clipBox)
                .width(220f)
                .height(45f)
                .row();

        /*
         * PREVIEW AREA
         */

        Table previewArea =
                new Table();

        previewArea.setColor(
                0.12f,
                0.18f,
                0.12f,
                1f
        );

        zombieActor =
                new PamZombieActor(
                        pamPlayer,
                        null,
                        null,
                        0.8f
                );

        zombieActor.setSize(
                500f,
                440f
        );

        previewArea.add(zombieActor)
                .width(500f)
                .height(440f);

        root.add(previewArea)
                .colspan(4)
                .width(700f)
                .height(450f)
                .padTop(20f)
                .padBottom(15f)
                .row();

        /*
         * BUTTONS
         */

        Table buttons =
                new Table();

        TextButton previous =
                new TextButton(
                        "< PREVIOUS",
                        skin
                );

        previous.addListener(
                new ChangeListener() {

                    @Override
                    public void changed(
                            ChangeEvent event,
                            Actor actor
                    ) {
                        previousZombie();
                    }
                }
        );

        buttons.add(previous)
                .width(170f)
                .height(48f)
                .padRight(8f);

        TextButton restart =
                new TextButton(
                        "RESTART",
                        skin
                );

        restart.addListener(
                new ChangeListener() {

                    @Override
                    public void changed(
                            ChangeEvent event,
                            Actor actor
                    ) {
                        zombieActor.restart();
                    }
                }
        );

        buttons.add(restart)
                .width(150f)
                .height(48f)
                .padRight(8f);

        TextButton next =
                new TextButton(
                        "NEXT >",
                        skin
                );

        next.addListener(
                new ChangeListener() {

                    @Override
                    public void changed(
                            ChangeEvent event,
                            Actor actor
                    ) {
                        nextZombie();
                    }
                }
        );

        buttons.add(next)
                .width(170f)
                .height(48f);

        root.add(buttons)
                .colspan(4)
                .padBottom(10f)
                .row();

        /*
         * INFO
         */

        infoLabel =
                new Label(
                        "",
                        skin
                );

        infoLabel.setAlignment(
                Align.center
        );

        infoLabel.setWrap(true);

        root.add(infoLabel)
                .colspan(4)
                .width(850f)
                .padBottom(12f)
                .row();

        /*
         * BACK
         */

        TextButton back =
                new TextButton(
                        "BACK",
                        skin
                );

        back.addListener(
                new ChangeListener() {

                    @Override
                    public void changed(
                            ChangeEvent event,
                            Actor actor
                    ) {

                        if (returnScreen != null) {
                            game.setScreen(
                                    returnScreen
                            );
                        }
                    }
                }
        );

        root.add(back)
                .colspan(4)
                .width(220f)
                .height(50f);
    }

    private void selectZombie(
            ZombieAnimationInfo info
    ) {

        if (info == null) {
            return;
        }

        loadingControls = true;

        String[] clips =
                info.getClips()
                        .toArray(
                                new String[0]
                        );

        clipBox.setItems(
                clips
        );

        /*
         * Prefer WALK because that is the animation
         * we need most often in normal gameplay.
         */
        String startingClip =
                findPreferredClip(
                        info
                );

        clipBox.setSelected(
                startingClip
        );

        loadingControls = false;

        zombieActor.setAnimation(
                info.getPath(),
                startingClip
        );

        updateInfo();
    }

    private String findPreferredClip(
            ZombieAnimationInfo info
    ) {

        for (String clip : info.getClips()) {

            if ("walk".equalsIgnoreCase(clip)) {
                return clip;
            }
        }

        for (String clip : info.getClips()) {

            if ("idle".equalsIgnoreCase(clip)) {
                return clip;
            }
        }

        return info
                .getClips()
                .get(0);
    }

    private void selectClip(
            String clip
    ) {

        if (clip == null) {
            return;
        }

        zombieActor.setClip(
                clip
        );

        updateInfo();
    }

    private void previousZombie() {

        if (zombieBox == null
                || zombieBox.getItems().isEmpty()) {

            return;
        }

        int size =
                zombieBox.getItems().size;

        int index =
                zombieBox.getSelectedIndex();

        if (index < 0) {
            index = 0;
        }

        index--;

        if (index < 0) {
            index = size - 1;
        }

        zombieBox.setSelectedIndex(
                index
        );
    }

    private void nextZombie() {

        if (zombieBox == null
                || zombieBox.getItems().isEmpty()) {

            return;
        }

        int size =
                zombieBox.getItems().size;

        int index =
                zombieBox.getSelectedIndex();

        if (index < 0) {
            index = 0;
        }

        index++;

        if (index >= size) {
            index = 0;
        }

        zombieBox.setSelectedIndex(
                index
        );
    }

    private void updateInfo() {

        ZombieAnimationInfo info =
                zombieBox.getSelected();

        if (info == null) {
            return;
        }

        infoLabel.setText(
                info.getName()
                        + "\n"
                        + info.getPath()
                        + "\nClip: "
                        + clipBox.getSelected()
        );
    }

    @Override
    public void render(float delta) {

        Gdx.gl.glClearColor(
                0.18f,
                0.34f,
                0.18f,
                1f
        );

        Gdx.gl.glClear(
                GL20.GL_COLOR_BUFFER_BIT
        );

        if (textureBank != null) {
            textureBank.update();
        }

        stage.act(delta);
        stage.draw();

        /*
         * Convenient keyboard testing.
         */
        if (Gdx.input.isKeyJustPressed(Input.Keys.RIGHT)) {
            nextZombie();
        }

        if (Gdx.input.isKeyJustPressed(Input.Keys.LEFT)) {
            previousZombie();
        }

        if (Gdx.input.isKeyJustPressed(Input.Keys.R)) {
            zombieActor.restart();
        }
    }

    @Override
    public void resize(
            int width,
            int height
    ) {

        stage.getViewport()
                .update(
                        width,
                        height,
                        true
                );
    }

    @Override
    public void pause() {
    }

    @Override
    public void resume() {
    }

    @Override
    public void hide() {
    }

    @Override
    public void dispose() {

        if (stage != null) {
            stage.dispose();
        }
    }
}