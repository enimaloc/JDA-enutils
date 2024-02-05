package fr.enimaloc.enutils.jda.listener;

import fr.enimaloc.enutils.jda.Constant;
import fr.enimaloc.enutils.jda.commands.RegisteredContext;
import fr.enimaloc.enutils.jda.commands.RegisteredSlash;
import net.dv8tion.jda.api.events.interaction.command.CommandAutoCompleteInteractionEvent;
import net.dv8tion.jda.api.events.interaction.command.GenericContextInteractionEvent;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;

import java.util.List;
import java.util.concurrent.ThreadFactory;

public class CommandsListener extends ListenerAdapter {

    private ThreadFactory threadFactory;
    private List<RegisteredSlash> commands;
    private List<RegisteredContext> contexts;

    public CommandsListener(ThreadFactory threadFactory, List<RegisteredSlash> commands, List<RegisteredContext> contexts) {
        this.threadFactory = threadFactory;
        this.commands = commands;
        this.contexts = contexts;
    }

    @Override
    public void onSlashCommandInteraction(SlashCommandInteractionEvent event) {
        commands.stream()
                .filter(command -> command.fullCommandName().equals(event.getInteraction().getFullCommandName()))
                .findFirst()
                .ifPresent(command -> {
                    Thread thread = threadFactory.newThread(() -> command.execute(event));
                    thread.setName(Constant.ThreadName.SLASH_COMMAND_EXECUTOR.formatted(
                            event.getFullCommandName(),
                            event.getCommandId(),
                            event.getInteraction().getId()
                    ));
                    thread.start();
                });
    }

    @Override
    public void onCommandAutoCompleteInteraction(CommandAutoCompleteInteractionEvent event) {
        commands.stream()
                .filter(command -> command.fullCommandName().equals(event.getInteraction().getFullCommandName()))
                .findFirst()
                .ifPresent(command -> {
                    Thread thread = threadFactory.newThread(() -> command.autoComplete(event));
                    thread.setName(Constant.ThreadName.SLASH_COMMAND_AUTOCOMPLETION.formatted(
                            event.getFullCommandName(),
                            event.getCommandId(),
                            event.getInteraction().getId()
                    ));
                    thread.start();
                });
    }

    @Override
    public void onGenericContextInteraction(GenericContextInteractionEvent<?> event) {
        contexts.stream()
                .filter(context -> context.fullCommandName().equals(event.getInteraction().getFullCommandName()))
                .findFirst()
                .ifPresent(context -> {
                    Thread thread = threadFactory.newThread(() -> context.execute(event));
                    thread.setName(Constant.ThreadName.CONTEXT_COMMAND_EXECUTOR.formatted(
                            event.getFullCommandName(),
                            event.getCommandId(),
                            event.getInteraction().getId()
                    ));
                    thread.start();
                });
    }
}
