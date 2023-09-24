package fr.enimaloc.enutils.jda;

import fr.enimaloc.enutils.jda.commands.RegisteredCommand;
import fr.enimaloc.enutils.jda.commands.UnionCommandData;
import fr.enimaloc.enutils.jda.listener.CommandsListener;
import fr.enimaloc.enutils.jda.register.processor.CommandsProcessor;
import fr.enimaloc.enutils.jda.register.processor.annotation.AnnotationCommandProcessor;
import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.interactions.commands.build.CommandData;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;

public class JDAEnutils {
    public static final Logger LOGGER = LoggerFactory.getLogger(JDAEnutils.class);

    private JDA jda;
    private List<RegisteredCommand> commands;

    public JDAEnutils(JDA jda, List<RegisteredCommand> commands) {
        this.jda = jda;
        this.jda.addEventListener(new CommandsListener(commands));
        this.commands = commands;
    }

    public static JDAEnutils.Builder builder() {
        return new JDAEnutils.Builder();
    }

    public void upsertAll() {
        jda.updateCommands()
                .addCommands(commands.stream().map(RegisteredCommand::data).map(UnionCommandData::getSlashCommandData).toArray(CommandData[]::new))
                .queue();
    }

    public void upsertAll(long guild) {
        jda.getGuildById(guild)
                .updateCommands()
                .addCommands(commands.stream()
                        .map(RegisteredCommand::data)
                        .filter(UnionCommandData::isSlashCommandData)
                        .map(UnionCommandData::getSlashCommandData)
                        .toArray(CommandData[]::new))
                .queue();
    }

    public static class Builder {
        private JDA jda;
        private List<Object> commands;
        private CommandsProcessor commandProcessor = new AnnotationCommandProcessor();

        public Builder setJda(JDA jda) {
            this.jda = jda;
            return this;
        }

        public Builder setCommands(List<Object> commands) {
            this.commands = commands;
            return this;
        }

        public JDAEnutils build() {
            return new JDAEnutils(jda, List.of(commandProcessor.registerCommands(commands.toArray())));
        }
    }
}
