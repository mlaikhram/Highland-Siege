package listener;

import config.YmlConfig;
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
        String[][] board = EmojiUtils.generateEmptyBoardEmojis(8, 18);
        sourceChannel.sendMessage(EmojiUtils.generateGameBoardMessage(64, "PeaQueueAre", "Kiloechovin", board)).queue();
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
