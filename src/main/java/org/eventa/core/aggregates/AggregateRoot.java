package org.eventa.core.aggregates;

import org.eventa.core.registry.EventSourcingHandlerRegistry;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.eventa.core.events.BaseEvent;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;


public abstract class AggregateRoot implements ApplicationContextAware {

    protected UUID id;
    protected int version = -1;
    private ApplicationContext applicationContext;
    private List<BaseEvent> changes = new ArrayList<>();

    public UUID getId() {
        return id;
    }

    public void setVersion(int version) {
        this.version = version;
    }

    public int getVersion() {
        return version;
    }

    @Override
    public void setApplicationContext(ApplicationContext applicationContext) {
        this.applicationContext = applicationContext;
    }

    public List<BaseEvent> getUncommitedChanges() {
        return this.changes;
    }

    public void markChangesAsCommitted() {
        this.changes.clear();
    }

    public void replayEvents(List<BaseEvent> events) {
        for (BaseEvent event : events) {
            apply(event, false);
        }
    }

    protected void apply(BaseEvent baseEvent) {
        apply(baseEvent, true);
    }

    protected void apply(BaseEvent baseEvent, boolean isNewEvent) {
        handleEvent(baseEvent);
        if (isNewEvent) {
            changes.add(baseEvent);
        }
    }

    private void handleEvent(BaseEvent baseEvent) {
        EventSourcingHandlerRegistry registry = applicationContext.getBean(EventSourcingHandlerRegistry.class);
        Class<?> clazz = baseEvent.getClass();
        Method handler = registry.getHandler(clazz);
        if (handler != null && handler.getDeclaringClass().isAssignableFrom(this.getClass())) {
            try {
                handler.invoke(this, baseEvent);
            } catch (Exception e) {
                throw new RuntimeException("Failed to handle event", e);
            }
        }
    }
}
