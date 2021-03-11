package moderator.listener;

import config.BotConfig;
import game.enums.Side;
import game.listener.GameBotListener;
import game.playables.Board;
import game.playables.Piece;
import game.playables.Position;

public class SampleBotListener extends GameBotListener {

    public SampleBotListener(BotConfig config) {
        super(config);
    }

    @Override
    protected int determineBoardValue(Board board) {
        return (int) (2 * board.getFriendlyPieces().values().stream().filter(Piece::isActive).count()
                - 2 * board.getEnemyPieces().values().stream().filter(Piece::isActive).count())
                - board.getFriendlyPieces().values().stream().filter(Piece::isActive).map((piece) -> distanceToPoint(board, piece)).reduce(0, Integer::sum)
                + 200 * (int) board.getCapturePoints().stream().filter((point) -> point == Side.FRIENDLY).count()
                + 5 * board.getCaptureForce();
    }

    private int distanceToPoint(Board board, Piece piece) {
        int distance = Integer.MIN_VALUE;
        for (Position position : board.getActiveCapturePoint()) {
            distance = Math.max(distance, Math.abs(position.getX() - piece.getPosition().getX()) + Math.abs(position.getY() - piece.getPosition().getY()));
        }
        return distance;
    }
}
