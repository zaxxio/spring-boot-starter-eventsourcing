package org.eventa.core.producer;

import org.eventa.core.events.BaseEvent;

import java.util.concurrent.CompletableFuture;

public interface EventProducer {
    void produce(String topic, BaseEvent baseEvent) throws Exception;

    CompletableFuture<String> produceEvent(String topic, BaseEvent baseEvent) throws Exception;
}
