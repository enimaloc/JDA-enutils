package fr.enimaloc.enutils.jda.listener;

import fr.enimaloc.enutils.jda.modals.ModalEvent;
import fr.enimaloc.enutils.jda.modals.RegisteredModal;
import net.dv8tion.jda.api.events.interaction.ModalInteractionEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.function.Function;

public class ModalListener extends ListenerAdapter {

    private Map<String, RegisteredModal<?>> registeredModals = new HashMap<>();

    public void registerModal(RegisteredModal<?> registeredModal) {
        registeredModals.put(registeredModal.modal().getId(), registeredModal);
    }

    @Override
    public void onModalInteraction(ModalInteractionEvent event) {
        if (registeredModals.containsKey(event.getModalId())) {
            RegisteredModal<?> registeredModal = registeredModals.get(event.getModalId());
            for (RegisteredModal.ModalField field : registeredModal.fields()) {
                try {
                    Object value = switch (field.component().getType()) {
                        case TEXT_INPUT -> event.getValue(field.id()).getAsString();
                        default -> null;
                    };
                    if (field.field().getType() == Optional.class) {
                        value = Optional.ofNullable(value);
                    }
                    field.field().set(registeredModal.instance(), value);
                } catch (IllegalAccessException e) {
                    e.printStackTrace();
                }
            }
            ((CompletableFuture) registeredModal.future()).complete(new ModalEvent<>(registeredModal.instance(), event));
        }
        super.onModalInteraction(event);
    }
}
