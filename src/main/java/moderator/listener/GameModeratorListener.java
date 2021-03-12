package moderator.listener;

import com.fasterxml.jackson.databind.ObjectMapper;
import config.ModeratorConfig;
import game.enums.KeyRole;
import game.enums.Side;
import game.enums.SpecialChannelType;
import game.playables.Move;
import moderator.GameSession;
import moderator.Player;
import net.dv8tion.jda.api.entities.*;
import net.dv8tion.jda.api.entities.ChannelType;
import net.dv8tion.jda.api.events.message.MessageReceivedEvent;
import net.dv8tion.jda.api.events.message.react.MessageReactionAddEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import moderator.util.*;

import javax.annotation.Nonnull;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;
import java.util.stream.Collectors;

public class GameModeratorListener extends ListenerAdapter {

    private static final Logger logger = LoggerFactory.getLogger(GameModeratorListener.class);

    private ModeratorConfig config;
    private HashMap<Long, GameSession> sessions;

    public GameModeratorListener(ModeratorConfig config) {
        this.config = config;
        this.sessions = new HashMap<>();
    }

    @Override
    public void onMessageReceived(@Nonnull MessageReceivedEvent event) {
        Guild guild = event.getGuild();
        MessageChannel sourceChannel = event.getChannel();
//        if (event.getAuthor().getIdLong() == guild.getSelfMember().getIdLong() && ) {
//            return;
//        }

        Message eventMessage = event.getMessage();
        User author = event.getAuthor();

        try {
            String rawMessage = eventMessage.getContentRaw();
            String[] messageTokens = rawMessage.split("[ ]+");

            if (event.isFromType(ChannelType.TEXT)) {
                // Register bot from #registration (register @bot)
                if (!author.isBot() && sourceChannel.getIdLong() == DBUtils.getSpecialChannel(guild.getIdLong(), SpecialChannelType.REGISTRATION) && messageTokens.length >= 2 && messageTokens[0].equalsIgnoreCase("register") && MessageUtils.isUserMention(messageTokens[1])) {
                    try {
                        long botId = MessageUtils.mentionToUserID(messageTokens[1]);
                        logger.info(messageTokens[1]);
                        logger.info(botId + "");
                        logger.info("819787176824012802");
                         for (Member member : guild.getMembers()) {
                             logger.info(member.getEffectiveName() + "\t\t" + member.getIdLong());
                         }
                        User botUser = guild.getMemberById(botId).getUser();
                        if (botUser.isBot()) {// && botId != jda.getSelfUser().getIdLong()) { // TODO: uncomment self check after testing
                            long coachId = DBUtils.getBotCoach(botId);
                            if (coachId == -1) {
                                DBUtils.registerBot(botId, author.getIdLong());
                                sourceChannel.sendMessage(botUser.getAsMention() + MessageUtils.REGISTRATION_MESSAGE).queue((message -> {
                                    message.pin().queue();
                                    message.addReaction(EmojiUtils.CONFIRM).queue();
                                    message.addReaction(EmojiUtils.CANCEL).queue();
                                }));
                            } else {
                                throw new Exception("This bot is already registered by " + guild.getMemberById(coachId).getAsMention());
                            }
                        } else {
                            throw new Exception("You can only register bots that you make!");
                        }
                    } catch (Exception e) {
                        e.printStackTrace();
                        sourceChannel.sendMessage(e.getMessage()).queue();
                    }
                }
                // make move from #arena
                else if (author.getIdLong() != guild.getSelfMember().getIdLong() && sourceChannel.getIdLong() == DBUtils.getSpecialChannel(guild.getIdLong(), SpecialChannelType.ARENA)) {
                    GameSession session = sessions.get(guild.getIdLong());
                    if (messageTokens.length == 3 && session != null && !session.isOver() && session.getActivePlayer().getId() == author.getIdLong()) {
                        try {
                            Move move = new Move(session.getBoard(), session.getActiveSide(), messageTokens[0], messageTokens[1], messageTokens[2]);
                            handleSendBoardMessages(session, author, move, sourceChannel, guild, eventMessage, true);
                        }
                        catch (Exception e) {
                            e.printStackTrace();
                            logger.info("invalid move: " + e.getMessage());
                            eventMessage.addReaction(EmojiUtils.CANCEL).queue();
                            eventMessage.delete().queueAfter(3, TimeUnit.SECONDS);
                        }
                    }
                    else {
                        eventMessage.addReaction(EmojiUtils.CANCEL).queue();
                        eventMessage.delete().queueAfter(3, TimeUnit.SECONDS);
                    }
                }
                // make move from #plumbing
                else if (messageTokens.length >= 1 && !MessageUtils.isUserMention(messageTokens[0]) && sourceChannel.getIdLong() == DBUtils.getSpecialChannel(guild.getIdLong(), SpecialChannelType.PLUMBING) && author.isBot()) {
                    GameSession session = sessions.get(guild.getIdLong());
                    if (session != null && !session.isOver() && session.getActivePlayer().getId() == author.getIdLong()) {
                        try {
                            Move move = new ObjectMapper().readValue(rawMessage, Move.class);
                            if (session.getActiveSide() == Side.ENEMY) {
                                move = move.getRotated(session.getBoard());
                            }
                            MessageChannel arenaChannel = guild.getTextChannelById(DBUtils.getSpecialChannel(guild.getIdLong(), SpecialChannelType.ARENA));
                            handleSendBoardMessages(session, author, move, arenaChannel, guild, eventMessage, false);
                        }
                        catch (Exception e) {
                            e.printStackTrace();
                            logger.info("invalid move: " + e.getMessage());
                            eventMessage.addReaction(EmojiUtils.CANCEL).queue();
//                            eventMessage.delete().queueAfter(3, TimeUnit.SECONDS);
                        }
                    }
//                    eventMessage.delete().queue();
                }
                // Set Special Channels (@this set arena|plumbing|registration)
                else if (!author.isBot() && event.getAuthor().getIdLong() == config.getAdmin() && messageTokens.length >= 3 && MessageUtils.isUserMention(messageTokens[0]) && MessageUtils.mentionToUserID(messageTokens[0]) == guild.getSelfMember().getIdLong() && messageTokens[1].equalsIgnoreCase("set")) {
                    try {
                        SpecialChannelType channelType = SpecialChannelType.valueOf(messageTokens[2].toUpperCase());
                        DBUtils.setSpecialChannel(guild.getIdLong(), channelType, sourceChannel.getIdLong());
                        sourceChannel.sendMessage("This channel was successfully set to " + channelType.name().toLowerCase()).queue();
                    } catch (Exception e) {
                        sourceChannel.sendMessage("Failed to set channel to " + messageTokens[2].toUpperCase()).queue();
                    }
                }
                // user challenge anyone from any channel
                else if (!author.isBot() && messageTokens.length >= 2 && messageTokens[0].equalsIgnoreCase("challenge") && MessageUtils.isUserMention(messageTokens[1])) {
                    if (!sessions.containsKey(guild.getIdLong()) || sessions.get(guild.getIdLong()).isOver()) {
                        User defender = guild.getMemberById(MessageUtils.mentionToUserID(messageTokens[1])).getUser();
                        if (!defender.isBot() || DBUtils.isBotVerified(defender.getIdLong())) {
                            Player challengerPlayer = new Player(author.getIdLong());
                            Player defenderPlayer = new Player(defender.getIdLong(), DBUtils.getBotCoach(defender.getIdLong()));

                            sessions.put(guild.getIdLong(), new GameSession(challengerPlayer, defenderPlayer));
                            TextChannel arenaChannel = guild.getTextChannelById(DBUtils.getSpecialChannel(guild.getIdLong(), SpecialChannelType.ARENA));
                            arenaChannel.sendMessage(challengerPlayer.toString(guild) + " has challenged " + defenderPlayer.toString(guild) + " to a game! " + MessageUtils.ARENA_REQUEST_MESSAGE).queue(message -> {
                                message.addReaction(EmojiUtils.CONFIRM).queue();
                                message.addReaction(EmojiUtils.CANCEL).queue();
                            });
                        }
                        else {
                            sourceChannel.sendMessage("You can only challenge other players or verified bots!").queue();
                        }
                    }
                    else {
                        sourceChannel.sendMessage("There's already a game pending or in session!").queue();
                    }
                }
                // TODO: bot challenge anyone from any channel
                else if (!author.isBot() && messageTokens.length >= 3 && messageTokens[1].equalsIgnoreCase("challenge") && MessageUtils.isUserMention(messageTokens[0]) && MessageUtils.isUserMention(messageTokens[2])) {
                    if (!sessions.containsKey(guild.getIdLong()) || sessions.get(guild.getIdLong()).isOver()) {
                        User challenger = guild.getMemberById(MessageUtils.mentionToUserID(messageTokens[0])).getUser();
                        if (DBUtils.getBotCoach(challenger.getIdLong()) == author.getIdLong()) {
                            User defender = guild.getMemberById(MessageUtils.mentionToUserID(messageTokens[2])).getUser();
                            if (!defender.isBot() || DBUtils.isBotVerified(defender.getIdLong())) {
                                Player challengerPlayer = new Player(challenger.getIdLong(), author.getIdLong());
                                Player defenderPlayer = new Player(defender.getIdLong(), DBUtils.getBotCoach(defender.getIdLong()));

                                sessions.put(guild.getIdLong(), new GameSession(challengerPlayer, defenderPlayer));
                                TextChannel arenaChannel = guild.getTextChannelById(DBUtils.getSpecialChannel(guild.getIdLong(), SpecialChannelType.ARENA));
                                arenaChannel.sendMessage(challengerPlayer.toString(guild) + " has challenged " + defenderPlayer.toString(guild) + " to a game! " + MessageUtils.ARENA_REQUEST_MESSAGE).queue(message -> {
                                    message.addReaction(EmojiUtils.CONFIRM).queue();
                                    message.addReaction(EmojiUtils.CANCEL).queue();
                                });
                            } else {
                                sourceChannel.sendMessage("You can only challenge other players or verified bots!").queue();
                            }
                        }
                        else {
                            sourceChannel.sendMessage("You can only start a challenge on behalf of yourself or one of your bots!").queue();
                        }
                    }
                    else {
                        sourceChannel.sendMessage("There's already a game pending or in session!").queue();
                    }
                }
                // TODO: help text
            }
        }
        catch (Exception e) {
            handleError(e, sourceChannel);
        }
    }

