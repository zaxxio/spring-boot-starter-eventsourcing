package org.eventa.core.query;

import java.util.List;
import java.util.stream.Collectors;

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
}
