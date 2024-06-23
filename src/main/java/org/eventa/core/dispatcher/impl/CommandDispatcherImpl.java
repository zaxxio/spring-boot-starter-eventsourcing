package org.eventa.core.dispatcher.impl;

import lombok.extern.log4j.Log4j2;
import org.eventa.core.aggregates.AggregateRoot;
import org.eventa.core.cache.CacheConcurrentHashMap;
import org.eventa.core.commands.BaseCommand;
import org.eventa.core.commands.CommandMessage;
import org.eventa.core.commands.CommandResultMessage;
import org.eventa.core.dispatcher.CommandDispatcher;
import org.eventa.core.events.BaseEvent;
import org.eventa.core.eventstore.EventStore;
import org.eventa.core.factory.AggregateFactory;
import org.eventa.core.interceptor.CommandInterceptorRegisterer;
import org.eventa.core.registry.CommandHandlerRegistry;
import org.eventa.core.repository.SnapshotRepository;
import org.eventa.core.streotype.CommandHandler;
import org.springframework.stereotype.Component;

import java.lang.reflect.Method;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;
import java.util.function.BiConsumer;

@Log4j2
@Component
public class CommandDispatcherImpl implements CommandDispatcher {

    private final CommandInterceptorRegisterer commandInterceptorRegisterer;
    private final CommandHandlerRegistry commandHandlerRegistry;
    private final AggregateFactory aggregateFactory;
    private final EventStore eventStore;
    private final SnapshotRepository snapshotRepository;
    private final CacheConcurrentHashMap<UUID, Lock> locks = new CacheConcurrentHashMap<>( 10); // LRUCache

    public CommandDispatcherImpl(CommandInterceptorRegisterer commandInterceptorRegisterer,
                                 CommandHandlerRegistry commandHandlerRegistry,
                                 AggregateFactory aggregateFactory,
                                 EventStore eventStore,
                                 SnapshotRepository snapshotRepository) {
        this.commandInterceptorRegisterer = commandInterceptorRegisterer;
        this.commandHandlerRegistry = commandHandlerRegistry;
        this.aggregateFactory = aggregateFactory;
        this.eventStore = eventStore;
        this.snapshotRepository = snapshotRepository;
    }

    private Lock getLock(UUID aggregateId) {
        return locks.computeIfAbsent(aggregateId, id -> new ReentrantLock());
    }

    @Override
    public <T extends BaseCommand> String send(T command) throws Exception {
        commandInterceptorRegisterer.getCommandInterceptors().forEach(commandInterceptor -> commandInterceptor.preHandle(command));
        Method commandHandlerMethod = commandHandlerRegistry.getHandler(command.getClass());

        if (commandHandlerMethod != null) {
            Class<?> aggregateClass = commandHandlerMethod.getDeclaringClass();
            UUID aggregateId = command.getId();
            Lock lock = getLock(aggregateId);
            lock.lock();
            try {
                AggregateRoot aggregate = aggregateFactory.loadAggregate(aggregateId, aggregateClass.asSubclass(AggregateRoot.class), commandHandlerMethod.getAnnotation(CommandHandler.class).constructor());
                commandHandlerMethod.invoke(aggregate, command);
                List<BaseEvent> uncommittedChanges = aggregate.getUncommittedChanges();
                CompletableFuture<String> future = eventStore.saveEvents(aggregateId, aggregateClass.getSimpleName(), uncommittedChanges, aggregate.getVersion(), commandHandlerMethod.getAnnotation(CommandHandler.class).constructor());
                aggregate.markChangesAsCommitted();
                commandInterceptorRegisterer.getCommandInterceptors().forEach(commandInterceptor -> commandInterceptor.postHandle(command));
                return future.join();
            } finally {
                lock.unlock();
            }
        }
        return null;
    }

    @Override
    public <T extends BaseCommand> void send(T command, BiConsumer<CommandMessage<T>, CommandResultMessage<?>> callback) throws Exception {
        commandInterceptorRegisterer.getCommandInterceptors().forEach(commandInterceptor -> commandInterceptor.preHandle(command));
        Method commandHandlerMethod = commandHandlerRegistry.getHandler(command.getClass());

        if (commandHandlerMethod != null) {
            CompletableFuture.runAsync(() -> {
                Lock lock = null;
                try {
                    Class<?> aggregateClass = commandHandlerMethod.getDeclaringClass();
                    UUID aggregateId = command.getId();
                    lock = getLock(aggregateId);
                    lock.lock();

                    AggregateRoot aggregate = aggregateFactory.loadAggregate(aggregateId, aggregateClass.asSubclass(AggregateRoot.class), commandHandlerMethod.getAnnotation(CommandHandler.class).constructor());
                    commandHandlerMethod.invoke(aggregate, command);

                    List<BaseEvent> uncommittedChanges = aggregate.getUncommittedChanges();
                    eventStore.saveEvents(aggregateId, aggregateClass.getSimpleName(), uncommittedChanges, aggregate.getVersion(), commandHandlerMethod.getAnnotation(CommandHandler.class).constructor());

                    aggregate.markChangesAsCommitted();
                    callback.accept(new CommandMessage<>(command), new CommandResultMessage<>(null));
                } catch (Exception e) {
                    callback.accept(new CommandMessage<>(command), new CommandResultMessage<>(e));
                } finally {
                    if (lock != null) {
                        lock.unlock();
                    }
                }
            });
        }

        commandInterceptorRegisterer.getCommandInterceptors().forEach(commandInterceptor -> commandInterceptor.postHandle(command));
    }

}
