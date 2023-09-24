package fr.enimaloc.enutils.jda.commands;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public record DebugInformation(
        long[] guilds,
        boolean forceUpsert,
        boolean exclude,
        boolean deleteOnExist
) {
    public static final List<Long> DEBUG_GUILDS = new ArrayList<>();

    public DebugInformation {
        DEBUG_GUILDS.addAll(Arrays.stream(guilds).boxed().toList());
    }
}
