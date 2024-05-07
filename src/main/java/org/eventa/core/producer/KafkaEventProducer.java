package org.eventa.core.producer;

import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.eventa.core.events.BaseEvent;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.KafkaHeaders;
import org.springframework.kafka.support.SendResult;
import org.springframework.messaging.Message;
import org.springframework.messaging.support.MessageBuilder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;

@Log4j2
@Service
@RequiredArgsConstructor
public class KafkaEventProducer implements EventProducer {
    private final KafkaTemplate<UUID, Object> kafkaTemplate;
    @Override
    @Transactional(transactionManager = "kafkaTransactionManager", rollbackFor = Exception.class)
    public void produce(String topic, BaseEvent baseEvent) {
        final Message<?> message = MessageBuilder
                .withPayload(baseEvent)
                .setHeader(KafkaHeaders.KEY, UUID.randomUUID())
                .setHeader("schema.version", "v1")
                .setHeader(KafkaHeaders.TOPIC, topic)
                .setHeader(KafkaHeaders.TIMESTAMP, System.currentTimeMillis())
                .build();
        CompletableFuture<? extends SendResult<UUID, ?>> future = kafkaTemplate.send(message);
        future.thenAccept(uuidSendResult -> {
            try {
                log.info("Produced : " + future.get().getProducerRecord().value());
            } catch (InterruptedException | ExecutionException e) {
                throw new RuntimeException(e);
            }
        }).exceptionally(exception -> {
            log.error(exception.getMessage());
            return null;
        });
    }
}
