package org.eventa.core.producer;

import org.eventa.core.events.BaseEvent;

public interface EventProducer {
    void produce(String topic, BaseEvent baseEvent) throws Exception;
}
