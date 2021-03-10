package game.playables;

import com.fasterxml.jackson.databind.ObjectMapper;
import game.enums.PieceType;
import game.enums.Side;

import java.util.Objects;

public class Move {

    private Piece piece;
    private Position newPosition;
    private Position attackPosition;

    public Move() { }

    public Move(Board board, Side side, String pieceName, String action, String tileName) throws Exception {
        // take command like: "medic to B4" or "sniper shoot H8"
        this.piece = new Piece(board.getPieces(side).get(PieceType.valueOf(pieceName.toUpperCase())));
        if (action.equalsIgnoreCase("to")) {
            this.newPosition = tileNameToPosition(tileName);
        }
        else if (action.equalsIgnoreCase("shoot")) {
            this.newPosition = new Position(this.piece.getPosition());
            this.attackPosition = tileNameToPosition(tileName);
        }
        else {
            throw new Exception(action.toLowerCase() + " is not a valid action");
        }
        System.out.println(new ObjectMapper().writeValueAsString(this));
    }

    public Move(Piece piece, Position newPosition, Position attackPosition) {
        this.piece = new Piece(piece);
        this.newPosition = newPosition == null ? null : new Position(newPosition);
        this.attackPosition = attackPosition == null ? null : new Position(attackPosition);
    }

    public Move(Piece piece, Position newPosition) {
        this(piece, newPosition, null);
    }

    public Move getRotated(Board board) {
        return new Move(
                piece.getSide() == Side.FRIENDLY ? board.getEnemyPieces().get(piece.getType()) : board.getFriendlyPieces().get(piece.getType()),
                newPosition == null ? null : newPosition.rotated(),
                attackPosition == null ? null : attackPosition.rotated()
        );
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

    private Position tileNameToPosition(String tileName) throws Exception {
        try {
            if (tileName.length() == 2) {
                int x = tileName.charAt(1) - '1';
                int y = tileName.toUpperCase().charAt(0) - 'A';
                return new Position(x, y);
            }
            else {
                throw new Exception("tile name must be 2 characters long");
            }
        }
        catch (Exception e) {
            throw new Exception(tileName + " is not a valid board position", e);
        }
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
