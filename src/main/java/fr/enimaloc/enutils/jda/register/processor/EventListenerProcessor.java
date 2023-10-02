package fr.enimaloc.enutils.jda.register.processor;

import fr.enimaloc.enutils.jda.eventListener.RegisteredEvent;

import java.util.ArrayList;
import java.util.List;

public interface EventListenerProcessor {
    RegisteredEvent[] register(Object listener);

    default RegisteredEvent[] register(Object... listeners) {
        List<RegisteredEvent> registeredEvents = new ArrayList<>();
        for (Object listener : listeners) {
            RegisteredEvent[] registeredEvents1 = register(listener);
            for (RegisteredEvent registeredEvent : registeredEvents1) {
                registeredEvents.add(registeredEvent);
            }
        }
        return registeredEvents.toArray(RegisteredEvent[]::new);
    }
}
