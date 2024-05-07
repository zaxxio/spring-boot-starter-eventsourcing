package org.eventa.core.processor;

import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.eventa.core.registry.CommandHandlerRegistry;
import org.eventa.core.registry.EventHandlerRegistry;
import org.eventa.core.registry.EventSourcingHandlerRegistry;
import org.eventa.core.registry.QueryHandlerRegistry;
import org.eventa.core.streotype.*;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationListener;
import org.springframework.context.event.ContextRefreshedEvent;
import org.springframework.stereotype.Component;
import org.wsd.core.streotype.*;

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

    @Override
    public void onApplicationEvent(ContextRefreshedEvent event) {
        final Map<String, Object> aggregates = this.applicationContext.getBeansWithAnnotation(Aggregate.class);

        final Map<String, Object> projectionGroups = this.applicationContext.getBeansWithAnnotation(ProjectionGroup.class);

        for (Map.Entry<String, Object> entry : projectionGroups.entrySet()) {
            Class<?> aClass = entry.getValue().getClass();
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
            Class<?> aClass = entry.getValue().getClass();

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
    }
}
