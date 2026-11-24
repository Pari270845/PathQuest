package pathquest.level;

import java.util.List;
import pathquest.model.Coin;
import pathquest.model.Enemy;
import pathquest.model.Gem;
import pathquest.model.Position;

public class Level {
    private final int width;
    private final int height;
    private final int timeSeconds;
    private final int lives;
    private final char[][] grid;
    private final Position start;
    private final Position flag;
    private final List<Coin> coins;
    private final List<Gem> gems;
    private final List<Enemy> enemies;

    public Level(int width, int height, int timeSeconds, int lives, char[][] grid, Position start, Position flag,
                 List<Coin> coins, List<Gem> gems, List<Enemy> enemies) {
        this.width = width;
        this.height = height;
        this.timeSeconds = timeSeconds;
        this.lives = lives;
        this.grid = grid;
        this.start = start;
        this.flag = flag;
        this.coins = coins;
        this.gems = gems;
        this.enemies = enemies;
    }

    public boolean inside(int row, int col) {
        return row >= 0 && row < height && col >= 0 && col < width;
    }

    public boolean isBlocked(int row, int col) {
        return inside(row, col) && grid[row][col] == '#';
    }

    public int getWidth() {
        return width;
    }

    public int getHeight() {
        return height;
    }

    public int getTimeSeconds() {
        return timeSeconds;
    }

    public int getLives() {
        return lives;
    }

    public char[][] getGrid() {
        return grid;
    }

    public Position getStart() {
        return start;
    }

    public Position getFlag() {
        return flag;
    }

    public List<Coin> getCoins() {
        return coins;
    }

    public List<Gem> getGems() {
        return gems;
    }

    public List<Enemy> getEnemies() {
        return enemies;
    }
}
