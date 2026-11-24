package pathquest.engine;

import java.util.ArrayList;
import java.util.List;
import pathquest.level.Level;
import pathquest.model.Command;
import pathquest.model.Position;

public class MoveRules {
    public static final List<String> MOVES =
            List.of(Command.RIGHT, Command.JUMP_RIGHT, Command.JUMP, Command.JUMP_LEFT, Command.LEFT);

    public static MoveResult apply(Level level, Position from, String move) {
        int steps;
        int dRow;
        int dCol;
        switch (move) {
            case Command.RIGHT:
                steps = 1; dRow = 0; dCol = 1;
                break;
            case Command.LEFT:
                steps = 1; dRow = 0; dCol = -1;
                break;
            case Command.JUMP:
                steps = 2; dRow = -1; dCol = 0;
                break;
            case Command.JUMP_RIGHT:
                steps = 2; dRow = -1; dCol = 1;
                break;
            case Command.JUMP_LEFT:
                steps = 2; dRow = -1; dCol = -1;
                break;
            default:
                throw new InvalidMoveException("Unknown move: " + move);
        }
        int row = from.getRow();
        int col = from.getCol();
        List<Position> cells = new ArrayList<>();
        for (int i = 0; i < steps && level.inside(row + dRow, col + dCol) && !level.isBlocked(row + dRow, col + dCol); i++) {
            row += dRow;
            col += dCol;
            cells.add(new Position(row, col));
        }
        int firstFall = cells.size();
        while (!level.isBlocked(row + 1, col)) {
            row++;
            if (row == level.getHeight()) {
                return new MoveResult(cells, firstFall, true, new Position(row, col));
            }
            cells.add(new Position(row, col));
        }
        return new MoveResult(cells, firstFall, false, new Position(row, col));
    }

    public static List<Position> neighbours(Level level, Position p) {
        List<Position> result = new ArrayList<>();
        for (String move : MOVES) {
            MoveResult r = apply(level, p, move);
            if (!r.isPit() && !r.getLanding().equals(p) && !result.contains(r.getLanding())) {
                result.add(r.getLanding());
            }
        }
        return result;
    }

    public static class MoveResult {
        private final List<Position> cells;
        private final int firstFall;
        private final boolean pit;
        private final Position landing;

        MoveResult(List<Position> cells, int firstFall, boolean pit, Position landing) {
            this.cells = cells;
            this.firstFall = firstFall;
            this.pit = pit;
            this.landing = landing;
        }

        public List<Position> getCells() {
            return cells;
        }

        public boolean enteredByFalling(int index) {
            return index >= firstFall;
        }

        public boolean isPit() {
            return pit;
        }

        public Position getLanding() {
            return landing;
        }
    }
}
