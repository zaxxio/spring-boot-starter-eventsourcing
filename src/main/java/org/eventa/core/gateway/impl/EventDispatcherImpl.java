package org.eventa.core.gateway.impl;

import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationContext;
import org.springframework.stereotype.Component;
import org.eventa.core.events.BaseEvent;
import org.eventa.core.gateway.EventDispatcher;
import org.eventa.core.registry.EventHandlerRegistry;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;


@Component
@RequiredArgsConstructor
public class EventDispatcherImpl implements EventDispatcher {
    private final EventHandlerRegistry eventHandlerRegistry;
    private final ApplicationContext applicationContext;

    @Override
    public void dispatch(BaseEvent baseEvent) {
        Method handler = eventHandlerRegistry.getHandler(baseEvent.getClass());
        try {
            Object bean = applicationContext.getBean(handler.getDeclaringClass());
            handler.invoke(bean, baseEvent);
        } catch (IllegalAccessException | InvocationTargetException e) {
            throw new IllegalStateException("Failed to invoke event handler", e);
        }
    }
}
