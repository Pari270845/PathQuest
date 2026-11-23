package pathquest.model;

public class Coin extends GameEntity {
    public Coin(int row, int col) {
        super(row, col);
    }

    public int getPoints() {
        return 10;
    }

    @Override
    public char symbol() {
        return 'o';
    }
}
