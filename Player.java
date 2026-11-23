package pathquest.model;

import java.time.LocalDateTime;

public class Player extends GameEntity implements Movable {
    private int playerId;
    private String username;
    private String pin;
    private LocalDateTime createdAt;

    private int lives;
    private int points;
    private int coins;
    private final int startRow;
    private final int startCol;

    public Player(int row, int col) {
        super(row, col);
        startRow = row;
        startCol = col;
    }

    @Override
    public void moveTo(int row, int col) {
        this.row = row;
        this.col = col;
    }

    @Override
    public char symbol() {
        return '@';
    }

    public int getPlayerId() {
        return playerId;
    }

    public void setPlayerId(int playerId) {
        this.playerId = playerId;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getPin() {
        return pin;
    }

    public void setPin(String pin) {
        this.pin = pin;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public int getLives() {
        return lives;
    }

    public void setLives(int lives) {
        this.lives = lives;
    }

    public int getPoints() {
        return points;
    }

    public void setPoints(int points) {
        this.points = points;
    }

    public int getCoins() {
        return coins;
    }

    public void setCoins(int coins) {
        this.coins = coins;
    }

    public int getStartRow() {
        return startRow;
    }

    public int getStartCol() {
        return startCol;
    }
}
