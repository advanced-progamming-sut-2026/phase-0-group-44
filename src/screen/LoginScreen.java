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
import controller.LoginMenuController;
import model.Result;
import pvz.skin.PvzSkin;

public final class LoginScreen implements Screen {

    private final PvzGame game;
    private final LoginMenuController loginController;

    private Stage stage;
    private Skin skin;
    private ToastManager toast;
    private Texture backgroundTexture;

    public LoginScreen(PvzGame game, App app) {
        this.game = game;
        this.loginController = app.getLoginController();
    }

    @Override
    public void show() {
        stage = new Stage(new ScreenViewport());

        skin = PvzSkin.get();

        PvzSkinExtras.ensureDialogStyle(skin);

        toast = new ToastManager(stage, skin);
        Gdx.input.setInputProcessor(stage);
        buildForm();
    }

    private void buildForm() {
        backgroundTexture = new Texture(Gdx.files.internal("ui/LoginMenu.png"));
        Table root = new Table();
        root.setBackground(new TextureRegionDrawable(backgroundTexture));
        root.setFillParent(true);
        stage.addActor(root);

        TextField usernameField = new TextField("", skin);
        TextField passwordField = new TextField("", skin);
        passwordField.setPasswordMode(true);
        passwordField.setPasswordCharacter('*');
        CheckBox stayLoggedInBox = new CheckBox(" Stay logged in", skin);

        root.add(new Label("Login", skin, "big_outline")).colspan(2).padBottom(20).row();
        root.add(new Label("Username", skin, "medium_outline")).left();
        root.add(usernameField).width(300).row();
        root.add(new Label("Password", skin, "medium_outline")).left();
        root.add(passwordField).width(300).row();
        root.add(stayLoggedInBox).colspan(2).left().padTop(5).row();

        TextButton loginButton = new TextButton("Login", skin);
        loginButton.addListener(new ChangeListener() {
            @Override
            public void changed(ChangeEvent event, Actor actor) {
                Result<String> result = loginController.login(
                        usernameField.getText(),
                        passwordField.getText(),
                        stayLoggedInBox.isChecked()
                );

                if (result.getStatus()) {
                    game.goToScreenForCurrentMenu(); // login() خودش Store.setCurrentMenu(MAIN) رو زده
                } else {
                    toast.showError(result.getMessage());
                }
            }
        });
        root.add(loginButton).colspan(2).padTop(20).row();

        TextButton forgotButton = new TextButton("Forgot password?", skin);
        forgotButton.addListener(new ChangeListener() {
            @Override
            public void changed(ChangeEvent event, Actor actor) {
                openForgotPasswordDialog();
            }
        });
        root.add(forgotButton).colspan(2).padTop(10).row();

        TextButton goToRegisterButton = new TextButton("Create an account", skin);
        goToRegisterButton.addListener(new ChangeListener() {
            @Override
            public void changed(ChangeEvent event, Actor actor) {
                model.Store.setCurrentMenu(model.enums.MenuName.REGISTER);
                game.goToScreenForCurrentMenu();
            }
        });
        root.add(goToRegisterButton).colspan(2).padTop(10);
    }

    /** Modal, 3-step recovery: forgetPassword -> answer -> newPassword, all in one dialog. */
    private void openForgotPasswordDialog() {
        Dialog dialog = new Dialog("Recover password", skin);

        // فاصله‌ی دور عنوان، تا به لبه‌ی بالای قاب دیالوگ نچسبد
        dialog.getTitleTable().pad(12, 20, 8, 20);

        Table content = dialog.getContentTable();
        content.pad(30);
        // فاصله‌ی یکسان زیر هر سلولی که از این به بعد اضافه می‌شود
        content.defaults().padBottom(16);

        showRecoveryStep1(content, dialog);

        dialog.show(stage);
        resizeAndCenterDialog(dialog);
    }

