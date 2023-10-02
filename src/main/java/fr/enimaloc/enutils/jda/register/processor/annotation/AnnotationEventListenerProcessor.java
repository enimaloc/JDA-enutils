package fr.enimaloc.enutils.jda.register.processor.annotation;

import fr.enimaloc.enutils.jda.eventListener.RegisteredEvent;
import fr.enimaloc.enutils.jda.exception.CommandException;
import fr.enimaloc.enutils.jda.exception.RegisteredExceptionHandler;
import fr.enimaloc.enutils.jda.register.annotation.Catch;
import fr.enimaloc.enutils.jda.register.annotation.On;
import fr.enimaloc.enutils.jda.register.processor.EventListenerProcessor;
import fr.enimaloc.enutils.jda.utils.AnnotationUtils;
import fr.enimaloc.enutils.jda.utils.Checks;
import fr.enimaloc.enutils.jda.utils.Utils;
import net.dv8tion.jda.api.events.Event;
import net.dv8tion.jda.api.events.GenericEvent;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.interactions.InteractionHook;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.function.Predicate;

public class AnnotationEventListenerProcessor implements EventListenerProcessor {
    @Override
    public RegisteredEvent[] register(Object listener) {
        List<RegisteredEvent> registeredEvents = new ArrayList<>();
        Class<?> clazz = listener.getClass();
        for (Method method : Arrays.stream(clazz.getDeclaredMethods())
                .filter(method -> method.isAnnotationPresent(On.class))
                .toList()) {
            registeredEvents.add(register(
                    (Class<? extends GenericEvent>) method.getParameterTypes()[0],
                    listener,
                    method,
                    method.getAnnotation(On.class)
            ));
        }
        return registeredEvents.toArray(RegisteredEvent[]::new);
    }

    private <T extends GenericEvent> RegisteredEvent<T> register(Class<T> event, Object instance, Method method, On annotation) {
        Class<?> clazz = instance.getClass();
        Checks.Reflection.trySetAccessible(method);
        Checks.check(method.getParameterTypes().length == 1 && Event.class.isAssignableFrom(method.getParameterTypes()[0]),
                "Method %s in class %s has an invalid signature. It must have only one parameter of type Event"
                        .formatted(method.getName(), clazz.getName()));
        Checks.or(
                () -> Checks.Reflection.returnTypeAssignable(method, boolean.class),
                () -> Checks.Reflection.returnTypeAssignable(method, Boolean.class),
                () -> Checks.Reflection.returnTypeAssignable(method, void.class),
                () -> Checks.Reflection.returnTypeAssignable(method, Void.class)
        );
        Predicate<T> filter = (unused) -> true;
        if (!AnnotationUtils.isDefault(annotation.filter().method())) {
            Optional<Method> checkMethod = Utils.getMethod(instance, annotation.filter());
            if (checkMethod.isEmpty()) {
                throw new IllegalArgumentException("Method %s in class %s does not exist"
                        .formatted(annotation.filter().method(), clazz.getName()));
            }
            Checks.check(checkMethod.get().getParameterTypes().length == 1 && checkMethod.get().getParameterTypes()[0].isAssignableFrom(event),
                    "Method %s in class %s has an invalid signature. It must have only one parameter of type %s"
                            .formatted(annotation.filter().method(), clazz.getName(), event.getName()));
            Checks.or(
                    () -> Checks.Reflection.returnTypeAssignable(checkMethod.get(), boolean.class),
                    () -> Checks.Reflection.returnTypeAssignable(checkMethod.get(), Boolean.class),
                    () -> Checks.Reflection.returnTypeAssignable(checkMethod.get(), Predicate.class)
            );
            Checks.Reflection.trySetAccessible(checkMethod.get());
            if (checkMethod.get().getReturnType() == Predicate.class) {
                try {
                    filter = (Predicate<T>) checkMethod.get().invoke(instance);
                } catch (IllegalAccessException | InvocationTargetException e) {
                    throw new CommandException(e);
                }
            } else {
                filter = (e) -> {
                    try {
                        return (boolean) checkMethod.get().invoke(instance, e);
                    } catch (IllegalAccessException | InvocationTargetException ex) {
                        throw new CommandException(ex);
                    }
                };
            }
        }
        return new RegisteredEvent<>(event, instance, method, getExceptionHandler(instance), annotation.priority(), annotation.terminal(), filter);
    }


    private RegisteredExceptionHandler[] getExceptionHandler(Object instance) {
        List<RegisteredExceptionHandler> exceptionsHandler = new ArrayList<>();
        for (Method method : instance.getClass().getDeclaredMethods()) {
            if (method.isAnnotationPresent(Catch.class)) {
                Catch exceptionHandler = method.getAnnotation(Catch.class);
                Checks.check(Arrays.stream(method.getParameterTypes())
                                .allMatch(t -> Throwable.class.isAssignableFrom(t)
                                        || InteractionHook.class.isAssignableFrom(t)
                                        || SlashCommandInteractionEvent.class.isAssignableFrom(t)),
                        "ExceptionHandler method supports only Throwable, InteractionHook and SlashCommandInteractionEvent parameters");
                Checks.Reflection.trySetAccessible(method, "ExceptionHandler method must be accessible");
                exceptionsHandler.add(new RegisteredExceptionHandler(
                        SlashCommandInteractionEvent.class,
                        exceptionHandler.value(),
                        instance,
                        method
                ));
            }
        }
        return exceptionsHandler.toArray(RegisteredExceptionHandler[]::new);
    }
}
