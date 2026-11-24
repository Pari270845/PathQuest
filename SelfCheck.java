package pathquest;

import java.io.BufferedReader;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintStream;
import java.io.StringReader;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.DoubleSummaryStatistics;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import pathquest.engine.BoundedBuffer;
import pathquest.engine.GameState;
import pathquest.engine.InvalidMoveException;
import pathquest.engine.MoveRules;
import pathquest.engine.Renderer;
import pathquest.engine.ReplayPlayer;
import pathquest.io.BackupManager;
import pathquest.io.CsvExporter;
import pathquest.io.ReplayFile;
import pathquest.io.SaveFile;
import pathquest.io.Settings;
import pathquest.level.InvalidLevelException;
import pathquest.level.Level;
import pathquest.level.LevelLoader;
import pathquest.model.Coin;
import pathquest.model.Command;
import pathquest.model.Enemy;
import pathquest.model.GameEntity;
import pathquest.model.Gem;
import pathquest.model.Player;
import pathquest.model.Position;
import pathquest.model.Score;
import pathquest.report.StatsService;
import pathquest.store.AccountLockedException;
import pathquest.store.PlayerStore;
import pathquest.store.ScoreStore;

public class SelfCheck {
    public static void main(String[] args) throws IOException, InvalidLevelException, InterruptedException {
        checkPosition();
        checkCommand();
        checkScore();
        checkEntityCount();
        checkLevels();
        checkRenderer();
        checkMoveRules();
        checkGameRules();
        checkEnemies();
        checkTimer();
        checkBuffer();
        checkStores();
        checkStats();
        checkFiles();
        checkAllLevels();
        checkImport();
        checkAnalytics();
        checkSettings();
        checkReplay();
        System.out.println("all checks passed");
    }

    static void checkAnalytics() {
        LocalDateTime t = LocalDateTime.of(2026, 11, 3, 9, 0);
        List<Score> scores = List.of(new Score("ann", 1, 100, 1, 10, true, t), new Score("bob", 1, 200, 2, 20, true, t),
                new Score("ann", 5, 300, 3, 30, true, t), new Score("cid", 1, 0, 0, 0, false, t), new Score("bob", 8, 50, 1, 5, false, t));
        assert StatsService.pointsByDifficulty(scores).equals(Map.of("EASY", 100.0, "MEDIUM", 300.0, "HARD", 50.0))
                : "average points per difficulty";
        assert StatsService.outcomes(scores).equals(Map.of(true, 3L, false, 2L)) : "3 wins, 2 losses";
        DoubleSummaryStatistics points = StatsService.scoreSummary(scores);
        assert points.getCount() == 5 && points.getMin() == 0 && points.getMax() == 300 && points.getAverage() == 130 && points.getSum() == 650;
        assert StatsService.players(scores).equals("ann, bob, cid");
        assert StatsService.statistics(List.of()).contains("No scores yet") : "an empty list gives a readable screen";

        assert StatsService.ofDifficulty(scores, "EASY").size() == 3 && StatsService.ofDifficulty(scores, "HARD").size() == 1;
        String ann = StatsService.profile(scores, "ann");
        assert ann.contains("Games played   2") && ann.contains("Wins           2 (100%)") && ann.contains("Best score     300 on level 5")
                && ann.contains("Coins in total 4") && ann.contains("Levels won     [1, 5]") && ann.contains("{EASY=1, MEDIUM=1}") : ann;
        assert StatsService.profile(scores, "bob").contains("Wins           1 (50%)");
        assert StatsService.profile(scores, "nobody").startsWith("No games yet") : "no games: max() gives an empty Optional";
    }

