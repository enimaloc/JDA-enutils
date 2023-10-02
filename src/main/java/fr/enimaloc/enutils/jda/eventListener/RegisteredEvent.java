package fr.enimaloc.enutils.jda.eventListener;

import fr.enimaloc.enutils.jda.JDAEnutils;
import fr.enimaloc.enutils.jda.commands.GlobalSlashCommandEvent;
import fr.enimaloc.enutils.jda.commands.GuildSlashCommandEvent;
import fr.enimaloc.enutils.jda.exception.RegisteredExceptionHandler;
import net.dv8tion.jda.api.events.Event;
import net.dv8tion.jda.api.events.GenericEvent;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.interactions.InteractionHook;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.function.Predicate;

public record RegisteredEvent<T extends GenericEvent>(
        Class<T> event,
        Object instance,
        Method method,
        RegisteredExceptionHandler<T>[] exceptionsHandler,
        Priority priority,
        boolean terminal,
        Predicate<T> filter
) {
    public boolean execute(T event) {
        if (!filter.test(event)) {
            return false;
        }
        try {
            if (method.getReturnType() == void.class || method.getReturnType() == Void.class) {
                method.invoke(instance, event);
                return terminal;
            } else {
                return (boolean) method.invoke(instance, event);
            }
        } catch (Throwable t) {
            while (t instanceof InvocationTargetException && t.getCause() != null) {
                t = t.getCause();
            }
            processThrowable(event, t);
        }
        return false;
    }

    private void processThrowable(T event, Throwable t) {
        for (RegisteredExceptionHandler<T> handler : exceptionsHandler) {
            Class<?> exceptionArg = handler.exceptions().length == 1 && handler.exceptions()[0] == Throwable.class ?
                    handler.method().getParameterTypes()[2] : Arrays.stream(handler.exceptions())
                    .filter(exceptionClass -> exceptionClass.isAssignableFrom(t.getClass()))
                    .findFirst()
                    .orElse(null);
            if (exceptionArg != null && exceptionArg.isAssignableFrom(t.getClass())) {
                handler.execute(event, null, t);
                return;
            }
        }
        t.printStackTrace();
//        JDAEnutils.DEFAULT_EXCEPTION_HANDLER.accept(t, hook, event);
    }
}
