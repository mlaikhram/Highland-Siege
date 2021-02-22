package listener;

import config.YmlConfig;
import game.*;
import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.entities.*;
import net.dv8tion.jda.api.events.message.MessageReceivedEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import util.*;

import javax.annotation.Nonnull;

public class GameModeratorListener extends ListenerAdapter {

    private static final Logger logger = LoggerFactory.getLogger(GameModeratorListener.class);

    private JDA jda;
    private YmlConfig config;

    public GameModeratorListener(YmlConfig config) {
        this.config = config;
    }

    public void setJda(JDA jda) {
        this.jda = jda;
    }

    @Override
    public void onMessageReceived(@Nonnull MessageReceivedEvent event) {
        if (event.getAuthor().getIdLong() == jda.getSelfUser().getIdLong()) {
            return;
        }
        MessageChannel sourceChannel = event.getChannel();

        Board board = new Board();

        board.applyMove(new Move(board.getPieces(Side.FRIENDLY).get(PieceType.SNIPER), new Position(5, 1)));
        board.applyMove(new Move(board.getPieces(Side.FRIENDLY).get(PieceType.ASSASSIN), new Position(6, 1)));
        board.applyMove(new Move(board.getPieces(Side.FRIENDLY).get(PieceType.MEDIC), new Position(7, 1)));
        board.applyMove(new Move(board.getPieces(Side.FRIENDLY).get(PieceType.PALADIN), new Position(0, 0)));
        board.applyMove(new Move(board.getPieces(Side.FRIENDLY).get(PieceType.SCOUT), new Position(2, 0)));
        board.applyMove(new Move(board.getPieces(Side.FRIENDLY).get(PieceType.BEAST), new Position(7, 0)));

        board.applyMove(new Move(board.getPieces(Side.ENEMY).get(PieceType.SNIPER), new Position(5, 17)));
        board.applyMove(new Move(board.getPieces(Side.ENEMY).get(PieceType.ASSASSIN), new Position(5, 5)));
        board.applyMove(new Move(board.getPieces(Side.ENEMY).get(PieceType.MEDIC), new Position(6, 2)));
        board.applyMove(new Move(board.getPieces(Side.ENEMY).get(PieceType.PALADIN), new Position(2, 5)));
        board.applyMove(new Move(board.getPieces(Side.ENEMY).get(PieceType.SCOUT), new Position(2, 4)));
        board.applyMove(new Move(board.getPieces(Side.ENEMY).get(PieceType.BEAST), new Position(7, 16)));

        try {
            board.applyMove(new Move(board, Side.FRIENDLY, "Sniper", "to", "J4"));
        }
        catch (Exception e) {
            e.printStackTrace();
        }

        sourceChannel.sendMessage(EmojiUtils.generateBoardMessage(board)).queue();
    }


    private boolean containsIgnoreCase(String target, String[] args, String filterRegex) {
        for (String arg : args) {
            if (arg.replaceAll(filterRegex, "").equalsIgnoreCase(target)) {
                return true;
            }
        }
        return false;
    }
}
