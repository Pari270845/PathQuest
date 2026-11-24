package pathquest.model;

import java.util.List;
import pathquest.engine.InvalidMoveException;

public class Command {
    public static final String LEFT = "a";
    public static final String RIGHT = "d";
    public static final String JUMP = "w";
    public static final String JUMP_RIGHT = "wd";
    public static final String JUMP_LEFT = "wa";
    public static final String PAUSE = "p";
    public static final String EXIT = "x";
    public static final String DEBUG = "dbg";

    private static final List<String> ALL = List.of(LEFT, RIGHT, JUMP, JUMP_RIGHT, JUMP_LEFT, PAUSE, EXIT, DEBUG);

    private final String name;

    public Command(String name) {
        this.name = name;
    }

    public static Command parse(String line) {
        String word = line.trim().toLowerCase();
        if (ALL.contains(word)) {
            return new Command(word);
        }
        throw new InvalidMoveException("Unknown move: " + line.trim());
    }

    public String getName() {
        return name;
    }

    @Override
    public String toString() {
        return name;
    }
}
