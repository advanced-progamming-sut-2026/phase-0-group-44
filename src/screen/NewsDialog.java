package screen;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.scenes.scene2d.Actor;
import com.badlogic.gdx.scenes.scene2d.Stage;
import com.badlogic.gdx.scenes.scene2d.ui.Dialog;
import com.badlogic.gdx.scenes.scene2d.ui.ImageButton;
import com.badlogic.gdx.scenes.scene2d.ui.Label;
import com.badlogic.gdx.scenes.scene2d.ui.ScrollPane;
import com.badlogic.gdx.scenes.scene2d.ui.Skin;
import com.badlogic.gdx.scenes.scene2d.ui.Table;
import com.badlogic.gdx.scenes.scene2d.utils.ChangeListener;
import controller.NewsMenuController;
import model.News;
import model.Result;

import java.util.ArrayList;

/** Floating news window used by the main menu, following figure 11 of Phase 2. */
final class NewsDialog extends Dialog {

    private static final float CONTENT_WIDTH = 690f;
    private static final float CONTENT_HEIGHT = 360f;

    private final Skin skin;
    private final NewsMenuController controller;
    private final Runnable onReadStateChanged;

    NewsDialog(Skin skin, NewsMenuController controller, Runnable onReadStateChanged) {
        super("NEWS & UPDATES", skin);
        this.skin = skin;
        this.controller = controller;
        this.onReadStateChanged = onReadStateChanged;
        configureTitleBar();
        buildContent();
    }

    void showCentered(Stage stage) {
        show(stage);
        pack();
        centerOn(stage);
    }

    void centerOn(Stage stage) {
        setPosition(
                Math.round((stage.getWidth() - getWidth()) / 2f),
                Math.round((stage.getHeight() - getHeight()) / 2f)
        );
    }

    private void configureTitleBar() {
        getTitleTable().pad(10f, 18f, 8f, 14f);
        getTitleTable().add().expandX();

        ImageButton closeButton = new ImageButton(skin, "generic_close_circle");
        closeButton.addListener(new ChangeListener() {
            @Override
            public void changed(ChangeEvent event, Actor actor) {
                hide();
            }
        });
        getTitleTable().add(closeButton).size(46f, 42f).right();
    }

    private void buildContent() {
        Result<ArrayList<News>> result = controller.showAllNews();
        Table panel = createPanel();

        if (!result.getStatus()) {
            panel.add(messageLabel(result.getMessage())).width(CONTENT_WIDTH).pad(30f);
        } else if (result.getData().isEmpty()) {
            panel.add(messageLabel("No news yet.")).width(CONTENT_WIDTH).pad(30f);
        } else {
            panel.add(createNewsScroll(result.getData())).width(CONTENT_WIDTH)
                    .height(CONTENT_HEIGHT).pad(12f);
        }

        getContentTable().pad(18f, 22f, 22f, 22f);
        getContentTable().add(panel);
        markDisplayedNewsRead();
    }

    private Table createPanel() {
        Table panel = new Table();
        panel.setBackground(skin.getDrawable("image_ui_dialog_asset_inner_bkgd_10"));
        return panel;
    }

    private ScrollPane createNewsScroll(ArrayList<News> newsList) {
        Table list = new Table();
        list.top().left();

        for (News news : newsList) {
            list.add(createNewsCard(news)).growX().pad(6f, 8f, 6f, 8f).row();
        }

        ScrollPane scrollPane = new ScrollPane(list, skin);
        scrollPane.setFadeScrollBars(false);
        scrollPane.setScrollingDisabled(true, false);
        return scrollPane;
    }

    private Table createNewsCard(News news) {
        Table card = new Table();
        card.left().top().pad(12f);
        card.setBackground(skin.getDrawable("image_ui_mainmenu_mm_settings_tab_10"));

        Label heading = new Label(formatHeading(news), skin, "medium_outline");
        heading.setWrap(true);
        if (!news.isRead()) {
            heading.setColor(Color.GOLD);
        }

        String body = news.getDescription() == null ? "" : news.getDescription();
        Label description = new Label(body, skin, "medium_outline");
        description.setWrap(true);
        card.add(heading).left().width(CONTENT_WIDTH - 70f).padBottom(7f).row();
        card.add(description).left().width(CONTENT_WIDTH - 70f);
        return card;
    }

    private Label messageLabel(String text) {
        Label label = new Label(text, skin, "medium_outline");
        label.setWrap(true);
        return label;
    }

    private String formatHeading(News news) {
        String type = news.getType() == null
                ? "NEWS"
                : news.getType().name().replace('_', ' ');
        String prefix = news.isRead() ? "" : "NEW  ";
        String title = news.getTitle() == null ? "" : news.getTitle();
        return prefix + type + "  -  " + title;
    }

    private void markDisplayedNewsRead() {
        Result<Integer> marked = controller.markAllAsRead();
        if (marked.getStatus() && marked.getData() != null && marked.getData() > 0) {
            onReadStateChanged.run();
        }
    }
}
