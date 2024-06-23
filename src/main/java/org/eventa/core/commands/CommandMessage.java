package org.eventa.core.commands;

public class CommandMessage<T> {
    private final T command;

    public CommandMessage(T command) {
        this.command = command;
    }

    public T getCommand() {
        return command;
    }
}
