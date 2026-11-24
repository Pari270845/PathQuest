package pathquest.engine;

import java.util.ArrayList;
import java.util.List;
import pathquest.io.Settings;
import pathquest.model.Command;
import pathquest.model.Player;

public class GameLoop implements Runnable {
    private final GameState game;
    private final int levelNo;
    private final String playerName;
    private final Settings settings;
    private final BoundedBuffer<Command> buffer;
    private final Thread reader;
    private final List<String> moves = new ArrayList<>();
    private boolean paused;
    private boolean trace;
    private String result;
    private int timeLeft;

    public GameLoop(GameState game, int levelNo, String playerName, Settings settings, BoundedBuffer<Command> buffer, Thread reader) {
        this.game = game;
        this.levelNo = levelNo;
        this.playerName = playerName;
        this.settings = settings;
        this.buffer = buffer;
        this.reader = reader;
    }

    @Override
    public void run() {
        render("Type a move and press Enter");
        while (result == null) {
            System.out.print("> ");
            System.out.flush();
            Command cmd = buffer.take();
            if (cmd == null || cmd.getName().equals(Command.EXIT)) {
                result = "exit";
            } else {
                handle(cmd);
                if (trace && !delay(1000)) {
                    result = "exit";
                }
            }
        }
        timeLeft = game.timeLeft(System.currentTimeMillis());
        buffer.close();
        if (!result.equals("exit")) {
            System.out.println(result.equals("won") ? "*** YOU WIN! ***" : "*** GAME OVER ***");
            System.out.println("Press Enter to continue");
            try {
                reader.join();
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }
    }

    private void handle(Command cmd) {
        String name = cmd.getName();
        long now = System.currentTimeMillis();
        if (name.equals(Command.PAUSE)) {
            record(now, name);
            paused = !paused;
            if (paused) {
                game.pause(now);
            } else {
                game.resume(now);
            }
            render(paused ? "Paused. Type p to resume or x to exit" : "Resumed");
        } else if (paused) {
            render("Paused. Only p and x work now");
        } else if (name.equals(Command.DEBUG)) {
            trace = !trace;
            buffer.setTrace(trace);
            render(trace ? "Buffer trace on, 1 s delay after each command" : "Buffer trace off");
        } else {
            record(now, name);
            GameState.Outcome out = game.step(name, now, false);
            render(out.getMessage());
            if (settings.isOn(Settings.BEEP) && (out.getStatus() == GameState.LIFE_LOST || out.getStatus() == GameState.LOST)) {
                System.out.print('\007');
            }
            if (out.getStatus() == GameState.WON) {
                result = "won";
            } else if (out.getStatus() == GameState.LOST) {
                result = "lost";
            }
        }
    }

    private void render(String status) {
        if (!trace && settings.isOn(Settings.CLEAR_SCREEN)) {
            Renderer.clear();
        }
        System.out.print(Renderer.frame(game.getLevel(), game.getPlayer(), playerName,
                game.timeLeft(System.currentTimeMillis()), levelNo, settings.isOn(Settings.SHOW_CONTROLS)));
        System.out.println(status);
    }

    private void record(long now, String move) {
        moves.add((now - game.getCreatedAt()) + " " + move);
    }

    public List<String> getMoves() {
        return moves;
    }

    private static boolean delay(long millis) {
        try {
            Thread.sleep(millis);
            return true;
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            return false;
        }
    }

    public String getResult() {
        return result;
    }

    public int getTimeLeft() {
        return timeLeft;
    }

    public Player getPlayer() {
        return game.getPlayer();
    }
}
