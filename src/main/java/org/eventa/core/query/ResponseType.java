/*
 *
 *  * MIT License
 *  *
 *  * Copyright (c) 2024 Partha Sutradhar.
 *  *
 *  * Permission is hereby granted, free of charge, to any person obtaining a copy
 *  * of this software and associated documentation files (the "Software"), to deal
 *  * in the Software without restriction, including without limitation the rights
 *  * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 *  * copies of the Software, and to permit persons to whom the Software is
 *  * furnished to do so, subject to the following conditions:
 *  *
 *  * The above copyright notice and this permission notice shall be included in all
 *  * copies or substantial portions of the Software.
 *  *
 *  * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 *  * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 *  * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 *  * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 *  * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 *  * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 *  * SOFTWARE.
 *
 */

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
