// Polymorphism: Gem overrides the abstract symbol() from GameEntity
package pathquest.model;

public class Gem extends GameEntity {
    public Gem(int row, int col) {
        super(row, col);
    }

    public int getPoints() {
        return 50;
    }

    @Override
    public char symbol() {
        return '$';
    }
}
