package org.eventa.core.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;

@Getter
@Setter
@ConfigurationProperties(prefix = "kafka")
public class KafkaProperties {
    private String eventBus;
    private String commandBus;
    private String bootstrapServers;
    private int concurrency;
    private String[] trustedPackages;
}