    static void checkAllLevels() throws IOException, InvalidLevelException {
        assert LevelLoader.LEVELS.size() == 9 : "3 levels for each of the 3 difficulties";
        int[] enemies = new int[LevelLoader.LEVELS.size() + 1];
        for (Map.Entry<Integer, String> entry : LevelLoader.LEVELS.entrySet()) {
            Level level = LevelLoader.load(entry.getValue());
            enemies[entry.getKey()] = level.getEnemies().size();
            assert LevelLoader.levelNumber(entry.getValue()) == entry.getKey();
        }
        assert LevelLoader.difficulty(1).equals("EASY") && LevelLoader.difficulty(3).equals("EASY");
        assert LevelLoader.difficulty(4).equals("MEDIUM") && LevelLoader.difficulty(6).equals("MEDIUM");
        assert LevelLoader.difficulty(7).equals("HARD") && LevelLoader.difficulty(9).equals("HARD");
        assert LevelLoader.difficulty(0).equals("CUSTOM");
        assert enemies[3] <= enemies[4] && enemies[6] <= enemies[7] : "each difficulty has at least as many enemies as the one before";
        assert LevelLoader.levelNumber("levels/blocked.txt") == 0 && LevelLoader.levelNumber("levels\\level2.txt") == 2;
    }

    static void checkImport() throws IOException {
        File dir = temp();
        assert dir.delete() && dir.mkdir();
        try {
            Main.importLevel("levels/bad_row_too_long.txt", dir.getPath());
            assert false : "a broken level must be refused";
        } catch (IOException e) {
            assert e.getMessage().contains("Row") : "the refusal names the row";
        }
        assert dir.list().length == 0 : "a refused file is not copied";

        String report = Main.importLevel("levels/blocked.txt", dir.getPath());
        File copy = new File(dir, "custom_blocked.txt");
        copy.deleteOnExit();
        assert report.startsWith("Valid level: 12 x 5") : "the import reports the level's size";
        assert sameBytes(new File("levels/blocked.txt"), copy) : "the imported copy is identical to the source";
        try {
            Main.importLevel("levels/blocked.txt", dir.getPath());
            assert false : "an existing custom file must not be overwritten";
        } catch (IOException e) {
            assert e.getMessage().contains("already exists");
        }

        report = Main.importLevel(LevelLoader.LEVELS.get(1), dir.getPath());
        new File(dir, "custom_level1.txt").deleteOnExit();
        assert report.contains("3 coins, 2 gems, 2 enemies");
    }

