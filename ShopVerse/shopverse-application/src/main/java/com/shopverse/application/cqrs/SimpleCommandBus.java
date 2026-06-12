package com.shopverse.application.cqrs;

import org.springframework.context.ApplicationContext;
import org.springframework.stereotype.Component;

import java.lang.reflect.ParameterizedType;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Ch07-05: Simple Spring-based command bus — resolves handlers by command type.
 * Handlers are Spring beans; bus looks them up from ApplicationContext.
 */
@Component
public class SimpleCommandBus implements CommandBus {

    private final ApplicationContext ctx;
    private final Map<Class<?>, CommandHandler<?, ?>> cache = new ConcurrentHashMap<>();

    public SimpleCommandBus(ApplicationContext ctx) {
        this.ctx = ctx;
    }

    @Override
    @SuppressWarnings("unchecked")
    public <R> R dispatch(Object command) {
        CommandHandler<Object, R> handler =
            (CommandHandler<Object, R>) cache.computeIfAbsent(
                command.getClass(), this::resolveHandler);
        return handler.handle(command);
    }

    @SuppressWarnings("rawtypes")
    private CommandHandler<?, ?> resolveHandler(Class<?> commandType) {
        Map<String, CommandHandler> beans = ctx.getBeansOfType(CommandHandler.class);
        return beans.values().stream()
            .filter(h -> {
                try {
                    ParameterizedType pt = (ParameterizedType)
                        h.getClass().getGenericInterfaces()[0];
                    return pt.getActualTypeArguments()[0].equals(commandType);
                } catch (Exception e) { return false; }
            })
            .findFirst()
            .orElseThrow(() -> new IllegalStateException(
                "No handler for command: " + commandType.getSimpleName()));
    }
}
