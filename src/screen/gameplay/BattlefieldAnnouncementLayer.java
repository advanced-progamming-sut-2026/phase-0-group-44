package screen.gameplay;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.scenes.scene2d.Actor;
import com.badlogic.gdx.scenes.scene2d.Group;
import com.badlogic.gdx.scenes.scene2d.InputEvent;
import com.badlogic.gdx.scenes.scene2d.Touchable;
import com.badlogic.gdx.scenes.scene2d.actions.Actions;
import com.badlogic.gdx.scenes.scene2d.ui.Image;
import com.badlogic.gdx.scenes.scene2d.ui.Label;
import com.badlogic.gdx.scenes.scene2d.ui.Skin;
import com.badlogic.gdx.scenes.scene2d.utils.ClickListener;
import com.badlogic.gdx.utils.Align;
import model.config.GameWorld;
import model.inGame.GameSession;
import pvz.libpvz.pam.PamPlayer;

import java.util.ArrayDeque;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Queue;
import java.util.Set;

/**
 * Phase-2 warning banners and NPC dialogue overlay.
 *
 * <p>The model remains authoritative. This layer only translates gameplay
 * events into animated visual cues and deliberately blocks simulation while a
 * cue is visible, so warnings/dialogue are readable before action continues.</p>
 */
public final class BattlefieldAnnouncementLayer {
    private static final float WIDTH = 1280f;
    private static final float HEIGHT = 720f;
    private static final float BANNER_SECONDS = 1.65f;
    private static final float TYPEWRITER_CHARS_PER_SECOND = 46f;
    private static final String DAVE_PAM =
            "768/INITIAL/CRAZYDAVE/CRAZYDAVE/CRAZYDAVE.PAM";

    private final Texture whiteTexture;
    private final Skin skin;
    private final PamPlayer pamPlayer;
    private final Group root = new Group();
    private final Queue<Cue> queued = new ArrayDeque<>();
    private final Set<String> queuedEventKeys = new HashSet<>();

    private Cue active;
    private Actor activeActor;
    private Label activeBodyLabel;
    private String activeFullText = "";
    private float visibleCharacters;
    private float bannerSeconds;
    private boolean suppressed;

    public BattlefieldAnnouncementLayer(
            Texture whiteTexture,
            Skin skin,
            PamPlayer pamPlayer
    ) {
        this.whiteTexture = whiteTexture;
        this.skin = skin;
        this.pamPlayer = pamPlayer;
        root.setSize(WIDTH, HEIGHT);
        root.setTouchable(Touchable.childrenOnly);
    }

    public Group root() {
        return root;
    }

    /** Adds the chapter-flavoured opening exchange and the first-wave warning. */
    public void queueLevelIntro(GameSession session, boolean wavesAlreadyStarted) {
        if (session == null || session.getLevel() == null) {
            return;
        }
        for (Cue cue : introDialogue(session.getLevel().getWorld())) {
            queued.add(cue);
        }
        if (wavesAlreadyStarted) {
            queueEventBanner("wave-start", "THE ZOMBIES ARE COMING!");
        }
    }

    /** Converts canonical GameEngine event strings into required Phase-2 alerts. */
    public void consumeEvents(List<String> events, int currentWave) {
        if (events == null || events.isEmpty()) {
            return;
        }
        for (String event : events) {
            consumeEvent(event, currentWave);
        }
    }

    public void setSuppressed(boolean suppressed) {
        this.suppressed = suppressed;
        root.setVisible(!suppressed);
    }

    public boolean isGameplayBlocked() {
        return !suppressed && active != null;
    }

    /** Must be called once per render frame before advancing the simulation. */
    public void update(float delta) {
        if (suppressed) {
            return;
        }
        if (active == null) {
            showNext();
            return;
        }

        float safeDelta = Math.max(0f, delta);
        if (active.kind() == CueKind.BANNER) {
            bannerSeconds -= safeDelta;
            if (bannerSeconds <= 0f) {
                dismissActive();
                showNext();
            }
            return;
        }

        if (activeBodyLabel != null && visibleCharacters < activeFullText.length()) {
            visibleCharacters = Math.min(
                    activeFullText.length(),
                    visibleCharacters + safeDelta * TYPEWRITER_CHARS_PER_SECOND);
            int characterCount = Math.min(activeFullText.length(), (int) visibleCharacters);
            activeBodyLabel.setText(activeFullText.substring(0, characterCount));
        }
    }

