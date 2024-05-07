package org.eventa.core.registry;

import org.springframework.stereotype.Component;

import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Component
public class EventSourcingHandlerRegistry {
    private final Map<Class<?>, List<Method>> routes = new HashMap<>();

    public void registerHandler(Class<?> type, Method method) {
        routes.computeIfAbsent(type, methods -> new LinkedList<>()).add(method);
    }

    public String[] getAllTopics() {
        return routes.keySet().stream()
                .map(Class::getSimpleName) // Convert class name to topic name
                .toArray(String[]::new); // Convert to array
    }

    public Method getHandler(Class<?> commandType) {
        List<Method> methods = routes.get(commandType);
        if (methods == null && !methods.isEmpty()) {
            throw new RuntimeException("No Event Sourcing Handler is registered");
        }
        if (methods.size() > 1) {
            throw new RuntimeException("More than one Event Sourcing handler is registered");
        }
        return methods.get(0);
    }
}
