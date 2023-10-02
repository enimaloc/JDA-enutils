package fr.enimaloc.enutils.jda;

import fr.enimaloc.enutils.jda.commands.RegisteredContext;
import fr.enimaloc.enutils.jda.commands.RegisteredSlash;
import fr.enimaloc.enutils.jda.commands.UnionCommandData;
import fr.enimaloc.enutils.jda.listener.CommandsListener;
import fr.enimaloc.enutils.jda.listener.InteractionListener;
import fr.enimaloc.enutils.jda.listener.ModalListener;
import fr.enimaloc.enutils.jda.register.processor.ContextCommandProcessor;
import fr.enimaloc.enutils.jda.register.processor.SlashCommandProcessor;
import fr.enimaloc.enutils.jda.register.processor.annotation.AnnotationContextCommandProcessor;
import fr.enimaloc.enutils.jda.register.processor.annotation.AnnotationSlashCommandProcessor;
import fr.enimaloc.enutils.jda.utils.TriConsumer;
import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.events.interaction.GenericInteractionCreateEvent;
import net.dv8tion.jda.api.interactions.InteractionHook;
import net.dv8tion.jda.api.interactions.commands.build.CommandData;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

public class JDAEnutils {
    public static final Logger LOGGER = LoggerFactory.getLogger(JDAEnutils.class);
    public static final TriConsumer<Throwable, InteractionHook, GenericInteractionCreateEvent> DEFAULT_EXCEPTION_HANDLER =
            new TriConsumer<>() {
                public static final Pattern CODE_REFERENCE = Pattern.compile("\\((.*):(\\d+)\\)");
                public static final String BASE_MESSAGE = "An exception occurred while executing the command\n" +
                        "```ansi\n" +
                        "%s\n" +
                        "```More information in the console";

                @Override
                public void accept(Throwable throwable, InteractionHook hook, GenericInteractionCreateEvent event) {
                    LOGGER.error("An exception occurred while executing the command", throwable);
                    String stacktrace = Arrays.stream(throwable.getStackTrace())
                            .map(StackTraceElement::toString)
                            .map(s -> CODE_REFERENCE.matcher(s).replaceAll("(\u001B[0;34m\u001B[4;34m$1:$2\u001B[0;31m)"))
                            .collect(Collectors.joining("\n\t\u001B[0;31m", "\t\u001B[0;31m", "\u001B[0m"));
                    String fStacktrace = "\u001B[0;31m" + throwable.getClass().getName() + "\n" + stacktrace;
                    if (BASE_MESSAGE.formatted(fStacktrace).length() > Message.MAX_CONTENT_LENGTH) {
                        fStacktrace = fStacktrace.substring(0, fStacktrace.lastIndexOf('\n', Message.MAX_CONTENT_LENGTH - BASE_MESSAGE.length() - 15)) + "\n\t\u001B[0;31m...";
                    } else {
                        fStacktrace = "\u001B[0;31m" + throwable + stacktrace;
                        if (BASE_MESSAGE.formatted(fStacktrace).length() > Message.MAX_CONTENT_LENGTH) {
                            fStacktrace = "\u001B[0;31m" + throwable.toString().substring(0, throwable.toString().lastIndexOf('\n', Message.MAX_CONTENT_LENGTH - BASE_MESSAGE.length() - stacktrace.length())) + "\n\t\u001B[0;31m...";
                        }
                    }
                    hook.sendMessage(BASE_MESSAGE.formatted(fStacktrace)).setEphemeral(true).queue();
                }
            };

    private JDA jda;
    private List<RegisteredSlash> commands;
    private List<RegisteredContext> contexts;

    public JDAEnutils(JDA jda, List<RegisteredSlash> commands, List<RegisteredContext> contexts) {
        this.jda = jda;
        this.jda.addEventListener(new CommandsListener(commands, contexts), new InteractionListener(), new ModalListener());
        this.commands = commands;
        this.contexts = contexts;
    }

    public static JDAEnutils.Builder builder() {
        return new JDAEnutils.Builder();
    }

    public void upsertAll() {
        jda.updateCommands()
                .addCommands(commands.stream()
                        .map(RegisteredSlash::data)
                        .map(UnionCommandData::getSlashCommandData)
                        .toArray(CommandData[]::new))
                .addCommands(contexts.stream()
                        .map(RegisteredContext::data)
                        .map(UnionCommandData::getCommandData)
                        .toArray(CommandData[]::new))
                .queue();
    }

    public void upsertAll(long guild) {
        jda.getGuildById(guild)
                .updateCommands()
                .addCommands(commands.stream()
                        .map(RegisteredSlash::data)
                        .filter(UnionCommandData::isSlashCommandData)
                        .map(UnionCommandData::getSlashCommandData)
                        .toArray(CommandData[]::new))
                .addCommands(contexts.stream()
                        .map(RegisteredContext::data)
                        .filter(UnionCommandData::isCommandData)
                        .map(UnionCommandData::getCommandData)
                        .toArray(CommandData[]::new))
                .queue();
    }

    public static class Builder {
        private JDA jda;
        private List<Object> commands = new ArrayList<>();
        private List<Object> contexts = new ArrayList<>();
        private SlashCommandProcessor commandProcessor = new AnnotationSlashCommandProcessor();
        private ContextCommandProcessor contextProcessor = new AnnotationContextCommandProcessor();

        public Builder setJda(JDA jda) {
            this.jda = jda;
            return this;
        }

        public Builder setCommands(List<Object> commands) {
            this.commands = commands;
            return this;
        }

        public Builder setContexts(List<Object> contexts) {
            this.contexts = contexts;
            return this;
        }

        public JDAEnutils build() {
            return new JDAEnutils(
                    jda,
                    List.of(commandProcessor.registerCommands(commands.toArray())),
                    List.of(contextProcessor.registerCommands(contexts.toArray()))
            );
        }
    }
}
