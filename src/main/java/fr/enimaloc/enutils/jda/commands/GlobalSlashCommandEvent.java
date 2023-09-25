/*
 * GlobalSlashCommand
 *
 * 0.0.1
 *
 * 27/09/2022
 */
package fr.enimaloc.enutils.jda.commands;

import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.interactions.DiscordLocale;
import net.dv8tion.jda.api.interactions.commands.SlashCommandInteraction;
import org.jetbrains.annotations.NotNull;

/**
 *
 */
public final class GlobalSlashCommandEvent extends CustomSlashCommandInteractionEvent {

    public GlobalSlashCommandEvent(@NotNull SlashCommandInteractionEvent event) {
        super(event.getJDA(), event.getResponseNumber(), event.getInteraction());
    }

    public GlobalSlashCommandEvent(
            @NotNull JDA api, long responseNumber,
            @NotNull SlashCommandInteraction interaction
    ) {
        super(api, responseNumber, interaction);
    }

    public boolean isFromDM() {
        return !isFromGuild();
    }

    public DiscordLocale getLocale(boolean favorMemberLocale) {
        return super.getLocale(isFromDM() || favorMemberLocale);
    }
}
