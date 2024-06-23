package org.eventa.core.aggregates;

import lombok.Getter;
import lombok.Setter;
import lombok.extern.log4j.Log4j2;
import org.eventa.core.registry.EventSourcingHandlerRegistry;
import org.eventa.core.streotype.AggregateSnapshot;
import org.eventa.core.streotype.RoutingKey;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.eventa.core.events.BaseEvent;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.*;
import java.util.concurrent.CopyOnWriteArrayList;


@Log4j2
public abstract class AggregateRoot implements ApplicationContextAware {

    @Getter
    protected UUID id;
    @Setter
    @Getter
    protected int version = -1;
    private ApplicationContext applicationContext;
    private final List<BaseEvent> changes = new CopyOnWriteArrayList<>();
    @Getter
    private final int snapshotInterval;

    public AggregateRoot() {
        AggregateSnapshot aggregateSnapshotAnnotation = this.getClass().getAnnotation(AggregateSnapshot.class);
        if (aggregateSnapshotAnnotation != null) {
            this.snapshotInterval = aggregateSnapshotAnnotation.interval();
        } else {
            this.snapshotInterval = 100;
        }
    }

    @Override
    public void setApplicationContext(ApplicationContext applicationContext) {
        this.applicationContext = applicationContext;
    }

    public List<BaseEvent> getUncommittedChanges() {
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

    private void apply(BaseEvent baseEvent, boolean isNewEvent) {
        handleEvent(baseEvent);
        if (isNewEvent) {
            changes.add(baseEvent);
        }
        this.version++;
    }

    public Snapshot takeSnapshot() {
        Snapshot snapshot = createSnapshot();
        log.info("Took Snapshot of the Aggregate.");
        return snapshot;
    }

    public void restoreSnapshot(Snapshot snapshot) {
        if (snapshot != null) {
            setAggregateState(snapshot.getState());
            this.id = snapshot.getId();
            this.version = snapshot.getVersion();
        }
    }

    private void setAggregateState(Object state) {
        try {
            if (state instanceof AggregateState aggregateState) {
                for (Field field : this.getClass().getDeclaredFields()) {
                    field.setAccessible(true);
                    if (aggregateState.hasField(field.getName())) {
                        field.set(this, aggregateState.getField(field.getName()));
                    }
                }
            }
        } catch (Exception e) {
            throw new RuntimeException("Failed to set aggregate state", e);
        }
    }

    private Snapshot createSnapshot() {
        Object state = getAggregateState();
        Snapshot snapshot = new Snapshot(id, version, state);
        System.out.println("Version : " + version);
        return snapshot;
    }

    private Object getAggregateState() {
        try {
            Field[] fields = this.getClass().getDeclaredFields();
            AggregateState state = new AggregateState();
            for (Field field : fields) {
                field.setAccessible(true);
                state.addField(field.getName(), field.get(this));
            }
            return state;
        } catch (Exception ex) {
            throw new RuntimeException("Failed to get aggregate state", ex);
        }
    }


    private void handleEvent(BaseEvent baseEvent) {
        EventSourcingHandlerRegistry registry = applicationContext.getBean(EventSourcingHandlerRegistry.class);
        Class<?> clazz = baseEvent.getClass();
        Method handler = registry.getHandler(clazz);
        if (handler != null && handler.getDeclaringClass().isAssignableFrom(this.getClass())) {

            for (Field field : this.getClass().getDeclaredFields()) {
                if (field.isAnnotationPresent(RoutingKey.class)) {
                    field.setAccessible(true);
                    try {
                        this.id = baseEvent.getId();
                        field.set(this, baseEvent.getId());
                    } catch (IllegalAccessException e) {
                        throw new RuntimeException(e);
                    }
                }
            }

            try {
                handler.invoke(this, baseEvent);
            } catch (Exception e) {
                throw new RuntimeException("Failed to handle event", e);
            }
        }
    }


}
