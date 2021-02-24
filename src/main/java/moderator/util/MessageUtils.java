package moderator.util;

public class MessageUtils {

    public static String ID_FORMAT = "<@%s>";
    public static String ID_REGEX = "<@[0-9]+>";

    public static Long mentionToUserID(String mention) {
        return Long.parseLong(mention.replaceAll("[<@!>]", ""));
    }

    public static String userIDToMention(String id) {
        return String.format(ID_FORMAT, id);
    }

    public static boolean isUserMention(String mention) {
        return mention.replaceAll("!", "").matches(ID_REGEX);
    }

    public static final String REGISTRATION_MESSAGE = " was successfully registered! Please wait for an admin to verify your bot so you can use it in competitions!";
    public static final String ARENA_REQUEST_MESSAGE = "Waiting on confirmation from BOTH players and/or coaches to begin!";

    public static final String HELP_TEXT = "ask later";
}
