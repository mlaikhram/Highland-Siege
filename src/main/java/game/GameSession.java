package game;

import net.dv8tion.jda.api.entities.Guild;
import util.EmojiUtils;

import java.util.HashMap;
import java.util.Random;

public class GameSession {

    private long confirmationId;
    private Board board;
    private HashMap<Side, Long> players;
    private Side activeSide;

    public GameSession(long confirmationId, long friendlyId, long enemyId) {
        this.confirmationId = confirmationId;
        players = new HashMap<>();
        players.put(Side.FRIENDLY, friendlyId);
        players.put(Side.ENEMY, enemyId);
    }

    public boolean confirm(long enemyId) {
        if (enemyId == players.get(Side.ENEMY)) {
            board = new Board();
            activeSide = new Random().nextBoolean() ? Side.FRIENDLY : Side.ENEMY;
            return true;
        }
        else {
            return false;
        }
    }

    public boolean applyMove(long playerId, Move move) {
        // TODO: if it is the current player's turn and the move is valid, apply the move
        return false;
    }

    public String getBoard(Guild guild) {
        return EmojiUtils.generateBoardMessage(board, guild.getMemberById(players.get(Side.FRIENDLY)).getEffectiveName(), guild.getMemberById(players.get(Side.ENEMY)).getEffectiveName());
    }
}
