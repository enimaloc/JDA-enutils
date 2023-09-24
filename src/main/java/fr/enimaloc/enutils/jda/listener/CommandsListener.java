package fr.enimaloc.enutils.jda.listener;

import fr.enimaloc.enutils.jda.commands.RegisteredCommand;
import net.dv8tion.jda.api.events.interaction.command.CommandAutoCompleteInteractionEvent;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;

import java.util.List;

public class CommandsListener extends ListenerAdapter {

    private List<RegisteredCommand> commands;

    public CommandsListener(List<RegisteredCommand> commands) {
        this.commands = commands;
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
}
