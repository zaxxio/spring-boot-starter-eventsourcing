package org.eventa.core.aggregates;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.Setter;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import java.util.UUID;

@Getter
@Setter
@RequiredArgsConstructor
@Document(collection = "snapshots")
public class Snapshot {
    @Id
    private final UUID id;
    private final int version;
    private final Object state;
}