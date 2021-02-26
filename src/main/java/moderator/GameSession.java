package moderator;

import com.fasterxml.jackson.databind.ObjectMapper;
import game.enums.Phase;
import game.enums.Side;
import game.playables.Board;
import game.playables.Move;
import net.dv8tion.jda.api.entities.Guild;
import moderator.util.EmojiUtils;

import java.util.Collection;
import java.util.HashMap;
import java.util.Random;

public class GameSession {

    private long boardMessageId;
    private long statusMessageId;
    private Board board;
    private HashMap<Side, Player> players;
    private Side activeSide;

    public GameSession(Player friendlyPlayer, Player enemyPlayer) {
        players = new HashMap<>();
        players.put(Side.FRIENDLY, friendlyPlayer);
        players.put(Side.ENEMY, enemyPlayer);
        activeSide = Side.NEUTRAL;
    }

    public void begin() {
        board = new Board();
        activeSide = new Random().nextBoolean() ? Side.FRIENDLY : Side.ENEMY;
    }

    // TODO: convert to surrender button
    public void end() {

    }

    public boolean tryMove(long playerId, Move move) throws Exception {
        System.out.println(new ObjectMapper().writeValueAsString(board.getPossibleMoves(activeSide)));
        if (getActivePlayer().getId() == playerId && board.getPossibleMoves(activeSide).contains(move)) {
            board.applyMove(move);
            if (board.getVictorySide() == Side.NEUTRAL) {
                activeSide = activeSide == Side.FRIENDLY ? Side.ENEMY : Side.FRIENDLY;
            }
            else {
                activeSide = Side.NEUTRAL;
            }
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

    public String getStatusAsEmojis() {
        return EmojiUtils.generateStatusMessage(board);
    }

    public String getBoardAsJson() {
        return board.toString(activeSide == Side.ENEMY);
    }

    public boolean isOver() {
        return board.getPhase() == Phase.END;
    }

    public Collection<Player> getPlayers() {
        return players.values();
    }

    public Player getActivePlayer() {
        return players.get(activeSide);
    }

    public long getBoardMessageId() {
        return boardMessageId;
    }

    public void setBoardMessageId(long boardMessageId) {
        this.boardMessageId = boardMessageId;
    }

    public long getStatusMessageId() {
        return statusMessageId;
    }

    public void setStatusMessageId(long statusMessageId) {
        this.statusMessageId = statusMessageId;
    }
}
