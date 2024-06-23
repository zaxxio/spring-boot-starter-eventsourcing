package org.eventa.core.config;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.Setter;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.NestedConfigurationProperty;


@Getter
@Setter
@ConfigurationProperties(prefix = "eventa")
public class EventaProperties {
    @NestedConfigurationProperty
    private MongoDBProperties mongodb;
    @NestedConfigurationProperty
    private KafkaProperties kafka;
    @NestedConfigurationProperty
    private CuratorProperties curator;
}