    static void checkFiles() throws IOException, InvalidLevelException {
        File original = temp();
        try (FileOutputStream out = new FileOutputStream(original)) {
            for (int b = 0; b < 256; b++) {
                out.write(b);
            }
        }
        File copy = temp();
        assert BackupManager.copy(original.getPath(), copy.getPath()) == 256;
        assert sameBytes(original, copy) : "the backup is a byte-for-byte copy";
        assert rejectsCopy(original.getPath() + ".missing") : "a missing source gets a clear message";

        File csv = temp();
        CsvExporter.write(csv.getPath(), "level,algorithm,explored,path_length,time_ms", List.of("1,BFS,84,29,2", "1,DFS,43,41,0"));
        List<String> skipped = new ArrayList<>();
        List<String[]> rows = CsvExporter.readBack(csv.getPath(), skipped);
        assert rows.size() == 3 && skipped.isEmpty() && String.join(",", rows.get(2)).equals("1,DFS,43,41,0") : "CSV round trip";
        CsvExporter.write(csv.getPath(), "level,algorithm,explored,path_length,time_ms", List.of("1,BFS,84,29,2", "1,DFS,43", "x,BFS,1,2,3"));
        skipped.clear();
        rows = CsvExporter.readBack(csv.getPath(), skipped);
        assert rows.size() == 2 : "only the good row is kept";
        assert skipped.equals(List.of("Row 3 skipped: expected 5 columns, found 3", "Row 4 skipped: level is not a number: 'x'"));

        File saveFile = temp();
        SaveFile save = new SaveFile("bob", 1, 4, 13, 2, 140, 2, 87, List.of(new Position(2, 7), new Position(4, 40)),
                "C:/path quest/levels/level1.txt");
        save.write(saveFile.getPath());
        SaveFile back = SaveFile.read(saveFile.getPath());
        assert back.getUser().equals("bob") && back.getLevelNo() == 1 && back.getRow() == 4 && back.getCol() == 13
                && back.getLives() == 2 && back.getPoints() == 140 && back.getCoins() == 2 && back.getTimeLeft() == 87
                && back.getCollected().equals(save.getCollected()) : "save then read gives back the same values";
        assert back.getFile().equals("C:/path quest/levels/level1.txt") : "a level path with a space survives";
        new SaveFile("bob", 0, 3, 0, 3, 0, 0, 60, List.of(), "levels/custom_blocked.txt").write(saveFile.getPath());
        assert SaveFile.read(saveFile.getPath()).getLevelNo() == 0 : "a custom level (number 0) can be saved and read back";
        try (FileWriter out = new FileWriter(saveFile)) {
            out.write("USER bob\nLEVEL one\n");
        }
        assert rejectsSave(saveFile.getPath(), "line 2: LEVEL") : "a corrupt save is rejected with a readable message";
        try (FileWriter out = new FileWriter(saveFile)) {
            out.write("USER bob\nLEVEL 1\nPLAYER 4 13\n");
        }
        assert rejectsSave(saveFile.getPath(), "line 4: LIVES is missing") : "a cut-off save is rejected";

        Level level = LevelLoader.load(LevelLoader.LEVELS.get(1));
        GameState game = new GameState(level, 0);
        game.restore(save, 1_000);
        assert game.getPlayer().getPosition().equals(new Position(4, 13)) && game.getPlayer().getLives() == 2;
        assert game.getPlayer().getPoints() == 140 && game.timeLeft(1_000) == 87;
        assert level.getCoins().size() == 2 && level.getGems().size() == 1 : "the saved coin and gem are gone";
        assert game.collected(LevelLoader.load(LevelLoader.LEVELS.get(1))).equals(save.getCollected());
        try {
            game.restore(new SaveFile("bob", 1, 3, 20, 2, 0, 0, 87, List.of(), LevelLoader.LEVELS.get(1)), 0);
            assert false : "a save inside a wall must be refused";
        } catch (IllegalArgumentException e) {
            assert e.getMessage().contains("(3,20)");
        }
    }

    static File temp() throws IOException {
        File f = File.createTempFile("pathquest", ".tmp");
        f.deleteOnExit();
        return f;
    }

    static boolean sameBytes(File a, File b) throws IOException {
        try (FileInputStream x = new FileInputStream(a); FileInputStream y = new FileInputStream(b)) {
            int bx;
            int by;
            do {
                bx = x.read();
                by = y.read();
                if (bx != by) {
                    return false;
                }
            } while (bx != -1);
            return true;
        }
    }

    static boolean rejectsCopy(String missing) {
        try {
            BackupManager.copy(missing, missing + ".bak");
            return false;
        } catch (IOException e) {
            return e.getMessage().contains("does not exist");
        }
    }

    static boolean rejectsSave(String path, String fragment) {
        try {
            SaveFile.read(path);
            return false;
        } catch (IOException e) {
            return e.getMessage().contains(fragment);
        }
    }

    static void checkStats() {
        LocalDateTime base = LocalDateTime.of(2026, 11, 1, 9, 0);
        List<Score> scores = new ArrayList<>();
        for (int i = 0; i < 25; i++) {
            scores.add(new Score(i < 15 ? "ann" : "bob", i % 2 == 0 ? 3 : 1, (i * 7) % 25 * 10, i, (i * 11) % 25, i % 3 == 0,
                    base.plusMinutes(i)));
        }
        List<Integer> topPoints = StatsService.topTen(scores).stream().map(Score::getPoints).collect(Collectors.toList());
        assert topPoints.equals(List.of(240, 230, 220, 210, 200, 190, 180, 170, 160, 150)) : "top 10, highest points first";
        assert StatsService.leaderboard(scores).split("\n").length == 11 : "a header and 10 ranked rows";

        List<Score> ann = StatsService.history(scores, "ann", false, false);
        assert ann.size() == 15 && StatsService.pageCount(ann) == 3;
        assert gameIds(StatsService.page(ann, 2)).equals(List.of(4, 3, 2, 1, 0)) : "page 3 holds the remaining (oldest) 5";
        assert StatsService.page(ann, 3).isEmpty();
        assert StatsService.levelsPlayed(ann).equals(List.of(1, 3));

        assert gameIds(StatsService.history(scores, "ann", true, false)).equals(List.of(12, 9, 6, 3, 0)) : "won only, newest first";
        List<Integer> timeLeft = StatsService.history(scores, "ann", false, true).stream()
                .map(Score::getTimeLeft).collect(Collectors.toList());
        assert timeLeft.equals(List.of(24, 22, 21, 19, 18, 16, 13, 11, 10, 8, 7, 5, 4, 2, 0)) : "most time left first";
    }