    private void showRecoveryStep1(Table content, Dialog dialog) {
        content.clear();

        TextField usernameField = new TextField("", skin);
        TextField emailField = new TextField("", skin);
        content.add(new Label("Username", skin, "medium_outline")).left();
        content.add(usernameField).width(320).row();
        content.add(new Label("Email", skin, "medium_outline")).left();
        content.add(emailField).width(320).row();

        TextButton nextButton = new TextButton("Next", skin);
        nextButton.addListener(new ChangeListener() {
            @Override
            public void changed(ChangeEvent event, Actor actor) {
                Result<String> result = loginController.forgetPassword(
                        usernameField.getText(), emailField.getText());

                if (result.getStatus()) {
                    // result.getData() سوال امنیتی است
                    showRecoveryStep2(content, dialog, result.getData());
                } else {
                    toast.showError(result.getMessage());
                }
            }
        });
        content.add(nextButton).colspan(2).padTop(10).row();

        TextButton cancelButton = new TextButton("Cancel", skin);
        cancelButton.addListener(new ChangeListener() {
            @Override
            public void changed(ChangeEvent event, Actor actor) {
                dialog.hide();
            }
        });
        content.add(cancelButton).colspan(2);

        resizeAndCenterDialog(dialog);
    }

    private void showRecoveryStep2(Table content, Dialog dialog, String question) {
        content.clear();

        Label questionLabel = new Label(question, skin, "medium_outline");
        questionLabel.setWrap(true);
        content.add(questionLabel).colspan(2).width(320).padBottom(16).row();

        TextField answerField = new TextField("", skin);
        content.add(new Label("Answer", skin, "medium_outline")).left();
        content.add(answerField).width(320).row();

        TextButton submitAnswerButton = new TextButton("Submit", skin);
        submitAnswerButton.addListener(new ChangeListener() {
            @Override
            public void changed(ChangeEvent event, Actor actor) {
                Result<String> result = loginController.answer(answerField.getText());

                if (result.getStatus()) {
                    showRecoveryStep3(content, dialog);
                } else {
                    // پاسخ غلط: خودِ answer() کاربر رو به منوی لاگین برمی‌گردونه
                    toast.showError(result.getMessage());
                    dialog.hide();
                }
            }
        });
        content.add(submitAnswerButton).colspan(2).padTop(10);

        resizeAndCenterDialog(dialog);
    }

    private void showRecoveryStep3(Table content, Dialog dialog) {
        content.clear();

        TextField newPasswordField = new TextField("", skin);
        newPasswordField.setPasswordMode(true);
        newPasswordField.setPasswordCharacter('*');
        content.add(new Label("New password", skin, "medium_outline")).left();
        content.add(newPasswordField).width(320).row();

        TextButton confirmButton = new TextButton("Change password", skin);
        confirmButton.addListener(new ChangeListener() {
            @Override
            public void changed(ChangeEvent event, Actor actor) {
                Result<String> result = loginController.newPassword(newPasswordField.getText());

                if (result.getStatus()) {
                    toast.showInfo(result.getMessage());
                    dialog.hide();
                } else {
                    toast.showError(result.getMessage());
                }
            }
        });
        content.add(confirmButton).colspan(2).padTop(10);

        resizeAndCenterDialog(dialog);
    }

    /**
     * چون هر مرحله محتوای دیالوگ را عوض می‌کند (content.clear() + پر کردن دوباره)،
     * باید دیالوگ دوباره اندازه‌گیری و وسط صفحه قرار بگیرد؛ وگرنه اندازه‌ی
     * مرحله‌ی قبلی می‌ماند یا از مرکز خارج می‌شود.
     */
    private void resizeAndCenterDialog(Dialog dialog) {
        dialog.pack();
        dialog.setPosition(
                Math.round((stage.getWidth() - dialog.getWidth()) / 2f),
                Math.round((stage.getHeight() - dialog.getHeight()) / 2f)
        );
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
        // مهم: skin.dispose() اینجا حذف شد. اگر PvzSkin.get() یک اسکین
        // مشترک (singleton) برای کل بازی برمی‌گرداند، dispose کردنش در
        // یک صفحه باعث می‌شود صفحه‌ی بعدی که از همان skin استفاده می‌کند
        // کرش کند. اسکین را یک‌بار، فقط هنگام بستن کل بازی (مثلاً در
        // متد dispose() کلاس PvzGame یا خود PvzSkin) دیسپوز کن.
    }
}