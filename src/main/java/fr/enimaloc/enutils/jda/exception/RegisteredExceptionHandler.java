package fr.enimaloc.enutils.jda.exception;

import fr.enimaloc.enutils.jda.JDAEnutils;
import net.dv8tion.jda.api.events.GenericEvent;
import net.dv8tion.jda.api.events.interaction.GenericInteractionCreateEvent;
import net.dv8tion.jda.api.interactions.InteractionHook;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public record RegisteredExceptionHandler<T extends GenericEvent>(
        @NotNull Class<T> eventClass,
        @NotNull Class<? extends Throwable>[] exceptions,
        @NotNull Object instance,
        @NotNull Method method
) {
    public void execute(T event, @Nullable InteractionHook hook, Throwable exception) {
        List<Object> params = new ArrayList<>();
        for (Class<?> type : method.getParameterTypes()) {
            if (eventClass.isAssignableFrom(type)) {
                params.add(event);
            } else if (InteractionHook.class.isAssignableFrom(type)) {
                params.add(hook);
            } else if (Arrays.stream(exceptions()).anyMatch(exceptionClass -> exceptionClass.isAssignableFrom(type))) {
                params.add(exception);
            }
        }
        try {
            method.invoke(instance, params.toArray());
        } catch (IllegalArgumentException e) {
            CommandException commandException = new CommandException("The exception handler method " +
                    instance().getClass().getName() + ":" + method().getName() +
                    " does not match with parameters SlashCommandInteractionEvent, InteractionHook, Throwable",
                    e);
            if (hook != null) {
                JDAEnutils.DEFAULT_EXCEPTION_HANDLER.accept(commandException, hook, (GenericInteractionCreateEvent) event);
            }
        } catch (Throwable t) {
            t.printStackTrace();
        }
    }
}
