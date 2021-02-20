package game;

public enum Direction {
    N(new Position(0, 1)),
    S(new Position(0, -1)),
    W(new Position(-1, 0)),
    E(new Position(1, 0)),
    NW(new Position(-1, 1)),
    NE(new Position(1, 1)),
    SW(new Position(-1, -1)),
    SE(new Position(1, -1));

    private final Position position;

    private Direction(Position position) {
        this.position = position;
    }

    public Position getPosition() {
        return position;
    }
}
