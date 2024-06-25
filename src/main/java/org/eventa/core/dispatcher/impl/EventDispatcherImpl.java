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