    private void consumeEvent(String event, int currentWave) {
        if (event == null || event.isBlank()) {
            return;
        }
        String normalized = event.trim().toLowerCase(Locale.ROOT);
        if (normalized.startsWith("wave ") && normalized.endsWith(" started.")) {
            queueWaveBanner(currentWave);
        } else if (normalized.startsWith("necromancy is stirring")
                || normalized.startsWith("necromancy spawned")) {
            queueEventBanner("necromancy-" + currentWave, "NECROMANCY!");
        } else if (normalized.startsWith("low tide zombies incoming")) {
            queueEventBanner("low-tide-" + currentWave, "LOW TIDE! ZOMBIES ARE EMERGING!");
        }
    }

    private void queueWaveBanner(int wave) {
        if (wave <= 1) {
            queueEventBanner("wave-start", "THE ZOMBIES ARE COMING!");
        } else {
            queueEventBanner("wave-" + wave, "A NEW WAVE IS APPROACHING!");
        }
    }

    private void queueEventBanner(String key, String text) {
        if (queuedEventKeys.add(key)) {
            queued.add(new Cue(CueKind.BANNER, "", text));
        }
    }

    private void showNext() {
        active = queued.poll();
        if (active == null) {
            return;
        }
        activeActor = active.kind() == CueKind.BANNER
                ? buildBanner(active.text())
                : buildDialogue(active.speaker(), active.text());
        root.addActor(activeActor);
        bannerSeconds = active.kind() == CueKind.BANNER ? BANNER_SECONDS : 0f;
    }

    private Actor buildBanner(String text) {
        Group group = new Group();
        group.setSize(WIDTH, HEIGHT);
        group.setTouchable(Touchable.disabled);

        Image darkBackdrop = tintedImage(new Color(0f, 0f, 0f, 0.24f));
        darkBackdrop.setBounds(0f, 278f, WIDTH, 132f);
        darkBackdrop.getColor().a = 0f;
        darkBackdrop.addAction(Actions.sequence(
                Actions.fadeIn(0.12f),
                Actions.delay(1.18f),
                Actions.fadeOut(0.22f)
        ));
        group.addActor(darkBackdrop);

        Image upperEdge = tintedImage(new Color(1f, 0.36f, 0.06f, 0.92f));
        upperEdge.setBounds(-WIDTH, 286f, WIDTH, 4f);
        upperEdge.addAction(Actions.moveTo(0f, 286f, 0.18f));
        group.addActor(upperEdge);

        Image bar = tintedImage(new Color(0.50f, 0.015f, 0.015f, 0.96f));
        bar.setBounds(WIDTH, 290f, WIDTH, 112f);
        bar.addAction(Actions.moveTo(0f, 290f, 0.18f));
        group.addActor(bar);

        Image lowerEdge = tintedImage(new Color(1f, 0.76f, 0.12f, 0.88f));
        lowerEdge.setBounds(WIDTH, 398f, WIDTH, 4f);
        lowerEdge.addAction(Actions.moveTo(0f, 398f, 0.18f));
        group.addActor(lowerEdge);

        Label label = new Label(text, skin, "medium_outline");
        label.setAlignment(Align.center);
        label.setFontScale(1.38f);
        label.setBounds(80f, 302f, WIDTH - 160f, 82f);
        label.setOrigin(Align.center);
        label.getColor().a = 0f;
        label.setScale(0.86f);
        label.addAction(Actions.parallel(
                Actions.fadeIn(0.22f),
                Actions.scaleTo(1f, 1f, 0.22f)
        ));
        group.addActor(label);
        return group;
    }

