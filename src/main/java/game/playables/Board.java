package game.playables;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import game.enums.Phase;
import game.enums.PieceType;
import game.enums.Side;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class Board {

    private static final Logger logger = LoggerFactory.getLogger(Board.class);

    public static final int WIDTH = 8;
    public static final int HEIGHT = 18;
    public static final int BANNER_SIZE = 61;
    private static final Position DOUBLED_CENTER = new Position(7, 17); // TODO: remove since it is just WIDTH-1 and HEIGHT-1

    public static final List<List<Position>> CAPTURE_POINT_POSITIONS = Arrays.asList(
            Arrays.asList(
                    new Position(3, 0),
                    new Position(4, 0),
                    new Position(3, 1),
                    new Position(4, 1)
            ),
            Arrays.asList(
                    new Position(3, 4),
                    new Position(4, 4),
                    new Position(3, 5),
                    new Position(4, 5)
            ),
            Arrays.asList(
                    new Position(3, 8),
                    new Position(4, 8),
                    new Position(3, 9),
                    new Position(4, 9)
            ),
            Arrays.asList(
                    new Position(3, 12),
                    new Position(4, 12),
                    new Position(3, 13),
                    new Position(4, 13)
            ),
            Arrays.asList(
                    new Position(3, 16),
                    new Position(4, 16),
                    new Position(3, 17),
                    new Position(4, 17)
            )
    );

    private HashMap<PieceType, Piece> friendlyPieces;
    private HashMap<PieceType, Piece> enemyPieces;
    private List<Side> capturePoints;
    private int captureForce;
    private Phase phase;
    private int turnCount;

    public Board() {
        this.friendlyPieces = initializePieces(Side.FRIENDLY);
        this.enemyPieces = initializePieces(Side.ENEMY);
        this.capturePoints = Arrays.asList(Side.FRIENDLY, Side.FRIENDLY, Side.NEUTRAL, Side.ENEMY, Side.ENEMY);
        this.captureForce = 0;
        this.phase = Phase.SETUP;
        this.turnCount = 0;
    }

    public Board rotated() {
        // rotate pieces on board [P(x,y) --> P'(-x+2x_O,-y+2y_O)] -3,-1 -> 1,-1 about -1,-1
        // flip Enemy <-> Friendly
        // flip capturePoints and captureForce
        Board rotatedBoard = new Board();
        rotatedBoard.phase = phase;
        rotatedBoard.turnCount = turnCount;
        for (PieceType pieceType : PieceType.values()) {
            rotatedBoard.friendlyPieces.get(pieceType).setActive(enemyPieces.get(pieceType).isActive());
            rotatedBoard.friendlyPieces.get(pieceType).setPosition(enemyPieces.get(pieceType).getPosition() == null ? null : enemyPieces.get(pieceType).getPosition().rotated());

            rotatedBoard.enemyPieces.get(pieceType).setActive(friendlyPieces.get(pieceType).isActive());
            rotatedBoard.enemyPieces.get(pieceType).setPosition(friendlyPieces.get(pieceType).getPosition() == null ? null : friendlyPieces.get(pieceType).getPosition().rotated());
        }
        rotatedBoard.captureForce = -captureForce;
        for (int i = 0; i < capturePoints.size(); ++i) {
            rotatedBoard.capturePoints.set(i, capturePoints.get(capturePoints.size() - 1 - i));
        }
        return rotatedBoard;
    }

    public Set<Move> getPossibleMoves(Side side) {
        if (side == side.ENEMY) {
            Board rotatedBoard = rotated();
            return rotatedBoard.getPossibleMoves(side.FRIENDLY).stream().map(move ->  move.getRotated(this)).collect(Collectors.toSet());
        }
        else if (side == side.FRIENDLY) {
            Set<Move> moves = new HashSet<>();
            if (phase == Phase.SETUP) {
                // player should only be allowed to add pieces to the board until setup is over
                for (Piece piece : friendlyPieces.values()) {
                    if (piece.getPosition() == null) {
                        for (int x = 0; x < WIDTH; ++x) {
                            for (int y = 0; y < 2; ++y) {
                                Position spawnPosition = new Position(x, y);
                                if (isValidSpawnPosition(spawnPosition))
                                {
                                    moves.add(new Move(piece, new Position(x, y)));
                                }
                            }
                        }
                    }
                }
            }
            else {
                for (Piece piece: friendlyPieces.values()) {
                    if (!piece.isActive()) {
                        // allow respawn if in siege phase
                        if (phase == Phase.SIEGE) {
                            int spawnCapturePointIndex = getBasePoint();
                            for (Position position : CAPTURE_POINT_POSITIONS.get(spawnCapturePointIndex)) {
                                if (isValidSpawnPosition(position)) {
                                    moves.add(new Move(piece, position));
                                }
                            }
//                            for entire 2 rows of spawn point
//                            for (int x = 0; x < WIDTH; ++x) {
//                                for (int y = 0; y < 2; ++y) {
//                                    Position spawnPosition = new Position(x, spawnCapturePoint * 4 + y);
//                                    if (isValidSpawnPosition(spawnPosition)) {
//                                        moves.add(new Move(piece, spawnPosition));
//                                    }
//                                }
//                            }
                        }
                        // allow respawn if near medic
                        if (friendlyPieces.get(PieceType.MEDIC).isActive() && piece.getPosition().isAdjacent(friendlyPieces.get(PieceType.MEDIC).getPosition())) {
                            for (int x = friendlyPieces.get(PieceType.MEDIC).getPosition().getX() - 1; x <= friendlyPieces.get(PieceType.MEDIC).getPosition().getX() + 1; ++x) {
                                for (int y = friendlyPieces.get(PieceType.MEDIC).getPosition().getY() - 1; y <= friendlyPieces.get(PieceType.MEDIC).getPosition().getY() + 1; ++y) {
                                    Position spawnPosition = new Position(x, y);
                                    if (isValidSpawnPosition(spawnPosition)) {
                                        moves.add(new Move(piece, spawnPosition));
                                    }
                                }
                            }
                        }
                    }
                    else {
                        moves.addAll(piece.getPossibleMoves(this));
                    }
                }
            }
            return moves; // TODO: shuffle moves for variance?
        }
        else {
            return new HashSet<>();
        }
    }

    public void applyMove(Move move) {
        move.getPiece().setActive(true);
        if (!move.getNewPosition().equals(move.getPiece().getPosition())) {
            Piece collision = getActivePieceAt(move.getNewPosition().getX(), move.getNewPosition().getY());
            if (collision != null) {
                collision.setActive(false);
            }
            move.getPiece().setPosition(move.getNewPosition());
        }
        if (move.getAttackPosition() != null) {
            Piece victim = getActivePieceAt(move.getAttackPosition().getX(), move.getAttackPosition().getY());
            if (victim != null) {
                victim.setActive(false);
            }
        }
        if (phase == Phase.SETUP && !Stream.concat(friendlyPieces.values().stream(), enemyPieces.values().stream()).anyMatch((piece) -> piece.getPosition() == null)) {
            phase = Phase.SIEGE;
        }
        if (phase != Phase.SETUP) {
            ++turnCount;
        }
    }

    public Piece getActivePieceAt(int x, int y) {
        return Stream.concat(friendlyPieces.values().stream(), enemyPieces.values().stream()).filter((piece) -> piece.getPosition() != null && piece.isActive() && piece.getPosition().getX() == x && piece.getPosition().getY() == y).findFirst().orElse(null);
    }

    public Map<PieceType, Piece> getPieces(Side side) {
        switch (side) {
            case FRIENDLY:
                return friendlyPieces;

            case ENEMY:
                return enemyPieces;

            default:
                return new HashMap<>();
        }
    }

    @Override
    public String toString() {
        return toString(false);
    }

    public String toString(boolean rotate) {
        try {
            return new ObjectMapper().writeValueAsString(rotate ? rotated() : this);
        }
        catch (JsonProcessingException e) {
            logger.info(e.getMessage());
            return "{}";
        }
    }

    private HashMap<PieceType, Piece> initializePieces(Side side) {
        HashMap<PieceType, Piece> pieces = new HashMap<>();
        for (PieceType type : PieceType.values()) {
            pieces.put(type, new Piece(type, side));
        }
        return pieces;
    }

    // all below for friendly

    private int getBasePoint() {
        for (int i = capturePoints.size(); i <= 0; --i) {
            if (capturePoints.get(i) == Side.FRIENDLY) {
                return i;
            }
        }
        return 0;
    }

    public boolean isOnBoard(Position position) {
        return position.getX() >= 0 && position.getX() < WIDTH && position.getY() >= 0 && position.getY() < HEIGHT;
    }

    public boolean isAnyActiveAdjacent(Piece targetPiece) {
        return Stream.concat(friendlyPieces.values().stream(), enemyPieces.values().stream()).anyMatch((piece) -> piece != targetPiece && piece.isActive() && piece.getPosition().isAdjacent(targetPiece.getPosition()));
    }

    private boolean isValidSpawnPosition(Position position) {
         return isOnBoard(position) && !friendlyPieces.values().stream().anyMatch((collisionPiece) -> collisionPiece.isActive() && collisionPiece.getPosition().equals(position));
    }

    public HashMap<PieceType, Piece> getFriendlyPieces() {
        return friendlyPieces;
    }

    public void setFriendlyPieces(HashMap<PieceType, Piece> friendlyPieces) {
        this.friendlyPieces = friendlyPieces;
    }

    public HashMap<PieceType, Piece> getEnemyPieces() {
        return enemyPieces;
    }

    public void setEnemyPieces(HashMap<PieceType, Piece> enemyPieces) {
        this.enemyPieces = enemyPieces;
    }

    public List<Side> getCapturePoints() {
        return capturePoints;
    }

    public void setCapturePoints(List<Side> capturePoints) {
        this.capturePoints = capturePoints;
    }

    public int getCaptureForce() {
        return captureForce;
    }

    public void setCaptureForce(int captureForce) {
        this.captureForce = captureForce;
    }

    public Phase getPhase() {
        return phase;
    }

    public void setPhase(Phase phase) {
        this.phase = phase;
    }

    public int getTurnCount() {
        return turnCount;
    }

    public void setTurnCount(int turnCount) {
        this.turnCount = turnCount;
    }

    public static void main(String[] args) throws JsonProcessingException {
        ObjectMapper mapper = new ObjectMapper();
        Board board = new Board();

        board.applyMove(new Move(board.friendlyPieces.get(PieceType.SNIPER), new Position(5, 1)));
        board.applyMove(new Move(board.friendlyPieces.get(PieceType.ASSASSIN), new Position(6, 1)));
        board.applyMove(new Move(board.friendlyPieces.get(PieceType.MEDIC), new Position(7, 1)));
        board.applyMove(new Move(board.friendlyPieces.get(PieceType.PALADIN), new Position(0, 0)));
        board.applyMove(new Move(board.friendlyPieces.get(PieceType.SCOUT), new Position(2, 0)));
        board.applyMove(new Move(board.friendlyPieces.get(PieceType.BEAST), new Position(7, 0)));

        board.applyMove(new Move(board.enemyPieces.get(PieceType.SNIPER), new Position(5, 17)));
        board.applyMove(new Move(board.enemyPieces.get(PieceType.ASSASSIN), new Position(5, 5)));
        board.applyMove(new Move(board.enemyPieces.get(PieceType.MEDIC), new Position(6, 2)));
        board.applyMove(new Move(board.enemyPieces.get(PieceType.PALADIN), new Position(2, 5)));
        board.applyMove(new Move(board.enemyPieces.get(PieceType.SCOUT), new Position(2, 4)));
        board.applyMove(new Move(board.enemyPieces.get(PieceType.BEAST), new Position(7, 16)));

        logger.info(mapper.writeValueAsString(board.friendlyPieces));
        logger.info(board.toString());
        logger.info(mapper.writeValueAsString(board.getPossibleMoves(Side.FRIENDLY).contains(new Move(board.friendlyPieces.get(PieceType.BEAST), new Position(6, 2)))));
    }
}
