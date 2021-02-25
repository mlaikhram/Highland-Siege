package game.playables;

import java.util.Objects;

public class Position {

    private int x;
    private int y;

    public Position(int x, int y) {
        this.x = x;
        this.y = y;
    }

    public Position rotated() {
        return new Position(Board.WIDTH - 1 - x, Board.HEIGHT - 1 - y);
    }

    public int getX() {
        return x;
    }

    public void setX(int x) {
        this.x = x;
    }

    public int getY() {
        return y;
    }

    public void setY(int y) {
        this.y = y;
    }

    public boolean isAdjacent(Position position) {
        return isInSquare(position, 1);
    }

    public boolean isInSquare(Position center, int distanceFromCenter) {
        return Math.abs(x - center.x) <= distanceFromCenter && Math.abs(y - center.y) <= distanceFromCenter;
    }

    public Position add(Position position) {
        return new Position(x + position.x, y + position.y);
    }

    public Position scale(int scale) {
        return new Position(scale * x, scale * y);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Position position = (Position) o;
        return x == position.x &&
                y == position.y;
    }

    @Override
    public int hashCode() {
        return Objects.hash(x, y);
    }
}
