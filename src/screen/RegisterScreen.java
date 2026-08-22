package screen;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Screen;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.scenes.scene2d.Actor;
import com.badlogic.gdx.scenes.scene2d.Stage;
import com.badlogic.gdx.scenes.scene2d.ui.*;
import com.badlogic.gdx.scenes.scene2d.utils.ChangeListener;
import com.badlogic.gdx.scenes.scene2d.utils.TextureRegionDrawable;
import com.badlogic.gdx.utils.viewport.ScreenViewport;
import controller.App;
import controller.RegisterMenuController;
import model.Result;
import pvz.skin.PvzSkin;

public final class RegisterScreen implements Screen {

    private final PvzGame game;
    private final RegisterMenuController registerController;

    private Stage stage;
    private Skin skin;
    private ToastManager toast;
    private Table root;
    private Texture backgroundTexture;


    public RegisterScreen(PvzGame game, App app) {
        this.game = game;
        this.registerController = app.getRegisterController();
    }

    @Override
    public void show() {
        stage = new Stage(new ScreenViewport());

        // همان باگ shadowing که در LoginScreen بود: باید روی فیلد کلاس
        // مقداردهی شود، نه یک متغیر محلی جدید.
        skin = PvzSkin.get();

        toast = new ToastManager(stage, skin);
        Gdx.input.setInputProcessor(stage);
        backgroundTexture = TextureQuality.load("ui/RegisterMenu.png");
        root = new Table();
        root.setBackground(new TextureRegionDrawable(backgroundTexture));
        root.setFillParent(true);
        root.pad(40);
        root.padTop(170);   // با افزایش این عدد، محتوا بیشتر به سمت پایین میره
        root.padRight(90); // با افزایش این عدد، محتوا بیشتر به سمت چپ میره
        stage.addActor(root);

        showAccountDetailsStep();
    }

    /** Step 1: register(username, password, repeatPassword, nickname, email, gender). */
    private void showAccountDetailsStep() {
        root.clear();

        TextField usernameField = new TextField("", skin);
        TextField passwordField = passwordField();
        TextField repeatPasswordField = passwordField();
        TextField nicknameField = new TextField("", skin);
        TextField emailField = new TextField("", skin);
        SelectBox<String> genderBox = new SelectBox<>(skin);
        genderBox.setItems("male", "female");

        root.add(new Label("Register", skin, "big_outline")).colspan(2).padBottom(20).row();
        addRow(root, "Username", usernameField);
        addRow(root, "Password", passwordField);
        addRow(root, "Repeat password", repeatPasswordField);
        addRow(root, "Nickname", nicknameField);
        addRow(root, "Email", emailField);
        root.add(new Label("Gender", skin, "medium_outline")).left();
        root.add(genderBox).width(300).row();

        TextButton nextButton = new TextButton("Next", skin);
        nextButton.addListener(new ChangeListener() {
            @Override
            public void changed(ChangeEvent event, Actor actor) {
                Result<String> result = registerController.register(
                        usernameField.getText(),
                        passwordField.getText(),
                        repeatPasswordField.getText(),
                        nicknameField.getText(),
                        emailField.getText(),
                        genderBox.getSelected()
                );

                if (result.getStatus()) {
                    // پیام موفقیت شامل متن رندرشده‌ی سوالات امنیتیه؛ اینجا فقط رفتن به مرحله‌ی بعد لازمه
                    showSecurityQuestionStep();
                } else {
                    toast.showError(result.getMessage());
                }
            }
        });
        root.add(nextButton).colspan(2).padTop(20).row();

        TextButton goToLoginButton = new TextButton("Already have an account? Login", skin);
        goToLoginButton.addListener(new ChangeListener() {
            @Override
            public void changed(ChangeEvent event, Actor actor) {
                model.Store.setCurrentMenu(model.enums.MenuName.LOGIN);
                game.goToScreenForCurrentMenu();
            }
        });
        root.add(goToLoginButton).colspan(2).padTop(10);
    }

    /**
     * Step 2: pickQuestion(questionNumber, answer, answerConfirm). The question
     * catalog text itself isn't exposed as structured data by the controller yet
     * (only as pre-rendered text in Result.getMessage() from register()), so for
     * now the player types the question's number directly.
     */
    private void showSecurityQuestionStep() {
        root.clear();

        root.add(new Label("Pick a security question", skin, "medium_outline")).colspan(2).padBottom(20).row();

        java.util.List<String> questions = registerController.getSecurityQuestions();
        SelectBox<String> questionBox = new SelectBox<>(skin);
        questionBox.setItems(questions.toArray(new String[0]));

        TextField answerField = new TextField("", skin);
        TextField answerConfirmField = new TextField("", skin);

        root.add(new Label("Question", skin, "medium_outline")).left();
        root.add(questionBox).width(300).row();
        addRow(root, "Answer", answerField);
        addRow(root, "Confirm answer", answerConfirmField);

        TextButton finishButton = new TextButton("Create account", skin);
        finishButton.addListener(new ChangeListener() {
            @Override
            public void changed(ChangeEvent event, Actor actor) {
                int questionNumber = questionBox.getSelectedIndex() + 1; // catalog is 1-based

                Result<String> result = registerController.pickQuestion(
                        questionNumber,
                        answerField.getText(),
                        answerConfirmField.getText()
                );

                if (result.getStatus()) {
                    toast.showInfo(result.getMessage());
                    model.Store.setCurrentMenu(model.enums.MenuName.LOGIN);
                    game.goToScreenForCurrentMenu();
                } else {
                    toast.showError(result.getMessage());
                }
            }
        });
        root.add(finishButton).colspan(2).padTop(20);
    }

    private TextField passwordField() {
        TextField field = new TextField("", skin);
        field.setPasswordMode(true);
        field.setPasswordCharacter('*');
        return field;
    }

    private void addRow(Table table, String labelText, TextField field) {
        table.add(new Label(labelText, skin, "medium_outline")).left();
        table.add(field).width(300).row();
    }

    @Override
    public void render(float delta) {
        Gdx.gl.glClearColor(0.1f, 0.12f, 0.15f, 1f);
        Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT);
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
        stage.dispose();
        backgroundTexture.dispose();
    }
}