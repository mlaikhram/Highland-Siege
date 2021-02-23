package listener;

import config.YmlConfig;
import game.*;
import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.entities.*;
import net.dv8tion.jda.api.entities.ChannelType;
import net.dv8tion.jda.api.events.message.MessageReceivedEvent;
import net.dv8tion.jda.api.events.message.react.MessageReactionAddEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import util.*;

import javax.annotation.Nonnull;
import java.util.List;

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

        try {
            String rawMessage = event.getMessage().getContentRaw();
            String[] messageTokens = rawMessage.split("[ ]+");

            if (event.isFromType(ChannelType.TEXT)) {
                // Register bot from #registration (register @bot)
                if (sourceChannel.getIdLong() == DBUtils.getSpecialChannel(event.getGuild().getIdLong(), SpecialChannelType.REGISTRATION) && messageTokens.length >= 2 && messageTokens[0].equalsIgnoreCase("register") && MessageUtils.isUserMention(messageTokens[1])) {
                    try {
                        long botId = MessageUtils.mentionToUserID(messageTokens[1]);
                        User botUser = jda.getUserById(botId);
                        if (botUser.isBot()) {// && botId != jda.getSelfUser().getIdLong()) { // TODO: uncomment self check after testing
                            long coachId = DBUtils.getBotCoach(botId);
                            if (coachId == -1) {
                                DBUtils.registerBot(botId, event.getAuthor().getIdLong());
                                sourceChannel.sendMessage(botUser.getAsMention() + MessageUtils.REGISTRATION_MESSAGE).queue((message -> {
                                    message.pin().queue();
                                    message.addReaction(EmojiUtils.CONFIRM).queue();
                                    message.addReaction(EmojiUtils.CANCEL).queue();
                                }));
                            } else {
                                throw new Exception("This bot is already registered by " + jda.getUserById(coachId).getAsMention());
                            }
                        } else {
                            throw new Exception("You can only register bots that you make!");
                        }
                    } catch (Exception e) {
                        sourceChannel.sendMessage(e.getMessage()).queue();
                    }
                }
                // TODO: made move from #arena
                // TODO: made move from #plumbing
                // Set Special Channels (@this set arena|plumbing)
                else if (event.getAuthor().getIdLong() == config.getAdmin() && messageTokens.length >= 3 && MessageUtils.isUserMention(messageTokens[0]) && MessageUtils.mentionToUserID(messageTokens[0]) == jda.getSelfUser().getIdLong() && messageTokens[1].equalsIgnoreCase("set")) {
                    try {
                        SpecialChannelType channelType = SpecialChannelType.valueOf(messageTokens[2].toUpperCase());
                        DBUtils.setSpecialChannel(event.getGuild().getIdLong(), channelType, sourceChannel.getIdLong());
                        sourceChannel.sendMessage("This channel was successfully set to " + channelType.name().toLowerCase()).queue();
                    } catch (Exception e) {
                        sourceChannel.sendMessage("Failed to set channel to " + messageTokens[2].toUpperCase()).queue();
                    }
                }
                // TODO: challenge user from any channel
                // TODO: bot challenge user from any channel
                // TODO: help text
                // TODO: verify bot via emote
            }
//        Board board = new Board();
//
//        board.applyMove(new Move(board.getPieces(Side.FRIENDLY).get(PieceType.SNIPER), new Position(5, 1)));
//        board.applyMove(new Move(board.getPieces(Side.FRIENDLY).get(PieceType.ASSASSIN), new Position(6, 1)));
//        board.applyMove(new Move(board.getPieces(Side.FRIENDLY).get(PieceType.MEDIC), new Position(7, 1)));
//        board.applyMove(new Move(board.getPieces(Side.FRIENDLY).get(PieceType.PALADIN), new Position(0, 0)));
//        board.applyMove(new Move(board.getPieces(Side.FRIENDLY).get(PieceType.SCOUT), new Position(2, 0)));
//        board.applyMove(new Move(board.getPieces(Side.FRIENDLY).get(PieceType.BEAST), new Position(7, 0)));
//
//        board.applyMove(new Move(board.getPieces(Side.ENEMY).get(PieceType.SNIPER), new Position(5, 17)));
//        board.applyMove(new Move(board.getPieces(Side.ENEMY).get(PieceType.ASSASSIN), new Position(5, 5)));
//        board.applyMove(new Move(board.getPieces(Side.ENEMY).get(PieceType.MEDIC), new Position(6, 2)));
//        board.applyMove(new Move(board.getPieces(Side.ENEMY).get(PieceType.PALADIN), new Position(2, 5)));
//        board.applyMove(new Move(board.getPieces(Side.ENEMY).get(PieceType.SCOUT), new Position(2, 4)));
//        board.applyMove(new Move(board.getPieces(Side.ENEMY).get(PieceType.BEAST), new Position(7, 16)));
//
//        try {
//            board.applyMove(new Move(board, Side.FRIENDLY, "Sniper", "to", "J4"));
//        }
//        catch (Exception e) {
//            e.printStackTrace();
//        }
//
//        sourceChannel.sendMessage(EmojiUtils.generateBoardMessage(board, "PeaQueueAre", "Kiloechovin")).queue();
        }
        catch (Exception e) {
            e.printStackTrace();
            sourceChannel.sendMessage("Something went wrong..." + e.getMessage()).queue();
        }
    }

    @Override
    public void onMessageReactionAdd(@Nonnull MessageReactionAddEvent event) {
        Guild guild = event.getGuild();
        TextChannel sourceChannel = event.getTextChannel();
        Message message = event.retrieveMessage().complete();
        User user = event.getUser();

        try {
            if (event.isFromType(ChannelType.TEXT) && message.getAuthor().getIdLong() == jda.getSelfUser().getIdLong() && event.getReactionEmote().isEmoji() && sourceChannel.getIdLong() == DBUtils.getSpecialChannel(event.getGuild().getIdLong(), SpecialChannelType.REGISTRATION) && user != null) {
                String[] messageTokens = message.getContentRaw().split("[ ]+");
                if (messageTokens.length > 0 && MessageUtils.isUserMention(messageTokens[0]) && message.getContentRaw().contains(MessageUtils.REGISTRATION_MESSAGE) && user.getIdLong() == config.getAdmin()) {
                    long botId = MessageUtils.mentionToUserID(messageTokens[0]);
                    long coachId = DBUtils.getBotCoach(botId);
                    String emoji = event.getReactionEmote().getEmoji();
                    if (emoji.equals(EmojiUtils.CONFIRM)) {
                        logger.info("verifying bot");
                        DBUtils.verifyBot(botId);
                        message.delete().queue();

                        // PM bot owner
                        jda.getUserById(coachId).openPrivateChannel().queue((privateChannel -> {
                            privateChannel.sendMessage("Your bot " + jda.getUserById(botId).getAsTag() + " was verified! You may now play Highland Siege with this bot!").queue();
                        }));

                        // assign roles
                        Role botRole = getRole(guild, KeyRole.BOT);
                        if (botRole != null) {
                            guild.addRoleToMember(botId, botRole).queue();
                        }
                        Role coachRole = getRole(guild, KeyRole.COACH);
                        if (coachRole != null) {
                            logger.info(coachId + "");
                            guild.addRoleToMember(coachId, coachRole).queue();
                        }
                    }
                    else if (emoji.equals(EmojiUtils.CANCEL)) {
                        logger.info("cancelling registration");
                        DBUtils.removeBot(botId);
                        message.delete().queue();

                        // PM bot owner
                        jda.getUserById(coachId).openPrivateChannel().queue((privateChannel -> {
                            privateChannel.sendMessage("Unfortunately, your registration for bot " + jda.getUserById(botId).getAsTag() + " was cancelled. Please contact an admin if you need further assistance.").queue();
                        }));
                    }
                }
            }
        }
        catch (Exception e) {
            e.printStackTrace();
            sourceChannel.sendMessage("Something went wrong..." + e.getMessage()).queue();
        }
    }

    private Role getRole(Guild guild, KeyRole role) {
        List<Role> roles = guild.getRolesByName(role.name(), true);
        if (!roles.isEmpty()) {
            return roles.get(0);
        }
        return null;
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
