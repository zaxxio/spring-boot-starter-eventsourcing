package org.eventa.core.commands;

public class CommandResultMessage<T> {
    private final T result;
    private final Throwable exception;

    public CommandResultMessage(T result) {
        this.result = result;
        this.exception = null;
    }

    public CommandResultMessage(Throwable exception) {
        this.result = null;
        this.exception = exception;
    }

    public T getResult() {
        return result;
    }

    public Throwable getException() {
        return exception;
    }

    public boolean isExceptional() {
        return exception != null;
    }
}