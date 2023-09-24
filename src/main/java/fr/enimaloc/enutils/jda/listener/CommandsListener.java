package fr.enimaloc.enutils.jda.listener;

import fr.enimaloc.enutils.jda.commands.RegisteredContext;
import fr.enimaloc.enutils.jda.commands.RegisteredSlash;
import net.dv8tion.jda.api.events.interaction.command.CommandAutoCompleteInteractionEvent;
import net.dv8tion.jda.api.events.interaction.command.GenericContextInteractionEvent;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;

import java.util.List;

public class CommandsListener extends ListenerAdapter {

    private List<RegisteredSlash> commands;
    private List<RegisteredContext> contexts;

    public CommandsListener(List<RegisteredSlash> commands, List<RegisteredContext> contexts) {
        this.commands = commands;
        this.contexts = contexts;
    }

    @Override
    public void onSlashCommandInteraction(SlashCommandInteractionEvent event) {
        commands.stream()
                .filter(command -> command.fullCommandName().equals(event.getInteraction().getFullCommandName()))
                .findFirst()
                .ifPresent(command -> command.execute(event));
    }

    @Override
    public void onCommandAutoCompleteInteraction(CommandAutoCompleteInteractionEvent event) {
        commands.stream()
                .filter(command -> command.fullCommandName().equals(event.getInteraction().getFullCommandName()))
                .findFirst()
                .ifPresent(command -> command.autoComplete(event));
    }

    @Override
    public void onGenericContextInteraction(GenericContextInteractionEvent<?> event) {
        contexts.stream()
                .filter(context -> context.fullCommandName().equals(event.getInteraction().getFullCommandName()))
                .findFirst()
                .ifPresent(context -> context.execute(event));
    }
}
