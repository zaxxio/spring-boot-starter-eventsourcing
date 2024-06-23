package org.eventa.core.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;

@Getter
@Setter
@ConfigurationProperties(prefix = "curator")
public class CuratorProperties {
    private String hostname;
    private int baseSleepTimeMs = 1000;
    private int maxRetries = 3;
}
