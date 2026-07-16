package model.quest;

public class QuestProgress {
    private long progress;
    private boolean completed;
    private boolean claimed;
    private String recurrenceDate;

    public QuestProgress() {}
    public long getProgress() { return progress; }
    public boolean isCompleted() { return completed; }
    public boolean isClaimed() { return claimed; }
    public String getRecurrenceDate() { return recurrenceDate; }
    public void add(long amount) { if (!completed && amount > 0) progress += amount; }
    public void complete() { completed = true; }
    public void claim() { claimed = true; }
    public void reset(String date) { progress = 0; completed = false; claimed = false; recurrenceDate = date; }
    public void setRecurrenceDate(String recurrenceDate) { this.recurrenceDate = recurrenceDate; }
}
