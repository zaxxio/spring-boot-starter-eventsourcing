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
