package pathquest.engine;

import java.util.List;
import pathquest.level.Level;
import pathquest.level.LevelLoader;
import pathquest.model.GameEntity;
import pathquest.model.Player;

public class Renderer {
    private static final String CONTROLS =
            "a/d = move   w = jump   wd/wa = jump right/left   p = pause   x = save and exit";

    public static void clear() {
        System.out.print("\033[H\033[2J");
        System.out.flush();
    }

    public static String frame(Level level, Player player, String playerName, int timeLeft, int levelNo, boolean showControls) {
        char[][] canvas = new char[level.getHeight()][];
        for (int r = 0; r < canvas.length; r++) {
            canvas[r] = level.getGrid()[r].clone();
        }
        draw(level, canvas, level.getCoins());
        draw(level, canvas, level.getGems());
        draw(level, canvas, level.getEnemies());
        canvas[level.getFlag().getRow()][level.getFlag().getCol()] = 'F';
        draw(level, canvas, List.of(player));

        StringBuilder sb = new StringBuilder();
        String levelText = levelNo == 0 ? "CUSTOM LEVEL" : "LEVEL " + levelNo + " (" + LevelLoader.difficulty(levelNo) + ")";
        sb.append("PLAYER: ").append(playerName).append("   ").append(levelText).append('\n');
        sb.append(String.format("SCORE: %d   COINS: %d   LIVES: %d   TIME: %03d",
                player.getPoints(), player.getCoins(), player.getLives(), timeLeft)).append('\n');
        sb.append("-".repeat(level.getWidth())).append('\n');
        for (char[] row : canvas) {
            sb.append(row).append('\n');
        }
        if (showControls) {
            sb.append(CONTROLS).append('\n');
        }
        return sb.toString();
    }

    private static void draw(Level level, char[][] canvas, List<? extends GameEntity> entities) {
        for (GameEntity e : entities) {
            if (level.inside(e.getRow(), e.getCol())) {
                canvas[e.getRow()][e.getCol()] = e.symbol();
            }
        }
    }
}
