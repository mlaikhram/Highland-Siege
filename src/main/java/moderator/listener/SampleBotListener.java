package moderator.listener;

import config.BotConfig;
import game.listener.GameBotListener;
import game.playables.Board;
import game.playables.Piece;

public class SampleBotListener extends GameBotListener {

    public SampleBotListener(BotConfig config) {
        super(config);
    }

    @Override
    protected int determineBoardValue(Board board) {
        return (int) (board.getFriendlyPieces().values().stream().filter(Piece::isActive).count()
                - board.getEnemyPieces().values().stream().filter(Piece::isActive).count())
                + board.getCaptureForce();
    }
}
