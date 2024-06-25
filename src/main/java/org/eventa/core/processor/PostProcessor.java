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

package org.eventa.core.processor;

import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.eventa.core.registry.*;
import org.eventa.core.streotype.*;
import org.springframework.aop.framework.AopProxyUtils;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationListener;
import org.springframework.context.event.ContextRefreshedEvent;
import org.springframework.stereotype.Component;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.Map;

@Log4j2
@Component
@RequiredArgsConstructor
public class PostProcessor implements ApplicationListener<ContextRefreshedEvent> {

    private final ApplicationContext applicationContext;
    private final CommandHandlerRegistry commandHandlerRegistry;
    private final EventSourcingHandlerRegistry eventSourcingHandlerRegistry;
    private final EventHandlerRegistry eventHandlerRegistry;
    private final QueryHandlerRegistry queryHandlerRegistry;
    private final SagaHandlerRegistry sagaHandlerRegistry;
    private final LeaderHandlerRegistry leaderHandlerRegistry;
    private final NotLeaderHandlerRegistry notLeaderHandlerRegistry;

    @Override
    public void onApplicationEvent(ContextRefreshedEvent event) {

        final Map<String, Object> aggregates = this.applicationContext.getBeansWithAnnotation(Aggregate.class);

        final Map<String, Object> projectionGroups = this.applicationContext.getBeansWithAnnotation(ProjectionGroup.class);

        final Map<String, Object> sagas = this.applicationContext.getBeansWithAnnotation(Saga.class);

        for (Map.Entry<String, Object> entry : projectionGroups.entrySet()) {
            Class<?> aClass = AopProxyUtils.ultimateTargetClass(entry.getValue());
            Arrays.stream(aClass.getDeclaredMethods())
                    .filter(method -> method.isAnnotationPresent(EventHandler.class))
                    .forEach(method -> {
                        Class<?>[] parameterTypes = method.getParameterTypes();
                        if (parameterTypes.length == 1) {
                            eventHandlerRegistry.registerHandler(parameterTypes[0], method);
                        } else {
                            log.error("Problem");
                        }
                    });

            Arrays.stream(aClass.getDeclaredMethods())
                    .filter(method -> method.isAnnotationPresent(QueryHandler.class))
                    .forEach(method -> {
                        Class<?>[] parameterTypes = method.getParameterTypes();
                        if (parameterTypes.length == 1) {
                            queryHandlerRegistry.registerHandler(parameterTypes[0], method);
                        } else {
                            log.error("Problem");
                        }
                    });
        }

        for (Map.Entry<String, Object> entry : aggregates.entrySet()) {
            Class<?> aClass = AopProxyUtils.ultimateTargetClass(entry.getValue());

            boolean isRoutingKeyExists = false;
            for (Field field : aClass.getDeclaredFields()) {
                if (field.isAnnotationPresent(RoutingKey.class)) {
                    isRoutingKeyExists = true;
                }
            }

            if (!isRoutingKeyExists) {
                throw new RuntimeException("Aggregate must have routing key with @RoutingKey annotation.");
            }

            Arrays.stream(aClass.getDeclaredMethods())
                    .filter(method -> method.isAnnotationPresent(CommandHandler.class))
                    .forEach(method -> {
                        Class<?>[] parameterTypes = method.getParameterTypes();
                        if (parameterTypes.length == 1) {
                            commandHandlerRegistry.registerHandler(parameterTypes[0], method);
                        } else {
                            log.error("Problem");
                        }
                    });


            Arrays.stream(aClass.getDeclaredMethods())
                    .filter(method -> method.isAnnotationPresent(EventSourcingHandler.class))
                    .forEach(method -> {
                        Class<?>[] parameterTypes = method.getParameterTypes();
                        if (parameterTypes.length == 1) {
                            eventSourcingHandlerRegistry.registerHandler(parameterTypes[0], method);
                        } else {
                            log.error("Problem in");
                        }
                    });
        }

        for (Object sagaBean : sagas.values()) {
            Class<?> sagaClass = AopProxyUtils.ultimateTargetClass(sagaBean);
            for (Method method : sagaClass.getDeclaredMethods()) {
                if (method.isAnnotationPresent(StartSaga.class)) {
                    sagaHandlerRegistry.registerStartSagaHandler(method.getParameterTypes()[0], method);
                }
                if (method.isAnnotationPresent(EndSaga.class)) {
                    sagaHandlerRegistry.registerEndSagaHandler(method.getParameterTypes()[0], method);
                }
                if (method.isAnnotationPresent(SagaEventHandler.class)) {
                    sagaHandlerRegistry.registerSagaEventHandler(method.getParameterTypes()[0], method);
                }
            }
        }

        for (String contextBeanDefinitionName : this.applicationContext.getBeanDefinitionNames()) {
            Object bean = this.applicationContext.getBean(contextBeanDefinitionName);
            Class<?> clazz = AopProxyUtils.ultimateTargetClass(bean);
            for (Method method : clazz.getDeclaredMethods()) {
                if (method.isAnnotationPresent(Leader.class)) {
                    leaderHandlerRegistry.registerHandler(clazz, method);
                }
                if (method.isAnnotationPresent(NotLeader.class)) {
                    notLeaderHandlerRegistry.registerHandler(clazz, method);
                }
            }
        }


        log.info("Eventa AutoConfiguration Loaded.");

        String logo = """

                    ______                 __      \s
                   / ____/   _____  ____  / /_____ _
                  / __/ | | / / _ \\/ __ \\/ __/ __ `/
                 / /___ | |/ /  __/ / / / /_/ /_/ /\s
                /_____/ |___/\\___/_/ /_/\\__/\\__,_/ \s
                                                   \s
                Eventa version 1.0.0
                """;
        System.out.println(logo);
    }
}
