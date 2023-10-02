package fr.enimaloc.enutils.jda.listener;

import fr.enimaloc.enutils.jda.eventListener.Priority;
import fr.enimaloc.enutils.jda.eventListener.RegisteredEvent;
import fr.enimaloc.enutils.jda.exception.RegisteredExceptionHandler;
import net.dv8tion.jda.api.events.GenericEvent;

import java.util.Comparator;
import java.util.List;

public class EventListener implements net.dv8tion.jda.api.hooks.EventListener {

    private final List<RegisteredEvent> listeners;

    public EventListener(List<RegisteredEvent> listeners) {
        this.listeners = listeners;
    }

    @Override
    public void onEvent(GenericEvent event) {
        for (RegisteredEvent registeredEvent : listeners.stream()
                .filter(e -> event.getClass().isAssignableFrom(e.event()))
                .sorted(Comparator.<RegisteredEvent, Priority>comparing(RegisteredEvent::priority).reversed())
                .toList()) {
            if (registeredEvent.execute(event)) {
                break;
            }
        }
    }
}
