package game.listener;

import com.fasterxml.jackson.databind.ObjectMapper;
import config.BotConfig;
import game.enums.Side;
import game.playables.Board;
import game.playables.Move;
import moderator.util.MessageUtils;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.entities.MessageChannel;
import net.dv8tion.jda.api.entities.User;
import net.dv8tion.jda.api.events.message.MessageReceivedEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.Nonnull;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;

public abstract class GameBotListener extends ListenerAdapter {

    private static final Logger logger = LoggerFactory.getLogger(GameBotListener.class);

    private BotConfig config;
    private ObjectMapper mapper;

    public GameBotListener(BotConfig config) {
        this.config = config;
        this.mapper = new ObjectMapper();
    }

    @Override
    public final void onMessageReceived(@Nonnull MessageReceivedEvent event) {
        Guild guild = event.getGuild();
//        if (event.getAuthor().getIdLong() == guild.getSelfMember().getIdLong()) {
//            return;
//        }

        MessageChannel sourceChannel = event.getChannel();
        Message eventMessage = event.getMessage();
        User author = event.getAuthor();

        try {
            String rawMessage = eventMessage.getContentRaw();
            String[] messageTokens = rawMessage.split("[ ]+");

            // respond to board json with best move json
            if (messageTokens.length >= 3 && messageTokens[1].equalsIgnoreCase("play") && MessageUtils.isUserMention(messageTokens[0]) && MessageUtils.mentionToUserID(messageTokens[0]) == guild.getSelfMember().getIdLong()) {
                Board board = mapper.readValue(messageTokens[2], Board.class);
                Move bestMove = determineBestMove(board);
                if (bestMove != null) {
                    logger.info("sending move");
                    sourceChannel.sendMessage(mapper.writeValueAsString(bestMove)).queue();
                }
                else {
                    logger.info("I couldn't come up with a move");
                }
            }
        }
        catch (Exception e) {
            handleError(e, sourceChannel);
        }
    }

    private int INITIAL_DEPTH = 1;
    private int TIMEOUT_MILLISECONDS = 10000;
    private long start = 0;
    private boolean timeout = false;

    private int currentDepth;

    private Move bestMove;
    private Move globalBestMove;

    private Move determineBestMove(Board board) throws Exception { // TODO: determine win/lose states
        timeout = false;
        start = System.currentTimeMillis();
        List<Move> possibleMoves = board.getPossibleMoves(Side.FRIENDLY);
        Iterator<Move> moveIterator = possibleMoves.iterator();
        if (moveIterator.hasNext()) {
            bestMove = moveIterator.next();
        }
        else {
            return null;
        }

        for (int d = 0;; ++d)
        {
            if (d > 0)
            {
                globalBestMove = bestMove;
                logger.info("Completed search with depth " + currentDepth + ". Best move so far: " + mapper.writeValueAsString(globalBestMove));
            }
            currentDepth = INITIAL_DEPTH + d;
            maximizer(board, currentDepth, Integer.MIN_VALUE, Integer.MAX_VALUE);

            if (timeout)
            {
                logger.info("time's up!");
                return globalBestMove;
            }
        }
    }

    private int maximizer(Board board, int depth, int alpha, int beta)
    {
        if (System.currentTimeMillis() - start > TIMEOUT_MILLISECONDS)
        {
            timeout = true;
            return alpha;
        }

        if (depth == 0)
        {
            return determineBoardValue(board);
        }
        List<Move> legalMoves = board.getPossibleMoves(Side.FRIENDLY);

        for (Move move : legalMoves)
        {
            Board nextBoard = new Board(board);
            nextBoard.applyMove(move);
            int rating = minimizer(nextBoard, depth - 1, alpha, beta);
            if (rating > alpha)
            {
                alpha = rating;

                if (depth == currentDepth)
                {
                    bestMove = move;
                }
            }

            if (alpha >= beta)
            {
                return alpha;
            }
        }
        return alpha;
    }

    private int minimizer(Board board, int depth, int alpha, int beta)
    {
        if (depth == 0)
        {
            return determineBoardValue(board);
        }
        List<Move> legalMoves = board.getPossibleMoves(Side.ENEMY);

        for (Move move : legalMoves)
        {
            Board nextBoard = new Board(board);
            nextBoard.applyMove(move);
            int rating = maximizer(nextBoard, depth - 1, alpha, beta);
            if (rating <= beta)
            {
                beta = rating;
            }

            if (alpha >= beta)
            {
                return beta;
            }
        }
        return beta;
    }

    protected abstract int determineBoardValue(Board board);

    private void handleError(Exception e, MessageChannel channel) {
        e.printStackTrace();
        if (channel != null) {
            channel.sendMessage("Something went wrong..." + e.getMessage()).queue();
        }
    }
}
