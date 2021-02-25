package moderator.util;

import game.enums.Phase;
import game.enums.PieceType;
import game.enums.Side;
import game.playables.Board;
import game.playables.Piece;

import java.util.Map;

public class EmojiUtils {

    public static final String CONFIRM = "\u2705";
    public static final String CANCEL = "\u274E";

    public static final String RED_SQUARE = "\uD83D\uDFE5";
    public static final String WHITE_SQUARE = "\u2B1C";
    public static final String BLUE_SQUARE = "\uD83D\uDFE6";
    public static final String YELLOW_SQUARE = "\uD83D\uDFE8";

    private static final String[] NUMBER_EMOJIS = {
            "\u0031\uFE0F\u20E3",
            "\u0032\uFE0F\u20E3",
            "\u0033\uFE0F\u20E3",
            "\u0034\uFE0F\u20E3",
            "\u0035\uFE0F\u20E3",
            "\u0036\uFE0F\u20E3",
            "\u0037\uFE0F\u20E3",
            "\u0038\uFE0F\u20E3"
    };

    private static final Map<PieceType, Map<Side, String>> PIECE_EMOJIS = Map.of(
            PieceType.ASSASSIN, Map.of(
                    Side.FRIENDLY, "\uD83E\uDD38\u200D\u2642\uFE0F",
                    Side.ENEMY, "\uD83E\uDD38\u200D\u2640\uFE0F"
            ),
            PieceType.BEAST, Map.of(
                    Side.FRIENDLY, "\uD83D\uDC3A",
                    Side.ENEMY, "\uD83E\uDD8A"
            ),
            PieceType.MEDIC, Map.of(
                    Side.FRIENDLY, "\uD83E\uDDD9\u200D\u2642\uFE0F",
                    Side.ENEMY, "\uD83E\uDDD9"
            ),
            PieceType.PALADIN, Map.of(
                    Side.FRIENDLY, "\uD83D\uDE45\u200D\u2642\uFE0F",
                    Side.ENEMY, "\uD83D\uDE45"
            ),
            PieceType.SCOUT, Map.of(
                    Side.FRIENDLY, "\uD83C\uDFC3\u200D\u2642\uFE0F",
                    Side.ENEMY, "\uD83C\uDFC3\u200D\u2640\uFE0F"
            ),
            PieceType.SNIPER, Map.of(
                    Side.FRIENDLY, "\uD83E\uDD3E\u200D\u2642\uFE0F",
                    Side.ENEMY, "\uD83E\uDD3E\u200D\u2640\uFE0F"
            )
    );

    private static final Map<Phase, String> PHASE_EMOJIS = Map.of(
            Phase.SETUP, "\uD83D\uDCE5",
            Phase.SIEGE, "\uD83C\uDFC1",
            Phase.SURVIVE, "\u2620\uFE0F"
    );

    public static String generateBoardMessage(Board board, String bluePlayerName, String redPlayerName, Side activeSide) {
        String[][] boardArr = generateEmptyBoardEmojis();
        // place capture points
        for (int i = 0; i < board.getCapturePoints().size(); ++i) {
            for (int j = 0; j < Board.CAPTURE_POINT_POSITIONS.get(i).size(); ++j) {
                String square;
                switch (board.getCapturePoints().get(i)) {
                    case FRIENDLY:
                        square = BLUE_SQUARE;
                        break;

                    case ENEMY:
                        square = RED_SQUARE;
                        break;

                    default:
                        square = WHITE_SQUARE;
                        break;
                }
                boardArr[Board.CAPTURE_POINT_POSITIONS.get(i).get(j).getX()][Board.CAPTURE_POINT_POSITIONS.get(i).get(j).getY()] = square;
            }
        }
        // TODO: place dead pieces
        // TODO: use custom pieces
        // place alive pieces
        for (Side side : Side.values()) {
            for (Piece piece : board.getPieces(side).values()) {
                if (piece.isActive() && piece.getPosition() != null) {
                    boardArr[piece.getPosition().getX()][piece.getPosition().getY()] = PIECE_EMOJIS.get(piece.getType()).get(side);
                }
            }
        }
        // TODO: place previous move tiles
        return generateGameBoardMessage(bluePlayerName, redPlayerName, boardArr, board.getPhase(), board.getTurnCount(), activeSide);
    }

    private static String[][] generateEmptyBoardEmojis() {
        String[][] board = new String[Board.WIDTH][Board.HEIGHT];
        for (int x = 0; x < Board.WIDTH; ++x) {
            for (int y = 0; y < Board.HEIGHT; ++y) {
                board[x][y] = (x % 2 == y % 2) ? "\u2B1B" : "\uD83D\uDFEB";
            }
        }
        return board;
    }

    private static String generateGameBoardMessage(String bluePlayer, String redPlayer, String[][] board, Phase phase, int turnCount, Side activeSide) {
        StringBuilder message = new StringBuilder();
        message.append('`');
        message.append((turnCount > 9 ? "" : " ") + turnCount);
        message.append(" ");
        message.append(bluePlayer + (activeSide == Side.FRIENDLY ? "*" : ""));
        message.append(" ".repeat(Board.BANNER_SIZE - bluePlayer.length() - redPlayer.length() - 1));
        message.append((activeSide == Side.ENEMY ? "*" : "") + redPlayer);
        message.append("   `\n");
        message.append(PHASE_EMOJIS.get(phase));
        message.append(" \uD83C\uDDE6 \uD83C\uDDE7 \uD83C\uDDE8 \uD83C\uDDE9 \uD83C\uDDEA \uD83C\uDDEB \uD83C\uDDEC \uD83C\uDDED \uD83C\uDDEE \uD83C\uDDEF \uD83C\uDDF0 \uD83C\uDDF1 \uD83C\uDDF2 \uD83C\uDDF3 \uD83C\uDDF4 \uD83C\uDDF5 \uD83C\uDDF6 \uD83C\uDDF7 ");
        message.append(PHASE_EMOJIS.get(phase));
        message.append("\n");
        for (int x = 0; x < board.length; ++x) {
            for (int y = -1; y <= board[0].length; ++y) {
                message.append(((y == -1 || y == board[0].length) ? NUMBER_EMOJIS[x] : board[x][y]) + " ");
            }
            message.append("\n");
        }
        return message.toString();
    }
}