    static List<Integer> gameIds(List<Score> scores) {
        return scores.stream().map(Score::getCoins).collect(Collectors.toList());
    }

    static void checkStores() throws IOException {
        File playersFile = temp();
        File scoresFile = temp();
        assert playersFile.delete() && scoresFile.delete() : "start with no files";
        PlayerStore players = new PlayerStore(playersFile.getPath());
        ScoreStore scores = new ScoreStore(scoresFile.getPath());
        LocalDateTime now = LocalDateTime.now().withNano(0);
        try {
            assert players.findAll().isEmpty() && scores.findAll().isEmpty() : "a missing file means no records yet";
            Player ann = players.register("ann", "1234");
            Player bob = players.register("bob", "5678");
            assert ann.getPlayerId() == 1 && bob.getPlayerId() == 2 && players.findById(2).isPresent();
            assert players.login("ann", "1234").isPresent() && players.login("ANN", "1234").isPresent() : "usernames ignore case";
            assert players.login("ann", "9999").isEmpty() && players.login("nobody", "1234").isEmpty();
            assert rejectsRegister(players, "Ann", "1111", "already taken") : "test 10: a duplicate username gets a friendly message";
            assert rejectsRegister(players, "ab", "1234", "Username") && rejectsRegister(players, "okname", "12a4", "PIN")
                    && rejectsRegister(players, "okname", "-123", "PIN") && rejectsRegister(players, "okname", "12345", "PIN");

            players.updatePin(ann, "4321");
            PlayerStore reopened = new PlayerStore(playersFile.getPath());
            assert reopened.login("ann", "4321").isPresent() && reopened.login("ann", "1234").isEmpty() : "the new PIN is saved in the file";
            assert reopened.findByUsername("ann").orElseThrow().getCreatedAt() != null;

            PlayerStore lockout = new PlayerStore(playersFile.getPath());
            for (int i = 0; i < PlayerStore.MAX_TRIES; i++) {
                assert lockout.login("bob", "0000").isEmpty();
            }
            assert lockout.triesLeft("bob") == 0 && lockout.triesLeft("BOB") == 0 : "the count ignores case";
            try {
                lockout.login("bob", "5678");
                assert false : "after 3 wrong PINs even the right PIN is refused";
            } catch (AccountLockedException e) {
                assert e.getMessage().contains("bob");
            }

            int first = scores.save(new Score("ann", 1, 123, 2, 45, true, now));
            int second = scores.save(new Score("bob", 4, 50, 1, 10, false, now));
            Score saved = scores.findById(first).orElseThrow();
            assert saved.getUsername().equals("ann") && saved.getPoints() == 123 && saved.isWon() && saved.getPlayedAt().equals(now)
                    : "a score comes back with every field";
            scores.update(first, new Score("ann", 1, 200, 3, 10, false, now));
            assert scores.findById(first).orElseThrow().getPoints() == 200 && scores.findAll().size() == 2;
            scores.delete(second);
            assert scores.findById(second).isEmpty() && scores.findAll().size() == 1;
            scores.save(new Score("ann", 2, 80, 1, 5, true, now));
            players.delete(ann.getPlayerId());
            scores.deleteAllOf("ANN");
            assert players.findByUsername("ann").isEmpty() && scores.findAll().isEmpty() : "deleting a player also deletes all their scores";

            try (FileWriter out = new FileWriter(scoresFile)) {
                out.write("1,ann,1,abc,2,45,true," + now + "\n");
            }
            try {
                scores.findAll();
                assert false : "a damaged scores file must be refused";
            } catch (IOException e) {
                assert e.getMessage().contains("line 1") && e.getMessage().contains("'abc' is not a number") : e.getMessage();
            }
        } catch (AccountLockedException e) {
            throw new AssertionError("an unexpected lockout: " + e.getMessage(), e);
        }
    }

