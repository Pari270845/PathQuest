package pathquest.io;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;

public class BackupManager {
    public static final String BACKUP_PATH = "backup/save.bak";

    public static long backup() throws IOException {
        return copy(SaveFile.PATH, BACKUP_PATH);
    }

    public static long restore() throws IOException {
        return copy(BACKUP_PATH, SaveFile.PATH);
    }

    public static long copy(String from, String to) throws IOException {
        if (!new File(from).exists()) {
            throw new IOException(from + " does not exist, so there is nothing to copy");
        }
        File parent = new File(to).getParentFile();
        if (parent != null) {
            parent.mkdirs();
        }
        long count = 0;
        try (FileInputStream in = new FileInputStream(from);
             FileOutputStream out = new FileOutputStream(to)) {
            int b;
            while ((b = in.read()) != -1) {
                out.write(b);
                count++;
            }
        }
        return count;
    }
}
