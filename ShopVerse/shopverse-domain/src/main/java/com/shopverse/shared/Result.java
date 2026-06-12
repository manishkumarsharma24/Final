package com.shopverse.shared;

import java.util.function.Function;

/**
 * Ch02-01: Sealed Result type — functional error handling without exceptions.
 * Ch14-xx: Used in command handlers and use-cases.
 *
 * @param <T> success value type
 * @param <E> failure value type
 */
public sealed interface Result<T, E> permits Result.Success, Result.Failure {

    record Success<T, E>(T value) implements Result<T, E> {}
    record Failure<T, E>(E error) implements Result<T, E> {}

    static <T, E> Result<T, E> success(T value) { return new Success<>(value); }
    static <T, E> Result<T, E> failure(E error)  { return new Failure<>(error); }

    default boolean isSuccess() { return this instanceof Success<T, E>; }
    default boolean isFailure() { return this instanceof Failure<T, E>; }

    default T getOrThrow(Function<E, RuntimeException> exceptionMapper) {
        return switch (this) {
            case Success<T, E> s -> s.value();
            case Failure<T, E> f -> throw exceptionMapper.apply(f.error());
        };
    }

    default <U> Result<U, E> map(Function<T, U> mapper) {
        return switch (this) {
            case Success<T, E> s -> Result.success(mapper.apply(s.value()));
            case Failure<T, E> f -> Result.failure(f.error());
        };
    }
}
