package fr.enimaloc.enutils.jda.listener;

import net.dv8tion.jda.api.events.interaction.component.ButtonInteractionEvent;
import net.dv8tion.jda.api.events.interaction.component.EntitySelectInteractionEvent;
import net.dv8tion.jda.api.events.interaction.component.GenericComponentInteractionCreateEvent;
import net.dv8tion.jda.api.events.interaction.component.StringSelectInteractionEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import net.dv8tion.jda.api.interactions.components.ActionComponent;
import net.dv8tion.jda.api.interactions.components.buttons.Button;
import net.dv8tion.jda.api.interactions.components.selections.EntitySelectMenu;
import net.dv8tion.jda.api.interactions.components.selections.StringSelectMenu;

import java.util.HashMap;
import java.util.Map;
import java.util.function.Function;

public class InteractionListener extends ListenerAdapter {

    private final Map<String, Object> callbacks = new HashMap<>();

    public void registerCallback(ActionComponent component, Function<? extends GenericComponentInteractionCreateEvent, Boolean> callback) {
        callbacks.put(component.getId(), callback);
    }

    public void registerCallback(Button button, Function<? extends ButtonInteractionEvent, Boolean> callback) {
        callbacks.put(button.getId(), callback);
    }

    public void registerCallback(StringSelectMenu menu, Function<? extends StringSelectInteractionEvent, Boolean> callback) {
        callbacks.put(menu.getId(), callback);
    }

    public void registerCallback(EntitySelectMenu menu, Function<? extends EntitySelectInteractionEvent, Boolean> callback) {
        callbacks.put(menu.getId(), callback);
    }

    @Override
    public void onButtonInteraction(ButtonInteractionEvent event) {
        if (callbacks.containsKey(event.getComponentId()) && callbacks.get(event.getComponentId()) instanceof Function function) {
            if ((boolean) function.apply(event)) {
                callbacks.remove(event.getComponentId());
                event.editButton(event.getButton().asDisabled()).queue();
            }
            return;
        }
        super.onButtonInteraction(event);
    }

    @Override
    public void onStringSelectInteraction(StringSelectInteractionEvent event) {
        if (callbacks.containsKey(event.getComponentId()) && callbacks.get(event.getComponentId()) instanceof Function function) {
            if ((boolean) function.apply(event)) {
                callbacks.remove(event.getComponentId());
                event.editSelectMenu(event.getSelectMenu().asDisabled()).queue();
            }
            return;
        }
        super.onStringSelectInteraction(event);
    }

    @Override
    public void onEntitySelectInteraction(EntitySelectInteractionEvent event) {
        if (callbacks.containsKey(event.getComponentId()) && callbacks.get(event.getComponentId()) instanceof Function function) {
            if ((boolean) function.apply(event)) {
                callbacks.remove(event.getComponentId());
                event.editSelectMenu(event.getSelectMenu().asDisabled()).queue();
            }
            return;
        }
        super.onEntitySelectInteraction(event);
    }
}
