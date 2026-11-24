package pathquest.model;

import java.time.LocalDateTime;

public class Score implements Comparable<Score> {
    private final String username;
    private final int levelNo;
    private final int points;
    private final int coins;
    private final int timeLeft;
    private final boolean won;
    private final LocalDateTime playedAt;

    public Score(String username, int levelNo, int points, int coins, int timeLeft, boolean won, LocalDateTime playedAt) {
        this.username = username;
        this.levelNo = levelNo;
        this.points = points;
        this.coins = coins;
        this.timeLeft = timeLeft;
        this.won = won;
        this.playedAt = playedAt;
    }

    @Override
    public int compareTo(Score other) {
        if (points != other.points) {
            return Integer.compare(other.points, points);
        }
        return playedAt.compareTo(other.playedAt);
    }

    public String getUsername() {
        return username;
    }

    public int getLevelNo() {
        return levelNo;
    }

    public int getPoints() {
        return points;
    }

    public int getCoins() {
        return coins;
    }

    public int getTimeLeft() {
        return timeLeft;
    }

    public boolean isWon() {
        return won;
    }

    public LocalDateTime getPlayedAt() {
        return playedAt;
    }
}
