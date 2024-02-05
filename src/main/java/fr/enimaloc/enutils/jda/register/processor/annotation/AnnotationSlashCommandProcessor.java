package fr.enimaloc.enutils.jda.register.processor.annotation;

import fr.enimaloc.enutils.jda.commands.RegisteredSlash;
import fr.enimaloc.enutils.jda.commands.RegisteredSlash.OptionTransformer;
import fr.enimaloc.enutils.jda.commands.UnionCommandData;
import fr.enimaloc.enutils.jda.exception.CommandException;
import fr.enimaloc.enutils.jda.exception.RegisteredExceptionHandler;
import fr.enimaloc.enutils.jda.register.annotation.*;
import fr.enimaloc.enutils.jda.register.processor.SlashCommandProcessor;
import fr.enimaloc.enutils.jda.utils.AnnotationUtils;
import fr.enimaloc.enutils.jda.utils.Checks;
import fr.enimaloc.enutils.jda.utils.Utils;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.entities.*;
import net.dv8tion.jda.api.entities.channel.Channel;
import net.dv8tion.jda.api.entities.channel.ChannelType;
import net.dv8tion.jda.api.entities.channel.attribute.IThreadContainer;
import net.dv8tion.jda.api.entities.channel.concrete.*;
import net.dv8tion.jda.api.entities.channel.middleman.*;
import net.dv8tion.jda.api.events.interaction.command.CommandAutoCompleteInteractionEvent;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.interactions.DiscordLocale;
import net.dv8tion.jda.api.interactions.InteractionHook;
import net.dv8tion.jda.api.interactions.commands.DefaultMemberPermissions;
import net.dv8tion.jda.api.interactions.commands.OptionMapping;
import net.dv8tion.jda.api.interactions.commands.OptionType;
import net.dv8tion.jda.api.interactions.commands.build.OptionData;
import net.dv8tion.jda.api.interactions.commands.build.SlashCommandData;
import net.dv8tion.jda.api.interactions.commands.build.SubcommandData;
import net.dv8tion.jda.api.interactions.commands.build.SubcommandGroupData;
import net.dv8tion.jda.internal.entities.*;
import net.dv8tion.jda.internal.entities.channel.AbstractChannelImpl;
import net.dv8tion.jda.internal.entities.channel.concrete.*;
import net.dv8tion.jda.internal.entities.channel.mixin.attribute.IThreadContainerMixin;
import net.dv8tion.jda.internal.entities.channel.mixin.middleman.GuildMessageChannelMixin;
import net.dv8tion.jda.internal.entities.channel.mixin.middleman.StandardGuildChannelMixin;
import net.dv8tion.jda.internal.entities.channel.mixin.middleman.StandardGuildMessageChannelMixin;
import net.dv8tion.jda.internal.interactions.CommandDataImpl;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.*;
import java.nio.file.Path;
import java.util.*;
import java.util.concurrent.ExecutionException;
import java.util.function.BiFunction;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.UnaryOperator;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class AnnotationSlashCommandProcessor implements SlashCommandProcessor {

    public static final Logger LOGGER = LoggerFactory.getLogger(AnnotationSlashCommandProcessor.class);
    public static final String DEFAULT_DESCRIPTION = "No description";

    private List<OptionTransformer<?>> transformers = new ArrayList<>(DEFAULT_TRANSFORMERS);

    // region Method command
    @Override
    public RegisteredSlash registerCommand(Object instance, Method method) {
        return processMethodCommand(instance, method);
    }
    // endregion

    // region Class command
    @Override
    public RegisteredSlash[] registerCommand(Object instance) {
        return processClassCommands(instance);
    }
    // endregion

    private RegisteredExceptionHandler[] getExceptionHandler(Object instance) {
        List<RegisteredExceptionHandler> exceptionsHandler = new ArrayList<>();
        for (Method method : instance.getClass().getDeclaredMethods()) {
            if (method.isAnnotationPresent(Catch.class)) {
                Catch exceptionHandler = method.getAnnotation(Catch.class);
                Checks.check(Arrays.stream(method.getParameterTypes())
                                .allMatch(t -> Throwable.class.isAssignableFrom(t)
                                        || InteractionHook.class.isAssignableFrom(t)
                                        || SlashCommandInteractionEvent.class.isAssignableFrom(t)),
                        "ExceptionHandler method supports only Throwable, InteractionHook and SlashCommandInteractionEvent parameters");
                Checks.Reflection.trySetAccessible(method, "ExceptionHandler method must be accessible");
                exceptionsHandler.add(new RegisteredExceptionHandler(
                        SlashCommandInteractionEvent.class,
                        exceptionHandler.value(),
                        instance,
                        method
                ));
            }
        }
        return exceptionsHandler.toArray(RegisteredExceptionHandler[]::new);
    }

    private RegisteredSlash processMethodCommand(Object instance, Method method) {
        if (!method.isAnnotationPresent(Slash.class)) {
            return null;
        }
        Checks.min(method.getParameterCount(), 1, "Command method must have at least 1 parameter");
        Checks.Reflection.assignableFrom(method.getParameterTypes()[0], SlashCommandInteractionEvent.class,
                "Command method first parameter must be SlashCommandInteractionEvent or a subclass");
        Checks.Reflection.trySetAccessible(method, "Command method must be accessible");

        Slash slash = method.getAnnotation(Slash.class);
        String name = normaliseName(AnnotationUtils.get(slash.name()).orElse(method.getName()));
        String description = AnnotationUtils.get(slash.description()).orElse(DEFAULT_DESCRIPTION);
        try {
            RegisteredSlash.ParameterData[] params = processParams(instance, method);
            CommandDataImpl data = applyData(new CommandDataImpl(name, description), instance, name, method);
            data.addOptions(Arrays.stream(params)
                    .map(RegisteredSlash.ParameterData::data)
                    .toArray(OptionData[]::new));
            return new RegisteredSlash(
                    new UnionCommandData(data),
                    params,
                    instance,
                    method,
                    name,
                    description,
                    getExceptionHandler(instance),
                    null
            );
        } catch (IllegalArgumentException e) {
            throw new CommandException("Error while registering %s (%s#%s)".formatted(name, method.getDeclaringClass().getName(), method.getName()), e);
        }
    }

    private RegisteredSlash[] processClassCommands(Object instance) {
        List<RegisteredSlash> commands = new ArrayList<>();
        Class<?> clazz = instance.getClass();
        for (Method method : clazz.getDeclaredMethods()) {
            if (method.isAnnotationPresent(Slash.class)) {
                RegisteredSlash command = processMethodCommand(instance, method);
                if (command != null) {
                    commands.add(command);
                }
            }
        }
        if (!clazz.isAnnotationPresent(Slash.class)) {
            return commands.toArray(RegisteredSlash[]::new);
        }
        Slash slash = clazz.getAnnotation(Slash.class);
        String name = normaliseName(AnnotationUtils.get(slash.name()).orElse(clazz.getSimpleName()));
        String description = AnnotationUtils.get(slash.description()).orElse(DEFAULT_DESCRIPTION);
        CommandDataImpl data = applyData(new CommandDataImpl(name, description), instance, name, clazz);
        for (Method method : Arrays.stream(instance.getClass().getDeclaredMethods())
                .filter(m -> m.isAnnotationPresent(Slash.Sub.class))
                .toList()) {
            RegisteredSlash subCommand = processMethodSubCommand(instance, name, method);
            if (subCommand != null) {
                commands.add(subCommand);
                data.addSubcommands(subCommand.data().getSubcommandData());
            }
        }

        for (Field field : Arrays.stream(instance.getClass().getDeclaredFields())
                .filter(f -> f.isAnnotationPresent(Slash.Sub.GroupProvider.class))
                .toList()) {
            RegisteredSlash[] groupCommands = processClassGroupCommands(instance, name, field);
            commands.addAll(Arrays.asList(groupCommands));
            Arrays.stream(groupCommands)
                    .filter(c -> c.data().isSubcommandGroupData())
                    .forEach(c -> data.addSubcommandGroups(c.data().getSubcommandGroupData()));
        }
        for (Method method : Arrays.stream(instance.getClass().getDeclaredMethods())
                .filter(m -> m.isAnnotationPresent(Slash.Sub.GroupProvider.class))
                .toList()) {
            RegisteredSlash[] groupCommands = processClassGroupCommands(instance, name, method);
            commands.addAll(Arrays.asList(groupCommands));
            Arrays.stream(groupCommands)
                    .filter(c -> c.data().isSubcommandGroupData())
                    .forEach(c -> data.addSubcommandGroups(c.data().getSubcommandGroupData()));
        }
        commands.add(new RegisteredSlash(
                new UnionCommandData(data),
                new RegisteredSlash.ParameterData[0],
                instance,
                null,
                name,
                description,
                getExceptionHandler(instance),
                null
        ));
        return commands.toArray(RegisteredSlash[]::new);
    }

    private RegisteredSlash processMethodSubCommand(Object instance, String baseName, Method method) {
        if (!method.isAnnotationPresent(Slash.Sub.class)) {
            return null;
        }
        Checks.Reflection.trySetAccessible(method, "Command method must be accessible");
        Slash.Sub sub = method.getAnnotation(Slash.Sub.class);
        String subName = normaliseName(AnnotationUtils.get(sub.name()).orElse(method.getName()));
        String subDescription = AnnotationUtils.get(sub.description()).orElse(DEFAULT_DESCRIPTION);
        RegisteredSlash.ParameterData[] params = processParams(instance, method);
        SubcommandData subData = applyData(new SubcommandData(subName, subDescription), instance,
                baseName.replace(" ", ".") + "." + subName, method);
        subData.addOptions(Arrays.stream(params)
                .map(RegisteredSlash.ParameterData::data)
                .toArray(OptionData[]::new));
        return new RegisteredSlash(
                new UnionCommandData(subData),
                params,
                instance,
                method,
                baseName + " " + subName,
                subDescription,
                getExceptionHandler(instance),
                null
        );
    }

    private <T extends AccessibleObject & java.lang.reflect.Member> RegisteredSlash[] processClassGroupCommands(
            Object instance, String baseName, T object) {
        if (!object.isAnnotationPresent(Slash.Sub.GroupProvider.class)) {
            return new RegisteredSlash[0];
        }
        Checks.Reflection.trySetAccessible(object, "Command field must be accessible");
        Object targetInstance;
        try {
            targetInstance = object instanceof Field field ? field.get(instance) : ((Method) object).invoke(instance);
        } catch (IllegalAccessException | InvocationTargetException e) {
            throw new CommandException(e);
        }
        Slash.Sub.GroupProvider groupProvider = object.getAnnotation(Slash.Sub.GroupProvider.class);
        String name = normaliseName(AnnotationUtils.get(groupProvider.name()).orElse(object.getName()));
        String description = AnnotationUtils.get(groupProvider.description()).orElse(DEFAULT_DESCRIPTION);
        SubcommandGroupData data = applyData(new SubcommandGroupData(name, description), targetInstance, baseName + " " + name, object);
        List<RegisteredSlash> commands = new ArrayList<>();
        for (Method method : targetInstance.getClass().getDeclaredMethods()) {
            RegisteredSlash subCommand = processMethodSubCommand(targetInstance, baseName + " " + name, method);
            if (subCommand != null) {
                commands.add(subCommand);
                data.addSubcommands(subCommand.data().getSubcommandData());
            }
        }
        commands.add(new RegisteredSlash(
                new UnionCommandData(data),
                new RegisteredSlash.ParameterData[0],
                targetInstance,
                null,
                baseName + " " + name,
                description,
                getExceptionHandler(instance),
                null
        ));
        return commands.toArray(RegisteredSlash[]::new);
    }

    private SubcommandGroupData applyData(SubcommandGroupData data, Object instance, String name, AnnotatedElement element) {
        if (element.isAnnotationPresent(Slash.Sub.GroupProvider.class)) {
            Slash.Sub.GroupProvider groupProvider = element.getAnnotation(Slash.Sub.GroupProvider.class);
            if (groupProvider.i18nName() != null) {
                processI18n(instance, name, groupProvider.i18nName(), (locale, i18n) -> data.setNameLocalization(locale, normaliseName(i18n)));
            }
            if (groupProvider.i18nDescription() != null) {
                processI18n(instance, name, groupProvider.i18nDescription(), data::setDescriptionLocalization);
            }
        }
        return data;
    }

    private <T extends SlashCommandData> T applyData(T data, Object instance, String name, AnnotatedElement element) {
        data.setNSFW(element.isAnnotationPresent(Command.Nsfw.class));
        data.setGuildOnly(element.isAnnotationPresent(Command.GuildOnly.class));
        if (element.isAnnotationPresent(Command.RequiredPermission.class)) {
            Command.RequiredPermission permission = element.getAnnotation(Command.RequiredPermission.class);
            if (permission.value().length != 0) {
                data.setDefaultPermissions(DefaultMemberPermissions.enabledFor(permission.value()));
            } else {
                data.setDefaultPermissions(permission.enabledForAll() ? DefaultMemberPermissions.ENABLED : DefaultMemberPermissions.DISABLED);
            }
        }
        I18n i18nName = null;
        I18n i18nDescription = null;
        if (element.isAnnotationPresent(Slash.class)) {
            Slash slash = element.getAnnotation(Slash.class);
            i18nName = slash.i18nName();
            i18nDescription = slash.i18nDescription();
        } else if (element.isAnnotationPresent(Slash.Sub.GroupProvider.class)) {
            Slash.Sub.GroupProvider groupProvider = element.getAnnotation(Slash.Sub.GroupProvider.class);
            i18nName = groupProvider.i18nName();
            i18nDescription = groupProvider.i18nDescription();
        }
        if (i18nName != null) {
            processI18n(instance, name, i18nName, (locale, i18n) -> data.setNameLocalization(locale, normaliseName(i18n)));
        }
        if (i18nDescription != null) {
            processI18n(instance, name, i18nDescription, data::setDescriptionLocalization);
        }
        return data;
    }

    private SubcommandData applyData(SubcommandData data, Object instance, String name, AnnotatedElement element) {
        if (element.isAnnotationPresent(Slash.Sub.class)) {
            Slash.Sub sub = element.getAnnotation(Slash.Sub.class);
            if (sub.i18nName() != null) {
                processI18n(instance, name, sub.i18nName(), (locale, i18n) -> data.setNameLocalization(locale, normaliseName(i18n)));
            }
            if (sub.i18nDescription() != null) {
                processI18n(instance, name, sub.i18nDescription(), data::setDescriptionLocalization);
            }
        }
        return data;
    }

    @Override
    public RegisteredSlash[] generateArray(int length) {
        return new RegisteredSlash[length];
    }

    // region Commands related
    private RegisteredSlash.ParameterData[] processParams(Object instance, Method method) {
        List<RegisteredSlash.ParameterData> params = new ArrayList<>();
        for (int i = 1; i < method.getParameters().length; i++) {
            Parameter parameter = method.getParameters()[i];
            Checks.Reflection.annotationPresent(parameter, Slash.Option.class);

            Slash.Option option = parameter.getAnnotation(Slash.Option.class);
            String name = normaliseName(AnnotationUtils.get(option.name()).orElse(parameter.getName()));
            String description = AnnotationUtils.get(option.description()).orElse(DEFAULT_DESCRIPTION);
            OptionTransformer<?> transformer = transformers.stream()
                    .filter(t -> parameter.getType().isAssignableFrom(t.clazz()))
                    .findFirst()
                    .orElse(DEFAULT_TRANSFORMER);
            OptionType type = transformer.type();

            if (Optional.class.isAssignableFrom(parameter.getType())) {
                OptionTransformer<?> retrievedTransformer = transformers.stream()
                        .filter(t -> parameter.getParameterizedType() instanceof ParameterizedType pt
                                && pt.getActualTypeArguments()[0] == t.clazz())
                        .findFirst()
                        .orElse(DEFAULT_TRANSFORMER);
                type = retrievedTransformer.type() != OptionType.UNKNOWN ? retrievedTransformer.type() : type;
                transformer = new OptionTransformer<>(
                        Optional.class,
                        type,
                        retrievedTransformer.builder().andThen(builder -> builder.setRequired(false)),
                        (event, mapping) -> mapping == null
                                ? Optional.empty()
                                : Optional.of(retrievedTransformer.transform().apply(event, mapping))
                );
            }

            if (parameter.getType().isEnum()) {
                transformer = ENUM_TRANSFORMER.apply(parameter.getType().asSubclass(Enum.class));
            }
            type = transformer.type() != OptionType.UNKNOWN ? transformer.type() : type;

            OptionData data = new OptionData(
                    type, name, description,
                    !Optional.class.isAssignableFrom(parameter.getType())
                            && !OptionalLong.class.isAssignableFrom(parameter.getType())
                            && !OptionalInt.class.isAssignableFrom(parameter.getType())
                            && !OptionalDouble.class.isAssignableFrom(parameter.getType())
            );
            if (parameter.isAnnotationPresent(Slash.Option.Choices.class)) {
                Slash.Option.Choices choices = parameter.getAnnotation(Slash.Option.Choices.class);
                for (Slash.Option.Choice choice : choices.value()) {
                    data.addChoice(choice.value(), AnnotationUtils.get(choice.devValue()).orElse(choice.value()));
                }
            }
            Consumer<CommandAutoCompleteInteractionEvent> autocompletionConsumer = null;
            if (parameter.isAnnotationPresent(Slash.Option.AutoCompletion.class)) {
                Slash.Option.AutoCompletion autoCompletion = parameter.getAnnotation(Slash.Option.AutoCompletion.class);
                Optional<Method> methodFound = Utils.getMethod(instance, autoCompletion.target());
                if (autoCompletion.array().length != 0) {
                    autocompletionConsumer = event -> event.replyChoices(Arrays.stream(autoCompletion.array())
                                    .filter(s -> s.startsWith(event.getFocusedOption().getValue()))
                                    .map(s -> new net.dv8tion.jda.api.interactions.commands.Command.Choice(s, s))
                                    .toArray(net.dv8tion.jda.api.interactions.commands.Command.Choice[]::new)
                            )
                            .queue();
                } else if (methodFound.isPresent()) {
                    Checks.equals(methodFound.get().getParameterTypes(), new Class[]{CommandAutoCompleteInteractionEvent.class},
                            "AutoCompletion method must have only one parameter of type CommandAutoCompleteInteractionEvent");
                    Checks.Reflection.trySetAccessible(methodFound.get(), "AutoCompletion method must be accessible");
                    autocompletionConsumer = event -> {
                        try {
                            methodFound.get().invoke(instance, event);
                        } catch (Exception e) {
                            throw new CommandException(e);
                        }
                    };
                } else {
                    LOGGER.warn("AutoCompletion method {}#{} not found, disabling it...", autoCompletion.target().clazz().getName(), autoCompletion.target().method());
                }

                data.setAutoComplete(autocompletionConsumer != null);
            }
            params.add(new RegisteredSlash.ParameterData(transformer.builder().apply(data, parameter), transformer, autocompletionConsumer));
        }
        return params.toArray(RegisteredSlash.ParameterData[]::new);
    }
    // endregion

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
                        i18nFunction.apply(locale.language(), bundle.getString(i18n.key().replace("{{command_name}}", commandName)));
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

    private String normaliseName(String str) {
        String normalized = str.replaceAll("([a-z])([A-Z])", "$1-$2").replace(' ', '-').toLowerCase();
        if (!normalized.equals(str)) {
            LOGGER.warn("Command name {} was normalized to {}", str, normalized);
        }
        return normalized;
    }

    // region Transformers
    private record RetGenOptionData(OptionData data, OptionTransformer<?> transformer,
                                    @Nullable Consumer<CommandAutoCompleteInteractionEvent> autocompletion) {
    }

    private static Function<Message.Attachment, File> tempFileSupplier = attachment -> {
        try {
            File file = File.createTempFile(
                    "jda-enutils.attachement-",
                    "." + attachment.getFileExtension() + ".tmp"
            );
            file.deleteOnExit();
            return file;
        } catch (IOException e) {
            throw new CommandException(e);
        }
    };

    public static final OptionTransformer<OptionMapping> DEFAULT_TRANSFORMER =
            new OptionTransformer<>(OptionMapping.class, OptionType.UNKNOWN, UnaryOperator.identity());
    @SuppressWarnings("rawtypes")
    public static final List<OptionTransformer<?>> DEFAULT_TRANSFORMERS = List.of(
            DEFAULT_TRANSFORMER,
            new OptionTransformer<OptionalInt>(
                    OptionalInt.class,
                    OptionType.INTEGER,
                    (builder, parameter) -> builder.setRequired(false),
                    mapping -> mapping == null ? OptionalInt.empty() : OptionalInt.of(mapping.getAsInt())),
            new OptionTransformer<OptionalLong>(
                    OptionalLong.class,
                    OptionType.INTEGER,
                    (builder, parameter) -> builder.setRequired(false),
                    mapping -> mapping == null ? OptionalLong.empty() : OptionalLong.of(mapping.getAsLong())),
            new OptionTransformer<OptionalDouble>(
                    OptionalDouble.class,
                    OptionType.INTEGER,
                    (builder, parameter) -> builder.setRequired(false),
                    mapping -> mapping == null ? OptionalDouble.empty() : OptionalDouble.of(mapping.getAsDouble())),

            new OptionTransformer<String>(String.class, OptionType.STRING, (builder, parameter) -> {
                if (parameter.isAnnotationPresent(Length.class)) {
                    Length range = parameter.getAnnotation(Length.class);
                    Checks.inRange(range.min(), 0, 6000, "String length min must be between 0 and 6000");
                    Checks.inRange(range.max(), 1, 6000, "String length max must be between 1 and 6000");
                    builder.setRequiredLength(range.min(), range.max());
                }
                return builder;
            }, OptionMapping::getAsString),

            new OptionTransformer<Integer>(Integer.class, OptionType.INTEGER, (builder, parameter) -> {
                if (parameter.isAnnotationPresent(Slash.Option.Range.class)) {
                    Slash.Option.Range range = parameter.getAnnotation(Slash.Option.Range.class);
                    builder.setRequiredRange(range.min(), range.max());
                }
                return builder;
            }, OptionMapping::getAsInt),
            new OptionTransformer<Integer>(int.class, OptionType.INTEGER, (builder, parameter) -> {
                if (parameter.isAnnotationPresent(Slash.Option.Range.class)) {
                    Slash.Option.Range range = parameter.getAnnotation(Slash.Option.Range.class);
                    builder.setRequiredRange((long) range.min(), (long) range.max());
                }
                return builder;
            }, OptionMapping::getAsInt),

            new OptionTransformer<Long>(Long.class, OptionType.INTEGER, (builder, parameter) -> {
                if (parameter.isAnnotationPresent(Slash.Option.Range.class)) {
                    Slash.Option.Range range = parameter.getAnnotation(Slash.Option.Range.class);
                    builder.setRequiredRange((long) range.min(), (long) range.max());
                }
                return builder;
            }, OptionMapping::getAsLong),
            new OptionTransformer<Long>(long.class, OptionType.INTEGER, (builder, parameter) -> {
                if (parameter.isAnnotationPresent(Slash.Option.Range.class)) {
                    Slash.Option.Range range = parameter.getAnnotation(Slash.Option.Range.class);
                    builder.setRequiredRange((long) range.min(), (long) range.max());
                }
                return builder;
            }, OptionMapping::getAsLong),

            new OptionTransformer<Double>(Double.class, OptionType.NUMBER, (builder, parameter) -> {
                if (parameter.isAnnotationPresent(Slash.Option.Range.class)) {
                    Slash.Option.Range range = parameter.getAnnotation(Slash.Option.Range.class);
                    builder.setRequiredRange(range.min(), range.max());
                }
                return builder;
            }, OptionMapping::getAsDouble),
            new OptionTransformer<Double>(double.class, OptionType.NUMBER, (builder, parameter) -> {
                if (parameter.isAnnotationPresent(Slash.Option.Range.class)) {
                    Slash.Option.Range range = parameter.getAnnotation(Slash.Option.Range.class);
                    builder.setRequiredRange(range.min(), range.max());
                }
                return builder;
            }, OptionMapping::getAsDouble),

            new OptionTransformer<>(Boolean.class, OptionType.BOOLEAN, OptionMapping::getAsBoolean),
            new OptionTransformer<>(boolean.class, OptionType.BOOLEAN, OptionMapping::getAsBoolean),

            new OptionTransformer<>(Message.Attachment.class, OptionType.ATTACHMENT,
                    OptionMapping::getAsAttachment),
            new OptionTransformer<>(InputStream.class, OptionType.ATTACHMENT,
                    mapping -> {
                        try {
                            return mapping.getAsAttachment().getProxy().download().get();
                        } catch (InterruptedException | ExecutionException e) {
                            Thread.currentThread().interrupt();
                            throw new CommandException(e);
                        }
                    }),
            new OptionTransformer<>(File.class, OptionType.ATTACHMENT,
                    mapping -> {
                        try {
                            return mapping.getAsAttachment()
                                    .getProxy()
                                    .downloadToFile(
                                            tempFileSupplier.apply(mapping.getAsAttachment()))
                                    .get();
                        } catch (InterruptedException | ExecutionException e) {
                            Thread.currentThread().interrupt();
                            throw new CommandException(e);
                        }
                    }),
            new OptionTransformer<>(Path.class, OptionType.ATTACHMENT,
                    mapping -> {
                        try {
                            return mapping.getAsAttachment()
                                    .getProxy()
                                    .downloadToFile(
                                            tempFileSupplier.apply(mapping.getAsAttachment()))
                                    .get()
                                    .toPath();
                        } catch (InterruptedException | ExecutionException e) {
                            Thread.currentThread().interrupt();
                            throw new CommandException(e);
                        }
                    }),

            /* Interface */
            new OptionTransformer<>(User.class, OptionType.USER, OptionMapping::getAsUser),
            new OptionTransformer<>(Member.class, OptionType.USER, OptionMapping::getAsMember),
            new OptionTransformer<>(Role.class, OptionType.ROLE, OptionMapping::getAsRole),
            new OptionTransformer<>(IMentionable.class, OptionType.MENTIONABLE, OptionMapping::getAsMentionable),

            new OptionTransformer<>(Channel.class, OptionType.CHANNEL, OptionMapping::getAsChannel),
            new OptionTransformer<MessageChannel>(MessageChannel.class, OptionType.CHANNEL,
                    builder -> builder.setChannelTypes(ChannelType.TEXT,
                            ChannelType.FORUM,
                            ChannelType.NEWS,
                            ChannelType.STAGE,
                            ChannelType.VOICE),
                    mapping -> mapping.getAsChannel().asGuildMessageChannel()),
            new OptionTransformer<GuildMessageChannel>(GuildMessageChannel.class, OptionType.CHANNEL,
                    builder -> builder.setChannelTypes(ChannelType.TEXT,
                            ChannelType.FORUM,
                            ChannelType.NEWS),
                    mapping -> mapping.getAsChannel().asGuildMessageChannel()),
            new OptionTransformer<TextChannel>(TextChannel.class, OptionType.CHANNEL,
                    builder -> builder.setChannelTypes(ChannelType.TEXT),
                    mapping -> mapping.getAsChannel().asTextChannel()),

            new OptionTransformer<AudioChannel>(AudioChannel.class, OptionType.CHANNEL,
                    builder -> builder.setChannelTypes(ChannelType.VOICE,
                            ChannelType.STAGE),
                    mapping -> mapping.getAsChannel().asAudioChannel()
            ),
            new OptionTransformer<VoiceChannel>(VoiceChannel.class, OptionType.CHANNEL,
                    builder -> builder.setChannelTypes(ChannelType.VOICE),
                    mapping -> mapping.getAsChannel().asVoiceChannel()),
            new OptionTransformer<NewsChannel>(NewsChannel.class, OptionType.CHANNEL,
                    builder -> builder.setChannelTypes(ChannelType.NEWS),
                    mapping -> mapping.getAsChannel().asNewsChannel()),
            new OptionTransformer<StageChannel>(StageChannel.class, OptionType.CHANNEL,
                    builder -> builder.setChannelTypes(ChannelType.STAGE),
                    mapping -> mapping.getAsChannel().asStageChannel()),
            new OptionTransformer<ThreadChannel>(ThreadChannel.class, OptionType.CHANNEL,
                    builder -> builder.setChannelTypes(ChannelType.GUILD_NEWS_THREAD,
                            ChannelType.GUILD_PRIVATE_THREAD,
                            ChannelType.GUILD_PUBLIC_THREAD),
                    mapping -> mapping.getAsChannel().asThreadChannel()),
            new OptionTransformer<ForumChannel>(ForumChannel.class, OptionType.CHANNEL,
                    builder -> builder.setChannelTypes(ChannelType.FORUM),
                    mapping -> mapping.getAsChannel().asForumChannel()),
            new OptionTransformer<Category>(Category.class, OptionType.CHANNEL,
                    builder -> builder.setChannelTypes(ChannelType.CATEGORY),
                    mapping -> mapping.getAsChannel().asCategory()),
            new OptionTransformer<IThreadContainer>(IThreadContainer.class, OptionType.CHANNEL,
                    builder -> builder.setChannelTypes(ChannelType.FORUM,
                            ChannelType.NEWS,
                            ChannelType.TEXT),
                    mapping -> mapping.getAsChannel().asThreadContainer()),
            new OptionTransformer<StandardGuildChannel>(StandardGuildChannel.class, OptionType.CHANNEL,
                    builder -> builder.setChannelTypes(ChannelType.TEXT,
                            ChannelType.NEWS,
                            ChannelType.STAGE,
                            ChannelType.VOICE,
                            ChannelType.FORUM),
                    mapping -> mapping.getAsChannel().asStandardGuildChannel()),
            new OptionTransformer<StandardGuildMessageChannel>(StandardGuildMessageChannel.class,
                    OptionType.CHANNEL,
                    builder -> builder.setChannelTypes(ChannelType.NEWS,
                            ChannelType.TEXT),
                    mapping -> mapping.getAsChannel()
                            .asStandardGuildMessageChannel()),

            /* Implementation */
            new OptionTransformer<>(UserImpl.class, OptionType.USER, mapping -> (UserImpl) mapping.getAsUser()),
            new OptionTransformer<>(MemberImpl.class, OptionType.USER, mapping -> (MemberImpl) mapping.getAsMember()),

            new OptionTransformer<>(RoleImpl.class, OptionType.ROLE, mapping -> (RoleImpl) mapping.getAsRole()),

            new OptionTransformer<>(AbstractChannelImpl.class, OptionType.CHANNEL, mapping -> (AbstractChannelImpl<?>) mapping.getAsChannel()),
            new OptionTransformer<GuildMessageChannelMixin>(GuildMessageChannelMixin.class, OptionType.CHANNEL,
                    builder -> builder.setChannelTypes(ChannelType.TEXT,
                            ChannelType.FORUM,
                            ChannelType.NEWS),
                    mapping -> (GuildMessageChannelMixin<?>) mapping.getAsChannel().asGuildMessageChannel()),
            new OptionTransformer<TextChannelImpl>(TextChannelImpl.class, OptionType.CHANNEL,
                    builder -> builder.setChannelTypes(ChannelType.TEXT),
                    mapping -> (TextChannelImpl) mapping.getAsChannel().asTextChannel()),

            new OptionTransformer<VoiceChannelImpl>(VoiceChannelImpl.class, OptionType.CHANNEL,
                    builder -> builder.setChannelTypes(ChannelType.VOICE),
                    mapping -> (VoiceChannelImpl) mapping.getAsChannel().asVoiceChannel()),
            new OptionTransformer<NewsChannelImpl>(NewsChannelImpl.class, OptionType.CHANNEL,
                    builder -> builder.setChannelTypes(ChannelType.NEWS),
                    mapping -> (NewsChannelImpl) mapping.getAsChannel().asNewsChannel()),
            new OptionTransformer<StageChannelImpl>(StageChannelImpl.class, OptionType.CHANNEL,
                    builder -> builder.setChannelTypes(ChannelType.STAGE),
                    mapping -> (StageChannelImpl) mapping.getAsChannel().asStageChannel()),
            new OptionTransformer<ThreadChannelImpl>(ThreadChannelImpl.class, OptionType.CHANNEL,
                    builder -> builder.setChannelTypes(ChannelType.GUILD_NEWS_THREAD,
                            ChannelType.GUILD_PRIVATE_THREAD,
                            ChannelType.GUILD_PUBLIC_THREAD),
                    mapping -> (ThreadChannelImpl) mapping.getAsChannel().asThreadChannel()),
            new OptionTransformer<ForumChannelImpl>(ForumChannelImpl.class, OptionType.CHANNEL,
                    builder -> builder.setChannelTypes(ChannelType.FORUM),
                    mapping -> (ForumChannelImpl) mapping.getAsChannel().asForumChannel()),
            new OptionTransformer<CategoryImpl>(CategoryImpl.class, OptionType.CHANNEL,
                    builder -> builder.setChannelTypes(ChannelType.CATEGORY),
                    mapping -> (CategoryImpl) mapping.getAsChannel().asCategory()),
            new OptionTransformer<IThreadContainerMixin>(IThreadContainerMixin.class, OptionType.CHANNEL,
                    builder -> builder.setChannelTypes(ChannelType.FORUM,
                            ChannelType.NEWS,
                            ChannelType.TEXT),
                    mapping -> (IThreadContainerMixin<?>) mapping.getAsChannel().asThreadContainer()),
            new OptionTransformer<StandardGuildChannelMixin>(StandardGuildChannelMixin.class, OptionType.CHANNEL,
                    builder -> builder.setChannelTypes(ChannelType.TEXT,
                            ChannelType.NEWS,
                            ChannelType.STAGE,
                            ChannelType.VOICE,
                            ChannelType.FORUM),
                    mapping -> (StandardGuildChannelMixin<?>) mapping.getAsChannel().asStandardGuildChannel()),
            new OptionTransformer<StandardGuildMessageChannelMixin>(StandardGuildMessageChannelMixin.class,
                    OptionType.CHANNEL,
                    builder -> builder.setChannelTypes(ChannelType.NEWS,
                            ChannelType.TEXT),
                    mapping -> (StandardGuildMessageChannelMixin<?>) mapping.getAsChannel()
                            .asStandardGuildMessageChannel()),

            new OptionTransformer<>(IMentionable.class,
                    OptionType.MENTIONABLE,
                    OptionMapping::getAsMentionable),

            new OptionTransformer<>(Message.class, OptionType.STRING,
                    (event, mapping) -> {
                        Matcher matcher = Message.JUMP_URL_PATTERN.matcher(
                                mapping.getAsString());
                        boolean foundChannel = true;
                        if (!matcher.find()) {
                            matcher = Pattern.compile(
                                            "(?<guild>\\d+)/(?<channel>\\d+)/(?<message>\\d+)")
                                    .matcher(mapping.getAsString());
                            if (!matcher.find()) {
                                matcher = Pattern.compile("(?<channel>\\d+)/(?<message>\\d+)")
                                        .matcher(mapping.getAsString());
                                if (!matcher.find()) {
                                    foundChannel = false;
                                    matcher = Pattern.compile("(?<message>\\d+)")
                                            .matcher(mapping.getAsString());
                                    if (!matcher.find()) {
                                        return null;
                                    }
                                }
                            }
                        }
                        TextChannel channel = event.getChannel().asTextChannel();
                        if (foundChannel) {
                            channel = event.getJDA().getTextChannelById(matcher.group("channel"));
                            if (channel == null) {
                                channel = event.getChannel().asTextChannel();
                            }
                        }
                        return channel.retrieveMessageById(matcher.group("message"))
                                .complete();
                    }),

            new OptionTransformer<>(ReceivedMessage.class, OptionType.STRING,
                    (event, mapping) -> {
                        Matcher matcher = Message.JUMP_URL_PATTERN.matcher(
                                mapping.getAsString());
                        boolean foundChannel = true;
                        if (!matcher.find()) {
                            matcher = Pattern.compile(
                                            "(?<guild>\\d+)/(?<channel>\\d+)/(?<message>\\d+)")
                                    .matcher(mapping.getAsString());
                            if (!matcher.find()) {
                                matcher = Pattern.compile("(?<channel>\\d+)/(?<message>\\d+)")
                                        .matcher(mapping.getAsString());
                                if (!matcher.find()) {
                                    foundChannel = false;
                                    matcher = Pattern.compile("(?<message>\\d+)")
                                            .matcher(mapping.getAsString());
                                    if (!matcher.find()) {
                                        return null;
                                    }
                                }
                            }
                        }
                        TextChannel channel = event.getChannel().asTextChannel();
                        if (foundChannel) {
                            channel = event.getJDA().getTextChannelById(matcher.group("channel"));
                            if (channel == null) {
                                channel = event.getChannel().asTextChannel();
                            }
                        }
                        return (ReceivedMessage) channel.retrieveMessageById(matcher.group("message"))
                                .complete();
                    })
    );
    public static final Function<Class<? extends Enum>, OptionTransformer<Enum<?>>> ENUM_TRANSFORMER = src ->
            new OptionTransformer<Enum<?>>(null, OptionType.STRING, builder -> {
                for (Enum<?> constant : src.getEnumConstants()) {
                    builder.addChoice(constant.name(), constant.name());
                }
                return builder;
            }, (event, option) -> Enum.valueOf(src, option.getAsString()));

    // endregion
}
