package fr.enimaloc.enutils.jda.register.processor;

import fr.enimaloc.enutils.jda.commands.RegisteredCommand;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;

public interface CommandsProcessor {
    Logger LOGGER = LoggerFactory.getLogger(CommandsProcessor.class);

    default RegisteredCommand[] registerCommands(Object instance, Method[] methods) {
        List<RegisteredCommand> commands = new ArrayList<>();
        for (Method method : methods) {
            commands.add(registerCommand(instance, method));
        }
        return commands.toArray(new RegisteredCommand[0]);
    }

    RegisteredCommand registerCommand(Object instance, Method method);

    default RegisteredCommand[] registerCommand(Object instance) {
        if (instance == null) {
            throw new IllegalArgumentException("Instance cannot be null");
        }
        return registerCommands(instance, instance.getClass().getDeclaredMethods());
    }

    default RegisteredCommand[] registerCommands(Object[] instances) {
        List<RegisteredCommand> commands = new ArrayList<>();
        for (Object instance : instances) {
            RegisteredCommand[] elements = registerCommand(instance);
            if (elements == null || elements.length == 0) {
                LOGGER.warn("No command found for instance {}", instance.getClass().getName());
                continue;
            }
            elements = Arrays.stream(elements).filter(Objects::nonNull).toArray(RegisteredCommand[]::new);
            commands.addAll(List.of(elements));
        }
        return commands.toArray(new RegisteredCommand[0]);
    }
}
