package org.eventa.core.registry;

import org.springframework.stereotype.Component;

import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.Map;

@Component
public class SagaHandlerRegistry {

    private final Map<Class<?>, Method> startSagaMethods = new HashMap<>();
    private final Map<Class<?>, Method> endSagaMethods = new HashMap<>();
    private final Map<Class<?>, Method> sagaEventHandlerMethods = new HashMap<>();

    public void registerStartSagaHandler(Class<?> eventType, Method method) {
        startSagaMethods.put(eventType, method);
    }

    public void registerEndSagaHandler(Class<?> eventType, Method method) {
        endSagaMethods.put(eventType, method);
    }

    public void registerSagaEventHandler(Class<?> eventType, Method method) {
        sagaEventHandlerMethods.put(eventType, method);
    }

    public Method getStartSagaMethod(Class<?> eventType) {
        return startSagaMethods.get(eventType);
    }

    public Method getEndSagaMethod(Class<?> eventType) {
        return endSagaMethods.get(eventType);
    }

    public Method getSagaEventHandlerMethod(Class<?> eventType) {
        return sagaEventHandlerMethods.get(eventType);
    }
}
