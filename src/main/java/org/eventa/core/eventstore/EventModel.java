package org.eventa.core.eventstore;

import lombok.Builder;
import lombok.Getter;
import lombok.Setter;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;
import org.eventa.core.events.BaseEvent;

import java.util.Date;
import java.util.UUID;

@Getter
@Setter
@Builder
@Document(collection = "events")
public class EventModel {
    @Id
    private String id;
    @Indexed
    private UUID aggregateIdentifier;
    private String aggregateType;
    private String eventType;
    @Indexed
    private Integer version;
    private BaseEvent baseEvent;
    private Date timestamp;
}
