package pathquest.store;

import java.io.IOException;
import java.time.LocalDateTime;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import pathquest.model.Score;

public class ScoreStore implements Repository<Score, Integer> {
    public static final String FILE = "data/scores.csv";

    private final String path;

    public ScoreStore(String path) {
        this.path = path;
    }

    @Override
    public Integer save(Score score) throws IOException {
        List<String> lines = DataFile.readLines(path);
        int id = 1;
        for (int i = 0; i < lines.size(); i++) {
            id = Math.max(id, idOf(lines.get(i), i + 1) + 1);
        }
        lines.add(id + "," + toCsv(score));
        DataFile.writeLines(path, lines);
        return id;
    }

    @Override
    public Optional<Score> findById(Integer id) throws IOException {
        List<String> lines = DataFile.readLines(path);
        for (int i = 0; i < lines.size(); i++) {
            if (idOf(lines.get(i), i + 1) == id) {
                return Optional.of(toScore(lines.get(i), i + 1));
            }
        }
        return Optional.empty();
    }

    @Override
    public List<Score> findAll() throws IOException {
        List<String> lines = DataFile.readLines(path);
        List<Score> scores = new ArrayList<>();
        for (int i = 0; i < lines.size(); i++) {
            scores.add(toScore(lines.get(i), i + 1));
        }
        return scores;
    }

    @Override
    public void update(Integer id, Score score) throws IOException {
        List<String> lines = DataFile.readLines(path);
        for (int i = 0; i < lines.size(); i++) {
            if (idOf(lines.get(i), i + 1) == id) {
                lines.set(i, id + "," + toCsv(score));
            }
        }
        DataFile.writeLines(path, lines);
    }

    @Override
    public void delete(Integer id) throws IOException {
        List<String> lines = DataFile.readLines(path);
        List<String> kept = new ArrayList<>();
        for (int i = 0; i < lines.size(); i++) {
            if (idOf(lines.get(i), i + 1) != id) {
                kept.add(lines.get(i));
            }
        }
        DataFile.writeLines(path, kept);
    }

    public void deleteAllOf(String username) throws IOException {
        List<String> lines = DataFile.readLines(path);
        List<String> kept = new ArrayList<>();
        for (int i = 0; i < lines.size(); i++) {
            if (!toScore(lines.get(i), i + 1).getUsername().equalsIgnoreCase(username)) {
                kept.add(lines.get(i));
            }
        }
        DataFile.writeLines(path, kept);
    }

    private static String toCsv(Score s) {
        return s.getUsername() + "," + s.getLevelNo() + "," + s.getPoints() + "," + s.getCoins() + "," + s.getTimeLeft()
                + "," + s.isWon() + "," + s.getPlayedAt();
    }

    private String[] fields(String line, int lineNo) throws IOException {
        String[] f = line.split(",");
        if (f.length != 8) {
            throw new IOException(path + " line " + lineNo + ": expected 8 fields, found " + f.length);
        }
        return f;
    }

    private int idOf(String line, int lineNo) throws IOException {
        return DataFile.number(fields(line, lineNo)[0], path, lineNo);
    }

    private Score toScore(String line, int lineNo) throws IOException {
        String[] f = fields(line, lineNo);
        if (!f[6].equals("true") && !f[6].equals("false")) {
            throw new IOException(path + " line " + lineNo + ": won must be true or false");
        }
        try {
            return new Score(f[1], DataFile.number(f[2], path, lineNo), DataFile.number(f[3], path, lineNo),
                    DataFile.number(f[4], path, lineNo), DataFile.number(f[5], path, lineNo), Boolean.parseBoolean(f[6]),
                    LocalDateTime.parse(f[7]));
        } catch (DateTimeParseException e) {
            throw new IOException(path + " line " + lineNo + ": '" + f[7] + "' is not a date and time");
        }
    }
}
