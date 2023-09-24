package fr.enimaloc.enutils.jda.commands;

import net.dv8tion.jda.api.interactions.commands.build.CommandData;
import net.dv8tion.jda.api.interactions.commands.build.SlashCommandData;
import net.dv8tion.jda.api.interactions.commands.build.SubcommandData;
import net.dv8tion.jda.api.interactions.commands.build.SubcommandGroupData;
import net.dv8tion.jda.internal.utils.Checks;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class UnionCommandData {
    @Nullable private final CommandData commandData;
    @Nullable private final SlashCommandData slashCommandData;
    @Nullable private final SubcommandGroupData subcommandGroupData;
    @Nullable private final SubcommandData subcommandData;

    public UnionCommandData(@NotNull CommandData commandData) {
        this(commandData, null, null, null);
    }

    public UnionCommandData(@NotNull SlashCommandData slashCommandData) {
        this(null, slashCommandData, null, null);
    }

    public UnionCommandData(@NotNull SubcommandGroupData subcommandGroupData) {
        this(null, null, subcommandGroupData, null);
    }

    public UnionCommandData(@NotNull SubcommandData subcommandData) {
        this(null, null, null, subcommandData);
    }

    public UnionCommandData(CommandData commandData, SlashCommandData slashCommandData, SubcommandGroupData subcommandGroupData, SubcommandData subcommandData) {
        if (commandData == null && slashCommandData == null && subcommandGroupData == null && subcommandData == null) {
            throw new IllegalArgumentException("All parameters are null");
        }
        this.commandData = commandData;
        this.slashCommandData = slashCommandData;
        this.subcommandGroupData = subcommandGroupData;
        this.subcommandData = subcommandData;
    }

    public boolean isCommandData() {
        return commandData != null;
    }

    public boolean isSlashCommandData() {
        return slashCommandData != null;
    }

    public boolean isSubcommandGroupData() {
        return subcommandGroupData != null;
    }

    public boolean isSubcommandData() {
        return subcommandData != null;
    }

    @NotNull
    public CommandData getCommandData() {
        Checks.check(isCommandData(), "This "+this+" is not a CommandData");
        return commandData;
    }

    @NotNull
    public SlashCommandData getSlashCommandData() {
        Checks.check(isSlashCommandData(), "This "+this+" is not a SlashCommandData");
        return slashCommandData;
    }

    @NotNull
    public SubcommandGroupData getSubcommandGroupData() {
        Checks.check(isSubcommandGroupData(), "This "+this+" is not a SubcommandGroupData");
        return subcommandGroupData;
    }

    @NotNull
    public SubcommandData getSubcommandData() {
        Checks.check(isSubcommandData(), "This "+this+" is not a SubcommandData");
        return subcommandData;
    }

    @NotNull
    public Object getData() {
        if (isCommandData()) {
            return commandData;
        } else if (isSlashCommandData()) {
            return slashCommandData;
        } else if (isSubcommandGroupData()) {
            return subcommandGroupData;
        } else {
            return subcommandData;
        }
    }

    @Override
    public String toString() {
        StringBuilder out = new StringBuilder("UnionCommandData<");
        if (isCommandData()) {
            out.append("commandData");
        } else if (isSlashCommandData()) {
            out.append("slashCommandData");
        } else if (isSubcommandGroupData()) {
            out.append("subCommandGroupData");
        } else if (isSubcommandData()) {
            out.append("subCommandData");
        }
        out.append(">{").append(getData()).append("}");
        return out.toString();
    }

    public Type getType() {
        if (isCommandData()) {
            return Type.COMMAND_DATA;
        } else if (isSlashCommandData()) {
            return Type.SLASH_COMMAND_DATA;
        } else if (isSubcommandGroupData()) {
            return Type.SUBCOMMAND_GROUP_DATA;
        } else {
            return Type.SUBCOMMAND_DATA;
        }
    }

    public enum Type {
        COMMAND_DATA,
        SLASH_COMMAND_DATA,
        SUBCOMMAND_GROUP_DATA,
        SUBCOMMAND_DATA
    }
}
