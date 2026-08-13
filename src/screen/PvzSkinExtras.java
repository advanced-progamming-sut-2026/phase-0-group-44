package screen;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.scenes.scene2d.ui.Skin;
import com.badlogic.gdx.scenes.scene2d.ui.Window;

/**
 * pvz2_skin.json هیچ استایلی برای Window/Dialog تعریف نکرده،
 * پس new Dialog(title, skin) به‌تنهایی کرش می‌کند.
 * این کلاس یک استایل "default" برای Window با اجزای موجود در همان
 * اسکین می‌سازد:
 *  - قاب دیالوگ: image_ui_dialog_asset_dialogborder_10
 *  - عنوان: فونت متوسطِ حاشیه‌دار (خواناتر روی هر بک‌گراندی)
 *  - stageBackground: کل صفحه‌ی پشت دیالوگ را نیمه‌تیره می‌کند تا
 *    محتوای صفحه‌ی زیرین (مثلاً فرم لاگین) از پشت دیالوگ دیده نشود.
 */
final class PvzSkinExtras {

    private PvzSkinExtras() {
    }

    static void ensureDialogStyle(Skin skin) {
        if (skin.has("default", Window.WindowStyle.class)) {
            return; // قبلاً (مثلاً توسط یک صفحه‌ی دیگر) اضافه شده
        }

        Window.WindowStyle windowStyle = new Window.WindowStyle(
                skin.getFont("FBUSV8C5EI_2_outline"), // فونت بزرگ‌تر و حاشیه‌دار برای عنوان دیالوگ
                Color.WHITE,
                skin.getDrawable("image_ui_dialog_asset_dialogborder_10")
        );

        // این drawable از قبل توی pvz2_skin.json تعریف شده (رنگ نیمه‌شفاف مشکی)
        windowStyle.stageBackground = skin.getDrawable("modal_background");

        skin.add("default", windowStyle, Window.WindowStyle.class);
    }
}