    static boolean rejectsRegister(PlayerStore players, String username, String pin, String fragment) throws IOException {
        try {
            players.register(username, pin);
            return false;
        } catch (IllegalArgumentException e) {
            return e.getMessage().contains(fragment);
        }
    }

    static void checkBuffer() throws InterruptedException {
        BoundedBuffer<Integer> buffer = new BoundedBuffer<>(5);
        List<Integer> received = new ArrayList<>();
        Thread producer = new Thread(() -> {
            for (int i = 0; i < 100; i++) {
                buffer.put(i);
            }
            buffer.close();
        });
        Thread consumer = new Thread(() -> {
            Integer n;
            while ((n = buffer.take()) != null) {
                received.add(n);
            }
        });
        producer.setDaemon(true);
        consumer.setDaemon(true);
        producer.start();
        consumer.start();
        producer.join(5000);
        consumer.join(5000);
        assert !consumer.isAlive() : "close() lets the consumer finish";
        assert received.size() == 100 : "all 100 numbers arrive";
        for (int i = 0; i < 100; i++) {
            assert received.get(i) == i : "numbers arrive in order";
        }

        BoundedBuffer<String> empty = new BoundedBuffer<>(5);
        String[] taken = {"nothing yet"};
        Thread taker = new Thread(() -> taken[0] = empty.take());
        taker.setDaemon(true);
        taker.start();
        while (taker.isAlive() && taker.getState() != Thread.State.WAITING) {
            Thread.sleep(1);
        }
        assert taker.isAlive() : "take() blocks on an empty buffer";
        empty.close();
        taker.join(2000);
        assert !taker.isAlive() && taken[0] == null : "close() releases a blocked take()";
        assert !empty.put("late") : "put() fails once the buffer is closed";
    }

    static Level level(String... lines) throws IOException, InvalidLevelException {
        return LevelLoader.read(new BufferedReader(new StringReader(String.join("\n", lines))));
    }

    static void checkMoveRules() throws IOException, InvalidLevelException {
        Level wall = level("WIDTH 5 HEIGHT 2 TIME 60 LIVES 3", "@#  F", "#####");
        GameState game = new GameState(wall, 0);
        assert game.step(Command.RIGHT, 0, false).getStatus() == GameState.CONTINUE;
        assert game.getPlayer().getPosition().equals(new Position(0, 0)) : "a wall stops the player";

        Level ledge = level("WIDTH 5 HEIGHT 4 TIME 60 LIVES 3", " @  F", "##", "", "#####");
        MoveRules.MoveResult fall = MoveRules.apply(ledge, new Position(0, 1), Command.RIGHT);
        assert fall.getCells().equals(List.of(new Position(0, 2), new Position(1, 2), new Position(2, 2)));
        assert !fall.enteredByFalling(0) && fall.enteredByFalling(1) && fall.enteredByFalling(2);
        assert fall.getLanding().equals(new Position(2, 2)) && !fall.isPit();

        Level gap = level("WIDTH 5 HEIGHT 4 TIME 60 LIVES 3", "", "", " @  F", "## ##");
        game = new GameState(gap, 0);
        game.step(Command.JUMP_RIGHT, 0, false);
        assert game.getPlayer().getPosition().equals(new Position(2, 3)) && game.getPlayer().getLives() == 3 : "wd clears a 1-column gap";
        assert MoveRules.neighbours(gap, new Position(2, 1)).equals(List.of(new Position(2, 3), new Position(2, 0)))
                : "neighbours leave out pits, repeats and the same cell, in the fixed order";
    }

