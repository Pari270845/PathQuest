package pathquest.report;

import java.time.format.DateTimeFormatter;
import java.util.Comparator;
import java.util.DoubleSummaryStatistics;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.TreeMap;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import pathquest.level.LevelLoader;
import pathquest.model.Score;

public class StatsService {
    public static final int PAGE_SIZE = 5;
    private static final DateTimeFormatter WHEN = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");

    public static final Predicate<Score> WON_ONLY = s -> s.isWon();
    public static final Comparator<Score> BY_TIME_LEFT = (a, b) -> Integer.compare(b.getTimeLeft(), a.getTimeLeft());

    public static List<Score> topTen(List<Score> scores) {
        return scores.stream().sorted().limit(10).collect(Collectors.toList());
    }

    public static List<Score> history(List<Score> scores, String username, boolean wonOnly, boolean byTimeLeft) {
        Stream<Score> mine = scores.stream().filter(s -> s.getUsername().equals(username));
        if (wonOnly) {
            mine = mine.filter(WON_ONLY);
        }
        Comparator<Score> order = byTimeLeft ? BY_TIME_LEFT : Comparator.comparing(Score::getPlayedAt).reversed();
        return mine.sorted(order).collect(Collectors.toList());
    }

    public static <T> List<T> page(List<T> items, int page) {
        return items.stream().skip((long) page * PAGE_SIZE).limit(PAGE_SIZE).collect(Collectors.toList());
    }

    public static int pageCount(List<?> items) {
        return Math.max(1, (items.size() + PAGE_SIZE - 1) / PAGE_SIZE);
    }

    public static List<Integer> levelsPlayed(List<Score> scores) {
        return scores.stream().map(Score::getLevelNo).distinct().sorted().collect(Collectors.toList());
    }

    public static List<Score> ofDifficulty(List<Score> scores, String difficulty) {
        return scores.stream().filter(s -> LevelLoader.difficulty(s.getLevelNo()).equals(difficulty)).collect(Collectors.toList());
    }

    public static String profile(List<Score> scores, String username) {
        List<Score> mine = scores.stream().filter(s -> s.getUsername().equals(username)).collect(Collectors.toList());
        Optional<Score> best = mine.stream().max(Comparator.comparingInt(Score::getPoints));
        if (best.isEmpty()) {
            return "No games yet: play a level first\n";
        }
        long wins = mine.stream().filter(WON_ONLY).count();
        int coins = mine.stream().mapToInt(Score::getCoins).sum();
        List<Integer> levelsWon = mine.stream().filter(WON_ONLY).map(Score::getLevelNo).distinct().sorted().collect(Collectors.toList());
        Map<String, Long> perDifficulty = mine.stream().collect(
                Collectors.groupingBy(s -> LevelLoader.difficulty(s.getLevelNo()), TreeMap::new, Collectors.counting()));
        return "Games played   " + mine.size() + "\n"
                + "Wins           " + wins + " (" + wins * 100 / mine.size() + "%)\n"
                + "Best score     " + best.get().getPoints() + " on level " + best.get().getLevelNo() + "\n"
                + "Coins in total " + coins + "\n"
                + "Levels won     " + (levelsWon.isEmpty() ? "none yet" : levelsWon) + "\n"
                + "Games by difficulty " + perDifficulty + "\n";
    }

    public static String leaderboard(List<Score> scores) {
        List<Score> top = topTen(scores);
        if (top.isEmpty()) {
            return "No scores yet\n";
        }
        String row = "%4s  %-20s %7s  %5s  %s\n";
        StringBuilder sb = new StringBuilder(String.format(row, "Rank", "Player", "Points", "Level", "Played at"));
        for (int i = 0; i < top.size(); i++) {
            Score s = top.get(i);
            sb.append(String.format(row, i + 1, s.getUsername(), s.getPoints(), s.getLevelNo(), WHEN.format(s.getPlayedAt())));
        }
        return sb.toString();
    }

    public static String historyTable(List<Score> scores) {
        if (scores.isEmpty()) {
            return "No games here\n";
        }
        String row = "%-16s  %5s  %6s  %5s  %9s  %s\n";
        StringBuilder sb = new StringBuilder(String.format(row, "Played at", "Level", "Points", "Coins", "Time left", "Result"));
        for (Score s : scores) {
            sb.append(String.format(row, WHEN.format(s.getPlayedAt()), s.getLevelNo(), s.getPoints(), s.getCoins(),
                    s.getTimeLeft(), s.isWon() ? "won" : "lost"));
        }
        return sb.toString();
    }

    public static Map<String, Double> pointsByDifficulty(List<Score> scores) {
        return scores.stream().collect(Collectors.groupingBy(s -> LevelLoader.difficulty(s.getLevelNo()), TreeMap::new,
                Collectors.averagingInt(Score::getPoints)));
    }

    public static Map<Boolean, Long> outcomes(List<Score> scores) {
        return scores.stream().collect(Collectors.partitioningBy(Score::isWon, Collectors.counting()));
    }

    public static DoubleSummaryStatistics scoreSummary(List<Score> scores) {
        return scores.stream().collect(Collectors.summarizingDouble(Score::getPoints));
    }

    public static String players(List<Score> scores) {
        return scores.stream().map(Score::getUsername).distinct().sorted().collect(Collectors.joining(", "));
    }

    public static String statistics(List<Score> scores) {
        StringBuilder sb = new StringBuilder("=== Average points by difficulty ===\n");
        Map<String, Double> byDifficulty = pointsByDifficulty(scores);
        if (byDifficulty.isEmpty()) {
            sb.append("No games played yet\n");
        } else {
            byDifficulty.forEach((difficulty, average) -> sb.append(String.format("%-7s %8.1f\n", difficulty, average)));
        }
        sb.append("\n=== Wins and losses ===\n");
        Map<Boolean, Long> outcomes = outcomes(scores);
        sb.append("Won   ").append(outcomes.get(true)).append('\n');
        sb.append("Lost  ").append(outcomes.get(false)).append('\n');
        sb.append("Players: ").append(scores.isEmpty() ? "none yet" : players(scores)).append('\n');
        sb.append("\n=== Points ===\n");
        DoubleSummaryStatistics points = scoreSummary(scores);
        if (points.getCount() == 0) {
            sb.append("No scores yet\n");
        } else {
            sb.append(String.format("Games %d   Min %.0f   Max %.0f   Average %.1f   Total %.0f\n",
                    points.getCount(), points.getMin(), points.getMax(), points.getAverage(), points.getSum()));
        }
        return sb.toString();
    }
}
