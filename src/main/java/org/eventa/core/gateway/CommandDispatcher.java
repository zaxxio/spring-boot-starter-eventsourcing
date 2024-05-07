package org.eventa.core.gateway;

import org.eventa.core.commands.BaseCommand;

public interface CommandDispatcher {
    <T extends BaseCommand> void send(T command) throws Exception;
}
