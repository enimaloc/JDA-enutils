package fr.enimaloc.enutils.jda.register.processor.annotation;

import fr.enimaloc.enutils.jda.commands.RegisteredContext;
import fr.enimaloc.enutils.jda.commands.UnionCommandData;
import fr.enimaloc.enutils.jda.exception.CommandException;
import fr.enimaloc.enutils.jda.exception.RegisteredExceptionHandler;
import fr.enimaloc.enutils.jda.register.annotation.*;
import fr.enimaloc.enutils.jda.register.processor.ContextCommandProcessor;
import fr.enimaloc.enutils.jda.utils.AnnotationUtils;
import fr.enimaloc.enutils.jda.utils.Checks;
import net.dv8tion.jda.api.events.interaction.command.GenericContextInteractionEvent;
import net.dv8tion.jda.api.events.interaction.command.UserContextInteractionEvent;
import net.dv8tion.jda.api.interactions.DiscordLocale;
import net.dv8tion.jda.api.interactions.InteractionHook;
import net.dv8tion.jda.api.interactions.commands.DefaultMemberPermissions;
import net.dv8tion.jda.api.interactions.commands.build.CommandData;
import net.dv8tion.jda.api.interactions.commands.build.Commands;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.*;
import java.util.function.BiFunction;

public class AnnotationContextCommandProcessor implements ContextCommandProcessor {

    public static final Logger LOGGER = LoggerFactory.getLogger(AnnotationSlashCommandProcessor.class);

    private RegisteredExceptionHandler[] getExceptionHandler(Object instance) {
        List<RegisteredExceptionHandler> exceptionsHandler = new ArrayList<>();
        for (Method method : instance.getClass().getDeclaredMethods()) {
            if (method.isAnnotationPresent(Catch.class)) {
                Catch exceptionHandler = method.getAnnotation(Catch.class);
                Checks.check(Arrays.stream(method.getParameterTypes())
                                .allMatch(t -> Throwable.class.isAssignableFrom(t)
                                        || InteractionHook.class.isAssignableFrom(t)
                                        || GenericContextInteractionEvent.class.isAssignableFrom(t)),
                        "ExceptionHandler method supports only Throwable, InteractionHook and GenericContextInteractionEvent parameters");
                Checks.Reflection.trySetAccessible(method, "ExceptionHandler method must be accessible");
                exceptionsHandler.add(new RegisteredExceptionHandler(
                        GenericContextInteractionEvent.class,
                        exceptionHandler.value(),
                        instance,
                        method
                ));
            }
        }
        return exceptionsHandler.toArray(RegisteredExceptionHandler[]::new);
    }

    @Override
    public RegisteredContext registerCommand(Object instance, Method method) {
        if (!method.isAnnotationPresent(Context.class)) {
            return null;
        }
        Checks.Reflection.trySetAccessible(method, "Cannot access method " + method.getName());
        Checks.equals(method.getParameterCount(), 1, "ContextCommand method must have 1 parameter");
        Checks.Reflection.assignableFrom(method.getParameterTypes()[0],
                                         GenericContextInteractionEvent.class,
                                         "ContextCommand method must have a ContextInteractionEvent parameter");
        Context context = method.getAnnotation(Context.class);
        String name = AnnotationUtils.get(context.name()).orElse(normaliseName(method.getName()));
        net.dv8tion.jda.api.interactions.commands.Command.Type type = UserContextInteractionEvent.class.isAssignableFrom(method.getParameterTypes()[0]) ? net.dv8tion.jda.api.interactions.commands.Command.Type.USER : net.dv8tion.jda.api.interactions.commands.Command.Type.MESSAGE;
        CommandData data = Commands.context(type, name);
        data.setNSFW(method.isAnnotationPresent(Command.Nsfw.class));
        data.setGuildOnly(method.isAnnotationPresent(Command.GuildOnly.class));
        if (method.isAnnotationPresent(Command.RequiredPermission.class)) {
            Command.RequiredPermission permission = method.getAnnotation(Command.RequiredPermission.class);
            if (permission.value().length != 0) {
                data.setDefaultPermissions(DefaultMemberPermissions.enabledFor(permission.value()));
            } else {
                data.setDefaultPermissions(permission.enabledForAll() ? DefaultMemberPermissions.ENABLED : DefaultMemberPermissions.DISABLED);
            }
        }
        if (context.i18n() != null) {
            processI18n(instance, name, context.i18n(), (locale, i18n) -> data.setNameLocalization(locale, normaliseName(i18n)));
        }

        return new RegisteredContext(new UnionCommandData(data), instance, method, name, getExceptionHandler(instance), null);
    }

