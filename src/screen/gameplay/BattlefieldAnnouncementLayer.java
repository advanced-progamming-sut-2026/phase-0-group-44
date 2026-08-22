package screen.gameplay;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.scenes.scene2d.Actor;
import com.badlogic.gdx.scenes.scene2d.Group;
import com.badlogic.gdx.scenes.scene2d.InputEvent;
import com.badlogic.gdx.scenes.scene2d.Touchable;
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
 * Phase-2 warning banners and lightweight NPC dialogue overlay.
 *
 * <p>The model remains authoritative. This layer only translates gameplay
 * events into short visual cues and deliberately blocks simulation while a
 * cue is visible, so warnings are readable before the action continues.</p>
 */
public final class BattlefieldAnnouncementLayer {
    private static final float WIDTH = 1280f;
    private static final float HEIGHT = 720f;
    private static final float BANNER_SECONDS = 1.65f;
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
        if (active.kind() == CueKind.BANNER) {
            bannerSeconds -= Math.max(0f, delta);
            if (bannerSeconds <= 0f) {
                dismissActive();
                showNext();
            }
        }
    }

    private void consumeEvent(String event, int currentWave) {
        if (event == null || event.isBlank()) {
            return;
        }
        String normalized = event.trim().toLowerCase(Locale.ROOT);
        if (normalized.startsWith("wave ") && normalized.endsWith(" started.")) {
            queueWaveBanner(currentWave);
        } else if (normalized.startsWith("necromancy spawned")) {
            queueEventBanner("necromancy-" + currentWave, "NECROMANCY!");
        } else if (normalized.startsWith("a zombie emerged from flooded low tide")) {
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

        Image bar = tintedImage(new Color(0.56f, 0.02f, 0.02f, 0.94f));
        bar.setBounds(0f, 286f, WIDTH, 116f);
        group.addActor(bar);

        Label label = new Label(text, skin, "medium_outline");
        label.setAlignment(Align.center);
        label.setFontScale(1.35f);
        label.setBounds(80f, 300f, WIDTH - 160f, 88f);
        group.addActor(label);
        return group;
    }

    private Actor buildDialogue(String speaker, String text) {
        Group group = new Group();
        group.setSize(WIDTH, HEIGHT);
        group.setTouchable(Touchable.enabled);
        group.addActor(dialogueShade());
        group.addActor(dialoguePanel());
        addSpeakerVisual(group, speaker);
        addDialogueText(group, speaker, text);
        group.addListener(new ClickListener() {
            @Override
            public void clicked(InputEvent event, float x, float y) {
                dismissActive();
                showNext();
            }
        });
        return group;
    }

    private Actor dialogueShade() {
        Image shade = tintedImage(new Color(0f, 0f, 0f, 0.22f));
        shade.setBounds(0f, 0f, WIDTH, HEIGHT);
        return shade;
    }

    private Actor dialoguePanel() {
        Image panel = tintedImage(new Color(0.08f, 0.07f, 0.05f, 0.94f));
        panel.setBounds(150f, 26f, 1010f, 194f);
        return panel;
    }

    private void addSpeakerVisual(Group group, String speaker) {
        if ("CRAZY DAVE".equals(speaker) && pamPlayer != null) {
            PamEnvironmentActor dave = new PamEnvironmentActor(
                    pamPlayer, DAVE_PAM, "anim_mediumtalk", 0.34f, 0f, -8f);
            dave.setBounds(36f, 18f, 250f, 250f);
            group.addActor(dave);
            return;
        }
        Label badge = new Label(speaker.substring(0, 1), skin, "medium_outline");
        badge.setAlignment(Align.center);
        badge.setFontScale(2.2f);
        badge.setBounds(58f, 72f, 110f, 110f);
        group.addActor(badge);
    }

    private void addDialogueText(Group group, String speaker, String text) {
        Label name = new Label(speaker, skin, "medium_outline");
        name.setColor(1f, 0.78f, 0.20f, 1f);
        name.setBounds(286f, 160f, 770f, 38f);
        group.addActor(name);

        Label body = new Label(text, skin);
        body.setWrap(true);
        body.setAlignment(Align.left, Align.top);
        body.setBounds(286f, 72f, 770f, 86f);
        group.addActor(body);

        Label hint = new Label("CLICK TO CONTINUE", skin);
        hint.setColor(0.78f, 0.78f, 0.78f, 1f);
        hint.setAlignment(Align.right);
        hint.setBounds(850f, 42f, 260f, 26f);
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
