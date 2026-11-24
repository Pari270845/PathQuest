package pathquest.level;

public class InvalidLevelException extends Exception {
    public InvalidLevelException(String message) {
        super(message);
    }

    public InvalidLevelException(int row, int col, String detail) {
        super("Row " + row + ", column " + col + ": " + detail);
    }
}
