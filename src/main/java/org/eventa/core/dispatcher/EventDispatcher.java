package org.eventa.core.dispatcher;

import org.eventa.core.events.BaseEvent;
import org.springframework.scheduling.annotation.Async;

import java.util.concurrent.CompletableFuture;

public interface EventDispatcher {
    CompletableFuture<Void> dispatch(BaseEvent baseEvent) throws Exception;
}
