package pathquest.engine;

import java.util.List;
import pathquest.model.Command;

public class ReplayPlayer implements Runnable {
    private final GameState game;
    private final int levelNo;
    private final String playerName;
    private final List<String> moves;
    private final boolean clearScreen;
    private final boolean showControls;
    private final long delayMillis;
    private String result = "stopped";
    private long lastAt;

    public ReplayPlayer(GameState game, int levelNo, String playerName, List<String> moves,
                        boolean clearScreen, boolean showControls, long delayMillis) {
        this.game = game;
        this.levelNo = levelNo;
        this.playerName = playerName;
        this.moves = moves;
        this.clearScreen = clearScreen;
        this.showControls = showControls;
        this.delayMillis = delayMillis;
    }

    @Override
    public void run() {
        render("Replay of " + playerName + "'s last game");
        for (int i = 0; i < moves.size(); i++) {
            try {
                Thread.sleep(delayMillis);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                return;
            }
            String[] parts = moves.get(i).split(" ");
            long at = Long.parseLong(parts[0]);
            lastAt = at;
            String move = parts[1];
            String status;
            if (move.equals(Command.PAUSE)) {
                if (game.isPaused()) {
                    game.resume(at);
                } else {
                    game.pause(at);
                }
                status = game.isPaused() ? "paused" : "resumed";
            } else {
                GameState.Outcome out = game.step(move, at, false);
                status = out.getMessage();
                if (out.getStatus() == GameState.WON) {
                    result = "won";
                } else if (out.getStatus() == GameState.LOST) {
                    result = "lost";
                }
            }
            render("Replay move " + (i + 1) + " of " + moves.size() + ": " + move + "   " + status);
            if (!result.equals("stopped")) {
                return;
            }
        }
        result = "end of recording";
    }

    private void render(String status) {
        if (clearScreen) {
            Renderer.clear();
        }
        System.out.print(Renderer.frame(game.getLevel(), game.getPlayer(), playerName, game.timeLeft(lastAt), levelNo, showControls));
        System.out.println(status);
    }

    public String getResult() {
        return result;
    }
}
