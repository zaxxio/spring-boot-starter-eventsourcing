package org.eventa.core.gateway;

import org.eventa.core.query.ResponseType;

public interface QueryDispatcher {
    <Q, R> R dispatch(Q query, ResponseType<R> responseType);
}

