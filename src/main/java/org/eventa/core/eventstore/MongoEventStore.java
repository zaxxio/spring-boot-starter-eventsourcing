package org.eventa.core.eventstore;

import lombok.RequiredArgsConstructor;
import org.eventa.core.cache.CacheConcurrentHashMap;
import org.eventa.core.events.BaseEvent;
import org.eventa.core.producer.EventProducer;
import org.eventa.core.repository.EventStoreRepository;
import org.springframework.dao.ConcurrencyFailureException;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.stereotype.Component;

import java.util.Date;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;
import java.util.stream.Collectors;


@Component
@RequiredArgsConstructor
public class MongoEventStore implements EventStore {

    private final EventStoreRepository eventStoreRepository;
    private final EventProducer eventProducer;
    private final MongoTemplate mongoTemplate;

    private final CacheConcurrentHashMap<UUID, Lock> locks = new CacheConcurrentHashMap<>(10); // Adjust the max size as needed

    private Lock getLock(UUID aggregateId) {
        synchronized (locks) {
            return locks.computeIfAbsent(aggregateId, id -> new ReentrantLock());
        }
    }

    @Override
    public void saveEvents(UUID aggregateId, String aggregateType, Iterable<BaseEvent> events, int expectedVersion, boolean constructor) throws Exception {
        Lock lock = getLock(aggregateId);
        lock.lock();
        try {
            List<EventModel> eventStream = eventStoreRepository.findByAggregateIdentifier(aggregateId);
            if (!isEmpty(eventStream) && expectedVersion != -1 && eventStream.get(eventStream.size() - 1).getVersion() != expectedVersion) {
                if (constructor) {
                    throw new RuntimeException("Aggregate with Id " + aggregateId + " already exists");
                }
                throw new ConcurrencyFailureException("Concurrency problem with aggregate " + aggregateId);
            }
            int version = expectedVersion;
            for (BaseEvent event : events) {
                version++;
                event.setVersion(version);
                final EventModel eventModel = EventModel.builder()
                        .timestamp(new Date())
                        .aggregateIdentifier(aggregateId)
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
        } finally {
            lock.unlock();
        }
    }

    private static boolean isEmpty(List<EventModel> eventStream) {
        return eventStream == null || eventStream.isEmpty();
    }

    @Override
    public List<BaseEvent> getEventsFromAggregate(UUID aggregateId) {
        Lock lock = getLock(aggregateId);
        lock.lock();
        try {
            List<EventModel> eventStream = eventStoreRepository.findByAggregateIdentifier(aggregateId);
            if (isEmpty(eventStream)) {
                throw new RuntimeException("Aggregate " + aggregateId + " not found");
            }
            return eventStream.stream().map(EventModel::getBaseEvent).collect(Collectors.toList());
        } finally {
            lock.unlock();
        }
    }

    @Override
    public List<BaseEvent> findEventsAfterVersion(UUID aggregateId, int version) {
        Lock lock = getLock(aggregateId);
        lock.lock();
        try {
            Query query = new Query();
            query.addCriteria(Criteria.where("aggregateIdentifier").is(aggregateId).and("version").gte(version));
            return mongoTemplate.find(query, BaseEvent.class);
        } finally {
            lock.unlock();
        }
    }

    @Override
    public CompletableFuture<String> saveEvents(UUID aggregateId, String aggregateType, List<BaseEvent> events, int expectedVersion, boolean constructor) throws Exception {
        Lock lock = getLock(aggregateId);
        lock.lock();
        try {
            List<EventModel> eventStream = eventStoreRepository.findByAggregateIdentifier(aggregateId);
            if (!isEmpty(eventStream) && expectedVersion != -1 && eventStream.get(eventStream.size() - 1).getVersion() != expectedVersion) {
                if (constructor) {
                    throw new RuntimeException("Aggregate with Id " + aggregateId + " already exists");
                }
                throw new ConcurrencyFailureException("Concurrency problem with aggregate " + aggregateId);
            }
            int version = expectedVersion;
            CompletableFuture<String> future = null;
            for (BaseEvent event : events) {
                version++;
                event.setVersion(version);
                final EventModel eventModel = EventModel.builder()
                        .timestamp(new Date())
                        .aggregateIdentifier(aggregateId)
                        .aggregateType(aggregateType)
                        .version(version)
                        .eventType(event.getClass().getTypeName())
                        .baseEvent(event)
                        .build();
                final EventModel persistedEventModel = eventStoreRepository.save(eventModel);
                if (persistedEventModel.getId() != null) {
                    future = eventProducer.produceEvent(event.getClass().getSimpleName(), event);
                }
            }
            return future;
        } finally {
            lock.unlock();
        }
    }
}