    static void checkGameRules() throws IOException, InvalidLevelException {
        Level pit = level("WIDTH 5 HEIGHT 2 TIME 60 LIVES 3", "@   F", "# ###");
        GameState game = new GameState(pit, 0);
        assert game.step(Command.RIGHT, 0, false).getStatus() == GameState.LIFE_LOST;
        assert game.getPlayer().getLives() == 2 && game.getPlayer().getPosition().equals(new Position(0, 0));
        game = new GameState(level("WIDTH 5 HEIGHT 2 TIME 60 LIVES 1", "@   F", "# ###"), 0);
        assert game.step(Command.RIGHT, 0, false).getStatus() == GameState.LOST : "no lives left is game over";

        Level loot = level("WIDTH 6 HEIGHT 2 TIME 60 LIVES 3", "@o$  F", "######");
        game = new GameState(loot, 0);
        game.step(Command.RIGHT, 0, false);
        assert game.getPlayer().getPoints() == 10 && game.getPlayer().getCoins() == 1 && loot.getCoins().isEmpty();
        game.step(Command.RIGHT, 0, false);
        assert game.getPlayer().getPoints() == 60 && loot.getGems().isEmpty();

        game = new GameState(level("WIDTH 3 HEIGHT 2 TIME 60 LIVES 3", "@F", "###"), 0);
        assert game.step(Command.RIGHT, 10_000, false).getStatus() == GameState.WON;
        assert game.getPlayer().getPoints() == 50 * 5 : "the flag adds remaining time x 5";

        Level stomp = level("WIDTH 6 HEIGHT 4 TIME 60 LIVES 3", "", "", "@ E  F", "######");
        game = new GameState(stomp, 0);
        assert game.step(Command.JUMP_RIGHT, 0, false).getStatus() == GameState.CONTINUE;
        assert game.getPlayer().getPoints() == 100 && stomp.getEnemies().isEmpty() : "landing on an enemy defeats it";

        Level side = level("WIDTH 6 HEIGHT 2 TIME 60 LIVES 3", "@E   F", "######");
        game = new GameState(side, 0);
        assert game.step(Command.RIGHT, 0, false).getStatus() == GameState.LIFE_LOST : "touching an enemy from the side";
        assert game.getPlayer().getPosition().equals(new Position(0, 0)) && side.getEnemies().size() == 1;

        game = new GameState(level("WIDTH 6 HEIGHT 2 TIME 60 LIVES 3", "E @  F", "######"), 0);
        assert game.step(Command.LEFT, 0, false).getStatus() == GameState.LIFE_LOST : "an enemy stepping onto the player";
    }

    static void checkEnemies() throws IOException, InvalidLevelException {
        Level wall = level("WIDTH 6 HEIGHT 2 TIME 60 LIVES 3", "@ E# F", "######");
        Enemy enemy = wall.getEnemies().get(0);
        enemy.step(wall);
        assert enemy.getCol() == 2 && enemy.getDirection() == -1 : "an enemy turns at a wall and stays";
        enemy.step(wall);
        assert enemy.getCol() == 1;
        Level drop = level("WIDTH 6 HEIGHT 2 TIME 60 LIVES 3", "@ E  F", "### ##");
        enemy = drop.getEnemies().get(0);
        enemy.step(drop);
        assert enemy.getCol() == 2 && enemy.getDirection() == -1 : "an enemy turns at a drop and stays";
    }

    static void checkTimer() throws IOException, InvalidLevelException {
        GameState game = new GameState(level("WIDTH 6 HEIGHT 2 TIME 5 LIVES 3", "@    F", "######"), 0);
        assert game.timeLeft(4_999) == 1;
        assert game.step(Command.RIGHT, 5_000, false).getStatus() == GameState.LIFE_LOST : "the timer reaching 0 costs a life";
        assert game.getPlayer().getLives() == 2 && game.getPlayer().getPosition().equals(new Position(0, 0));
        assert game.timeLeft(5_000) == 5 : "the timer restarts";
        game.pause(6_000);
        game.resume(9_000);
        assert game.timeLeft(9_000) == 4 : "the timer stands still while paused";
    }

