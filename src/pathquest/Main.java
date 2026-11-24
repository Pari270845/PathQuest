package pathquest;

import java.io.File;
import java.io.IOException;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.Scanner;
import java.util.stream.Collectors;
import pathquest.engine.BoundedBuffer;
import pathquest.engine.GameLoop;
import pathquest.engine.GameState;
import pathquest.engine.InputReader;
import pathquest.engine.ReplayPlayer;
import pathquest.io.BackupManager;
import pathquest.io.CsvExporter;
import pathquest.io.ReplayFile;
import pathquest.io.ReportWriter;
import pathquest.io.SaveFile;
import pathquest.io.Settings;
import pathquest.level.InvalidLevelException;
import pathquest.level.Level;
import pathquest.level.LevelLoader;
import pathquest.model.Command;
import pathquest.model.Player;
import pathquest.model.Score;
import pathquest.report.StatsService;
import pathquest.store.AccountLockedException;
import pathquest.store.PlayerStore;
import pathquest.store.ScoreStore;

public class Main {
    private static final PlayerStore PLAYERS = new PlayerStore(PlayerStore.FILE);
    private static final ScoreStore SCORES = new ScoreStore(ScoreStore.FILE);
    private static final String STATS_REPORT = "reports/statistics.txt";

    public static void main(String[] args) {
        String extraLevel = args.length > 0 ? args[0] : null;
        Scanner in = new Scanner(System.in);
        while (true) {
            System.out.println();
            System.out.println("=== PathQuest ===");
            System.out.println("1 Register   2 Login   0 Exit");
            String choice = choose(in);
            if (choice == null || choice.equals("0")) {
                return;
            }
            try {
                switch (choice) {
                    case "1":
                        Player player = PLAYERS.register(ask(in, "New username (3-20 letters or digits): "), ask(in, "New PIN (4 digits): "));
                        System.out.println("Registered " + player.getUsername() + ". You can log in now.");
                        break;
                    case "2":
                        String username = ask(in, "Username: ");
                        Optional<Player> account = PLAYERS.login(username, ask(in, "PIN: "));
                        if (account.isPresent()) {
                            accountMenu(in, account.get(), extraLevel);
                        } else {
                            System.out.println("Wrong username or PIN. Tries left: " + PLAYERS.triesLeft(username));
                        }
                        break;
                    default:
                        System.out.println("Choose 1, 2 or 0");
                }
            } catch (IOException | IllegalArgumentException | AccountLockedException e) {
                System.out.println(readable(e));
            }
        }
    }

