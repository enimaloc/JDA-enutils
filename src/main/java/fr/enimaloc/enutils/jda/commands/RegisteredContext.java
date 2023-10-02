package fr.enimaloc.enutils.jda.commands;

import fr.enimaloc.enutils.jda.JDAEnutils;
import fr.enimaloc.enutils.jda.exception.RegisteredExceptionHandler;
import fr.enimaloc.enutils.jda.registered.RegisteredCommand;
import fr.enimaloc.enutils.jda.utils.Checks;
import net.dv8tion.jda.api.events.interaction.command.GenericContextInteractionEvent;
import net.dv8tion.jda.api.events.interaction.command.MessageContextInteractionEvent;
import net.dv8tion.jda.api.events.interaction.command.UserContextInteractionEvent;
import net.dv8tion.jda.api.interactions.InteractionHook;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.lang.reflect.Method;
import java.util.Arrays;

public record RegisteredContext(
        @NotNull UnionCommandData data,
        @NotNull Object instance,
        @Nullable Method method,
        @NotNull String fullCommandName,
        @NotNull RegisteredExceptionHandler<GenericContextInteractionEvent<?>>[] exceptionsHandler,
        @Nullable DebugInformation debugInformation
) implements RegisteredCommand {

    public void execute(GenericContextInteractionEvent<?> event) {
        Checks.check((event instanceof UserContextInteractionEvent && method.getParameterTypes()[0] == UserContextInteractionEvent.class)
                || (event instanceof MessageContextInteractionEvent && method.getParameterTypes()[0] == MessageContextInteractionEvent.class)
                || method.getParameterTypes()[0] == GenericContextInteractionEvent.class,
                "The event type doesn't match the command type");


        try {
            method.invoke(instance, event);
        } catch (Throwable t) {
            processThrowable(event, event.deferReply().complete(), t.getCause());
        }
    }


    private void processThrowable(GenericContextInteractionEvent<?> event, InteractionHook hook, Throwable t) {
        for (RegisteredExceptionHandler<GenericContextInteractionEvent<?>> handler : exceptionsHandler) {
            Class<?> exceptionArg = Arrays.stream(handler.method().getParameterTypes())
                    .filter(c -> c.isAssignableFrom(t.getClass()))
                    .findFirst()
                    .or(() -> Arrays.stream(handler.exceptions())
                            .filter(c -> c.isAssignableFrom(t.getClass()))
                            .findFirst())
                    .orElse(Void.class);
            if (exceptionArg.isAssignableFrom(t.getClass())) {
                handler.execute(event, hook, t);
                return;
            }
        }
        JDAEnutils.DEFAULT_EXCEPTION_HANDLER.accept(t, hook, event);
    }
}
