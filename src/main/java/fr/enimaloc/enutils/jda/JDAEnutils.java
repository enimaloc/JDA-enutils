package fr.enimaloc.enutils.jda;

import fr.enimaloc.enutils.jda.commands.RegisteredContext;
import fr.enimaloc.enutils.jda.commands.RegisteredSlash;
import fr.enimaloc.enutils.jda.commands.UnionCommandData;
import fr.enimaloc.enutils.jda.listener.CommandsListener;
import fr.enimaloc.enutils.jda.listener.InteractionListener;
import fr.enimaloc.enutils.jda.register.processor.ContextCommandProcessor;
import fr.enimaloc.enutils.jda.register.processor.SlashCommandProcessor;
import fr.enimaloc.enutils.jda.register.processor.annotation.AnnotationSlashCommandProcessor;
import fr.enimaloc.enutils.jda.register.processor.annotation.AnnotationContextCommandProcessor;
import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.interactions.commands.build.CommandData;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;

public class JDAEnutils {
    public static final Logger LOGGER = LoggerFactory.getLogger(JDAEnutils.class);

    private JDA jda;
    private List<RegisteredSlash> commands;
    private List<RegisteredContext> contexts;

    public JDAEnutils(JDA jda, List<RegisteredSlash> commands, List<RegisteredContext> contexts) {
        this.jda = jda;
        this.jda.addEventListener(new CommandsListener(commands, contexts), new InteractionListener());
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
        private List<Object> commands;
        private List<Object> contexts;
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
