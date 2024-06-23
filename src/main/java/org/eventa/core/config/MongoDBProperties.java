package org.eventa.core.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;

@Getter
@Setter
@ConfigurationProperties(prefix = "mongodb")
public class MongoDBProperties {
    private String host;
    private Integer port;
    private String username;
    private String password;
    private String authenticationDatabase;
    private String database;
}
