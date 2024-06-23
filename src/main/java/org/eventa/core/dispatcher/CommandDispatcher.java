package org.eventa.core.dispatcher;

import org.eventa.core.commands.BaseCommand;
import org.eventa.core.commands.CommandMessage;
import org.eventa.core.commands.CommandResultMessage;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;
import java.util.function.BiConsumer;

public interface CommandDispatcher {
    <T extends BaseCommand> String send(T command) throws Exception;

    <T extends BaseCommand> void send(T command, BiConsumer<CommandMessage<T>, CommandResultMessage<?>> callback) throws Exception;
}
