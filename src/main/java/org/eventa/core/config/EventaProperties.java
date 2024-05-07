package org.eventa.core.config;

import lombok.Getter;
import lombok.Setter;
import lombok.Value;
import org.springframework.boot.context.properties.ConfigurationProperties;


@Getter
@Setter
@ConfigurationProperties(prefix = "eventa")
public class EventaProperties {
    private String mongoDbHost;
    private Integer mongoDbPort;
    private String mongoDbUsername;
    private String mongoDbPassword;
    private String mongoDbAuthenticationDatabase;
    private String mongoDbDatabase;
}
