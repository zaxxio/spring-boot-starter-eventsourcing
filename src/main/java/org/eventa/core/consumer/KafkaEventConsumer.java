package org.eventa.core.consumer;

import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.annotation.AliasFor;
import org.springframework.core.task.TaskExecutor;
import org.springframework.kafka.annotation.DltHandler;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.annotation.RetryableTopic;
import org.springframework.kafka.annotation.TopicPartition;
import org.springframework.kafka.support.Acknowledgment;
import org.springframework.kafka.support.KafkaHeaders;
import org.springframework.messaging.handler.annotation.Header;
import org.springframework.retry.annotation.Backoff;
import org.springframework.stereotype.Service;
import org.eventa.core.events.BaseEvent;
import org.eventa.core.dispatcher.EventDispatcher;

import java.util.concurrent.CompletableFuture;

@Log4j2
@Service
@RequiredArgsConstructor
public class KafkaEventConsumer implements EventConsumer {

    private final EventDispatcher eventDispatcher;

    @Override
    @KafkaListener(topicPattern = "${eventa.kafka.event-bus}", concurrency = "${eventa.kafka.concurrency}", containerFactory = "kafkaListenerContainerFactory")
    public void consume(BaseEvent baseEvent, @Header(KafkaHeaders.OFFSET) String offset, Acknowledgment ack) throws Exception {
        log.info("Received event: {}, Offset {}", baseEvent, offset);
        log.info("Thread Id : {}", Thread.currentThread().getId());
        CompletableFuture<Void> future = this.eventDispatcher.dispatch(baseEvent);
        future.whenComplete((result, ex) -> {
            if (ex == null) {
                log.debug("Successfully processed event: {}", baseEvent);
                ack.acknowledge();
            } else {
                log.error("Error processing event: {}", baseEvent, ex);
                // Handle exception (e.g., retry logic, dead-letter queue, etc.)
            }
        });
    }

    @DltHandler
    public void problem(BaseEvent baseEvent, Acknowledgment ack) {
        log.error("Not Processed {}", baseEvent);
    }

}

