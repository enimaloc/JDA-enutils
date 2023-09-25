/*
 * GlobalSlashCommand
 *
 * 0.0.1
 *
 * 27/09/2022
 */
package fr.enimaloc.enutils.jda.commands;

import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.interactions.commands.SlashCommandInteraction;
import org.jetbrains.annotations.NotNull;

import java.util.Objects;

/**
 *
 */
public final class GuildSlashCommandEvent extends CustomSlashCommandInteractionEvent {

    public GuildSlashCommandEvent(@NotNull SlashCommandInteractionEvent event) {
        super(event.getJDA(), event.getResponseNumber(), event.getInteraction());
    }

    public GuildSlashCommandEvent(
            @NotNull JDA api, long responseNumber,
            @NotNull SlashCommandInteraction interaction
    ) {
        super(api, responseNumber, interaction);
    }

    @Override
    @NotNull
    public Guild getGuild() {
        return Objects.requireNonNull(super.getGuild());
    }

    @Override
    @NotNull
    public Member getMember() {
        return Objects.requireNonNull(super.getMember());
    }
}
