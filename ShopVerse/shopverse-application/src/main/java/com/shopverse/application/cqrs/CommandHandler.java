package com.shopverse.application.cqrs;

/** Ch07-05: Typed command handler — each command maps to exactly one handler. */
public interface CommandHandler<C, R> {
    R handle(C command);
}
