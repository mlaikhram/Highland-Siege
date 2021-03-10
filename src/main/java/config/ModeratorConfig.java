package config;

import com.fasterxml.jackson.annotation.JsonProperty;

public class ModeratorConfig extends BotConfig {

    @JsonProperty
    private String dbPath;

    @JsonProperty
    private long admin;

    public String getDbPath() {
        return dbPath;
    }

    public long getAdmin() {
        return admin;
    }
}
