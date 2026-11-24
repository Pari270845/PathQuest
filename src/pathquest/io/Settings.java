package pathquest.io;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

public class Settings {
    public static final String BEEP = "BEEP";
    public static final String CLEAR_SCREEN = "CLEAR_SCREEN";
    public static final String SHOW_CONTROLS = "SHOW_CONTROLS";

    private final String file;
    private final TreeMap<String, Boolean> values = new TreeMap<>();

    public Settings(String file) {
        this.file = file;
        values.put(BEEP, false);
        values.put(CLEAR_SCREEN, true);
        values.put(SHOW_CONTROLS, true);
    }

    public static String fileFor(String username) {
        return "settings/" + username + ".txt";
    }

    public static Settings load(String file, List<String> problems) throws IOException {
        Settings settings = new Settings(file);
        if (!new File(file).exists()) {
            return settings;
        }
        try (BufferedReader in = new BufferedReader(new FileReader(file))) {
            String line;
            int lineNo = 0;
            while ((line = in.readLine()) != null) {
                lineNo++;
                if (line.isBlank()) {
                    continue;
                }
                String[] parts = line.trim().split("\\s+");
                if (parts.length != 2 || !settings.values.containsKey(parts[0])
                        || !(parts[1].equals("ON") || parts[1].equals("OFF"))) {
                    problems.add("Settings line " + lineNo + " ignored: '" + line.trim() + "'");
                    continue;
                }
                settings.values.put(parts[0], parts[1].equals("ON"));
            }
        }
        return settings;
    }

    public void save() throws IOException {
        File parent = new File(file).getParentFile();
        if (parent != null) {
            parent.mkdirs();
        }
        try (BufferedWriter out = new BufferedWriter(new FileWriter(file))) {
            for (Map.Entry<String, Boolean> e : values.entrySet()) {
                out.write(e.getKey() + " " + (e.getValue() ? "ON" : "OFF"));
                out.newLine();
            }
        }
    }

    public boolean isOn(String name) {
        return values.get(name);
    }

    public void toggle(String name) {
        values.put(name, !values.get(name));
    }

    public List<String> names() {
        return new ArrayList<>(values.keySet());
    }
}