    private void handleSendBoardMessages(GameSession session, User author, Move move, MessageChannel sourceChannel, Guild guild, Message eventMessage, boolean deleteSent) throws Exception {
        if (session.tryMove(author.getIdLong(), move)) {
            updateBoardMessage(session, sourceChannel, guild, eventMessage, deleteSent);
            //                                sourceChannel.sendMessage(session.getBoardAsEmojis(guild)).queue();
        }
        else {
            eventMessage.addReaction(EmojiUtils.CANCEL).queue();
            if (deleteSent) {
                eventMessage.delete().queueAfter(3, TimeUnit.SECONDS);
            }
        }
    }

    private void updateBoardMessage(GameSession session, MessageChannel sourceChannel, Guild guild, Message eventMessage, boolean deleteSent) {
        sourceChannel.editMessageById(session.getBoardMessageId(), session.getBoardAsEmojis(guild)).queue(message -> {
            Side winningSide = session.getBoard().getVictorySide();
            if (winningSide == Side.NEUTRAL) {
                try {
                    if (session.getActivePlayer().isBot()) {
                        TextChannel plumbingChannel = guild.getTextChannelById(DBUtils.getSpecialChannel(guild.getIdLong(), SpecialChannelType.PLUMBING));
                        plumbingChannel.sendMessage(guild.getMemberById(session.getActivePlayer().getId()).getAsMention() + " play " + session.getBoardAsJson()).queue();
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                    logger.error(e.getMessage());
                }
            }
            else {
                Role challengerRole = getRole(guild, KeyRole.CHALLENGER);
                if (challengerRole != null) {
                    for (Player player : session.getPlayers()) {
                        guild.removeRoleFromMember(player.getId(), challengerRole).queue();
                    }
                }
                sessions.remove(guild.getIdLong());
            }
            sourceChannel.editMessageById(session.getStatusMessageId(), session.getStatusAsEmojis()).queue(statusMessage -> {
                if (deleteSent) {
                    eventMessage.delete().queue();
                }
            });
        }, (error) -> {
            logger.error(error.getMessage());
            error.printStackTrace();
            sourceChannel.sendMessage(error.getMessage()).queue();
        });
    }

    @Override
    public void onMessageReactionAdd(@Nonnull MessageReactionAddEvent event) {
        Guild guild = event.getGuild();
        TextChannel sourceChannel = event.getTextChannel();
        Message message = event.retrieveMessage().complete();
        User user = event.getUser();

        try {
            if (event.isFromType(ChannelType.TEXT) && message.getAuthor().getIdLong() == guild.getSelfMember().getIdLong() && user != null) {

                // check for bot registration requests
                if (sourceChannel.getIdLong() == DBUtils.getSpecialChannel(guild.getIdLong(), SpecialChannelType.REGISTRATION)) {
                    String[] messageTokens = message.getContentRaw().split("[ ]+");
                    if (messageTokens.length > 0 && MessageUtils.isUserMention(messageTokens[0]) && message.getContentRaw().endsWith(MessageUtils.REGISTRATION_MESSAGE)) {
                        long botId = MessageUtils.mentionToUserID(messageTokens[0]);
                        long coachId = DBUtils.getBotCoach(botId);
                        handleConfirmationMessageReaction(event, Collections.singleton(config.getAdmin()),
                                (author) -> {
                                    logger.info("verifying bot");
                                    try {
                                        DBUtils.verifyBot(botId);
                                        message.delete().queue();

                                        // PM bot owner
                                        guild.getMemberById(coachId).getUser().openPrivateChannel().queue((privateChannel -> {
                                            privateChannel.sendMessage("Your bot " + guild.getMemberById(botId).getUser().getAsTag() + " was verified! You may now play Highland Siege with this bot!").queue();
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
                                    } catch (Exception e) {
                                        handleError(e, sourceChannel);
                                    }
                                },
                                (author) -> {
                                    try {
                                        logger.info("cancelling registration");
                                        DBUtils.removeBot(botId);
                                        message.delete().queue();

                                        // PM bot owner
                                        guild.getMemberById(coachId).getUser().openPrivateChannel().queue((privateChannel -> {
                                            privateChannel.sendMessage("Unfortunately, your registration for bot " + guild.getMemberById(botId).getUser().getAsTag() + " was cancelled. Please contact an admin if you need further assistance.").queue();
                                        }));
                                    } catch (Exception e) {
                                        handleError(e, sourceChannel);
                                    }
                                }
                        );
                    }
                }

                // check for challenge requests
                else if (sourceChannel.getIdLong() == DBUtils.getSpecialChannel(guild.getIdLong(), SpecialChannelType.ARENA) && message.getContentRaw().endsWith(MessageUtils.ARENA_REQUEST_MESSAGE) && sessions.containsKey(guild.getIdLong())) {
                    GameSession session = sessions.get(guild.getIdLong());
                    Collection<Long> playersOrCoaches = session.getPlayers().stream().map(player -> player.isBot() ? player.getCoachId() : player.getId()).collect(Collectors.toList());
                    handleConfirmationMessageReaction(event, playersOrCoaches,
                            (author) -> {
                                try {
                                    MessageReaction confirmReaction = message.getReactions().stream().filter(messageReaction -> messageReaction.getReactionEmote().isEmoji() && messageReaction.getReactionEmote().getEmoji().equals(EmojiUtils.CONFIRM.trim())).findFirst().get();
                                    // check if both players accepted the request
                                    confirmReaction.retrieveUsers().queue((users) -> {
                                        try {
                                            if (users.stream().map(reactUser -> reactUser.getIdLong()).collect(Collectors.toSet()).containsAll(playersOrCoaches)) {
                                                // start the match!
                                                session.begin();
                                                Role challengerRole = getRole(guild, KeyRole.CHALLENGER);
                                                if (challengerRole != null) {
                                                    for (Player player : session.getPlayers()) {
                                                        guild.addRoleToMember(player.getId(), challengerRole).queue();
                                                    }
                                                }
                                                sourceChannel.sendMessage(session.getBoardAsEmojis(guild)).queue(boardMessage -> {
                                                    session.setBoardMessageId(boardMessage.getIdLong());
                                                    sourceChannel.sendMessage(session.getStatusAsEmojis()).queue(statusMessage -> {
                                                        session.setStatusMessageId(statusMessage.getIdLong());
                                                    });
                                                });
                                                if (session.getActivePlayer().isBot()) {
                                                    TextChannel plumbingChannel = guild.getTextChannelById(DBUtils.getSpecialChannel(guild.getIdLong(), SpecialChannelType.PLUMBING));
                                                    plumbingChannel.sendMessage(guild.getMemberById(session.getActivePlayer().getId()).getAsMention() + " play " + session.getBoardAsJson()).queue();
                                                }
                                                message.delete().queue();

                                            }
                                        }
                                        catch (Exception e) {
                                            handleError(e, sourceChannel);
                                        }
                                    });
                                }
                                catch (Exception e) {
                                    handleError(e, sourceChannel);
                                }
                            },
                            (author) -> {
                                session.end();
                                message.delete().queue();
                            }
                    );
                }
            }
        }
        catch (Exception e) {
            handleError(e, sourceChannel);
        }
    }

    private void handleConfirmationMessageReaction(MessageReactionAddEvent event, Collection<Long> verifiedUsers, Consumer<User> onConfirm, Consumer<User> onCancel) {
        Message message = event.retrieveMessage().complete();
        User user = event.getUser();
        if (event.getReactionEmote().isEmoji()) {
            String emoji = event.getReactionEmote().getEmoji();
            if (verifiedUsers.contains(user.getIdLong()) && emoji.equals(EmojiUtils.CONFIRM.trim())) {
                onConfirm.accept(user);
            } else if (verifiedUsers.contains(user.getIdLong()) && emoji.equals(EmojiUtils.CANCEL.trim())) {
                onCancel.accept(user);
            } else {
                message.removeReaction(emoji, user);
            }
        }
        else {
            message.removeReaction(event.getReactionEmote().getEmote(), user);
        }
    }

    private void handleError(Exception e, MessageChannel channel) {
        e.printStackTrace();
        if (channel != null) {
            channel.sendMessage("Something went wrong..." + e.getMessage()).queue();
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
