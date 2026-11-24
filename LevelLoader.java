package pathquest.level;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.StreamTokenizer;
import java.io.StringReader;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import pathquest.model.Coin;
import pathquest.model.Enemy;
import pathquest.model.Gem;
import pathquest.model.Position;

public class LevelLoader {
    private static final int MAX_WIDTH = 200;
    private static final int MAX_HEIGHT = 50;

    public static final String[] DIFFICULTIES = {"EASY", "MEDIUM", "HARD"};
    public static final int LEVELS_PER_DIFFICULTY = 3;

    public static final TreeMap<Integer, String> LEVELS = new TreeMap<>();

    static {
        for (int n = 1; n <= DIFFICULTIES.length * LEVELS_PER_DIFFICULTY; n++) {
            LEVELS.put(n, "levels/level" + n + ".txt");
        }
    }

    public static String difficulty(int levelNo) {
        return levelNo == 0 ? "CUSTOM" : DIFFICULTIES[(levelNo - 1) / LEVELS_PER_DIFFICULTY];
    }

    public static int levelNumber(String path) {
        String file = new File(path).getPath();
        return LEVELS.entrySet().stream()
                .filter(e -> new File(e.getValue()).getPath().equals(file))
                .map(Map.Entry::getKey).findFirst().orElse(0);
    }

    public static String copyAsCustom(String source, String dir) throws IOException {
        String name = new File(source).getName().replaceFirst("\\.txt$", "").replaceAll("[^A-Za-z0-9_-]", "_");
        File target = new File(dir, "custom_" + name + ".txt");
        if (target.exists()) {
            throw new IOException(target.getPath() + " already exists; rename the file you import");
        }
        try (FileReader in = new FileReader(source);
             FileWriter out = new FileWriter(target)) {
            char[] buffer = new char[1024];
            int n;
            while ((n = in.read(buffer)) != -1) {
                out.write(buffer, 0, n);
            }
        }
        return target.getPath();
    }

    public static Level load(String path) throws InvalidLevelException, IOException {
        try (BufferedReader in = new BufferedReader(new FileReader(path))) {
            return read(in);
        }
    }

    public static Level read(BufferedReader in) throws InvalidLevelException, IOException {
        String first = in.readLine();
        if (first == null) {
            throw new InvalidLevelException("Row 1: the settings line is missing");
        }
        Map<String, Integer> settings = readSettings(first);
        int width = settings.get("WIDTH");
        int height = settings.get("HEIGHT");

        char[][] grid = new char[height][width];
        Position start = null;
        Position flag = null;
        List<Coin> coins = new ArrayList<>();
        List<Gem> gems = new ArrayList<>();
        List<Enemy> enemies = new ArrayList<>();
        for (int r = 0; r < height; r++) {
            int fileRow = r + 2;
            String line = in.readLine();
            if (line == null) {
                throw new InvalidLevelException("Row " + fileRow + ": HEIGHT is " + height + " but the file has only " + r + " grid rows");
            }
            if (line.length() > width) {
                throw new InvalidLevelException(fileRow, width + 1, "row is longer than WIDTH " + width);
            }
            for (int c = 0; c < width; c++) {
                char ch = c < line.length() ? line.charAt(c) : ' ';
                grid[r][c] = ch == '#' ? '#' : ' ';
                switch (ch) {
                    case ' ':
                    case '#':
                        break;
                    case '@':
                        if (start != null) {
                            throw new InvalidLevelException(fileRow, c + 1, "a second start @ (only one allowed)");
                        }
                        start = new Position(r, c);
                        break;
                    case 'F':
                        if (flag != null) {
                            throw new InvalidLevelException(fileRow, c + 1, "a second flag F (only one allowed)");
                        }
                        flag = new Position(r, c);
                        break;
                    case 'E':
                        enemies.add(new Enemy(r, c));
                        break;
                    case 'o':
                        coins.add(new Coin(r, c));
                        break;
                    case '$':
                        gems.add(new Gem(r, c));
                        break;
                    default:
                        throw new InvalidLevelException(fileRow, c + 1, "unknown symbol '" + ch + "'");
                }
            }
        }
        String rows = "Rows 2-" + (height + 1) + ": ";
        if (start == null) {
            throw new InvalidLevelException(rows + "there is no start @");
        }
        if (flag == null) {
            throw new InvalidLevelException(rows + "there is no flag F");
        }
        String extra;
        for (int fileRow = height + 2; (extra = in.readLine()) != null; fileRow++) {
            if (!extra.isBlank()) {
                throw new InvalidLevelException("Row " + fileRow + ": more grid rows than HEIGHT " + height);
            }
        }
        return new Level(width, height, settings.get("TIME"), settings.get("LIVES"), grid, start, flag, coins, gems, enemies);
    }

    private static Map<String, Integer> readSettings(String line) throws IOException, InvalidLevelException {
        StreamTokenizer st = new StreamTokenizer(new StringReader(line));
        Map<String, Integer> settings = new HashMap<>();
        while (st.nextToken() != StreamTokenizer.TT_EOF) {
            if (st.ttype != StreamTokenizer.TT_WORD) {
                throw new InvalidLevelException("Row 1: expected a setting name such as WIDTH");
            }
            String name = st.sval;
            if (st.nextToken() != StreamTokenizer.TT_NUMBER || st.nval < 1 || st.nval != (int) st.nval) {
                throw new InvalidLevelException("Row 1: " + name + " must be followed by a whole number above 0");
            }
            settings.put(name, (int) st.nval);
        }
        for (String name : List.of("WIDTH", "HEIGHT", "TIME", "LIVES")) {
            if (!settings.containsKey(name)) {
                throw new InvalidLevelException("Row 1: " + name + " is missing");
            }
        }
        if (settings.get("WIDTH") > MAX_WIDTH || settings.get("HEIGHT") > MAX_HEIGHT) {
            throw new InvalidLevelException("Row 1: a level can be at most WIDTH " + MAX_WIDTH + " and HEIGHT " + MAX_HEIGHT);
        }
        return settings;
    }
}
