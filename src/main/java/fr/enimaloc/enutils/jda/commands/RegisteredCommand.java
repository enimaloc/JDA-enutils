package fr.enimaloc.enutils.jda.commands;

import fr.enimaloc.enutils.jda.exception.RegisteredExceptionHandler;
import net.dv8tion.jda.api.events.interaction.command.CommandAutoCompleteInteractionEvent;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.interactions.InteractionHook;
import net.dv8tion.jda.api.interactions.commands.OptionMapping;
import net.dv8tion.jda.api.interactions.commands.OptionType;
import net.dv8tion.jda.api.interactions.commands.build.OptionData;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.lang.reflect.Method;
import java.lang.reflect.Parameter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.function.BiFunction;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.UnaryOperator;

public record RegisteredCommand(
        @NotNull UnionCommandData data,
        @NotNull ParameterData[] parameters,
        @NotNull Object instance,
        @Nullable Method method,
        @NotNull String fullCommandName,
        @Nullable String description,
        @NotNull RegisteredExceptionHandler[] exceptionsHandler,
        @Nullable DebugInformation debugInformation
) {

    public void execute(SlashCommandInteractionEvent event) {
        List<Object> params = new ArrayList<>();
        if (method.getParameterTypes()[0] == SlashCommandInteractionEvent.class) {
            params.add(event);
        } else if (method.getParameterTypes()[0] == GlobalSlashCommandEvent.class) {
            params.add(new GlobalSlashCommandEvent(event));
        } else if (method.getParameterTypes()[0] == GuildSlashCommandEvent.class) {
            params.add(new GuildSlashCommandEvent(event));
        }
        for (int i = 1; i < method.getParameters().length; i++) {
            if (event.getOptions().size() > i - 1) {
                params.add(parameters[i - 1].transformer().transform().apply(event, event.getOptions().get(i - 1)));
            } else {
                params.add((parameters[i - 1].transformer().transform().apply(event, null)));
            }
        }


        try {
            method.invoke(instance, params.toArray());
        } catch (Throwable t) {
            processThrowable(event, event.deferReply().complete(), t.getCause());
        }
    }

    private void processThrowable(SlashCommandInteractionEvent event, InteractionHook hook, Throwable t) {
        for (RegisteredExceptionHandler handler : exceptionsHandler) {
            Class<?> exceptionArg = Arrays.stream(handler.method().getParameterTypes())
                    .filter(c -> c.isAssignableFrom(t.getClass()))
                    .findFirst()
                    .or(() -> Arrays.stream(handler.exceptions())
                            .filter(c -> c.isAssignableFrom(t.getClass()))
                            .findFirst())
                    .orElse(Void.class);
            if (exceptionArg.isAssignableFrom(t.getClass())) {
                handler.execute(event, hook, t);
                return;
            }
        }
        t.printStackTrace();
    }

    public void autoComplete(CommandAutoCompleteInteractionEvent event) {
        Arrays.stream(parameters)
                .filter(data -> data.data().getName().equals(event.getFocusedOption().getName()))
                .findFirst()
                .ifPresent(data -> data.autocompletionConsumer().accept(event));
    }

    public record ParameterData(OptionData data, OptionTransformer<?> transformer,
                         Consumer<CommandAutoCompleteInteractionEvent> autocompletionConsumer) {}


    public record OptionTransformer<T>(Class<T> clazz, OptionType type,
                                        BiFunction<OptionData, Parameter, OptionData> builder,
                                        BiFunction<SlashCommandInteractionEvent, OptionMapping, T> transform,
                                        T defaultValue) {
        public OptionTransformer(
                Class<T> clazz, OptionType type, UnaryOperator<OptionData> builder,
                BiFunction<SlashCommandInteractionEvent, OptionMapping, T> transformer, T defaultValue
        ) {
            this(clazz, type, (b, parameter) -> builder.apply(b), transformer, defaultValue);
        }


        public OptionTransformer(
                Class<T> clazz, OptionType type, BiFunction<OptionData, Parameter, OptionData> builder,
                BiFunction<SlashCommandInteractionEvent, OptionMapping, T> transformer
        ) {
            this(clazz, type, builder, transformer, null);
        }

        public OptionTransformer(
                Class<T> clazz, OptionType type, UnaryOperator<OptionData> builder,
                BiFunction<SlashCommandInteractionEvent, OptionMapping, T> transformer
        ) {
            this(clazz, type, (b, parameter) -> builder.apply(b), transformer, null);
        }

        public OptionTransformer(
                Class<T> clazz, OptionType type,
                BiFunction<SlashCommandInteractionEvent, OptionMapping, T> transformer,
                T defaultValue
        ) {
            this(clazz, type, UnaryOperator.identity(), transformer, defaultValue);
        }

        public OptionTransformer(
                Class<T> clazz, OptionType type,
                BiFunction<SlashCommandInteractionEvent, OptionMapping, T> transformer
        ) {
            this(clazz, type, transformer, null);
        }

        public OptionTransformer(
                Class<T> clazz, OptionType type, UnaryOperator<OptionData> builder,
                Function<OptionMapping, T> transformer, T defaultValue
        ) {
            this(clazz, type, builder, (unusedEvent, mapping) -> transformer.apply(mapping), defaultValue);
        }

        public OptionTransformer(
                Class<T> clazz, OptionType type, UnaryOperator<OptionData> builder,
                Function<OptionMapping, T> transformer
        ) {
            this(clazz, type, builder, transformer, null);
        }

        public OptionTransformer(Class<T> clazz, OptionType type, Function<OptionMapping, T> transformer, T defaultValue) {
            this(clazz, type, UnaryOperator.identity(), transformer, defaultValue);
        }

        public OptionTransformer(Class<T> clazz, OptionType type, Function<OptionMapping, T> transformer) {
            this(clazz, type, transformer, null);
        }

        public OptionTransformer(
                Class<T> clazz, OptionType type, BiFunction<OptionData, Parameter, OptionData> builder,
                Function<OptionMapping, T> mapping
        ) {
            this(clazz, type, builder, (unusedEvent, option) -> mapping.apply(option), null);
        }

        @Override
        public String toString() {
            return "OptionTransformer{" +
                    "clazz=" + clazz +
                    ", type=" + type +
                    ", builder=" + builder +
                    ", transform=" + transform +
                    ", defaultValue=" + defaultValue +
                    '}';
        }
    }
}
