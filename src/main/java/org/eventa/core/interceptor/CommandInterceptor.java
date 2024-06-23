package org.eventa.core.interceptor;

import org.eventa.core.commands.BaseCommand;

public interface CommandInterceptor {
    void preHandle(BaseCommand command);
    void postHandle(BaseCommand command);
}
