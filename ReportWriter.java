package pathquest.io;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;

public class ReportWriter {
    public static void write(String path, String text) throws IOException {
        File parent = new File(path).getParentFile();
        if (parent != null) {
            parent.mkdirs();
        }
        try (FileWriter out = new FileWriter(path)) {
            out.write(text);
        }
    }
}
