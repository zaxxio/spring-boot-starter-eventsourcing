package org.eventa.core.registry;

import org.springframework.stereotype.Component;

import java.lang.reflect.Method;
import java.util.*;

@Component
public class CommandHandlerRegistry {

    private final Map<Class<?>, List<Method>> routes = new HashMap<>();

    public void registerHandler(Class<?> type, Method method) {
        routes.computeIfAbsent(type, methods -> new LinkedList<>()).add(method);
    }

    public Method getHandler(Class<?> commandType) {
        List<Method> methods = routes.get(commandType);
        if (methods == null && !methods.isEmpty()) {
            throw new RuntimeException("No Command Handler is registered");
        }
        if (methods.size() > 1) {
            throw new RuntimeException("More than one Command handler is registered");
        }
        return methods.get(0);
    }

}