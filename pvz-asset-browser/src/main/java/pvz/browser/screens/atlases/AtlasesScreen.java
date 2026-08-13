package pvz.browser.screens.atlases;

import pvz.browser.core.UiManager;
import pvz.browser.core.Screens;
import pvz.browser.core.AbstractScreen;

import java.util.ArrayList;
import java.util.List;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.math.Interpolation;
import com.badlogic.gdx.scenes.scene2d.Actor;
import com.badlogic.gdx.scenes.scene2d.InputEvent;
import com.badlogic.gdx.scenes.scene2d.InputListener;
import com.badlogic.gdx.scenes.scene2d.actions.Actions;
import com.badlogic.gdx.scenes.scene2d.ui.Image;
import com.badlogic.gdx.scenes.scene2d.ui.Label;
import com.badlogic.gdx.scenes.scene2d.ui.Stack;
import com.badlogic.gdx.scenes.scene2d.ui.Table;
import com.badlogic.gdx.scenes.scene2d.ui.TextButton;
import com.badlogic.gdx.scenes.scene2d.utils.ClickListener;
import com.badlogic.gdx.utils.Align;

import pvz.browser.core.BrowserContext;
import pvz.browser.core.UiManager.SlideDirection;
import pvz.browser.widgets.SplitPane;
import pvz.browser.widgets.Toast;
import pvz.browser.widgets.WorldActor;
import pvz.libpvz.textures.ResourceIndex;

public class AtlasesScreen extends AbstractScreen {
    private final Table imagesWrapper = new Table();
    private final TextButton exportBtn;
    private final Label atlasLabel;
    private final Label imageLabel;
    private String currentId = null;

    public AtlasesScreen() {

        AtlasList atlasList = new AtlasList() {
            @Override
            public void onSelect(String id) {
                currentId = id;
                List<String> imagesForAtlas;

                String atlasId = individualImages ? BrowserContext.textures.resourceIndex().image(id).atlasId : id;

                if (individualImages) {
                    imagesForAtlas = new ArrayList<>();
                    imagesForAtlas.add(id);
                } else {
                    imagesForAtlas = BrowserContext.textures.resourceIndex().getImagesForAtlas(id);
                }

                if (individualImages) {
                    exportBtn.setText("export image");
                } else {
                    exportBtn.setText("export atlas");
                }

                imagesWrapper.clearChildren();

                atlasLabel.setText(cleanName(id));
                imageLabel.setVisible(true);
                if(individualImages){
                    imageLabel.setText(atlasId);
                }
                exportBtn.setVisible(true);

                BrowserContext.textures.loadAsync(atlasId, () -> {
                    
                    for (String s : imagesForAtlas) {
                        TextureRegion region = BrowserContext.textures.region(s);
                        Image image = new Image(region);
                        if (!individualImages) {
                            image.setPosition(region.getRegionX(), region.getRegionY());
                        }
                        image.addListener(new ClickListener() {
                            public void clicked(InputEvent event, float x, float y) {
                                Gdx.app.getClipboard().setContents(s);
                                new Toast("copied " + s + " to clipboard.");
                            };
                            public void enter(InputEvent event, float x, float y, int pointer, Actor fromActor) {
                                // if(pointer!= -1) return;
    
                                image.setOrigin(Align.center);
                                image.addAction(Actions.parallel(
                                        Actions.alpha(0.5f, 0.2f, Interpolation.smoother)
                                // Actions.scaleTo(1.05f, 1.05f, 0.2f, Interpolation.swingOut)
                                ));
                                if(!individualImages) imageLabel.setText(s);
                            };
    
                            public void exit(InputEvent event, float x, float y, int pointer, Actor fromActor) {
                                // if(pointer!= -1) return;
    
                                image.setOrigin(Align.center);
    
                                image.addAction(Actions.parallel(
                                        Actions.alpha(1f, 0.2f, Interpolation.smoother)
                                // Actions.scaleTo(1f, 1f, 0.2f, Interpolation.swingOut)
                                ));
                                if(!individualImages) imageLabel.setText("no image hovered");
                            };
                        });
                        imagesWrapper.addActor(image);
                    }
                });
            }
        };

        WorldActor world = new WorldActor();
        world.addActor(imagesWrapper);

        Table left = new Table();
        TextButton switchToAnimations = new TextButton("animations", skin, "green_small");

        Table worldWrapper = new Table(UiManager.skin);
        worldWrapper.setBackground("image_ui_if_bundle_reward_multiplier_bg_10");

        Stack worldStack = new Stack();
        Table detailsTable = new Table();
        detailsTable.top();

        atlasLabel = new Label("No atlas selected", UiManager.skin, "big_outline");
        atlasLabel.setAlignment(Align.topLeft);
        imageLabel = new Label("no image hovered", UiManager.skin, "secondary");
        imageLabel.setAlignment(Align.topLeft);
        exportBtn = new TextButton("export atlas", UiManager.skin, "green_small");
        exportBtn.setVisible(false);
        detailsTable.pad(10);
        detailsTable.add(atlasLabel).minWidth(0).growX();
        detailsTable.add(exportBtn).row();
        detailsTable.add(imageLabel).minWidth(0).growX();

        worldStack.add(world);
        worldStack.add(detailsTable);
        worldWrapper.add(worldStack).grow();

        rootTable.defaults().space(10);
        rootTable.pad(10);
        left.defaults().space(10);

        left.add(switchToAnimations).growX().row();
        left.add(atlasList).grow();

        SplitPane splitPane = new SplitPane(left, worldWrapper);
        splitPane.setSplitAmount(0.23f);

        rootTable.add(splitPane).grow();

        switchToAnimations.addListener(new ClickListener() {
            @Override
            public void clicked(InputEvent event, float x, float y) {
                UiManager.setScreen(Screens.animations, SlideDirection.UP, Interpolation.smoother, 0.5f);
            }
        });

        exportBtn.addListener(new ClickListener() {
            @Override
            public void clicked(InputEvent event, float x, float y) {
                if (currentId != null) {
                    if (atlasList.individualImages) {
                        com.badlogic.gdx.files.FileHandle outDir = Gdx.files
                                .absolute(BrowserContext.settings.exportsRootPath + "/");
                        BrowserContext.textures.exportImage(currentId, outDir);
                        new Toast("Exported the image to " + outDir.path());
                    } else {
                        com.badlogic.gdx.files.FileHandle outDir = Gdx.files
                                .absolute(BrowserContext.settings.exportsRootPath + "/" + currentId + "/");
                        int count = BrowserContext.textures.exportAtlasParts(currentId, outDir);
                        new Toast("Exported " + count + " parts to " + outDir.path());
                    }
                }
            }
        });
    }

    private String displayName(String id) {
        ResourceIndex.AtlasEntry atlas = BrowserContext.textures.resourceIndex().atlas(id);
        if (atlas == null || atlas.path == null) {
            return id;
        }
        String path = atlas.path.replace('\\', '/');
        int slash = path.lastIndexOf('/');
        return slash >= 0 ? path.substring(slash + 1) : path;
    }

    private String cleanName(String id) {
        return displayName(id).replaceFirst("_(768|1536)_\\d+$", "");
    }
}
