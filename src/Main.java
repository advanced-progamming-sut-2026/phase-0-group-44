import controller.App;
import model.Store;
import view.MenuRouter;

import java.util.Scanner;

public final class Main {

    private Main() {
    }

    public static void main(String[] args) {
        App app = new App();
        System.out.println(app.start().getMessage());

        MenuRouter router = new MenuRouter(app);
        Scanner scanner = new Scanner(System.in);

        while (Store.isRunning() && scanner.hasNextLine()) {
            router.route(scanner.nextLine());
        }

        // Reached when input ends without "menu exit": progress is still saved.
        app.getUserService().shutdown();
    }
}
