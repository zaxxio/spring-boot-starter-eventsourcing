package org.eventa.core.snapshot;

import java.util.UUID;

public interface Snapshot {
    UUID getAggregateId();
    int version();
}
