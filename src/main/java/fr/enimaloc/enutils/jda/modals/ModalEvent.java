package fr.enimaloc.enutils.jda.modals;

import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.events.interaction.ModalInteractionEvent;
import net.dv8tion.jda.api.interactions.modals.ModalInteraction;

public class ModalEvent<T> extends ModalInteractionEvent {

    private T instance;

    public ModalEvent(T instance, ModalInteractionEvent baseEvent) {
        this(instance, baseEvent.getJDA(), baseEvent.getResponseNumber(), baseEvent.getInteraction());
    }

    public ModalEvent(T instance, JDA api, long responseNumber, ModalInteraction interaction) {
        super(api, responseNumber, interaction);
        this.instance = instance;
    }

    public T getInstance() {
        return instance;
    }
}
