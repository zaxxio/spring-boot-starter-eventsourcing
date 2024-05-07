package org.eventa.core.gateway;

import org.eventa.core.events.BaseEvent;

public interface EventDispatcher {
    void dispatch(BaseEvent baseEvent);
}
