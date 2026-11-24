package pathquest.store;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class DataFile {
    public static List<String> readLines(String path) throws IOException {
        List<String> lines = new ArrayList<>();
        if (!new File(path).exists()) {
            return lines;
        }
        try (BufferedReader in = new BufferedReader(new FileReader(path))) {
            String line;
            while ((line = in.readLine()) != null) {
                if (!line.isBlank()) {
                    lines.add(line);
                }
            }
        }
        return lines;
    }

    public static void writeLines(String path, List<String> lines) throws IOException {
        File parent = new File(path).getParentFile();
        if (parent != null) {
            parent.mkdirs();
        }
        try (BufferedWriter out = new BufferedWriter(new FileWriter(path))) {
            for (String line : lines) {
                out.write(line);
                out.newLine();
            }
        }
    }

    static int number(String value, String path, int lineNo) throws IOException {
        try {
            return Integer.parseInt(value.trim());
        } catch (NumberFormatException e) {
            throw new IOException(path + " line " + lineNo + ": '" + value + "' is not a number");
        }
    }
}
