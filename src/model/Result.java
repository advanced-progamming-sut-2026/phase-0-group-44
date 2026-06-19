package model;

public class Result<T> {
    private boolean status;
    private boolean gameOver;
    private boolean dead;

    private final StringBuilder message = new StringBuilder();

    private T data;

    public void setStatus(boolean status) {
        this.status = status;
    }

    public boolean getStatus() {
        return this.status;
    }

    public void appendToMessage(String input) {
        message.append(input);
    }

    public String getMessage() {
        return message.toString();
    }

    public void setDead(boolean dead) {
        this.dead = dead;
    }

    public boolean isDead() {
        return dead;
    }

    public boolean isGameOver() {
        return gameOver;
    }

    public void setGameOver(boolean gameOver) {
        this.gameOver = gameOver;
    }

    public T getData() {
        return data;
    }

    public void setData(T data) {
        this.data = data;
    }
}
