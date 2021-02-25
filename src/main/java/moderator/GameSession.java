package moderator;

import com.fasterxml.jackson.databind.ObjectMapper;
import game.enums.Side;
import game.playables.Board;
import game.playables.Move;
import net.dv8tion.jda.api.entities.Guild;
import moderator.util.EmojiUtils;

import java.util.Collection;
import java.util.HashMap;
import java.util.Random;

public class GameSession {

    private long sessionMessageId;
    private Board board;
    private HashMap<Side, Player> players;
    private Side activeSide;
    private boolean isOver;

    public GameSession(Player friendlyPlayer, Player enemyPlayer) {
        players = new HashMap<>();
        players.put(Side.FRIENDLY, friendlyPlayer);
        players.put(Side.ENEMY, enemyPlayer);
        activeSide = Side.NEUTRAL;
        isOver = false;
    }

    public void begin() {
        board = new Board();
        activeSide = new Random().nextBoolean() ? Side.FRIENDLY : Side.ENEMY;
    }

    public void end() {
        isOver = true;
    }

    public boolean tryMove(long playerId, Move move) throws Exception {
        // TODO: if it is the current player's turn and the move is valid, apply the move
        System.out.println(new ObjectMapper().writeValueAsString(board.getPossibleMoves(activeSide)));
        if (getActivePlayer().getId() == playerId && board.getPossibleMoves(activeSide).contains(move)) {
            board.applyMove(move);
            activeSide = activeSide == Side.FRIENDLY ? Side.ENEMY : Side.FRIENDLY;
            return true;
        }
        return false;
    }

    public Side getActiveSide() {
        return activeSide;
    }

    public Board getBoard() {
        return board;
    }

    public String getBoardAsEmojis(Guild guild) {
        return EmojiUtils.generateBoardMessage(board, guild.getMemberById(players.get(Side.FRIENDLY).getId()).getEffectiveName(), guild.getMemberById(players.get(Side.ENEMY).getId()).getEffectiveName(), activeSide);
    }

    public String getBoardAsJson() {
        return board.toString(activeSide == Side.ENEMY);
    }

    public boolean isOver() {
        return isOver;
    }

    public Collection<Player> getPlayers() {
        return players.values();
    }

    public Player getActivePlayer() {
        return players.get(activeSide);
    }

    public long getSessionMessageId() {
        return sessionMessageId;
    }

    public void setSessionMessageId(long sessionMessageId) {
        this.sessionMessageId = sessionMessageId;
    }
}
