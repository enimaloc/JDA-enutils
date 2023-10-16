package fr.enimaloc.enutils.jda.listener;

import fr.enimaloc.enutils.jda.Constant;
import fr.enimaloc.enutils.jda.eventListener.Priority;
import fr.enimaloc.enutils.jda.eventListener.RegisteredEvent;
import net.dv8tion.jda.api.events.GenericEvent;

import java.util.Comparator;
import java.util.List;
import java.util.concurrent.ThreadFactory;

public class EventListener implements net.dv8tion.jda.api.hooks.EventListener {

    private final ThreadFactory threadFactory;
    private final List<RegisteredEvent> listeners;

    public EventListener(ThreadFactory threadFactory, List<RegisteredEvent> listeners) {
        this.threadFactory = threadFactory;
        this.listeners = listeners;
    }

    @Override
    public void onEvent(GenericEvent event) {
        for (RegisteredEvent registeredEvent : listeners.stream()
                .filter(e -> event.getClass().isAssignableFrom(e.event()))
                .sorted(Comparator.<RegisteredEvent, Priority>comparing(RegisteredEvent::priority).reversed())
                .toList()) {
            Thread thread = threadFactory.newThread(() -> registeredEvent.execute(event));
            thread.setName(Constant.ThreadName.EVENT_LISTENER_EXECUTOR.formatted(
                    event.getClass().getSimpleName(),
                    event.getJDA().getShardInfo().getShardId(),
                    event.getJDA().getShardInfo().getShardTotal()
            ));
            thread.start();
        }
    }
}
