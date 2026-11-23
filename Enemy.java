package pathquest.model;

import pathquest.level.Level;

public class Enemy extends GameEntity implements Movable {
    private final int startRow;
    private final int startCol;
    private int direction = 1;

    public Enemy(int row, int col) {
        super(row, col);
        startRow = row;
        startCol = col;
    }

    @Override
    public void moveTo(int row, int col) {
        this.row = row;
        this.col = col;
    }

    @Override
    public char symbol() {
        return 'E';
    }

    public int getDirection() {
        return direction;
    }

    public void reverse() {
        direction = -direction;
    }

    public void step(Level level) {
        int next = col + direction;
        if (!level.inside(row, next) || level.isBlocked(row, next) || !level.isBlocked(row + 1, next)) {
            reverse();
        } else {
            col = next;
        }
    }

    public void reset() {
        moveTo(startRow, startCol);
        direction = 1;
    }
}
