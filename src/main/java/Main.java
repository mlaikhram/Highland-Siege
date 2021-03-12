import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import config.ModeratorConfig;
import moderator.listener.GameModeratorListener;
import moderator.listener.SampleBotListener;
import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.JDABuilder;
import net.dv8tion.jda.api.entities.Activity;
import moderator.util.DBUtils;
import net.dv8tion.jda.api.requests.GatewayIntent;
import net.dv8tion.jda.api.utils.MemberCachePolicy;

import java.io.File;

public class Main {

    public static void main(String[] args) throws Exception {
        File file = new File("bot.yml");
        ModeratorConfig config = new ObjectMapper(new YAMLFactory()).readValue(file, ModeratorConfig.class);
        DBUtils.init(config.getDbPath());
        GameModeratorListener gameModeratorListener = new GameModeratorListener(config);
        JDA jda = JDABuilder.createDefault(config.getToken())
                .setMemberCachePolicy(MemberCachePolicy.ALL)
                .enableIntents(GatewayIntent.GUILD_MEMBERS)
                .addEventListeners(gameModeratorListener)
                .addEventListeners(new SampleBotListener(config))
                .setActivity(Activity.playing("Highland Siege"))
                .build();
        jda.awaitReady();
    }
}
