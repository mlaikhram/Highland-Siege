package game;

import java.util.Objects;

public class Move {

    private Piece piece;
    private Position newPosition;
    private Position attackPosition;

    public Move(Piece piece, Position newPosition, Position attackPosition) {
        this.piece = piece;
        this.newPosition = newPosition;
        this.attackPosition = attackPosition;
    }

    public Move(Piece piece, Position newPosition) {
        this(piece, newPosition, null);
    }

    public Piece getPiece() {
        return piece;
    }

    public void setPiece(Piece piece) {
        this.piece = piece;
    }

    public Position getNewPosition() {
        return newPosition;
    }

    public void setNewPosition(Position newPosition) {
        this.newPosition = newPosition;
    }

    public Position getAttackPosition() {
        return attackPosition;
    }

    public void setAttackPosition(Position attackPosition) {
        this.attackPosition = attackPosition;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Move move = (Move) o;
        return Objects.equals(piece, move.piece) &&
                Objects.equals(newPosition, move.newPosition) &&
                Objects.equals(attackPosition, move.attackPosition);
    }

    @Override
    public int hashCode() {
        return Objects.hash(piece, newPosition, attackPosition);
    }
}
