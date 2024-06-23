package org.eventa.core.producer;

import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.eventa.core.events.BaseEvent;
import org.springframework.beans.factory.annotation.Value;
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
import java.util.concurrent.ThreadLocalRandom;



@Log4j2
@Service
@RequiredArgsConstructor
public class KafkaEventProducer implements EventProducer {

    @Value("${eventa.kafka.event-bus}")
    private String eventStoreName;
    private final KafkaTemplate<UUID, Object> kafkaTemplate;

    @Override
    @Transactional(transactionManager = "kafkaTransactionManager", rollbackFor = Exception.class)
    public void produce(String topic, BaseEvent baseEvent) {
        final Message<?> message = MessageBuilder
                .withPayload(baseEvent)
                .setHeader(KafkaHeaders.KEY, baseEvent.getId())
                .setHeader("schema.version", "v1")
                .setHeader(KafkaHeaders.TOPIC, eventStoreName)
                .setHeader(KafkaHeaders.PARTITION, ThreadLocalRandom.current().nextInt(0, 2))
                .setHeader(KafkaHeaders.TIMESTAMP, System.currentTimeMillis())
                .build();
        CompletableFuture<? extends SendResult<UUID, ?>> future = kafkaTemplate.send(message);
        future.thenAccept(uuidSendResult -> {
            try {
                log.info("Produced : {}, Offset {}", future.get().getProducerRecord().value(), future.get().getRecordMetadata().offset());
            } catch (InterruptedException | ExecutionException e) {
                throw new RuntimeException(e);
            }
        }).exceptionally(exception -> {
            log.error(exception.getMessage());
            return null;
        });
    }

    @Override
    @Transactional(transactionManager = "kafkaTransactionManager", rollbackFor = Exception.class)
    public CompletableFuture<String> produceEvent(String topic, BaseEvent baseEvent) {
        final Message<?> message = MessageBuilder
                .withPayload(baseEvent)
                .setHeader(KafkaHeaders.KEY, UUID.randomUUID())
                .setHeader("schema.version", "v1")
                .setHeader(KafkaHeaders.TOPIC, eventStoreName)
                .setHeader(KafkaHeaders.PARTITION, ThreadLocalRandom.current().nextInt(0, 2))
                .setHeader(KafkaHeaders.TIMESTAMP, System.currentTimeMillis())
                .build();

        CompletableFuture<? extends SendResult<UUID, ?>> future = kafkaTemplate.send(message);

        return future.thenApply(sendResult -> {
            try {
                log.info("Produced : {}, Offset {}", future.get().getProducerRecord().value(), future.get().getRecordMetadata().offset());
                return sendResult.getProducerRecord().key().toString();
            } catch (Exception e) {
                log.error("Error producing Kafka message", e);
                throw new RuntimeException(e);
            }
        });
    }
}
