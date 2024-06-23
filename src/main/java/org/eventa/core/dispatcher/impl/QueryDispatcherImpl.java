package org.eventa.core.dispatcher.impl;

import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationContext;
import org.springframework.stereotype.Service;
import org.eventa.core.dispatcher.QueryDispatcher;
import org.eventa.core.query.ResponseType;
import org.eventa.core.registry.QueryHandlerRegistry;

import java.lang.reflect.Method;

@Service
@RequiredArgsConstructor
public class QueryDispatcherImpl implements QueryDispatcher {
    private final QueryHandlerRegistry queryHandlerRegistry;
    private final ApplicationContext applicationContext;

    @Override
    public <Q, R> R dispatch(Q query, ResponseType<R> responseType) {
        Method queryMethod = queryHandlerRegistry.getHandler(query.getClass());
        if (queryMethod == null) {
            throw new RuntimeException("No handler found for query: " + query.getClass().getName());
        }
        try {
            Object handlerBean = applicationContext.getBean(queryMethod.getDeclaringClass());
            Object result = queryMethod.invoke(handlerBean, query);
            return responseType.convert(result);
        } catch (Exception e) {
            throw new RuntimeException("Failed to invoke handler for query: " + query.getClass().getName(), e);
        }
    }
}
