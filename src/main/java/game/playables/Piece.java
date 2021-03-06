package game.playables;

import game.enums.Direction;
import game.enums.PieceType;
import game.enums.Side;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;

public class Piece {

    private static final Logger logger = LoggerFactory.getLogger(Piece.class);

    private PieceType type;
    private Side side;
    private Position position;
    private boolean isActive;
    private int timeToRespawn;

    public Piece() { }

    public Piece(PieceType type, Side side) {
        this.type = type;
        this.side = side;
        this.position = null;
        this.isActive = false;
        this.timeToRespawn = 0;
    }

    public Piece(Piece piece) {
        this.type = piece.type;
        this.side = piece.side;
        this.position = piece.position == null ? null : new Position(piece.position);
        this.isActive = piece.isActive;
        this.timeToRespawn = piece.timeToRespawn;
    }

    public Set<Move> getPossibleMoves(Board board) {
        Set<Move> moves = new HashSet<>();
        switch (type) {
            case ASSASSIN:
                moves.addAll(getBasicMoves(board, 2));

                for (Piece piece : board.getPieces(side == Side.FRIENDLY ? Side.ENEMY : Side.FRIENDLY).values()) {
                    if (Math.abs(piece.getPosition().getX() - position.getX()) == Math.abs(piece.getPosition().getY() - position.getY())) {
                        moves.add(new Move(this, piece.getPosition()));
                    }
                }
                break;

            case BEAST:
                moves.addAll(getBasicMoves(board, 2));

                for (Piece piece : board.getPieces(side == Side.FRIENDLY ? Side.ENEMY : Side.FRIENDLY).values()) {
                    if (piece.getPosition().isInSquare(position, 2)) {
                        moves.add(new Move(this, piece.getPosition()));
                    }
                }
                break;

            case MEDIC:
                moves.addAll(getBasicMoves(board, Map.of(
                        Direction.N, 2,
                        Direction.S, 4,
                        Direction.W, 2,
                        Direction.E, 2,
                        Direction.NW, 2,
                        Direction.NE, 2,
                        Direction.SW, 2,
                        Direction.SE, 2
                )));
                break;

            case PALADIN:
                moves.addAll(getBasicMoves(board, Map.of( // TODO: somehow can move 2 in diagonal
                        Direction.N, 2,
                        Direction.S, 1,
                        Direction.W, 1,
                        Direction.E, 1,
                        Direction.NW, 1,
                        Direction.NE, 1,
                        Direction.SW, 0,
                        Direction.SE, 0
                )));
                break;

            case SCOUT:
                int moveDistance = board.isAnyActiveAdjacent(this) ? 1 : 3;
                moves.addAll(getBasicMoves(board, moveDistance));
                break;

            case SNIPER:
                moves.addAll(getBasicMoves(board, Map.of(
                        Direction.N, 1,
                        Direction.S, 1,
                        Direction.W, 1,
                        Direction.E, 1,
                        Direction.NW, 0,
                        Direction.NE, 0,
                        Direction.SW, 0,
                        Direction.SE, 0
                )));

                for (int i = 1; i <= 5; ++i) {
                    Position shootPosition = position.add(Direction.N.getPosition().scale(i));
                    Piece collision = board.getActivePieceAt(shootPosition.getX(), shootPosition.getY());
                    if (!board.isOnBoard(shootPosition) || (collision != null && (collision.getSide() == side || collision.getType() == PieceType.PALADIN))) {
                        break;
                    } else if (collision != null) {
                        moves.add(new Move(this, position, shootPosition));
                        break;
                    }
                }
                break;

            default:
        }
        return moves;
    }

    private Set<Move> getBasicMoves(Board board, int moveDistance) {
        return getBasicMoves(board, Map.of(
                Direction.N, moveDistance,
                Direction.S, moveDistance,
                Direction.W, moveDistance,
                Direction.E, moveDistance,
                Direction.NW, moveDistance,
                Direction.NE, moveDistance,
                Direction.SW, moveDistance,
                Direction.SE, moveDistance
        ));
    }

    private Set<Move> getBasicMoves(Board board, Map<Direction, Integer> immutableRanges) {
        Map<Direction, Integer> ranges = new HashMap<>(immutableRanges);
        Set<Move> moves = new HashSet<>();
        int i = 1;
        while (i <= ranges.values().stream().max(Integer::compare).get()) {
            for (Direction direction : ranges.keySet()) {
                if (i <= ranges.get(direction)) {
                    Position newPosition = position.add(direction.getPosition().scale(i));
                    Piece collision = board.getActivePieceAt(newPosition.getX(), newPosition.getY());
                    if (!board.isOnBoard(newPosition) || (collision != null && (collision.getSide() == side || (direction == Direction.N && collision.getType() == PieceType.PALADIN)))) {
                        ranges.put(direction, 0);
                    } else {
                        moves.add(new Move(this, newPosition));
                        if (collision != null) {
                            ranges.put(direction, 0);
                        }
                    }
                }
            }
            ++i;
        }
        return moves;
    }

    public PieceType getType() {
        return type;
    }

    public void setType(PieceType type) {
        this.type = type;
    }

    public Side getSide() {
        return side;
    }

    public void setSide(Side side) {
        this.side = side;
    }

    public Position getPosition() {
        return position;
    }

    public void setPosition(Position position) {
        this.position = position;
    }

    public boolean isActive() {
        return isActive;
    }

    public void setActive(boolean active) {
        isActive = active;
    }

    public int getTimeToRespawn() {
        return timeToRespawn;
    }

    public void setTimeToRespawn(int timeToRespawn) {
        this.timeToRespawn = timeToRespawn;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Piece piece = (Piece) o;
        return type == piece.type &&
                side == piece.side;
    }

    @Override
    public int hashCode() {
        return Objects.hash(type, side);
    }
}
