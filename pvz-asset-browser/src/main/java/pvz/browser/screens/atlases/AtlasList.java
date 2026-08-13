package pvz.browser.screens.atlases;

import pvz.browser.widgets.VirtualizedList;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.math.Interpolation;
import com.badlogic.gdx.scenes.scene2d.Actor;
import com.badlogic.gdx.scenes.scene2d.InputEvent;
import com.badlogic.gdx.scenes.scene2d.InputListener;
import com.badlogic.gdx.scenes.scene2d.ui.CheckBox;
import com.badlogic.gdx.scenes.scene2d.ui.ScrollPane;
import com.badlogic.gdx.scenes.scene2d.ui.Skin;
import com.badlogic.gdx.scenes.scene2d.ui.Table;
import com.badlogic.gdx.scenes.scene2d.ui.TextField;
import com.badlogic.gdx.scenes.scene2d.utils.ChangeListener;
import com.badlogic.gdx.utils.Align;
import com.badlogic.gdx.utils.Array;

import pvz.browser.core.BrowserContext;
import pvz.browser.core.Screens;
import pvz.browser.core.UiManager;
import pvz.libpvz.textures.ResourceIndex;

public class AtlasList extends Table {
    private static final float CARD_HEIGHT = 72f;

    private final Skin skin = UiManager.skin;
    private final Array<String> atlasIds;
    private Array<String> filteredAtlasIds;
    private Array<String> filteredImageIds;

    private TextField search;

    private VirtualizedList<String> list;

    public boolean individualImages = false;

    public AtlasList() {
        super(UiManager.skin);

        atlasIds = new Array<>();
        filteredAtlasIds = new Array<>();
        filteredImageIds = new Array<>();

        indexAtlases();
        setupUi();
        applyFilter();
    }

    private void indexAtlases() {
        ResourceIndex index = BrowserContext.textures.resourceIndex();
        for (String id : index.atlasIds()) {
            ResourceIndex.AtlasEntry atlas = index.atlas(id);
            String displayName = displayName(id).toLowerCase();
            if (atlas == null || atlas.path == null) {
                continue;
            }
            if (!displayName.contains("_768_") || !atlas.path.contains("_768_") || displayName.startsWith("plant")
            || displayName.startsWith("zombie") || displayName.contains("mower")) {
                continue;
            }
            atlasIds.add(id);
        }
        atlasIds.sort(Comparator.comparing(id -> id.toLowerCase(Locale.ROOT)));
    }

    private void setupUi() {

        setBackground("image_ui_if_bundle_reward_multiplier_bg_10");
        pad(10);

        search = new TextField("", skin);
        search.setMessageText("Search atlases...");

        list = new VirtualizedList<>(CARD_HEIGHT, new VirtualizedList.CardFactory<String>() {
            @Override
            public Actor create(String item, int index) {
                AtlasCard card;
                if(individualImages){
                    String imageId = filteredImageIds.get(index);
                    card = new AtlasCard(filteredImageIds.get(index)) {
                        @Override
                        public void onClick() {
                            onSelect(item);
                        }
                    };
                    BrowserContext.textures.loadAsync(BrowserContext.textures.resourceIndex().image(item).atlasId, () -> {
                        card.setImage(BrowserContext.textures.region(imageId));
                    });
                } else {
                    card = new AtlasCard(filteredAtlasIds.get(index)) {
                        @Override
                        public void onClick() {
                            onSelect(item);
                        }
                    };
                    BrowserContext.textures.loadAsync(item, () -> {
                        card.setImage(BrowserContext.textures.atlas(item));
                    });
                }
                return card;
            }
        });

        CheckBox checkBox = new CheckBox("individual images", skin);
        checkBox.addListener(new ChangeListener() {
            @Override
            public void changed(ChangeEvent event, Actor actor) {
                individualImages = checkBox.isChecked();
                applyFilter();
            }
        });
        checkBox.align(Align.left);
        checkBox.getLabel().setColor(Color.BLACK);

        ScrollPane scroll = new ScrollPane(list, skin);
        scroll.setFadeScrollBars(false);
        scroll.setScrollingDisabled(true, false);

        scroll.addListener(new InputListener() {
            @Override
            public void enter(InputEvent event, float x, float y, int pointer, Actor fromActor) {
                getStage().setScrollFocus(scroll);
            }
        });

        add(checkBox).growX().row();
        add(search).growX().row();
        add(scroll).grow().row();

        search.addListener(new ChangeListener() {
            @Override
            public void changed(ChangeEvent e, Actor a) {
                applyFilter();
            }
        });
    }

    private void applyFilter() {
        ResourceIndex resourceIndex = BrowserContext.textures.resourceIndex();
        String q = search.getText() == null ? "" : displayName(search.getText().trim().toLowerCase(Locale.ROOT)).replace(" ", "_");

        filteredAtlasIds.clear();
        filteredImageIds.clear();

        for (String id : atlasIds) {

            if (!q.isEmpty() && !id.toLowerCase(Locale.ROOT).contains(q)) {
                continue;
            }
            filteredAtlasIds.add(id);
        }

        if (!q.isEmpty() || individualImages) {
            Set<String> images = resourceIndex.imageIds();
            for (String image : images) {
                if (!q.isEmpty() && !image.toLowerCase(Locale.ROOT).contains(q)) {
                    continue;
                }
                String atlasId = resourceIndex.image(image).atlasId;
                if (!filteredAtlasIds.contains(atlasId, false)) {
                    filteredAtlasIds.add(atlasId);
                }
                filteredImageIds.add(image);
            }
        }

        if(individualImages){
            list.setItems(filteredImageIds);
        } else {
            list.setItems(filteredAtlasIds);
        }

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

    public void onSelect(String atlasId) {

    }
}
