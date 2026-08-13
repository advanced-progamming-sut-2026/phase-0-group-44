package pvz.browser.screens.animations;


import pvz.browser.widgets.VirtualizedList;


import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.files.FileHandle;
import com.badlogic.gdx.math.Interpolation;
import com.badlogic.gdx.scenes.scene2d.Actor;
import com.badlogic.gdx.scenes.scene2d.InputEvent;
import com.badlogic.gdx.scenes.scene2d.InputListener;
import com.badlogic.gdx.scenes.scene2d.Touchable;
import com.badlogic.gdx.scenes.scene2d.actions.Actions;
import com.badlogic.gdx.scenes.scene2d.ui.ImageButton;
import com.badlogic.gdx.scenes.scene2d.ui.Label;
import com.badlogic.gdx.scenes.scene2d.ui.ProgressBar;
import com.badlogic.gdx.scenes.scene2d.ui.ScrollPane;
import com.badlogic.gdx.scenes.scene2d.ui.Skin;
import com.badlogic.gdx.scenes.scene2d.ui.Stack;
import com.badlogic.gdx.scenes.scene2d.ui.Table;
import com.badlogic.gdx.scenes.scene2d.ui.TextButton;
import com.badlogic.gdx.scenes.scene2d.ui.TextField;
import com.badlogic.gdx.scenes.scene2d.utils.ChangeListener;
import com.badlogic.gdx.scenes.scene2d.utils.ClickListener;
import com.badlogic.gdx.utils.Align;
import com.badlogic.gdx.utils.Array;

import pvz.skin.BorderedTable;
import pvz.browser.utils.PamUtils;
import pvz.browser.core.BrowserContext;
import pvz.browser.core.UiManager;

import pvz.libpvz.pam.PamPlayer;

public class AnimationsList extends Table {
    private static final float CARD_HEIGHT = 72f;

    private final Skin skin;
    private final PamPlayer player;

    private FileHandle imagesRoot;
    private ArrayList<String> allPamPaths = new ArrayList<>();
    private ArrayList<String> filteredPamPaths = new ArrayList<>();

    private TextField search;
    private VirtualizedList<String> list;

    public AnimationsList() {
        super(UiManager.skin);
        skin = UiManager.skin;
        player = BrowserContext.player;
        imagesRoot = new FileHandle(BrowserContext.settings.assetsRootPath);

        scanAnimations();
        setupUi();
        applyFilter();
    }

    private void setupUi() {
        setBackground("image_ui_if_bundle_reward_multiplier_bg_10");
        pad(10);

        search = new TextField("", skin);
        search.setMessageText("Search animations...");

        list = new VirtualizedList<>(CARD_HEIGHT, new VirtualizedList.CardFactory<String>() {
            @Override
            public Actor create(String item, int index) {
                return new AnimationCard(filteredPamPaths.get(index)) {
                    @Override
                    public void onClick() {
                        onAnimationSelect(filteredPamPaths.get(index));
                    }
                };
            }
        });

        ScrollPane scroll = new ScrollPane(list, skin);
        scroll.setFadeScrollBars(false);
        scroll.setScrollingDisabled(true, false);

        scroll.addListener(new InputListener(){
            @Override
            public void enter(InputEvent event, float x, float y, int pointer, Actor fromActor) {
                getStage().setScrollFocus(scroll);
            }
        });

        add(search).growX().row();
        add(scroll).grow().row();

        search.addListener(new ChangeListener() {
            @Override
            public void changed(ChangeEvent e, Actor a) {
                applyFilter();
            }
        });
    }

    private void scanAnimations() {
        ArrayList<FileHandle> found = new ArrayList<>();
        collect(imagesRoot, found);
        found.sort(Comparator.comparing(f -> f.path().toLowerCase(Locale.ROOT)));
        String rootPath = imagesRoot.file().getAbsolutePath();
        for (FileHandle f : found) {
            String rel = f.file().getAbsolutePath();
            if (rel.startsWith(rootPath)) {
                rel = rel.substring(rootPath.length() + 1);
            }
            allPamPaths.add(rel.replace('\\', '/'));
        }
    }

    private static void collect(FileHandle dir, ArrayList<FileHandle> out) {
        if (!dir.exists() || !dir.isDirectory()) {
            return;
        }
        for (FileHandle child : dir.list()) {
            if (child.isDirectory()) {
                collect(child, out);
            } else if (child.name().toUpperCase(Locale.ROOT).endsWith(".PAM")) {
                out.add(child);
            }
        }
    }

    private void applyFilter() {
        String q = search.getText() == null ? "" : search.getText().trim().toLowerCase(Locale.ROOT);
        filteredPamPaths.clear();
        Array<String> items = new Array<>();
        for (int i = 0; i < allPamPaths.size(); i++) {
            if (q.isEmpty() || PamUtils.prettify(allPamPaths.get(i)).toLowerCase(Locale.ROOT).contains(q)) {
                filteredPamPaths.add(allPamPaths.get(i));
                items.add(allPamPaths.get(i));
            }
        }
        list.setItems(items);
    }

    // private void updateSelected(String pamName) {
    //     currentPam = pamName;
    //     List<String> clips = player.clips(pamName);
    //     currentClip = clips.isEmpty() ? null : clips.get(0);
    //     currentClipLabel.setText(currentClip == null ? "NO_CLIP" : currentClip);
    //     previewTitleLabel.setText(PamUtils.getPrettiyPamNameFromPath(pamName));

    //     pamActor.setPam(currentPam);
    //     pamActor.setClip(currentClip);
    // }

    /**
     * Dump every scanned PAM's clip metadata (name/path/canvas/labels+durations) to
     * one JSON file.
     */
    // private void exportAllPamData() {
    //     // Alongside the assets: <IMAGES>/../.. is the "PVZ2 Assets" root, so write into
    //     // its Assets folder.
    //     File imagesDir = imagesRoot.file().getAbsoluteFile();
    //     File out = new File(imagesDir.getParentFile().getParentFile(), "Assets/animations.json");
    //     try {
    //         AnimationDataExporter.Result result = AnimationDataExporter.export(allPamPaths, imagesRoot,
    //                 BrowserContext.player, out);
    //         String skippedNote = result.skipped > 0 ? " (" + result.skipped + " skipped)" : "";
    //         previewTitleLabel.setText("Exported " + result.exported + " animations" + skippedNote
    //                 + "\n" + result.output.getAbsolutePath());
    //     } catch (IOException e) {
    //         previewTitleLabel.setText("Export failed: " + e.getMessage());
    //     }
    // }

    public void onAnimationSelect(String pamName) {

    }
}