    private static void accountMenu(Scanner in, Player account, String extraLevel) {
        Settings settings = loadSettings(account);
        while (true) {
            SaveFile save = savedGameOf(account);
            System.out.println();
            System.out.println("=== Logged in as " + account.getUsername() + " ===");
            System.out.println("1 Play a level   2 Change PIN   3 Delete my account   4 Logout");
            System.out.println("5 Leaderboard    6 My history   7 Files and exports   8 Statistics");
            System.out.println("9 My profile     10 Settings    11 Watch my last replay");
            if (save != null) {
                System.out.println("12 Continue saved game");
            }
            String choice = choose(in);
            if (choice == null || choice.equals("4")) {
                return;
            }
            try {
                switch (choice) {
                    case "1":
                        String path = chooseLevel(in, extraLevel);
                        if (path != null) {
                            play(in, account, path, null, settings);
                        }
                        break;
                    case "12":
                        if (save == null) {
                            System.out.println("No saved game for " + account.getUsername());
                        } else {
                            play(in, account, save.getFile(), save, settings);
                        }
                        break;
                    case "9":
                        System.out.println("=== Profile of " + account.getUsername() + " ===");
                        System.out.print(StatsService.profile(SCORES.findAll(), account.getUsername()));
                        break;
                    case "10":
                        settingsMenu(in, settings);
                        break;
                    case "11":
                        watchReplay(account, settings);
                        break;
                    case "7":
                        filesMenu(in);
                        break;
                    case "8":
                        String stats = StatsService.statistics(SCORES.findAll());
                        System.out.print(stats);
                        ReportWriter.write(STATS_REPORT, stats);
                        System.out.println("Saved to " + STATS_REPORT);
                        break;
                    case "2":
                        PLAYERS.updatePin(account, ask(in, "New PIN (4 digits): "));
                        System.out.println("PIN changed");
                        break;
                    case "3":
                        if (ask(in, "Delete " + account.getUsername() + " and all its scores? Type yes to confirm: ").equals("yes")) {
                            PLAYERS.delete(account.getPlayerId());
                            SCORES.deleteAllOf(account.getUsername());
                            new File(Settings.fileFor(account.getUsername())).delete();
                            new File(ReplayFile.fileFor(account.getUsername())).delete();
                            System.out.println("Account deleted");
                            return;
                        }
                        System.out.println("Nothing deleted");
                        break;
                    case "5":
                        String[] names = LevelLoader.DIFFICULTIES;
                        String which = ask(in, "Leaderboard for 0 ALL   1 " + names[0] + "   2 " + names[1] + "   3 " + names[2] + ": ");
                        List<Score> scores = SCORES.findAll();
                        String title = "ALL LEVELS";
                        for (int d = 0; d < names.length; d++) {
                            if (which.equals(String.valueOf(d + 1))) {
                                scores = StatsService.ofDifficulty(scores, names[d]);
                                title = names[d];
                            }
                        }
                        System.out.println("=== Top 10: " + title + " ===");
                        System.out.print(StatsService.leaderboard(scores));
                        break;
                    case "6":
                        historyScreen(in, account);
                        break;
                    default:
                        System.out.println("Choose a number from the menu");
                }
            } catch (IOException | IllegalArgumentException e) {
                System.out.println(readable(e));
            }
        }
    }

    private static Settings loadSettings(Player account) {
        List<String> problems = new ArrayList<>();
        try {
            Settings settings = Settings.load(Settings.fileFor(account.getUsername()), problems);
            problems.forEach(System.out::println);
            return settings;
        } catch (IOException e) {
            System.out.println("Settings not readable, using the defaults: " + e.getMessage());
            return new Settings(Settings.fileFor(account.getUsername()));
        }
    }

    private static void settingsMenu(Scanner in, Settings settings) throws IOException {
        while (true) {
            System.out.println();
            System.out.println("=== Settings (type a number to switch it ON or OFF) ===");
            List<String> names = settings.names();
            for (int i = 0; i < names.size(); i++) {
                System.out.println((i + 1) + " " + names.get(i) + "   " + (settings.isOn(names.get(i)) ? "ON" : "OFF"));
            }
            System.out.println("0 Back");
            String choice = choose(in);
            if (choice == null || choice.equals("0")) {
                return;
            }
            try {
                int n = Integer.parseInt(choice);
                if (n < 1 || n > names.size()) {
                    throw new NumberFormatException();
                }
                settings.toggle(names.get(n - 1));
                settings.save();
            } catch (NumberFormatException e) {
                System.out.println("Choose a number from 0 to " + names.size());
            }
        }
    }

