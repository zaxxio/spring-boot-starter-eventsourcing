package org.eventa.core.eventstore;

import lombok.RequiredArgsConstructor;
import org.eventa.core.events.BaseEvent;
import org.eventa.core.producer.EventProducer;
import org.eventa.core.repository.EventStoreRepository;
import org.springframework.dao.ConcurrencyFailureException;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.Date;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;


@Component
@RequiredArgsConstructor
public class MongoEventStore implements EventStore {

    private final EventStoreRepository eventStoreRepository;
    private final EventProducer eventProducer;

    @Override
    @Transactional
    public void saveEvents(UUID aggregateId, String aggregateType, Iterable<BaseEvent> events, int expectedVersion) throws Exception {
        List<EventModel> eventStream = eventStoreRepository.findByAggregateIdentifier(aggregateId.toString());
        if (expectedVersion != -1 && eventStream.get(eventStream.size() - 1).getVersion() != expectedVersion) {
            throw new ConcurrencyFailureException("Concurrency problem with aggregate " + aggregateId);
        }
        int version = expectedVersion;
        for (BaseEvent event : events) {
            version++;
            event.setVersion(version);
            final EventModel eventModel = EventModel.builder()
                    .timestamp(new Date())
                    .aggregateIdentifier(String.valueOf(aggregateId))
                    .aggregateType(aggregateType)
                    .version(version)
                    .eventType(event.getClass().getTypeName())
                    .baseEvent(event)
                    .build();
            final EventModel persistedEventModel = eventStoreRepository.save(eventModel);
            if (persistedEventModel.getId() != null) {
                eventProducer.produce(event.getClass().getSimpleName(), event);
            }
        }
    }

    @Override
    public List<BaseEvent> getEventsFromAggregate(UUID aggregateId) {
        List<EventModel> eventStream = eventStoreRepository.findByAggregateIdentifier(aggregateId.toString());
        if (eventStream == null || eventStream.isEmpty()) {
            throw new RuntimeException("Aggregate " + aggregateId + " not found");
        }
        return eventStream.stream().map(EventModel::getBaseEvent).collect(Collectors.toList());
    }
}
