package fr.enimaloc.enutils.jda.modals;

import net.dv8tion.jda.api.interactions.components.ItemComponent;
import net.dv8tion.jda.api.interactions.modals.Modal;

import java.lang.reflect.Field;
import java.util.concurrent.Future;

public record RegisteredModal<T>(
        Class<T> clazz,
        Object instance,
        Modal modal,
        ModalField[] fields,
        Future<ModalEvent<T>> future
) {

    public record ModalField(
            Field field,
            String id,
            int actionRowId,
            ItemComponent component
    ) {}

}
