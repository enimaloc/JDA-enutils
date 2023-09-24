package fr.enimaloc.enutils.jda.register.processor;

import fr.enimaloc.enutils.jda.registered.RegisteredCommand;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;

public interface CommandsProcessor<T extends RegisteredCommand> {
    Logger LOGGER = LoggerFactory.getLogger(CommandsProcessor.class);

    default T[] registerCommands(Object instance, Method[] methods) {
        List<T> commands = new ArrayList<>();
        for (Method method : methods) {
            commands.add(registerCommand(instance, method));
        }
        return commands.toArray(this::generateArray);
    }

    T registerCommand(Object instance, Method method);

    default T[] registerCommand(Object instance) {
        if (instance == null) {
            throw new IllegalArgumentException("Instance cannot be null");
        }
        return registerCommands(instance, instance.getClass().getDeclaredMethods());
    }

    default T[] registerCommands(Object[] instances) {
        List<T> commands = new ArrayList<>();
        for (Object instance : instances) {
            T[] elements = registerCommand(instance);
            if (elements == null || elements.length == 0) {
                LOGGER.warn("No command found for instance {}", instance.getClass().getName());
                continue;
            }
            commands.addAll(Arrays.stream(elements).filter(Objects::nonNull).toList());
        }
    return commands.toArray(this::generateArray);
    }

    T[] generateArray(int length);
}
