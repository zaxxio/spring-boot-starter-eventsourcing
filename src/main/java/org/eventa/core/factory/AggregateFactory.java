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
