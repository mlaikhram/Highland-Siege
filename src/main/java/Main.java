import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import config.YmlConfig;
import moderator.listener.GameModeratorListener;
import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.JDABuilder;
import net.dv8tion.jda.api.entities.Activity;
import moderator.util.DBUtils;

import java.io.File;

public class Main {

    public static void main(String[] args) throws Exception {
        File file = new File("bot.yml");
        YmlConfig config = new ObjectMapper(new YAMLFactory()).readValue(file, YmlConfig.class);
        DBUtils.init(config.getDbPath());
        GameModeratorListener gameModeratorListener = new GameModeratorListener(config);
        JDA jda = JDABuilder.createDefault(config.getToken())
                .addEventListeners(gameModeratorListener)
                .setActivity(Activity.playing("Highland Siege"))
                .build();
        gameModeratorListener.setJda(jda);
        jda.awaitReady();
    }
}
