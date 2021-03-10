package config;

import com.fasterxml.jackson.annotation.JsonProperty;

public class BotConfig {

    @JsonProperty
    private String token;

    public String getToken() {
        return token;
    }
}