    @Override
    public RegisteredContext[] generateArray(int length) {
        return new RegisteredContext[length];
    }

    // region I18n
    private void processI18n(Object instance, String commandName, I18n i18n, BiFunction<DiscordLocale, String, ?> i18nFunction) {
        for (I18n.Locale locale : i18n.locales()) {
            if (i18n.locales().length != 0) {
                i18nFunction.apply(locale.language(), locale.value());
            } else if (!AnnotationUtils.isDefault(i18n.target().method())) {
                ResourceBundle bundle = null;
                if (ResourceBundle.class.isAssignableFrom(i18n.target().clazz())) {
                    try {
                        bundle = (ResourceBundle) i18n.target().clazz().getConstructor().newInstance();
                        Arrays.stream(instance.getClass().getDeclaredMethods())
                                .filter(method -> method.isAnnotationPresent(I18n.LocaleSetter.class))
                                .filter(method -> Arrays.equals(method.getParameterTypes(), new Class[]{DiscordLocale.class}))
                                .findFirst()
                                .ifPresent(method -> {
                                    try {
                                        method.invoke(instance, locale.language());
                                    } catch (IllegalAccessException | InvocationTargetException e) {
                                        throw new CommandException(e);
                                    }
                                });
                    } catch (InstantiationException | IllegalAccessException | InvocationTargetException |
                             NoSuchMethodException e) {
                        throw new CommandException(e);
                    }
                } else {
                    try {
                        bundle = ResourceBundle.getBundle(i18n.basePath(), Locale.forLanguageTag(locale.language().getLocale()));
                    } catch (MissingResourceException e) {
                        // Ignore
                    }
                }
                if (bundle != null) {
                    try {
                        i18nFunction.apply(locale.language(), bundle.getString(i18n.key().replace("{{context_name}}", commandName)));
                    } catch (MissingResourceException e) {
                        // Ignore
                    }
                }
            } else {
                try {
                    Object invoke = i18n.target().clazz().getMethod(i18n.target().method(), Locale.class).invoke(null, locale.language());
                    if (invoke instanceof String s) {
                        i18nFunction.apply(locale.language(), s);
                    }
                } catch (IllegalAccessException | InvocationTargetException | NoSuchMethodException e) {
                    throw new CommandException(e);
                }
            }
        }
    }
    // endregion

    // region Utils
    private Optional<Method> getMethod(Object sourceInstance, MethodTarget target) {
        return getMethod(sourceInstance.getClass(), target);
    }

    private Optional<Method> getMethod(Class<?> sourceClazz, MethodTarget target) {
        return getMethod(sourceClazz, target.clazz(), target.method());
    }

    private Optional<Method> getMethod(Object sourceInstance, Class<?> clazz, String name) {
        return getMethod(sourceInstance.getClass(), clazz, name);
    }
    // endregion

    private Optional<Method> getMethod(Class<?> sourceClazz, Class<?> clazz, String name) {
        if (clazz == null || clazz == Void.class) {
            clazz = sourceClazz;
        }
        return Arrays.stream(clazz.getDeclaredMethods()).filter(m -> m.getName().equals(name)).findFirst();
    }

    private String normaliseName(String str) {
        return str.replaceAll("([a-z])([A-Z])", "$1 $2")
                .toLowerCase()
                .replaceFirst(String.valueOf(str.charAt(0)), String.valueOf(Character.toUpperCase(str.charAt(0))));
    }
}
