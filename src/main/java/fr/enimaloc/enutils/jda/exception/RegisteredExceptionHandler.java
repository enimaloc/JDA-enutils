package fr.enimaloc.enutils.jda.exception;

import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.interactions.InteractionHook;
import org.jetbrains.annotations.NotNull;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public record RegisteredExceptionHandler(
        @NotNull Class<? extends Throwable>[] exceptions,
        @NotNull Object instance,
        @NotNull Method method
) {
    public void execute(SlashCommandInteractionEvent event, InteractionHook hook, Throwable exception) {
        List<Object> params = new ArrayList<>();
        for (Class<?> type : method.getParameterTypes()) {
            if (SlashCommandInteractionEvent.class.isAssignableFrom(type)) {
                params.add(event);
            } else if (InteractionHook.class.isAssignableFrom(type)) {
                params.add(hook);
            } else if (Arrays.stream(exceptions()).anyMatch(exceptionClass -> exceptionClass.isAssignableFrom(type))) {
                params.add(exception);
            }
        }
        try {
            method.invoke(instance, params.toArray());
        } catch (Throwable t) {
            t.printStackTrace();
        }
    }
}
