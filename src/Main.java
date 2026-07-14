import controller.App;

public final class Main {
    private Main() {
    }

    public static void main(String[] args) {
        App app = new App();
        System.out.println(app.start().getMessage());
    }
}
