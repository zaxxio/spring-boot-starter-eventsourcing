package org.eventa.core.repository;

import org.eventa.core.saga.SagaState;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface SagaStateRepository extends MongoRepository<SagaState, UUID> {
    Optional<SagaState> findBySagaId(UUID sagaId);
}
