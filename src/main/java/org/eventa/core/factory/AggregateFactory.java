package org.eventa.core.factory;

import lombok.extern.log4j.Log4j2;
import org.eventa.core.aggregates.Snapshot;
import org.eventa.core.eventstore.EventStore;
import org.eventa.core.repository.SnapshotRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.stereotype.Component;
import org.eventa.core.aggregates.AggregateRoot;
import org.eventa.core.events.BaseEvent;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Log4j2
@Component
public class AggregateFactory {

    @Autowired
    private EventStore eventStore;
    @Autowired
    private SnapshotRepository snapshotRepository;
    @Autowired
    private ApplicationContext applicationContext;

    public <T extends AggregateRoot> T loadAggregate(UUID aggregateId, Class<T> aggregateClass, boolean construct) throws Exception {
        if (construct) {
            return applicationContext.getBean(aggregateClass);
        }
        T aggregate = applicationContext.getBean(aggregateClass);
        try {
            /*Optional<Snapshot> snapshotOpt = snapshotRepository.findById(aggregateId);
            if (snapshotOpt.isPresent()) {
                aggregate.restoreSnapshot(snapshotOpt.get());
                List<BaseEvent> events = eventStore.findEventsAfterVersion(aggregateId, snapshotOpt.get().getVersion());
                aggregate.replayEvents(events);
            } else {

            }*/

            List<BaseEvent> events = eventStore.getEventsFromAggregate(aggregateId);
            aggregate.replayEvents(events);

            return aggregate;
        } catch (Exception e) {
            log.error(e.getMessage());
        }
        List<BaseEvent> events = eventStore.getEventsFromAggregate(aggregateId);
        if (events == null && events.isEmpty()) {
            throw new Exception("No event's found in the event store for this aggregate id " + aggregateId);
        }

        aggregate.replayEvents(events);
        return aggregate;
    }

}
