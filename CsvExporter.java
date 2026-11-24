package pathquest.io;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import pathquest.store.ScoreStore;
import pathquest.model.Score;
import pathquest.report.StatsService;

public class CsvExporter {
    public static final String LEADERBOARD = "exports/leaderboard.csv";
    private static final DateTimeFormatter WHEN = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
    private static final Set<String> NUMERIC = Set.of("rank", "points", "level", "coins", "time_left");

    public static int exportLeaderboard(String path) throws IOException {
        List<Score> top = StatsService.topTen(new ScoreStore(ScoreStore.FILE).findAll());
        List<String> rows = new ArrayList<>();
        for (int i = 0; i < top.size(); i++) {
            Score s = top.get(i);
            rows.add((i + 1) + "," + s.getUsername() + "," + s.getPoints() + "," + s.getLevelNo() + "," + WHEN.format(s.getPlayedAt()));
        }
        write(path, "rank,username,points,level,played_at", rows);
        return rows.size();
    }

    public static void write(String path, String header, List<String> rows) throws IOException {
        File parent = new File(path).getParentFile();
        if (parent != null) {
            parent.mkdirs();
        }
        try (BufferedWriter out = new BufferedWriter(new FileWriter(path))) {
            out.write(header);
            out.newLine();
            for (String row : rows) {
                out.write(row);
                out.newLine();
            }
        }
    }

    public static List<String[]> readBack(String path, List<String> skipped) throws IOException {
        List<String[]> rows = new ArrayList<>();
        try (BufferedReader in = new BufferedReader(new FileReader(path))) {
            String first = in.readLine();
            if (first == null) {
                throw new IOException(path + " is empty");
            }
            String[] header = first.split(",", -1);
            rows.add(header);
            String line;
            for (int n = 2; (line = in.readLine()) != null; n++) {
                String[] cells = line.split(",", -1);
                String problem = problem(header, cells);
                if (problem == null) {
                    rows.add(cells);
                } else {
                    skipped.add("Row " + n + " skipped: " + problem);
                }
            }
        }
        return rows;
    }

    private static String problem(String[] header, String[] cells) {
        if (cells.length != header.length) {
            return "expected " + header.length + " columns, found " + cells.length;
        }
        for (int i = 0; i < cells.length; i++) {
            if (NUMERIC.contains(header[i])) {
                try {
                    Long.parseLong(cells[i]);
                } catch (NumberFormatException e) {
                    return header[i] + " is not a number: '" + cells[i] + "'";
                }
            }
        }
        return null;
    }
}
