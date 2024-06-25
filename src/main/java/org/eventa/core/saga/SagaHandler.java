/*
 * MIT License
 *
 * Copyright (c) 2020 Partha Sutradhar.
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */
package org.eventa.core.saga;


import lombok.RequiredArgsConstructor;
import org.eventa.core.cache.CacheConcurrentHashMap;
import org.eventa.core.events.BaseEvent;
import org.eventa.core.registry.SagaHandlerRegistry;
import org.eventa.core.repository.SagaStateRepository;
import org.eventa.core.streotype.EndSaga;
import org.eventa.core.streotype.SagaEventHandler;
import org.eventa.core.streotype.StartSaga;
import org.springframework.context.ApplicationContext;
import org.springframework.stereotype.Component;

import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.Optional;
import java.util.UUID;





import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationContext;
import org.springframework.stereotype.Component;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

@Component
@RequiredArgsConstructor
public class SagaHandler {

    private final ApplicationContext applicationContext;
    private final SagaStateRepository sagaStateRepository;
    private final SagaHandlerRegistry sagaHandlerRegistry;
    private final CacheConcurrentHashMap<UUID, Lock> sagaLocks = new CacheConcurrentHashMap<>(10000);

    public void handleSagaEvent(BaseEvent event) {
        Method method = findSagaMethod(event.getClass());
        if (method == null) {
            return;
        }
        UUID sagaId = getSagaId(event, method);
        Lock lock = getLock(sagaId);
        lock.lock();
        try {
            Object sagaInstance = applicationContext.getBean(method.getDeclaringClass());
            method.invoke(sagaInstance, event);
            manageSagaState(event, method);
        } catch (Exception e) {
            throw new RuntimeException("Failed to invoke saga method", e);
        } finally {
            lock.unlock();
        }
    }

    private Method findSagaMethod(Class<?> eventClass) {
        Method method = sagaHandlerRegistry.getStartSagaMethod(eventClass);
        if (method == null) {
            method = sagaHandlerRegistry.getEndSagaMethod(eventClass);
        }
        if (method == null) {
            method = sagaHandlerRegistry.getSagaEventHandlerMethod(eventClass);
        }
        return method;
    }

    private void manageSagaState(Object event, Method method) throws Exception {
        if (method.isAnnotationPresent(StartSaga.class)) {
            saveSagaState(event, method);
        } else if (method.isAnnotationPresent(EndSaga.class)) {
            removeSagaState(event, method);
        }
    }

    private void saveSagaState(Object event, Method method) throws Exception {
        SagaState sagaState = new SagaState();
        sagaState.setSagaId(getSagaId(event, method));
        sagaState.setStepName(method.getParameterTypes()[0].getName());
        sagaState.setPayload(event);
        sagaStateRepository.save(sagaState);
    }

    private void removeSagaState(Object event, Method method) {
        UUID sagaId = getSagaId(event, method);
        Optional<SagaState> sagaState = sagaStateRepository.findBySagaId(sagaId);
        sagaState.ifPresent(sagaStateRepository::delete);
    }

    private Field findFieldInClassHierarchy(Class<?> clazz, String fieldName) throws NoSuchFieldException {
        while (clazz != null) {
            try {
                return clazz.getDeclaredField(fieldName);
            } catch (NoSuchFieldException e) {
                clazz = clazz.getSuperclass();
            }
        }
        throw new NoSuchFieldException("Field " + fieldName + " not found in class hierarchy");
    }

    private UUID getSagaId(Object event, Method method) {
        if (method != null && method.isAnnotationPresent(SagaEventHandler.class)) {
            SagaEventHandler annotation = method.getAnnotation(SagaEventHandler.class);
            String associationProperty = annotation.associationProperty();
            try {
                Field field = findFieldInClassHierarchy(event.getClass(), associationProperty);
                field.setAccessible(true);
                Object value = field.get(event);
                if (value != null) {
                    if (value instanceof UUID) {
                        return (UUID) value;
                    } else if (value instanceof String) {
                        return UUID.fromString((String) value);
                    }
                }
            } catch (NoSuchFieldException | IllegalAccessException | IllegalArgumentException e) {
                e.printStackTrace();
            }
        }
        return UUID.randomUUID();
    }

    private Lock getLock(UUID sagaId) {
        return sagaLocks.computeIfAbsent(sagaId, id -> new ReentrantLock());
    }
}


