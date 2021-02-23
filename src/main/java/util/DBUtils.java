package util;

import game.SpecialChannelType;

import java.io.File;
import java.sql.*;

public class DBUtils {

    private static String connectionString = null;

    public static void init(String dbPath) throws Exception {
        connectionString = "jdbc:sqlite:" + dbPath;
        Class.forName("org.sqlite.JDBC");
        File dbFile = new File(dbPath);
        dbFile.createNewFile();
        try (Connection connection = DriverManager.getConnection(connectionString)) {
            Statement statement = connection.createStatement();
            statement.executeUpdate("CREATE TABLE IF NOT EXISTS guilds(guild_id INTEGER, registration_channel_id INTEGER, arena_channel_id INTEGER, plumbing_channel_id INTEGER, PRIMARY KEY(guild_id))");
            statement.executeUpdate("CREATE TABLE IF NOT EXISTS bots(bot_id INTEGER, coach_id INTEGER, status TEXT, PRIMARY KEY(bot_id))");
            // TODO: games history table
        }
    }

    public static void registerBot(long botId, long coachId) throws Exception {
        try (Connection connection = DriverManager.getConnection(connectionString)) {
            PreparedStatement statement = connection.prepareStatement("INSERT INTO bots values(?, ?, 'unverified')");
            statement.setLong(1, botId);
            statement.setLong(2, coachId);
            statement.execute();
        }
    }

    public static long getBotCoach(long botId) throws Exception {
        try (Connection connection = DriverManager.getConnection(connectionString)) {
            PreparedStatement statement = connection.prepareStatement("SELECT coach_id FROM bots WHERE bot_id=?");
            statement.setLong(1, botId);
            ResultSet resultSet = statement.executeQuery();
            if (resultSet.next()) {
                return resultSet.getLong("coach_id");
            }
            return -1;
        }
    }

    public static boolean isBotValid(long botId, long coachId) throws Exception {
        try (Connection connection = DriverManager.getConnection(connectionString)) {
            PreparedStatement statement = connection.prepareStatement("SELECT * FROM bots WHERE bot_id=? AND coach_id=? AND status='verified'");
            statement.setLong(1, botId);
            statement.setLong(2, coachId);
            ResultSet resultSet = statement.executeQuery();
            return resultSet.next();
        }
    }

    public static void verifyBot(long botId) throws Exception {
        try (Connection connection = DriverManager.getConnection(connectionString)) {
            PreparedStatement statement = connection.prepareStatement("UPDATE bots SET status='verified' WHERE bot_id=? AND status='unverified'");
            statement.setLong(1, botId);
            statement.execute();
            // TODO: verify if a row was updated
        }
    }

    public static void removeBot(long botId) throws Exception {
        try (Connection connection = DriverManager.getConnection(connectionString)) {
            PreparedStatement statement = connection.prepareStatement("DELETE from bots WHERE bot_id=? AND status='unverified'");
            statement.setLong(1, botId);
            statement.execute();
            // TODO: verify if a row was updated
        }
    }

    public static long getSpecialChannel(long guildId, SpecialChannelType specialChannelType) throws Exception {
        try (Connection connection = DriverManager.getConnection(connectionString)) {
            PreparedStatement statement = connection.prepareStatement(String.format("SELECT %s_channel_id FROM guilds WHERE guild_id=?", specialChannelType.name().toLowerCase()));
            statement.setLong(1, guildId);
            ResultSet resultSet = statement.executeQuery();
            if (resultSet.next()) {
                return resultSet.getLong(specialChannelType.name().toLowerCase() + "_channel_id");
            }
            return -1;
        }
    }

    public static void setSpecialChannel(long guildId, SpecialChannelType specialChannelType, long channelId) throws Exception {
        try (Connection connection = DriverManager.getConnection(connectionString)) {
            PreparedStatement insertStatement = connection.prepareStatement("INSERT INTO guilds(guild_id) SELECT ? WHERE NOT EXISTS(SELECT 1 FROM guilds WHERE guild_id=?)");
            insertStatement.setLong(1, guildId);
            insertStatement.setLong(2, guildId);
            insertStatement.execute();

            PreparedStatement statement = connection.prepareStatement(String.format("UPDATE guilds SET %s_channel_id=? WHERE guild_id=?", specialChannelType.name().toLowerCase()));
            statement.setLong(1, channelId);
            statement.setLong(2, guildId);
            statement.execute();
            // TODO: verify if a row was updated
        }
    }
}
