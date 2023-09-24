package fr.enimaloc.enutils.jda.registered;

import fr.enimaloc.enutils.jda.commands.DebugInformation;
import fr.enimaloc.enutils.jda.commands.UnionCommandData;
import fr.enimaloc.enutils.jda.exception.RegisteredExceptionHandler;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.lang.reflect.Method;

public interface RegisteredCommand {

    @NotNull UnionCommandData data();
    @NotNull Object instance();
    @Nullable Method method();
    @NotNull String fullCommandName();
    @NotNull RegisteredExceptionHandler[] exceptionsHandler();
    @Nullable DebugInformation debugInformation();
}
