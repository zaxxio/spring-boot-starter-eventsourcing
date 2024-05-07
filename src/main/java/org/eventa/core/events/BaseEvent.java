package org.eventa.core.events;

import lombok.*;
import lombok.experimental.SuperBuilder;
import org.eventa.core.messages.Message;

@Getter
@Setter
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
public abstract class BaseEvent extends Message {
    private int version;
}
