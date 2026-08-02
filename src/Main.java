import controller.App;
import model.Store;
import view.MenuRouter;

import java.io.UnsupportedEncodingException;
import java.util.Scanner;

public final class Main {

    private Main() {
    }

    public static void main(String[] args) throws UnsupportedEncodingException {
        App app = new App();
        System.out.println(app.start().getMessage());

        MenuRouter router = new MenuRouter(app);
        Scanner scanner = new Scanner(System.in);

        System.setOut(new java.io.PrintStream(System.out, true, "UTF-8"));

        while (Store.isRunning() && scanner.hasNextLine()) {
            router.route(scanner.nextLine());
        }

        // Reached when input ends without "menu exit": progress is still saved.
        app.getUserService().shutdown();
    }
}
