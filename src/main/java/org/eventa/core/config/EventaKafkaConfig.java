package org.eventa.core.config;

import org.apache.kafka.clients.admin.NewTopic;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.annotation.EnableKafka;
import org.springframework.kafka.config.TopicBuilder;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.retrytopic.RetryTopicConfiguration;
import org.springframework.kafka.retrytopic.RetryTopicConfigurationBuilder;
import org.springframework.retry.backoff.ExponentialBackOffPolicy;
import org.springframework.retry.policy.SimpleRetryPolicy;
import org.springframework.retry.support.RetryTemplate;

import java.util.List;
@Configuration
public class EventaKafkaConfig {

    @Value("${eventa.kafka.bootstrap-servers}")
    private String bootstrapServers;

    @Value("${eventa.kafka.event-bus}")
    private String eventBus;

    @Value("${eventa.kafka.command-bus:BaseCommand}")
    private String commandBus;

    @Value("${eventa.kafka.concurrency}")
    private int concurrency;

    @Value("${eventa.kafka.trusted-packages}")
    private String[] trustedPackages;

    @Bean
    public NewTopic eventBusTopic() {
        return TopicBuilder.name(eventBus)
                .partitions(3)
                .config("cleanup.policy", "delete")
                .build();
    }

}
