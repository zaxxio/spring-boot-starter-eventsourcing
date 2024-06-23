package org.eventa.core.repository;

import org.eventa.core.aggregates.Snapshot;
import org.springframework.data.mongodb.repository.MongoRepository;

import java.util.UUID;

public interface SnapshotRepository extends MongoRepository<Snapshot, UUID> {
}
