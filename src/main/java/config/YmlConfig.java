package config;

import com.fasterxml.jackson.annotation.JsonProperty;

public class YmlConfig {

    @JsonProperty
    private String token;

    @JsonProperty
    private String dbPath;

    public String getToken() {
        return token;
    }

    public String getDbPath() {
        return dbPath;
    }
}
