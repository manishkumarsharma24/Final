package com.shopverse.application.cqrs;

/**
 * Ch07-05: Query bus — routes queries to read handlers.
 */
public interface QueryBus {
    <R> R dispatch(Object query);
}
