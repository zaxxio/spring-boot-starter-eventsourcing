package org.eventa.core.query;

import java.util.List;
import java.util.stream.Collectors;
import java.util.Map;
import java.util.Set;

public interface ResponseType<T> {
    T convert(Object result);

    static <T> ResponseType<T> instanceOf(Class<T> type) {
        return result -> type.cast(result);
    }

    static <T> ResponseType<List<T>> multipleInstancesOf(Class<T> type) {
        return result -> {
            if (result instanceof List<?>) {
                return ((List<?>) result).stream()
                        .filter(type::isInstance)
                        .map(type::cast)
                        .collect(Collectors.toList());
            } else {
                throw new IllegalArgumentException("Expected a list of " + type.getName() + " but got " + result.getClass().getName());
            }
        };
    }

    static <K, V> ResponseType<Map<K, V>> mapOf(Class<K> keyType, Class<V> valueType) {
        return result -> {
            if (result instanceof Map<?, ?>) {
                return ((Map<?, ?>) result).entrySet().stream()
                        .filter(entry -> keyType.isInstance(entry.getKey()) && valueType.isInstance(entry.getValue()))
                        .collect(Collectors.toMap(
                                entry -> keyType.cast(entry.getKey()),
                                entry -> valueType.cast(entry.getValue())
                        ));
            } else {
                throw new IllegalArgumentException("Expected a map but got " + result.getClass().getName());
            }
        };
    }

    static <T> ResponseType<Set<T>> setOf(Class<T> type) {
        return result -> {
            if (result instanceof Set<?>) {
                return ((Set<?>) result).stream()
                        .filter(type::isInstance)
                        .map(type::cast)
                        .collect(Collectors.toSet());
            } else {
                throw new IllegalArgumentException("Expected a set of " + type.getName() + " but got " + result.getClass().getName());
            }
        };
    }
}