    static void checkPosition() {
        Set<Position> cells = new HashSet<>();
        cells.add(new Position(2, 3));
        cells.add(new Position(2, 3));
        assert cells.size() == 1 : "equal Positions should collapse to one in a HashSet";
        assert new Position(2, 3).toString().equals("(2,3)");
    }

    static void checkCommand() {
        assert Command.parse("wd").getName().equals(Command.JUMP_RIGHT);
        assert Command.parse("  WA ").getName().equals(Command.JUMP_LEFT) : "parse should trim and lowercase";
        assert rejects("jump", "Unknown move: jump");
        assert rejects("ai bfs", "Unknown move: ai bfs") : "AI mode is not part of Evaluation 1";
        assert rejects("", "Unknown move: ");
    }

    static boolean rejects(String line, String message) {
        try {
            Command.parse(line);
            return false;
        } catch (InvalidMoveException e) {
            return e.getMessage().equals(message);
        }
    }

    static void checkScore() {
        LocalDateTime t = LocalDateTime.of(2026, 11, 3, 10, 0);
        Score low = new Score("ann", 1, 50, 5, 10, false, t);
        Score high = new Score("bob", 1, 200, 3, 40, true, t.plusMinutes(5));
        Score tieLater = new Score("cid", 1, 200, 3, 40, true, t.plusMinutes(9));
        List<Score> scores = new ArrayList<>(List.of(low, tieLater, high));
        Collections.sort(scores);
        assert scores.get(0) == high : "higher points first, earlier playedAt breaks a tie";
        assert scores.get(1) == tieLater;
        assert scores.get(2) == low;
    }

    static void checkEntityCount() {
        int before = GameEntity.getCount();
        new Coin(0, 0);
        new Gem(0, 1);
        assert GameEntity.getCount() == before + 2 : "creating entities should grow the count";
    }

    static void checkLevels() throws IOException, InvalidLevelException {
        Level level = LevelLoader.load(LevelLoader.LEVELS.get(1));
        assert level.getStart() != null && level.getFlag() != null : "level1 needs one start and one flag";
        assert level.getWidth() == 60 && level.getHeight() == 6;
        assert level.getCoins().size() == 3 && level.getGems().size() == 2 && level.getEnemies().size() == 2;
        for (char[] row : level.getGrid()) {
            for (char ch : row) {
                assert ch == '#' || ch == ' ' : "the grid keeps only walls and spaces";
            }
        }
        assert rejectsLevel("levels/bad_row_too_long.txt", "Row 3, column 11");
        assert rejectsLevel("levels/bad_symbol.txt", "Row 3, column 5");
        assert rejectsLevel("levels/bad_no_flag.txt", "no flag F");
        try {
            level("WIDTH 100000 HEIGHT 100000 TIME 60 LIVES 3", "@F");
            assert false : "a huge level must be refused before its grid is allocated";
        } catch (InvalidLevelException e) {
            assert e.getMessage().startsWith("Row 1: a level can be at most") : "the size limit names row 1";
        }
        LevelLoader.load("levels/blocked.txt");
    }

    static boolean rejectsLevel(String path, String fragment) throws IOException {
        try {
            LevelLoader.load(path);
            return false;
        } catch (InvalidLevelException e) {
            return e.getMessage().contains("Row") && e.getMessage().contains(fragment);
        }
    }

