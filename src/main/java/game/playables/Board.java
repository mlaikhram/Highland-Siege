package game.playables;

import com.fasterxml.jackson.annotation.JsonIgnore;
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

    private final int CAPTURE_THRESHOLD = 15;
    private final int RESPAWN_DELAY = 1;

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

    public Board(Board board) {
        this.friendlyPieces = new HashMap<>();
        this.enemyPieces = new HashMap<>();
        for (PieceType pieceType : PieceType.values()) {
            this.friendlyPieces.put(pieceType, new Piece(board.friendlyPieces.get(pieceType)));
            this.enemyPieces.put(pieceType, new Piece(board.enemyPieces.get(pieceType)));
        }
        this.capturePoints = new LinkedList<>(board.capturePoints);
        this.captureForce = board.captureForce;
        this.phase = board.phase;
        this.turnCount = board.turnCount;
    }

    public Board rotated() {
        // rotate pieces on board [P(x,y) --> P'(-x+2x_O,-y+2y_O)] -3,-1 -> 1,-1 about -1,-1
        // flip Enemy <-> Friendly
        // flip captureForce
        Board rotatedBoard = new Board();
        rotatedBoard.phase = phase;
        rotatedBoard.turnCount = turnCount;
        for (PieceType pieceType : PieceType.values()) {
            rotatedBoard.friendlyPieces.get(pieceType).setActive(enemyPieces.get(pieceType).isActive());
            rotatedBoard.friendlyPieces.get(pieceType).setPosition(enemyPieces.get(pieceType).getPosition() == null ? null : enemyPieces.get(pieceType).getPosition().rotated());
            rotatedBoard.friendlyPieces.get(pieceType).setTimeToRespawn(enemyPieces.get(pieceType).getTimeToRespawn());

            rotatedBoard.enemyPieces.get(pieceType).setActive(friendlyPieces.get(pieceType).isActive());
            rotatedBoard.enemyPieces.get(pieceType).setPosition(friendlyPieces.get(pieceType).getPosition() == null ? null : friendlyPieces.get(pieceType).getPosition().rotated());
            rotatedBoard.enemyPieces.get(pieceType).setTimeToRespawn(friendlyPieces.get(pieceType).getTimeToRespawn());

        }
        rotatedBoard.captureForce = -captureForce;
        for (int i = 0; i < capturePoints.size(); ++i) {
            rotatedBoard.capturePoints.set(i, capturePoints.get(capturePoints.size() - 1 - i).reverse());
        }
        return rotatedBoard;
    }

    @JsonIgnore
    public Set<Move> getPossibleMoves(Side side) {
        if (side == side.ENEMY) {
            Board rotatedBoard = rotated(); // TODO: this could maybe be optimized to avoid a double rotation
            return rotatedBoard.getPossibleMoves(side.FRIENDLY).stream().map(move ->  move.getRotated(this)).collect(Collectors.toSet());
        }
        else if (getVictorySide() != Side.NEUTRAL) {
            return new HashSet<>();
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
            else if (phase != Phase.END) {
                for (Piece piece: friendlyPieces.values()) {
                    if (!piece.isActive() && piece.getTimeToRespawn() <= 0) {
                        // allow respawn if in siege phase
                        if (phase == Phase.SIEGE) {
                            int spawnCapturePointIndex = getBasePoint();
                            if (spawnCapturePointIndex >= 0) {
                                for (Position position : CAPTURE_POINT_POSITIONS.get(spawnCapturePointIndex)) {
                                    if (isValidSpawnPosition(position)) {
                                        moves.add(new Move(piece, position));
                                    }
                                }
                            }
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
                    else if (piece.isActive()) {
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
        Piece boardPiece = getPieces(move.getPiece().getSide()).get(move.getPiece().getType());
        if (!boardPiece.isActive() || !move.getNewPosition().equals(boardPiece.getPosition())) {
            Piece collision = getActivePieceAt(move.getNewPosition().getX(), move.getNewPosition().getY());
            if (collision != null) {
                collision.setActive(false);
                collision.setTimeToRespawn(RESPAWN_DELAY + 1);
            }
            boardPiece.setPosition(move.getNewPosition());
        }
        boardPiece.setActive(true);
        if (move.getAttackPosition() != null) {
            Piece victim = getActivePieceAt(move.getAttackPosition().getX(), move.getAttackPosition().getY());
            if (victim != null) {
                victim.setActive(false);
                victim.setTimeToRespawn(RESPAWN_DELAY + 1);
            }
        }
        if (phase == Phase.SETUP && !Stream.concat(friendlyPieces.values().stream(), enemyPieces.values().stream()).anyMatch((piece) -> piece.getPosition() == null)) {
            phase = Phase.SIEGE;
        }
        if (phase != Phase.SETUP) {
            updateCaptureForce();
            Side winner = getVictorySide();
            if (winner != Side.NEUTRAL) {
                phase = Phase.END;
            }
            else {
                ++turnCount;
                Stream.concat(friendlyPieces.values().stream(), enemyPieces.values().stream()).filter(piece -> !piece.isActive() && piece.getTimeToRespawn() > 0).forEach(piece -> piece.setTimeToRespawn(piece.getTimeToRespawn() - 1));
            }
        }
    }

    @JsonIgnore
    public Side getVictorySide() {
        if (!capturePoints.contains(Side.FRIENDLY) && (capturePoints.get(0) == Side.ENEMY || !friendlyPieces.values().stream().anyMatch(piece -> piece.isActive()))) {
            return Side.ENEMY;
        }
        else if (!capturePoints.contains(Side.ENEMY) && (capturePoints.get(capturePoints.size() - 1) == Side.FRIENDLY || !enemyPieces.values().stream().anyMatch(piece -> piece.isActive()))) {
            return Side.FRIENDLY;
        }
        return Side.NEUTRAL;
    }

    private void updateCaptureForce() {
        int activeCapturePoint = capturePoints.indexOf(Side.NEUTRAL);
        int friendlyCount = 0;
        int enemyCount = 0;
        for (Position position : CAPTURE_POINT_POSITIONS.get(activeCapturePoint)) {
            Piece capturingPiece = getActivePieceAt(position.getX(), position.getY());
            if (capturingPiece != null) {
                if (capturingPiece.getSide() == Side.FRIENDLY) {
                    ++friendlyCount;
                }
                else if (capturingPiece.getSide() == Side.ENEMY) {
                    ++enemyCount;
                }
            }
        }
        if (friendlyCount > 0 && enemyCount == 0) {
            captureForce += (captureForce >= 0 ? 1 : 2) * friendlyCount;
            if (captureForce >= CAPTURE_THRESHOLD) {
                capturePoints.set(activeCapturePoint, Side.FRIENDLY);
                if (activeCapturePoint < capturePoints.size() - 1) {
                    capturePoints.set(activeCapturePoint + 1, Side.NEUTRAL);
                    captureForce = 0;
                }
            }
        }
        else if (enemyCount > 0 && friendlyCount == 0) {
            captureForce -= (captureForce <= 0 ? 1 : 2) * enemyCount;
            if (-captureForce >= CAPTURE_THRESHOLD) {
                capturePoints.set(activeCapturePoint, Side.ENEMY);
                if (activeCapturePoint > 0) {
                    capturePoints.set(activeCapturePoint - 1, Side.NEUTRAL);
                    captureForce = 0;
                }
            }
        }
    }

    @JsonIgnore
    public Piece getActivePieceAt(int x, int y) {
        return Stream.concat(friendlyPieces.values().stream(), enemyPieces.values().stream()).filter((piece) -> piece.getPosition() != null && piece.isActive() && piece.getPosition().getX() == x && piece.getPosition().getY() == y).findFirst().orElse(null);
    }

    @JsonIgnore
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
    @JsonIgnore
    private int getBasePoint() {
        for (int i = capturePoints.size() - 1; i >= 0; --i) {
            if (capturePoints.get(i) == Side.FRIENDLY) {
                return i;
            }
        }
        return -1;
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
}
