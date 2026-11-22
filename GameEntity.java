// Abstract classes: shared position, a static counter and an abstract symbol() for everything on the grid
package pathquest.model;

public abstract class GameEntity {
    private static int count;

    protected int row;
    protected int col;

    protected GameEntity(int row, int col) {
        this.row = row;
        this.col = col;
        count++;
    }

    public static int getCount() {
        return count;
    }

    public int getRow() {
        return row;
    }

    public int getCol() {
        return col;
    }

    public Position getPosition() {
        return new Position(row, col);
    }

    public abstract char symbol();

    @Override
    public String toString() {
        return symbol() + " at (" + row + "," + col + ")";
    }
}