    static void checkRenderer() throws IOException, InvalidLevelException {
        Level level = LevelLoader.load("levels/blocked.txt");
        Player player = new Player(3, 0);
        String[] lines = Renderer.frame(level, player, "alice", 60, 4, true).split("\n");
        assert lines[0].equals("PLAYER: alice   LEVEL 4 (MEDIUM)") : "the HUD shows the logged-in name and the difficulty";
        assert lines[1].startsWith("SCORE: 0") && lines[1].endsWith("TIME: 060");
        assert lines[6].startsWith("@") : "the player is drawn on the grid";
        assert lines[5].charAt(9) == 'F';
        assert lines[lines.length - 1].startsWith("a/d = move") : "the controls line is shown when the setting is ON";
        String noControls = Renderer.frame(level, player, "bob", 60, 0, false);
        assert noControls.startsWith("PLAYER: bob   CUSTOM LEVEL") && !noControls.contains("a/d = move") : "SHOW_CONTROLS OFF hides it";
    }

    static void checkSettings() throws IOException {
        File file = temp();
        assert file.delete() : "start without a file";
        List<String> problems = new ArrayList<>();
        Settings defaults = Settings.load(file.getPath(), problems);
        assert !defaults.isOn(Settings.BEEP) && defaults.isOn(Settings.CLEAR_SCREEN) && defaults.isOn(Settings.SHOW_CONTROLS) && problems.isEmpty()
                : "no file means the defaults";
        defaults.toggle(Settings.BEEP);
        defaults.toggle(Settings.SHOW_CONTROLS);
        defaults.save();
        Settings back = Settings.load(file.getPath(), problems);
        assert back.isOn(Settings.BEEP) && !back.isOn(Settings.SHOW_CONTROLS) && back.isOn(Settings.CLEAR_SCREEN) : "saved settings come back";
        try (FileWriter out = new FileWriter(file)) {
            out.write("BEEP ON\nVOLUME 11\nCLEAR_SCREEN maybe\n");
        }
        Settings damaged = Settings.load(file.getPath(), problems);
        assert damaged.isOn(Settings.BEEP) && damaged.isOn(Settings.CLEAR_SCREEN) : "good lines are used, bad ones keep the default";
        assert problems.size() == 2 && problems.get(0).startsWith("Settings line 2") && problems.get(1).startsWith("Settings line 3");
    }

    static void checkReplay() throws IOException, InvalidLevelException {
        GameState real = new GameState(LevelLoader.load(LevelLoader.LEVELS.get(1)), 5_000);
        List<String> moves = new ArrayList<>();
        String[] played = {"d", "d", "d", "p", "p", "w", "d", "d", "d", "d", "d", "d", "d", "wd", "d", "d"};
        for (int i = 0; i < played.length; i++) {
            long now = 5_000 + i * 700L;
            moves.add((now - real.getCreatedAt()) + " " + played[i]);
            if (played[i].equals(Command.PAUSE)) {
                if (real.isPaused()) {
                    real.resume(now);
                } else {
                    real.pause(now);
                }
            } else {
                real.step(played[i], now, false);
            }
        }
        File file = temp();
        ReplayFile.write(file.getPath(), LevelLoader.LEVELS.get(1), moves);
        List<String> lines = ReplayFile.read(file.getPath());
        assert lines.get(0).equals("LEVEL " + LevelLoader.LEVELS.get(1)) && lines.subList(1, lines.size()).equals(moves) : "replay file round trip";

        GameState replayed = new GameState(LevelLoader.load(LevelLoader.LEVELS.get(1)), 0);
        PrintStream screen = System.out;
        System.setOut(new PrintStream(new ByteArrayOutputStream()));
        try {
            new ReplayPlayer(replayed, 1, "bob", lines.subList(1, lines.size()), false, false, 0).run();
        } finally {
            System.setOut(screen);
        }
        assert replayed.getPlayer().getPosition().equals(real.getPlayer().getPosition())
                && replayed.getPlayer().getPoints() == real.getPlayer().getPoints()
                && replayed.getPlayer().getLives() == real.getPlayer().getLives() : "the replay ends exactly where the game did";

        try (FileWriter out = new FileWriter(file)) {
            out.write("LEVEL levels/level1.txt\n100 d\n200 jump\n");
        }
        try {
            ReplayFile.read(file.getPath());
            assert false : "a damaged replay must be refused";
        } catch (IOException e) {
            assert e.getMessage().startsWith("Replay line 3") : "the refusal names the line";
        }
    }
}
