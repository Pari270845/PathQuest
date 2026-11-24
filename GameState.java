package pathquest.engine;

import java.util.Iterator;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import pathquest.io.SaveFile;
import pathquest.level.Level;
import pathquest.model.Coin;
import pathquest.model.Enemy;
import pathquest.model.GameEntity;
import pathquest.model.Gem;
import pathquest.model.Player;
import pathquest.model.Position;

public class GameState {
    public static final int CONTINUE = 0;
    public static final int LIFE_LOST = 1;
    public static final int WON = 2;
    public static final int LOST = 3;

    private final Level level;
    private final Player player;
    private final List<Coin> coins;
    private final List<Gem> gems;
    private final List<Enemy> enemies;
    private final long createdAt;
    private long timerStart;
    private long pausedAt;
    private boolean paused;

    public GameState(Level level, long nowMillis) {
        this.level = level;
        player = new Player(level.getStart().getRow(), level.getStart().getCol());
        player.setLives(level.getLives());
        coins = level.getCoins();
        gems = level.getGems();
        enemies = level.getEnemies();
        timerStart = nowMillis;
        createdAt = nowMillis;
    }

    public Outcome step(String move, long nowMillis, boolean freezeEnemies) {
        if (timeLeft(nowMillis) == 0) {
            timerStart = nowMillis;
            return loseLife("Time is up");
        }
        MoveRules.MoveResult result = MoveRules.apply(level, player.getPosition(), move);
        String message = "";
        List<Position> cells = result.getCells();
        for (int i = 0; i < cells.size(); i++) {
            Position cell = cells.get(i);
            Coin coin = removeAt(coins, cell);
            if (coin != null) {
                addPoints(coin.getPoints());
                player.setCoins(player.getCoins() + 1);
                message = "Coin +" + coin.getPoints();
            }
            Gem gem = removeAt(gems, cell);
            if (gem != null) {
                addPoints(gem.getPoints());
                message = "Gem +" + gem.getPoints();
            }
            if (enemyAt(cell)) {
                if (!result.enteredByFalling(i)) {
                    return loseLife("An enemy got you");
                }
                while (removeAt(enemies, cell) != null) {
                    addPoints(100);
                    message = "Enemy defeated +100";
                }
            }
            if (cell.equals(level.getFlag())) {
                player.moveTo(cell.getRow(), cell.getCol());
                int bonus = timeLeft(nowMillis) * 5;
                addPoints(bonus);
                return new Outcome(WON, "You reached the flag! Time bonus +" + bonus);
            }
        }
        if (result.isPit()) {
            return loseLife("You fell into a pit");
        }
        player.moveTo(result.getLanding().getRow(), result.getLanding().getCol());
        if (!freezeEnemies) {
            for (Enemy enemy : enemies) {
                enemy.step(level);
            }
            if (enemyAt(player.getPosition())) {
                return loseLife("An enemy got you");
            }
        }
        return new Outcome(CONTINUE, message);
    }

    public int timeLeft(long nowMillis) {
        long elapsed = (paused ? pausedAt : nowMillis) - timerStart;
        return (int) Math.max(0, level.getTimeSeconds() - elapsed / 1000);
    }

    public void pause(long nowMillis) {
        if (!paused) {
            paused = true;
            pausedAt = nowMillis;
        }
    }

    public void resume(long nowMillis) {
        if (paused) {
            paused = false;
            timerStart += nowMillis - pausedAt;
        }
    }

    public boolean isPaused() {
        return paused;
    }

    public long getCreatedAt() {
        return createdAt;
    }

    public List<Position> collected(Level fresh) {
        List<Position> left = Stream.concat(coins.stream(), gems.stream())
                .map(GameEntity::getPosition).collect(Collectors.toList());
        return Stream.concat(fresh.getCoins().stream(), fresh.getGems().stream())
                .map(GameEntity::getPosition).filter(p -> !left.contains(p)).collect(Collectors.toList());
    }

    public void restore(SaveFile save, long nowMillis) {
        if (!level.inside(save.getRow(), save.getCol()) || level.isBlocked(save.getRow(), save.getCol())) {
            throw new IllegalArgumentException("The saved position (" + save.getRow() + "," + save.getCol()
                    + ") is not a free cell of this level");
        }
        player.moveTo(save.getRow(), save.getCol());
        player.setLives(save.getLives());
        player.setPoints(save.getPoints());
        player.setCoins(save.getCoins());
        coins.removeIf(c -> save.getCollected().contains(c.getPosition()));
        gems.removeIf(g -> save.getCollected().contains(g.getPosition()));
        int timeLeft = Math.min(save.getTimeLeft(), level.getTimeSeconds());
        timerStart = nowMillis - (level.getTimeSeconds() - timeLeft) * 1000L;
    }

    public Level getLevel() {
        return level;
    }

    public Player getPlayer() {
        return player;
    }

    private Outcome loseLife(String why) {
        player.setLives(player.getLives() - 1);
        if (player.getLives() <= 0) {
            return new Outcome(LOST, why + ". Game over!");
        }
        player.moveTo(player.getStartRow(), player.getStartCol());
        for (Enemy enemy : enemies) {
            enemy.reset();
        }
        return new Outcome(LIFE_LOST, why + ". Lives left: " + player.getLives());
    }

    private void addPoints(int points) {
        player.setPoints(player.getPoints() + points);
    }

    private boolean enemyAt(Position cell) {
        return enemies.stream().anyMatch(e -> e.getPosition().equals(cell));
    }

    private static <T extends GameEntity> T removeAt(List<T> entities, Position cell) {
        Iterator<T> it = entities.iterator();
        while (it.hasNext()) {
            T e = it.next();
            if (e.getPosition().equals(cell)) {
                it.remove();
                return e;
            }
        }
        return null;
    }

    public static class Outcome {
        private final int status;
        private final String message;

        Outcome(int status, String message) {
            this.status = status;
            this.message = message;
        }

        public int getStatus() {
            return status;
        }

        public String getMessage() {
            return message;
        }
    }
}