    private Actor buildDialogue(String speaker, String text) {
        Group group = new Group();
        group.setSize(WIDTH, HEIGHT);
        group.setTouchable(Touchable.enabled);

        Image shade = dialogueShade();
        shade.getColor().a = 0f;
        shade.addAction(Actions.fadeIn(0.18f));
        group.addActor(shade);

        Group card = new Group();
        card.setBounds(122f, -34f, 1036f, 236f);
        card.getColor().a = 0f;
        card.addAction(Actions.parallel(
                Actions.moveTo(122f, 24f, 0.24f),
                Actions.fadeIn(0.20f)
        ));
        buildDialogueCard(card, speaker, text);
        group.addActor(card);

        group.addListener(new ClickListener() {
            @Override
            public void clicked(InputEvent event, float x, float y) {
                if (activeBodyLabel != null && visibleCharacters < activeFullText.length()) {
                    visibleCharacters = activeFullText.length();
                    activeBodyLabel.setText(activeFullText);
                    return;
                }
                dismissActive();
                showNext();
            }
        });
        return group;
    }

    private void buildDialogueCard(Group card, String speaker, String text) {
        boolean penny = "PENNY".equals(speaker);
        Color accent = penny
                ? new Color(0.18f, 0.88f, 1f, 1f)
                : new Color(1f, 0.70f, 0.10f, 1f);

        Image shadow = tintedImage(new Color(0f, 0f, 0f, 0.52f));
        shadow.setBounds(8f, -8f, 1028f, 220f);
        card.addActor(shadow);

        Image panel = tintedImage(new Color(0.055f, 0.047f, 0.035f, 0.97f));
        panel.setBounds(0f, 0f, 1028f, 220f);
        card.addActor(panel);

        Image accentTop = tintedImage(accent.cpy());
        accentTop.setBounds(0f, 216f, 1028f, 4f);
        card.addActor(accentTop);

        Image accentLeft = tintedImage(accent.cpy());
        accentLeft.setBounds(0f, 0f, 6f, 220f);
        card.addActor(accentLeft);

        Image portraitBack = tintedImage(new Color(
                penny ? 0.02f : 0.12f,
                penny ? 0.12f : 0.075f,
                penny ? 0.16f : 0.025f,
                0.98f));
        portraitBack.setBounds(18f, 18f, 190f, 184f);
        card.addActor(portraitBack);

        addSpeakerVisual(card, speaker, accent);
        addDialogueText(card, speaker, text, accent);
    }

    private Image dialogueShade() {
        Image shade = tintedImage(new Color(0f, 0f, 0f, 0.36f));
        shade.setBounds(0f, 0f, WIDTH, HEIGHT);
        return shade;
    }

    private void addSpeakerVisual(Group group, String speaker, Color accent) {
        if ("CRAZY DAVE".equals(speaker) && pamPlayer != null) {
            PamEnvironmentActor dave = new PamEnvironmentActor(
                    pamPlayer, DAVE_PAM, "anim_mediumtalk", 0.31f, 0f, -9f);
            dave.setBounds(-4f, -4f, 238f, 238f);
            dave.getColor().a = 0f;
            dave.setScale(0.9f);
            dave.setOrigin(Align.center);
            dave.addAction(Actions.parallel(
                    Actions.fadeIn(0.20f),
                    Actions.scaleTo(1f, 1f, 0.24f)
            ));
            group.addActor(dave);
            return;
        }

        Group penny = new Group();
        penny.setBounds(36f, 34f, 154f, 150f);

        Image glow = tintedImage(new Color(0.08f, 0.54f, 0.78f, 0.28f));
        glow.setBounds(-8f, -8f, 170f, 166f);
        glow.addAction(Actions.forever(Actions.sequence(
                Actions.alpha(0.18f, 0.7f),
                Actions.alpha(0.38f, 0.7f)
        )));
        penny.addActor(glow);

        Image terminal = tintedImage(new Color(0.015f, 0.08f, 0.11f, 0.98f));
        terminal.setBounds(0f, 0f, 154f, 150f);
        penny.addActor(terminal);

        Image topRail = tintedImage(accent.cpy());
        topRail.setBounds(0f, 144f, 154f, 6f);
        penny.addActor(topRail);

        Label p = new Label("P", skin, "medium_outline");
        p.setColor(accent);
        p.setAlignment(Align.center);
        p.setFontScale(2.35f);
        p.setBounds(13f, 42f, 128f, 76f);
        penny.addActor(p);

        Label ai = new Label("PENNY // AI", skin);
        ai.setColor(0.70f, 0.95f, 1f, 1f);
        ai.setAlignment(Align.center);
        ai.setBounds(8f, 12f, 138f, 26f);
        penny.addActor(ai);

        Image scan = tintedImage(new Color(0.40f, 0.96f, 1f, 0.72f));
        scan.setBounds(13f, 26f, 128f, 3f);
        scan.addAction(Actions.forever(Actions.sequence(
                Actions.moveTo(13f, 127f, 1.05f),
                Actions.alpha(0.22f, 0.08f),
                Actions.moveTo(13f, 26f),
                Actions.alpha(0.72f, 0.08f)
        )));
        penny.addActor(scan);

        penny.getColor().a = 0f;
        penny.setScale(0.90f);
        penny.setOrigin(Align.center);
        penny.addAction(Actions.parallel(
                Actions.fadeIn(0.22f),
                Actions.scaleTo(1f, 1f, 0.24f)
        ));
        group.addActor(penny);
    }

