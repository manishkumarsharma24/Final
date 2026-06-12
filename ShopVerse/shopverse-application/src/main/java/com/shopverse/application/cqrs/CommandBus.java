package com.shopverse.application.cqrs;

/**
 * Ch07-05: Command bus interface — routes commands to handlers.
 * Separates write path from read path (CQRS).
 */
public interface CommandBus {
    <R> R dispatch(Object command);
}
