package org.eventa.core.consumer;

import org.springframework.kafka.support.Acknowledgment;
import org.eventa.core.events.BaseEvent;

public interface EventConsumer {
    void consume(BaseEvent baseEvent, String offset, Acknowledgment acknowledgment) throws Exception;
}
