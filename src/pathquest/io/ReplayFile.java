package pathquest.io;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import pathquest.engine.InvalidMoveException;
import pathquest.model.Command;

public class ReplayFile {
    public static String fileFor(String username) {
        return "replays/" + username + ".txt";
    }

    public static void write(String path, String levelFile, List<String> moves) throws IOException {
        File parent = new File(path).getParentFile();
        if (parent != null) {
            parent.mkdirs();
        }
        try (BufferedWriter out = new BufferedWriter(new FileWriter(path))) {
            out.write("LEVEL " + levelFile);
            out.newLine();
            for (String move : moves) {
                out.write(move);
                out.newLine();
            }
        }
    }

    public static List<String> read(String path) throws IOException {
        List<String> lines = new ArrayList<>();
        try (BufferedReader in = new BufferedReader(new FileReader(path))) {
            String first = in.readLine();
            if (first == null || !first.startsWith("LEVEL ") || first.substring(6).isBlank()) {
                throw new IOException("Replay line 1: expected LEVEL followed by the level file");
            }
            lines.add(first);
            String line;
            int lineNo = 1;
            while ((line = in.readLine()) != null) {
                lineNo++;
                String[] parts = line.trim().split("\\s+");
                try {
                    if (parts.length != 2 || Long.parseLong(parts[0]) < 0) {
                        throw new IOException("Replay line " + lineNo + ": expected a time and a move");
                    }
                    Command.parse(parts[1]);
                } catch (NumberFormatException | InvalidMoveException e) {
                    throw new IOException("Replay line " + lineNo + ": " + e.getMessage());
                }
                lines.add(line.trim());
            }
        }
        return lines;
    }
}