    private static void watchReplay(Player account, Settings settings) throws IOException {
        String file = ReplayFile.fileFor(account.getUsername());
        if (!new File(file).exists()) {
            System.out.println("No replay yet: play a level first (a continued game is not recorded)");
            return;
        }
        List<String> lines = ReplayFile.read(file);
        String levelPath = lines.get(0).substring("LEVEL ".length());
        GameState game = new GameState(loadLevel(levelPath), 0);
        ReplayPlayer replay = new ReplayPlayer(game, LevelLoader.levelNumber(levelPath), account.getUsername(),
                lines.subList(1, lines.size()), settings.isOn(Settings.CLEAR_SCREEN), settings.isOn(Settings.SHOW_CONTROLS), 400);
        Thread thread = new Thread(replay, "Replay");
        thread.start();
        try {
            thread.join();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
        System.out.println("Replay finished: " + replay.getResult());
    }

    private static SaveFile savedGameOf(Player account) {
        if (!new File(SaveFile.PATH).exists()) {
            return null;
        }
        try {
            SaveFile save = SaveFile.read(SaveFile.PATH);
            return save.getUser().equals(account.getUsername()) ? save : null;
        } catch (IOException e) {
            System.out.println("Saved game not usable: " + e.getMessage());
            return null;
        }
    }

    private static String chooseLevel(Scanner in, String extraLevel) {
        List<String> others = new ArrayList<>();
        File[] customs = new File("levels").listFiles((dir, name) -> name.startsWith("custom_") && name.endsWith(".txt"));
        if (customs != null) {
            Arrays.stream(customs).map(File::getPath).sorted().forEach(others::add);
        }
        if (extraLevel != null && LevelLoader.levelNumber(extraLevel) == 0 && !others.contains(extraLevel)) {
            others.add(extraLevel);
        }
        String[] names = LevelLoader.DIFFICULTIES;
        for (int d = 0; d < names.length; d++) {
            System.out.print((d + 1) + " " + names[d] + "   ");
        }
        System.out.println(others.isEmpty() ? "" : (names.length + 1) + " OTHER FILES");
        String difficulty = ask(in, "Difficulty: ");
        List<String> files;
        if (difficulty.equals(String.valueOf(names.length + 1)) && !others.isEmpty()) {
            files = others;
        } else {
            files = LevelLoader.LEVELS.entrySet().stream()
                    .filter(e -> String.valueOf((e.getKey() - 1) / LevelLoader.LEVELS_PER_DIFFICULTY + 1).equals(difficulty))
                    .map(e -> e.getValue()).collect(Collectors.toList());
        }
        if (files.isEmpty()) {
            System.out.println("No difficulty " + difficulty);
            return null;
        }
        for (int i = 0; i < files.size(); i++) {
            int levelNo = LevelLoader.levelNumber(files.get(i));
            System.out.println((i + 1) + " " + (levelNo > 0 ? "Level " + levelNo : files.get(i)));
        }
        String choice = ask(in, "Level: ");
        for (int i = 0; i < files.size(); i++) {
            if (choice.equals(String.valueOf(i + 1))) {
                return files.get(i);
            }
        }
        System.out.println("No level " + choice);
        return null;
    }

    static String importLevel(String source, String dir) throws IOException {
        Level level = loadLevel(source);
        String target = LevelLoader.copyAsCustom(source, dir);
        return "Valid level: " + level.getWidth() + " x " + level.getHeight() + ", " + level.getCoins().size() + " coins, "
                + level.getGems().size() + " gems, " + level.getEnemies().size() + " enemies\nImported as " + target;
    }

    private static void filesMenu(Scanner in) {
        while (true) {
            System.out.println();
            System.out.println("=== Files and exports ===");
            System.out.println("1 Export CSV   2 View exported CSV   3 Back up save file   4 Restore save file");
            System.out.println("5 Import a level file   0 Back");
            String choice = choose(in);
            if (choice == null || choice.equals("0")) {
                return;
            }
            try {
                switch (choice) {
                    case "1":
                        int top = CsvExporter.exportLeaderboard(CsvExporter.LEADERBOARD);
                        System.out.println("Wrote " + top + " rows to " + CsvExporter.LEADERBOARD);
                        break;
                    case "2":
                        List<String> skipped = new ArrayList<>();
                        List<String[]> rows = CsvExporter.readBack(CsvExporter.LEADERBOARD, skipped);
                        System.out.println("--- " + CsvExporter.LEADERBOARD + " (" + (rows.size() - 1) + " rows)");
                        for (String[] row : rows) {
                            System.out.println(String.join(" | ", row));
                        }
                        skipped.forEach(System.out::println);
                        break;
                    case "3":
                        System.out.println("Copied " + BackupManager.backup() + " bytes to " + BackupManager.BACKUP_PATH);
                        break;
                    case "4":
                        System.out.println("Copied " + BackupManager.restore() + " bytes back to " + SaveFile.PATH);
                        break;
                    case "5":
                        System.out.println(importLevel(ask(in, "Path of the level file to import: "), "levels"));
                        break;
                    default:
                        System.out.println("Choose a number from 0 to 5");
                }
            } catch (IOException | IllegalArgumentException e) {
                System.out.println(readable(e));
            }
        }
    }

    private static void historyScreen(Scanner in, Player account) throws IOException {
        List<Score> all = SCORES.findAll();
        List<Integer> levels = StatsService.levelsPlayed(StatsService.history(all, account.getUsername(), false, false));
        boolean wonOnly = false;
        boolean byTimeLeft = false;
        int page = 0;
        while (true) {
            List<Score> mine = StatsService.history(all, account.getUsername(), wonOnly, byTimeLeft);
            int pages = StatsService.pageCount(mine);
            page = Math.min(page, pages - 1);
            System.out.println();
            System.out.println("=== My history (" + (wonOnly ? "won only" : "all games") + ", "
                    + (byTimeLeft ? "most time left first" : "newest first") + ") ===   Levels played: " + levels);
            System.out.print(StatsService.historyTable(StatsService.page(mine, page)));
            System.out.println("Page " + (page + 1) + " of " + pages
                    + "   n next   p previous   w won only on/off   t sort by time left on/off   q quit");
            String choice = choose(in);
            if (choice == null || choice.equals("q")) {
                return;
            }
            switch (choice) {
                case "n":
                    page = Math.min(page + 1, pages - 1);
                    break;
                case "p":
                    page = Math.max(page - 1, 0);
                    break;
                case "w":
                    wonOnly = !wonOnly;
                    page = 0;
                    break;
                case "t":
                    byTimeLeft = !byTimeLeft;
                    page = 0;
                    break;
                default:
                    System.out.println("Choose n, p, w, t or q");
            }
        }
    }

    private static void play(Scanner in, Player account, String levelPath, SaveFile save, Settings settings) throws IOException {
        int levelNo = LevelLoader.levelNumber(levelPath);
        GameState game = new GameState(loadLevel(levelPath), System.currentTimeMillis());
        if (save != null) {
            game.restore(save, System.currentTimeMillis());
        }
        BoundedBuffer<Command> buffer = new BoundedBuffer<>(5);
        Thread reader = new Thread(new InputReader(in, buffer), "InputReader");
        GameLoop loop = new GameLoop(game, levelNo, account.getUsername(), settings, buffer, reader);
        Thread gameThread = new Thread(loop, "GameLoop");
        reader.start();
        gameThread.start();
        try {
            gameThread.join();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            return;
        }
        Player player = loop.getPlayer();
        System.out.println("Result: " + loop.getResult() + "   Points: " + player.getPoints() + "   Coins: " + player.getCoins()
                + "   Time left: " + loop.getTimeLeft() + "   Lives: " + player.getLives());
        if (loop.getResult().equals("exit")) {
            new SaveFile(account.getUsername(), levelNo, player.getRow(), player.getCol(), player.getLives(), player.getPoints(),
                    player.getCoins(), loop.getTimeLeft(), game.collected(loadLevel(levelPath)), levelPath).write(SaveFile.PATH);
            System.out.println("Game saved to " + SaveFile.PATH);
        } else {
            SCORES.save(new Score(account.getUsername(), levelNo, player.getPoints(), player.getCoins(), loop.getTimeLeft(),
                    loop.getResult().equals("won"), LocalDateTime.now()));
            System.out.println("Score saved");
        }
        if (save == null && !loop.getMoves().isEmpty()) {
            ReplayFile.write(ReplayFile.fileFor(account.getUsername()), levelPath, loop.getMoves());
            System.out.println("Replay saved: choose 11 to watch it");
        }
    }

    private static Level loadLevel(String path) throws IOException {
        try {
            return LevelLoader.load(path);
        } catch (InvalidLevelException e) {
            throw new IOException(e.getMessage(), e);
        }
    }

    static String readable(Exception e) {
        return e.getMessage();
    }

    private static String choose(Scanner in) {
        System.out.print("Choose: ");
        return in.hasNextLine() ? in.nextLine().trim() : null;
    }

    private static String ask(Scanner in, String prompt) {
        System.out.print(prompt);
        return in.hasNextLine() ? in.nextLine().trim() : "";
    }
}
