package pathquest.io;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;
import pathquest.model.Position;

public class SaveFile {
    public static final String PATH = "saves/save.txt";

    private final String user;
    private final int levelNo;
    private final int row;
    private final int col;
    private final int lives;
    private final int points;
    private final int coins;
    private final int timeLeft;
    private final List<Position> collected;
    private final String file;

    public SaveFile(String user, int levelNo, int row, int col, int lives, int points, int coins, int timeLeft,
                    List<Position> collected, String file) {
        this.user = user;
        this.levelNo = levelNo;
        this.row = row;
        this.col = col;
        this.lives = lives;
        this.points = points;
        this.coins = coins;
        this.timeLeft = timeLeft;
        this.collected = collected;
        this.file = file;
    }

    public void write(String path) throws IOException {
        File parent = new File(path).getParentFile();
        if (parent != null) {
            parent.mkdirs();
        }
        String items = collected.isEmpty() ? "COLLECTED" : "COLLECTED " + collectedText(collected);
        try (BufferedWriter out = new BufferedWriter(new FileWriter(path))) {
            for (String line : List.of("USER " + user, "LEVEL " + levelNo, "PLAYER " + row + " " + col, "LIVES " + lives,
                    "POINTS " + points, "COINS " + coins, "TIMELEFT " + timeLeft, items, "FILE " + file)) {
                out.write(line);
                out.newLine();
            }
        }
    }

    public static SaveFile read(String path) throws IOException {
        try (BufferedReader in = new BufferedReader(new FileReader(path))) {
            String user = fields(in, 1, "USER", 1)[1];
            int levelNo = number(fields(in, 2, "LEVEL", 1)[1], 2, "LEVEL", 0);
            String[] player = fields(in, 3, "PLAYER", 2);
            int row = number(player[1], 3, "PLAYER row", 0);
            int col = number(player[2], 3, "PLAYER column", 0);
            int lives = number(fields(in, 4, "LIVES", 1)[1], 4, "LIVES", 1);
            int points = number(fields(in, 5, "POINTS", 1)[1], 5, "POINTS", 0);
            int coins = number(fields(in, 6, "COINS", 1)[1], 6, "COINS", 0);
            int timeLeft = number(fields(in, 7, "TIMELEFT", 1)[1], 7, "TIMELEFT", 0);
            String[] items = fields(in, 8, "COLLECTED", -1);
            List<Position> collected;
            try {
                collected = parseCollected(String.join(" ", Arrays.copyOfRange(items, 1, items.length)));
            } catch (IOException e) {
                throw new IOException("Save file line 8: " + e.getMessage());
            }
            String fileLine = in.readLine();
            if (fileLine == null || !fileLine.startsWith("FILE ") || fileLine.substring(5).isBlank()) {
                throw new IOException("Save file line 9: expected FILE followed by the level file");
            }
            String extra;
            for (int lineNo = 10; (extra = in.readLine()) != null; lineNo++) {
                if (!extra.isBlank()) {
                    throw new IOException("Save file line " + lineNo + ": unexpected extra line");
                }
            }
            return new SaveFile(user, levelNo, row, col, lives, points, coins, timeLeft, collected, fileLine.substring(5).trim());
        }
    }

    public static String collectedText(List<Position> cells) {
        return cells.stream().map(p -> p.getRow() + "," + p.getCol()).collect(Collectors.joining(" "));
    }

    public static List<Position> parseCollected(String text) throws IOException {
        List<Position> cells = new ArrayList<>();
        for (String item : text.trim().split("\\s+")) {
            if (item.isEmpty()) {
                continue;
            }
            String[] rc = item.split(",");
            int row = -1;
            int col = -1;
            if (rc.length == 2) {
                try {
                    row = Integer.parseInt(rc[0]);
                    col = Integer.parseInt(rc[1]);
                } catch (NumberFormatException e) {
                }
            }
            if (row < 0 || col < 0) {
                throw new IOException("'" + item + "' is not a row,column pair");
            }
            cells.add(new Position(row, col));
        }
        return cells;
    }

    public boolean sameGameAs(SaveFile other) {
        return user.equals(other.user) && levelNo == other.levelNo && row == other.row && col == other.col
                && lives == other.lives && points == other.points && coins == other.coins && timeLeft == other.timeLeft
                && collected.equals(other.collected);
    }

    private static String[] fields(BufferedReader in, int lineNo, String key, int count) throws IOException {
        String line = in.readLine();
        if (line == null) {
            throw new IOException("Save file line " + lineNo + ": " + key + " is missing");
        }
        String[] parts = line.trim().split("\\s+");
        if (!parts[0].equals(key) || (count >= 0 && parts.length != count + 1)) {
            throw new IOException("Save file line " + lineNo + ": expected " + key
                    + (count >= 0 ? " followed by " + count + " value(s)" : ""));
        }
        return parts;
    }

    private static int number(String text, int lineNo, String name, int min) throws IOException {
        try {
            int n = Integer.parseInt(text);
            if (n >= min) {
                return n;
            }
        } catch (NumberFormatException e) {
        }
        throw new IOException("Save file line " + lineNo + ": " + name + " must be a whole number of at least " + min
                + ", found '" + text + "'");
    }

    public String getUser() {
        return user;
    }

    public int getLevelNo() {
        return levelNo;
    }

    public int getRow() {
        return row;
    }

    public int getCol() {
        return col;
    }

    public int getLives() {
        return lives;
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

    public List<Position> getCollected() {
        return collected;
    }

    public String getFile() {
        return file;
    }
}
