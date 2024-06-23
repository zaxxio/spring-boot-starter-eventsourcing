package org.eventa.core.dispatcher;

import org.eventa.core.query.ResponseType;

public interface QueryDispatcher {
    <Q, R> R dispatch(Q query, ResponseType<R> responseType);
}

