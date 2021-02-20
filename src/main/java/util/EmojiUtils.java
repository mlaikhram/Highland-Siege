package util;

public class EmojiUtils {

    private static final String[] NUMBER_EMOJIS = {
            "\u0031\uFE0F\u20E3 ",
            "\u0032\uFE0F\u20E3 ",
            "\u0033\uFE0F\u20E3 ",
            "\u0034\uFE0F\u20E3 ",
            "\u0035\uFE0F\u20E3 ",
            "\u0036\uFE0F\u20E3 ",
            "\u0037\uFE0F\u20E3 ",
            "\u0038\uFE0F\u20E3 "
    };

    public static String[][] generateEmptyBoardEmojis(int width, int height) {
        String[][] board = new String[width][height];
        for (int x = 0; x < width; ++x) {
            for (int y = 0; y < height; ++y) {
                board[x][y] = (x % 2 == y % 2) ? "\u2B1B" : "\uD83D\uDFEB";
            }
        }
        return board;
    }

    public static String generateGameBoardMessage(int bannerSize, String bluePlayer, String redPlayer, String[][] board) {
        StringBuilder message = new StringBuilder();
        message.append('`');
        message.append(bluePlayer);
        message.append(" ".repeat(bannerSize - bluePlayer.length() - redPlayer.length()));
        message.append(redPlayer);
        message.append('`');
        message.append('\n');
        message.append("\u25B6 \uD83C\uDDE6 \uD83C\uDDE7 \uD83C\uDDE8 \uD83C\uDDE9 \uD83C\uDDEA \uD83C\uDDEB \uD83C\uDDEC \uD83C\uDDED \uD83C\uDDEE \uD83C\uDDEF \uD83C\uDDF0 \uD83C\uDDF1 \uD83C\uDDF2 \uD83C\uDDF3 \uD83C\uDDF4 \uD83C\uDDF5 \uD83C\uDDF6 \uD83C\uDDF7 \n");
        for (int x = 0; x < board.length; ++x) {
            for (int y = -1; y < board[0].length; ++y) {
                message.append((y == -1) ? (NUMBER_EMOJIS[x]) : (board[x][y] + " "));
            }
            message.append('\n');
        }
        return message.toString();
    }

    public static void main(String[] args) {
        String[][] board = generateEmptyBoardEmojis(8, 18);
        System.out.println(generateGameBoardMessage(64, "PeaQueueAre", "Kiloechovin", board));
    }
}