    private void addDialogueText(Group group, String speaker, String text, Color accent) {
        Label name = new Label(speaker, skin, "medium_outline");
        name.setColor(accent);
        name.setBounds(232f, 168f, 720f, 36f);
        group.addActor(name);

        Image separator = tintedImage(new Color(accent.r, accent.g, accent.b, 0.58f));
        separator.setBounds(232f, 157f, 742f, 2f);
        group.addActor(separator);

        activeFullText = text == null ? "" : text;
        visibleCharacters = 0f;
        activeBodyLabel = new Label("", skin);
        activeBodyLabel.setWrap(true);
        activeBodyLabel.setAlignment(Align.left, Align.top);
        activeBodyLabel.setBounds(232f, 61f, 748f, 88f);
        group.addActor(activeBodyLabel);

        Label hint = new Label("CLICK TO SKIP / CONTINUE  >", skin);
        hint.setColor(0.78f, 0.82f, 0.82f, 1f);
        hint.setAlignment(Align.right);
        hint.setBounds(698f, 22f, 282f, 26f);
        hint.addAction(Actions.forever(Actions.sequence(
                Actions.alpha(0.48f, 0.55f),
                Actions.alpha(1f, 0.55f)
        )));
        group.addActor(hint);
    }

    private Image tintedImage(Color color) {
        Image image = new Image(whiteTexture);
        image.setColor(color);
        return image;
    }

    private void dismissActive() {
        if (activeActor != null) {
            activeActor.remove();
        }
        activeActor = null;
        activeBodyLabel = null;
        activeFullText = "";
        visibleCharacters = 0f;
        active = null;
        bannerSeconds = 0f;
    }

    private List<Cue> introDialogue(GameWorld world) {
        if (world == null) {
            return genericDialogue();
        }
        return switch (world) {
            case ANCIENT_EGYPT -> List.of(
                    dialogue("CRAZY DAVE", "Penny, did you pack sunscreen for ancient Egypt?"),
                    dialogue("PENNY", "No. I calculated something more useful: keep the zombies away from the house."));
            case FROSTBITE_CAVES -> List.of(
                    dialogue("CRAZY DAVE", "My taco is frozen solid!"),
                    dialogue("PENNY", "Watch the icy lanes. Frozen ground can change how this battle plays."));
            case BIG_WAVE_BEACH -> List.of(
                    dialogue("CRAZY DAVE", "Surf's up! Also... zombies are up."),
                    dialogue("PENNY", "Watch the tide. Flooded tiles can change when a new wave arrives."));
            case DARK_AGES -> List.of(
                    dialogue("CRAZY DAVE", "Graves, darkness, and medieval zombies. Cozy!"),
                    dialogue("PENNY", "Necromancy can raise zombies from the marked graves. Stay alert."));
        };
    }

    private List<Cue> genericDialogue() {
        return List.of(
                dialogue("CRAZY DAVE", "The zombies are coming!"),
                dialogue("PENNY", "Defend the house. Nothing crosses the lawn."));
    }

    private Cue dialogue(String speaker, String text) {
        return new Cue(CueKind.DIALOGUE, speaker, text);
    }

    private enum CueKind {
        BANNER,
        DIALOGUE
    }

    private record Cue(CueKind kind, String speaker, String text) { }
}
