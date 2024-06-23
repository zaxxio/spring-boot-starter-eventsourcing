package org.eventa.core.saga;

import lombok.Data;
import org.eventa.core.events.BaseEvent;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

import java.util.UUID;

@Data
@Document(collection = "sagas")
public class SagaState {
    @Id
    private String id;
    @Indexed
    private UUID sagaId;
    private String stepName;
    private Object payload;
}
