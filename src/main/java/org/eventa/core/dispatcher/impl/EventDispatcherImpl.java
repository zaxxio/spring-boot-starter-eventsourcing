package org.eventa.core.dispatcher.impl;

import lombok.RequiredArgsConstructor;
import org.eventa.core.saga.SagaHandler;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.ApplicationContext;
import org.springframework.core.task.TaskExecutor;
import org.springframework.stereotype.Component;
import org.eventa.core.events.BaseEvent;
import org.eventa.core.dispatcher.EventDispatcher;
import org.eventa.core.registry.EventHandlerRegistry;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.concurrent.CompletableFuture;


@Component
@RequiredArgsConstructor
public class EventDispatcherImpl implements EventDispatcher {

    private final EventHandlerRegistry eventHandlerRegistry;
    private final ApplicationContext applicationContext;
    private final SagaHandler sagaHandler;

    @Qualifier("eventaTaskExecutor")
    private final TaskExecutor taskExecutor;

    @Override
    public CompletableFuture<Void> dispatch(BaseEvent baseEvent) {
        return CompletableFuture.runAsync(() -> {
            try {
                handleEventHandler(baseEvent);
                handleSagaHandler(baseEvent);
            } catch (Exception e) {
                throw e;
            }
        }, taskExecutor);
    }

    private void handleSagaHandler(BaseEvent baseEvent) {
        sagaHandler.handleSagaEvent(baseEvent);
    }

    private void handleEventHandler(BaseEvent baseEvent) {
        try {
            Method handler = eventHandlerRegistry.getHandler(baseEvent.getClass());
            if (handler == null) {
                return;
            }
            try {
                Object bean = applicationContext.getBean(handler.getDeclaringClass());
                handler.invoke(bean, baseEvent);
            } catch (IllegalAccessException | InvocationTargetException e) {
                throw new IllegalStateException("Failed to invoke event handler", e);
            }
        }catch (Exception e){
            throw e;
        }
    }
}
