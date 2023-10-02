package fr.enimaloc.enutils.jda.commands;

import fr.enimaloc.enutils.jda.listener.InteractionListener;
import fr.enimaloc.enutils.jda.listener.ModalListener;
import fr.enimaloc.enutils.jda.modals.RegisteredModal;
import fr.enimaloc.enutils.jda.register.annotation.Length;
import fr.enimaloc.enutils.jda.utils.AnnotationUtils;
import fr.enimaloc.enutils.jda.utils.Checks.Reflection;
import fr.enimaloc.enutils.jda.utils.StringUtils;
import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.entities.MessageEmbed;
import net.dv8tion.jda.api.entities.channel.ChannelType;
import net.dv8tion.jda.api.entities.emoji.Emoji;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.events.interaction.component.ButtonInteractionEvent;
import net.dv8tion.jda.api.events.interaction.component.EntitySelectInteractionEvent;
import net.dv8tion.jda.api.events.interaction.component.StringSelectInteractionEvent;
import net.dv8tion.jda.api.interactions.DiscordLocale;
import net.dv8tion.jda.api.interactions.commands.SlashCommandInteraction;
import net.dv8tion.jda.api.interactions.components.ActionRow;
import net.dv8tion.jda.api.interactions.components.Component;
import net.dv8tion.jda.api.interactions.components.ItemComponent;
import net.dv8tion.jda.api.interactions.components.LayoutComponent;
import net.dv8tion.jda.api.interactions.components.buttons.Button;
import net.dv8tion.jda.api.interactions.components.buttons.ButtonStyle;
import net.dv8tion.jda.api.interactions.components.selections.EntitySelectMenu;
import net.dv8tion.jda.api.interactions.components.selections.SelectMenu;
import net.dv8tion.jda.api.interactions.components.selections.SelectOption;
import net.dv8tion.jda.api.interactions.components.selections.StringSelectMenu;
import net.dv8tion.jda.api.interactions.components.text.TextInput;
import net.dv8tion.jda.api.requests.restaction.interactions.ReplyCallbackAction;
import net.dv8tion.jda.internal.interactions.component.ButtonImpl;
import net.dv8tion.jda.internal.interactions.component.EntitySelectMenuImpl;
import net.dv8tion.jda.internal.interactions.component.StringSelectMenuImpl;
import net.dv8tion.jda.internal.interactions.modal.ModalImpl;
import net.dv8tion.jda.internal.utils.Checks;
import net.dv8tion.jda.internal.utils.Helpers;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.reflect.Field;
import java.lang.reflect.ParameterizedType;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Consumer;
import java.util.function.Predicate;
import java.util.stream.Collectors;

public class CustomSlashCommandInteractionEvent extends SlashCommandInteractionEvent {
    public static final Logger LOGGER = LoggerFactory.getLogger(CustomSlashCommandInteractionEvent.class);

    public CustomSlashCommandInteractionEvent(JDA api, long responseNumber, SlashCommandInteraction interaction) {
        super(api, responseNumber, interaction);
    }

    public ReplyCallbackAction replyEphemeral(String message) {
        return reply(message).setEphemeral(true);
    }

    public ReplyCallbackAction replyEphemeralEmbeds(MessageEmbed embed, MessageEmbed... embeds) {
        return replyEmbeds(embed, embeds).setEphemeral(true);
    }

    public ReplyCallbackAction replyEphemeralEmbeds(Collection<MessageEmbed> embeds) {
        return replyEmbeds(embeds).setEphemeral(true);
    }

    public ReplyCallbackAction deferReplyEphemeral() {
        return deferReply(true);
    }

    public DiscordLocale getLocale(boolean favorMemberLocale) {
        return favorMemberLocale ? getUserLocale() : getGuildLocale();
    }

    public ComponentBuilder buildComponent() {
        return new ComponentBuilder(api.getEventManager()
                .getRegisteredListeners()
                .stream()
                .filter(InteractionListener.class::isInstance)
                .map(InteractionListener.class::cast)
                .findFirst()
                .orElseThrow());
    }

    public RegisteredModal buildModal(Object instance) {
        return buildModal(UUID.randomUUID().toString(), instance);
    }

    public <T> RegisteredModal buildModal(String id, Object instance) {
        return buildModal(id, (Class<T>) instance.getClass(), (T) instance);
    }

    private <T> RegisteredModal buildModal(String id, Class<T> clazz, T instance) {
        fr.enimaloc.enutils.jda.utils.Checks.notNull(clazz, "Class cannot be null");
        fr.enimaloc.enutils.jda.utils.Checks.notNull(instance, "Instance cannot be null");
        Reflection.annotationPresent(clazz, fr.enimaloc.enutils.jda.register.annotation.Modal.class);
        fr.enimaloc.enutils.jda.register.annotation.Modal modal = clazz.getAnnotation(fr.enimaloc.enutils.jda.register.annotation.Modal.class);
        String name = AnnotationUtils.get(modal.title()).orElse(clazz.getSimpleName());
        List<RegisteredModal.ModalField> fields = new ArrayList<>();

        for (Field field : Arrays.stream(clazz.getDeclaredFields())
                .filter(field -> field.isAnnotationPresent(fr.enimaloc.enutils.jda.register.annotation.Modal.Component.class))
                .toList()) {
            Reflection.trySetAccessible(field);
            fr.enimaloc.enutils.jda.register.annotation.Modal.Component component
                    = field.getAnnotation(fr.enimaloc.enutils.jda.register.annotation.Modal.Component.class);
            String compId = AnnotationUtils.get(component.id()).orElse(field.getName());
            String label = AnnotationUtils.get(component.label()).orElse(field.getName());
            int actionRowId = field.isAnnotationPresent(fr.enimaloc.enutils.jda.register.annotation.Modal.ActionRow.class)
                    ? field.getAnnotation(fr.enimaloc.enutils.jda.register.annotation.Modal.ActionRow.class).id()
                    : AnnotationUtils.INT_DEFAULT;
            ItemComponent comp = null;
            if (String.class.isAssignableFrom(field.getType()) ||
                    (Optional.class.isAssignableFrom(field.getType()) && field.getGenericType() instanceof ParameterizedType pt
                            && pt.getActualTypeArguments()[0] == String.class)
            ) {
                fr.enimaloc.enutils.jda.register.annotation.Modal.TextInput textInput
                        = field.isAnnotationPresent(fr.enimaloc.enutils.jda.register.annotation.Modal.TextInput.class)
                                ? field.getAnnotation(fr.enimaloc.enutils.jda.register.annotation.Modal.TextInput.class)
                                : fr.enimaloc.enutils.jda.register.annotation.Modal.TextInput.DEFAULT_TEXT_INPUT;

                TextInput.Builder builder = TextInput.create(compId, label, textInput.type());
                AnnotationUtils.get(textInput.placeholder())
                        .map(s -> StringUtils.truncate(s, TextInput.MAX_PLACEHOLDER_LENGTH))
                        .ifPresent(builder::setPlaceholder);
                if (field.isAnnotationPresent(Length.class)) {
                    Length length = field.getAnnotation(Length.class);
                    AnnotationUtils.get(length.min())
                            .stream()
                            .map(i -> Math.max(0, Math.min(i, TextInput.MAX_VALUE_LENGTH)))
                            .findFirst()
                            .ifPresent(builder::setMinLength);
                    AnnotationUtils.get(length.max())
                            .stream()
                            .map(i -> Math.max(1, Math.min(i, TextInput.MAX_VALUE_LENGTH)))
                            .findFirst()
                            .ifPresent(builder::setMaxLength);
                }
                String value = null;
                try {
                    if (field.getType() == String.class) {
                        value = (String) field.get(instance);
                    } else if (field.getType() == Optional.class) {
                        value = ((Optional<String>) field.get(instance)).orElse(null);
                    }
                    if (value.length() > builder.getMaxLength()) {
                        LOGGER.warn("Value of field '{}' in class {} is longer than the max length of a text input ({}), truncate it !", field.getName(), clazz.getName(), builder.getMaxLength());
                        value = StringUtils.truncate(value, builder.getMaxLength(), null);
                    }
                } catch (IllegalAccessException e) {
                    LOGGER.error("Cannot get value of field {} in class {}", field.getName(), clazz.getName());
                }
                if (value != null) {
                    builder.setValue(value);
                }
                builder.setRequired(!Optional.class.isAssignableFrom(field.getType()));
                comp = builder.build();
            }
            fields.add(new RegisteredModal.ModalField(field, compId, actionRowId, comp));
        }
        List<LayoutComponent> rows = new ArrayList<>();
        rows.add(null);
        int lastId = 0;
        for (RegisteredModal.ModalField field : fields) {
            int rowId = AnnotationUtils.get(field.actionRowId()).orElse(lastId);
            if (rowId != lastId) {
                while (rows.size() <= rowId) {
                    rows.add(null);
                }
                rows.add(rowId, ActionRow.of(field.component()));
            } else {
                if (actionRowComplete((ActionRow) rows.get(rowId), field.component().getType())) {
                    rowId++;
                    while (rows.size() <= rowId) {
                        rows.add(null);
                    }
                }
                if (rows.get(rowId) == null) {
                    rows.set(rowId, ActionRow.of(field.component()));
                } else {
                    rows.set(rowId, addComponent((ActionRow) rows.get(rowId), field.component()));
                }
            }
        }
        RegisteredModal registeredModal = new RegisteredModal(clazz,
                instance,
                new ModalImpl(id, name, rows.stream().filter(Objects::nonNull).toList()),
                fields.toArray(RegisteredModal.ModalField[]::new),
                new CompletableFuture());
        getJDA().getEventManager()
                .getRegisteredListeners()
                .stream()
                .filter(ModalListener.class::isInstance)
                .map(ModalListener.class::cast)
                .findFirst()
                .orElseThrow()
                .registerModal(registeredModal);
        return registeredModal;
    }

    private ActionRow addComponent(ActionRow row, ItemComponent component) {
        List<ItemComponent> components = row.getComponents();
        components.add(component);
        return ActionRow.of(components);
    }

    private boolean actionRowComplete(ActionRow row, Component.Type type) {
        return row != null && row.getComponents().stream().filter(component -> component.getType() == type).count() >= type.getMaxPerRow();
    }

    public class ComponentBuilder {

        private final InteractionListener listener;

        public ComponentBuilder(InteractionListener listener) {
            this.listener = listener;
        }

        public ButtonBuilder button() {
            return new ButtonBuilder(listener);
        }

        public SelectMenuBuilder selectMenu() {
            return new SelectMenuBuilder(listener);
        }


        public class ButtonBuilder {
            private final InteractionListener listener;

            public ButtonBuilder(InteractionListener listener) {
                this.listener = listener;
            }

            @NotNull
            public ButtonComp primary(@NotNull String id, @NotNull String label) {
                Checks.notEmpty(id, "Id");
                Checks.notEmpty(label, "Label");
                Checks.notLonger(id, Button.ID_MAX_LENGTH, "Id");
                Checks.notLonger(label, Button.LABEL_MAX_LENGTH, "Label");
                return new ButtonComp(listener, id, label, ButtonStyle.PRIMARY, false, null);
            }

            @NotNull
            public ButtonComp primary(@NotNull String id, @NotNull Emoji emoji) {
                Checks.notEmpty(id, "Id");
                Checks.notNull(emoji, "Emoji");
                Checks.notLonger(id, Button.ID_MAX_LENGTH, "Id");
                return new ButtonComp(listener, id, "", ButtonStyle.PRIMARY, false, emoji);
            }

            @NotNull
            public ButtonComp secondary(@NotNull String id, @NotNull String label) {
                Checks.notEmpty(id, "Id");
                Checks.notEmpty(label, "Label");
                Checks.notLonger(id, Button.ID_MAX_LENGTH, "Id");
                Checks.notLonger(label, Button.LABEL_MAX_LENGTH, "Label");
                return new ButtonComp(listener, id, label, ButtonStyle.SECONDARY, false, null);
            }

            @NotNull
            public ButtonComp secondary(@NotNull String id, @NotNull Emoji emoji) {
                Checks.notEmpty(id, "Id");
                Checks.notNull(emoji, "Emoji");
                Checks.notLonger(id, Button.ID_MAX_LENGTH, "Id");
                return new ButtonComp(listener, id, "", ButtonStyle.SECONDARY, false, emoji);
            }

            @NotNull
            public ButtonComp success(@NotNull String id, @NotNull String label) {
                Checks.notEmpty(id, "Id");
                Checks.notEmpty(label, "Label");
                Checks.notLonger(id, Button.ID_MAX_LENGTH, "Id");
                Checks.notLonger(label, Button.LABEL_MAX_LENGTH, "Label");
                return new ButtonComp(listener, id, label, ButtonStyle.SUCCESS, false, null);
            }

            @NotNull
            public ButtonComp success(@NotNull String id, @NotNull Emoji emoji) {
                Checks.notEmpty(id, "Id");
                Checks.notNull(emoji, "Emoji");
                Checks.notLonger(id, Button.ID_MAX_LENGTH, "Id");
                return new ButtonComp(listener, id, "", ButtonStyle.SUCCESS, false, emoji);
            }

            @NotNull
            public ButtonComp danger(@NotNull String id, @NotNull String label) {
                Checks.notEmpty(id, "Id");
                Checks.notEmpty(label, "Label");
                Checks.notLonger(id, Button.ID_MAX_LENGTH, "Id");
                Checks.notLonger(label, Button.LABEL_MAX_LENGTH, "Label");
                return new ButtonComp(listener, id, label, ButtonStyle.DANGER, false, null);
            }

            @NotNull
            public ButtonComp danger(@NotNull String id, @NotNull Emoji emoji) {
                Checks.notEmpty(id, "Id");
                Checks.notNull(emoji, "Emoji");
                Checks.notLonger(id, Button.ID_MAX_LENGTH, "Id");
                return new ButtonComp(listener, id, "", ButtonStyle.DANGER, false, emoji);
            }

            @NotNull
            public ButtonComp link(@NotNull String url, @NotNull String label) {
                Checks.notEmpty(url, "URL");
                Checks.notEmpty(label, "Label");
                Checks.notLonger(url, Button.URL_MAX_LENGTH, "URL");
                Checks.notLonger(label, Button.LABEL_MAX_LENGTH, "Label");
                return new ButtonComp(listener, null, label, ButtonStyle.LINK, url, false, null);
            }

            @NotNull
            public ButtonComp link(@NotNull String url, @NotNull Emoji emoji) {
                Checks.notEmpty(url, "URL");
                Checks.notNull(emoji, "Emoji");
                Checks.notLonger(url, Button.URL_MAX_LENGTH, "URL");
                return new ButtonComp(listener, null, "", ButtonStyle.LINK, url, false, emoji);
            }

            @NotNull
            public ButtonComp of(@NotNull ButtonStyle style, @NotNull String idOrUrl, @NotNull String label) {
                Checks.check(style != ButtonStyle.UNKNOWN, "Cannot make button with unknown style!");
                Checks.notNull(style, "Style");
                Checks.notNull(label, "Label");
                Checks.notLonger(label, Button.ID_MAX_LENGTH, "Label");
                if (style == ButtonStyle.LINK)
                    return link(idOrUrl, label);
                Checks.notEmpty(idOrUrl, "Id");
                Checks.notLonger(idOrUrl, Button.ID_MAX_LENGTH, "Id");
                return new ButtonComp(listener, idOrUrl, label, style, false, null);
            }

            @NotNull
            public ButtonComp of(@NotNull ButtonStyle style, @NotNull String idOrUrl, @NotNull Emoji emoji) {
                Checks.check(style != ButtonStyle.UNKNOWN, "Cannot make button with unknown style!");
                Checks.notNull(style, "Style");
                Checks.notNull(emoji, "Emoji");
                if (style == ButtonStyle.LINK)
                    return link(idOrUrl, emoji);
                Checks.notEmpty(idOrUrl, "Id");
                Checks.notLonger(idOrUrl, Button.ID_MAX_LENGTH, "Id");
                return new ButtonComp(listener, idOrUrl, "", style, false, emoji);
            }

            @NotNull
            public ButtonComp of(@NotNull ButtonStyle style, @NotNull String idOrUrl, @Nullable String label, @Nullable Emoji emoji) {
                if (label != null)
                    return of(style, idOrUrl, label).withEmoji(emoji);
                else if (emoji != null)
                    return of(style, idOrUrl, emoji);
                throw new IllegalArgumentException("Cannot build a button without a label and emoji. At least one has to be provided as non-null.");
            }

            public class ButtonComp extends ButtonImpl {

                private final InteractionListener listener;
                private int maxUses = 0;
                private AtomicInteger uses = new AtomicInteger(0);
                private long maxAge = 0L;
                private Predicate<ButtonInteractionEvent> filter = unused -> true;

                public ButtonComp(InteractionListener listener, String id, String label, ButtonStyle style, String url, boolean disabled, Emoji emoji) {
                    super(id, label, style, url, disabled, emoji);
                    this.listener = listener;
                }

                public ButtonComp(InteractionListener listener, String id, String label, ButtonStyle style, boolean disabled, Emoji emoji) {
                    super(id, label, style, disabled, emoji);
                    this.listener = listener;
                }

                public ButtonComp withMaxUses(int maxUses) {
                    Checks.check(maxUses > 0, "Max uses must be greater than 0!");
                    this.maxUses = maxUses;
                    return this;
                }

                public ButtonComp withMaxAge(long maxAge, TimeUnit unit) {
                    Checks.check(maxAge > 0, "Max age must be greater than 0!");
                    this.maxAge = unit.toMillis(maxAge);
                    return this;
                }

                public ButtonComp withFilter(Predicate<ButtonInteractionEvent> filter) {
                    Checks.notNull(filter, "Filter");
                    this.filter = filter;
                    return this;
                }

                public ButtonComp withCallback(Consumer<ButtonInteractionEvent> callback) {
                    Checks.notNull(callback, "Callback");
                    listener.registerCallback(this, event -> {
                        if (filter.test(event)) {
                            if (maxAge > 0 && System.currentTimeMillis() - event.getInteraction().getTimeCreated().toInstant().toEpochMilli() >= maxAge) {
                                return true;
                            }
                            callback.accept(event);
                            return maxUses > 0 && uses.incrementAndGet() >= maxUses;
                        }
                        return false;
                    });
                    return this;
                }

                // region Overrides
                @NotNull
                @Override
                public ButtonComp asDisabled() {
                    return withDisabled(true);
                }

                @NotNull
                @Override
                public ButtonComp asEnabled() {
                    return withDisabled(false);
                }

                @NotNull
                @Override
                public ButtonComp withDisabled(boolean disabled) {
                    return new ButtonComp(listener, getId(), getLabel(), getStyle(), getUrl(), disabled, getEmoji());
                }

                @NotNull
                @Override
                public ButtonComp withEmoji(@Nullable Emoji emoji) {
                    return new ButtonComp(listener, getId(), getLabel(), getStyle(), getUrl(), isDisabled(), emoji);
                }

                @NotNull
                @Override
                public ButtonComp withLabel(@NotNull String label) {
                    Checks.notEmpty(label, "Label");
                    Checks.notLonger(label, LABEL_MAX_LENGTH, "Label");
                    return new ButtonComp(listener, getId(), label, getStyle(), getUrl(), isDisabled(), getEmoji());
                }

                @NotNull
                @Override
                public ButtonComp withId(@NotNull String id) {
                    Checks.notEmpty(id, "ID");
                    Checks.notLonger(id, ID_MAX_LENGTH, "ID");
                    return new ButtonComp(listener, id, getLabel(), getStyle(), null, isDisabled(), getEmoji());
                }

                @NotNull
                @Override
                public ButtonComp withUrl(@NotNull String url) {
                    Checks.notEmpty(url, "URL");
                    Checks.notLonger(url, URL_MAX_LENGTH, "URL");
                    return new ButtonComp(listener, null, getLabel(), ButtonStyle.LINK, url, isDisabled(), getEmoji());
                }

                @NotNull
                @Override
                public ButtonComp withStyle(@NotNull ButtonStyle style) {
                    Checks.notNull(style, "Style");
                    Checks.check(style != ButtonStyle.UNKNOWN, "Cannot make button with unknown style!");
                    if (getStyle() == ButtonStyle.LINK && style != ButtonStyle.LINK)
                        throw new IllegalArgumentException("You cannot change a link button to another style!");
                    if (getStyle() != ButtonStyle.LINK && style == ButtonStyle.LINK)
                        throw new IllegalArgumentException("You cannot change a styled button to a link button!");
                    return new ButtonComp(listener, getId(), getLabel(), style, getUrl(), isDisabled(), getEmoji());
                }
                // endregion
            }
        }

        public class SelectMenuBuilder {
            private final InteractionListener listener;

            public SelectMenuBuilder(InteractionListener listener) {
                this.listener = listener;
            }

            public StringSelectMenuBuilder stringSelectMenu(String id) {
                return new StringSelectMenuBuilder(listener, id);
            }

            public EntitySelectMenuBuilder entitySelectMenu(String id, EntitySelectMenu.SelectTarget target, @Nullable ChannelType... channelTypes) {
                return new EntitySelectMenuBuilder(listener, id, target, channelTypes);
            }

            public EntitySelectMenuBuilder entitySelectMenu(String id, EntitySelectMenu.SelectTarget target) {
                return entitySelectMenu(id, target, null);
            }

            public EntitySelectMenuBuilder roleSelectMenu(String id) {
                return entitySelectMenu(id, EntitySelectMenu.SelectTarget.ROLE);
            }

            public EntitySelectMenuBuilder userSelectMenu(String id) {
                return entitySelectMenu(id, EntitySelectMenu.SelectTarget.USER);
            }

            public EntitySelectMenuBuilder channelSelectMenu(String id, @Nullable ChannelType... channelTypes) {
                return entitySelectMenu(id, EntitySelectMenu.SelectTarget.CHANNEL, channelTypes);
            }

            public class StringSelectMenuBuilder extends SelectMenu.Builder<StringSelectMenu, StringSelectMenuBuilder> {
                private final List<SelectOption> options = new ArrayList<>();
                private final InteractionListener listener;
                private int maxUses = 0;
                private AtomicInteger uses = new AtomicInteger(0);
                private long maxAge = 0L;
                private Predicate<StringSelectInteractionEvent> filter = unused -> true;
                private Consumer<StringSelectInteractionEvent> callback = unused -> {
                };

                protected StringSelectMenuBuilder(InteractionListener listener, String customId) {
                    super(customId);
                    this.listener = listener;
                }

                @NotNull
                public StringSelectMenuBuilder addOptions(@NotNull SelectOption... options) {
                    Checks.noneNull(options, "Options");
                    Checks.check(this.options.size() + options.length <= SelectMenu.OPTIONS_MAX_AMOUNT, "Cannot have more than %d options for a select menu!", SelectMenu.OPTIONS_MAX_AMOUNT);
                    Collections.addAll(this.options, options);
                    return this;
                }

                @NotNull
                public StringSelectMenuBuilder addOptions(@NotNull Collection<? extends SelectOption> options) {
                    Checks.noneNull(options, "Options");
                    Checks.check(this.options.size() + options.size() <= SelectMenu.OPTIONS_MAX_AMOUNT, "Cannot have more than %d options for a select menu!", SelectMenu.OPTIONS_MAX_AMOUNT);
                    this.options.addAll(options);
                    return this;
                }

                @NotNull
                public StringSelectMenuBuilder addOption(@NotNull String label, @NotNull String value) {
                    return addOptions(SelectOption.of(label, value));
                }

                @NotNull
                public StringSelectMenuBuilder addOption(@NotNull String label, @NotNull String value, @NotNull Emoji emoji) {
                    return addOption(label, value, null, emoji);
                }

                @NotNull
                public StringSelectMenuBuilder addOption(@NotNull String label, @NotNull String value, @NotNull String description) {
                    return addOption(label, value, description, null);
                }

                @NotNull
                public StringSelectMenuBuilder addOption(@NotNull String label, @NotNull String value, @Nullable String description, @Nullable Emoji emoji) {
                    return addOptions(SelectOption.of(label, value).withDescription(description).withDefault(false).withEmoji(emoji));
                }

                @NotNull
                public List<SelectOption> getOptions() {
                    return options;
                }

                @NotNull
                public StringSelectMenuBuilder setDefaultValues(@NotNull Collection<String> values) {
                    Checks.noneNull(values, "Values");
                    Set<String> set = new HashSet<>(values);
                    for (ListIterator<SelectOption> it = getOptions().listIterator(); it.hasNext(); ) {
                        SelectOption option = it.next();
                        it.set(option.withDefault(set.contains(option.getValue())));
                    }
                    return this;
                }

                @NotNull
                public StringSelectMenuBuilder setDefaultValues(@NotNull String... values) {
                    Checks.noneNull(values, "Values");
                    return setDefaultValues(Arrays.asList(values));
                }

                @NotNull
                public StringSelectMenuBuilder setDefaultOptions(@NotNull Collection<? extends SelectOption> values) {
                    Checks.noneNull(values, "Values");
                    return setDefaultValues(values.stream().map(SelectOption::getValue).collect(Collectors.toSet()));
                }

                @NotNull
                public StringSelectMenuBuilder setDefaultOptions(@NotNull SelectOption... values) {
                    Checks.noneNull(values, "Values");
                    return setDefaultOptions(Arrays.asList(values));
                }

                public StringSelectMenuBuilder withMaxUses(int maxUses) {
                    Checks.check(maxUses > 0, "Max uses must be greater than 0!");
                    this.maxUses = maxUses;
                    return this;
                }

                public StringSelectMenuBuilder withMaxAge(long maxAge, TimeUnit unit) {
                    Checks.check(maxAge > 0, "Max age must be greater than 0!");
                    this.maxAge = unit.toMillis(maxAge);
                    return this;
                }

                public StringSelectMenuBuilder withFilter(Predicate<StringSelectInteractionEvent> filter) {
                    Checks.notNull(filter, "Filter");
                    this.filter = filter;
                    return this;
                }

                public StringSelectMenuBuilder withCallback(Consumer<StringSelectInteractionEvent> callback) {
                    Checks.notNull(callback, "Callback");
                    this.callback = callback;
                    return this;
                }

                @Override
                public StringSelectMenu build() {
                    Checks.check(minValues <= maxValues, "Min values cannot be greater than max values!");
                    Checks.check(!options.isEmpty(), "Cannot build a select menu without options. Add at least one option!");
                    Checks.check(options.size() <= SelectMenu.OPTIONS_MAX_AMOUNT, "Cannot build a select menu with more than %d options.", SelectMenu.OPTIONS_MAX_AMOUNT);
                    int min = Math.min(minValues, options.size());
                    int max = Math.min(maxValues, options.size());
                    StringSelectMenuImpl menu = new StringSelectMenuImpl(customId, placeholder, min, max, disabled, options);
                    listener.registerCallback(menu, event -> {
                        if (filter.test(event)) {
                            if (maxAge > 0 && System.currentTimeMillis() - event.getInteraction().getTimeCreated().toInstant().toEpochMilli() >= maxAge) {
                                return true;
                            }
                            callback.accept(event);
                            return maxUses > 0 && uses.incrementAndGet() >= maxUses;
                        }
                        return false;
                    });
                    return menu;
                }
            }

            public class EntitySelectMenuBuilder extends SelectMenu.Builder<EntitySelectMenu, EntitySelectMenuBuilder> {
                private final InteractionListener listener;
                private Component.Type componentType;
                private EnumSet<ChannelType> channelTypes = EnumSet.noneOf(ChannelType.class);
                private int maxUses = 0;
                private AtomicInteger uses = new AtomicInteger(0);
                private long maxAge = 0L;
                private Predicate<EntitySelectInteractionEvent > filter = unused -> true;
                private Consumer<EntitySelectInteractionEvent > callback = unused -> {
                };

                protected EntitySelectMenuBuilder(InteractionListener listener, String customId, EntitySelectMenu.SelectTarget target, @Nullable ChannelType... channelTypes) {
                    super(customId);
                    this.listener = listener;
                    this.componentType = switch (target) {
                        case ROLE -> Component.Type.ROLE_SELECT;
                        case USER -> Component.Type.USER_SELECT;
                        case CHANNEL -> Component.Type.CHANNEL_SELECT;
                    };
                    if (channelTypes != null) {
                        this.channelTypes.addAll(Arrays.asList(channelTypes));
                    }
                }

                @NotNull
                public EntitySelectMenuBuilder setEntityTypes(@NotNull Collection<EntitySelectMenu.SelectTarget> types) {
                    Checks.notEmpty(types, "Types");
                    Checks.noneNull(types, "Types");

                    EnumSet<EntitySelectMenu.SelectTarget> set = Helpers.copyEnumSet(EntitySelectMenu.SelectTarget.class, types);
                    if (set.size() == 1) {
                        if (set.contains(EntitySelectMenu.SelectTarget.CHANNEL))
                            this.componentType = Component.Type.CHANNEL_SELECT;
                        else if (set.contains(EntitySelectMenu.SelectTarget.ROLE))
                            this.componentType = Component.Type.ROLE_SELECT;
                        else if (set.contains(EntitySelectMenu.SelectTarget.USER))
                            this.componentType = Component.Type.USER_SELECT;
                    } else if (set.size() == 2) {
                        if (set.contains(EntitySelectMenu.SelectTarget.USER) && set.contains(EntitySelectMenu.SelectTarget.ROLE))
                            this.componentType = Component.Type.MENTIONABLE_SELECT;
                        else
                            throw new IllegalArgumentException("The provided combination of select targets is not supported. Provided: " + set);
                    } else {
                        throw new IllegalArgumentException("The provided combination of select targets is not supported. Provided: " + set);
                    }

                    return this;
                }

                @NotNull
                public EntitySelectMenuBuilder setEntityTypes(@NotNull EntitySelectMenu.SelectTarget type, @NotNull EntitySelectMenu.SelectTarget... types) {
                    Checks.notNull(type, "Type");
                    Checks.noneNull(types, "Types");
                    return setEntityTypes(EnumSet.of(type, types));
                }

                @NotNull
                public EntitySelectMenuBuilder setChannelTypes(@NotNull Collection<ChannelType> types) {
                    Checks.noneNull(types, "Types");
                    for (ChannelType type : types)
                        Checks.check(type.isGuild(), "Only guild channel types are allowed! Provided: %s", type);
                    this.channelTypes = Helpers.copyEnumSet(ChannelType.class, types);
                    return this;
                }

                @NotNull
                public EntitySelectMenuBuilder setChannelTypes(@NotNull ChannelType... types) {
                    return setChannelTypes(Arrays.asList(types));
                }

                public EntitySelectMenuBuilder withMaxUses(int maxUses) {
                    Checks.check(maxUses > 0, "Max uses must be greater than 0!");
                    this.maxUses = maxUses;
                    return this;
                }

                public EntitySelectMenuBuilder withMaxAge(long maxAge, TimeUnit unit) {
                    Checks.check(maxAge > 0, "Max age must be greater than 0!");
                    this.maxAge = unit.toMillis(maxAge);
                    return this;
                }

                public EntitySelectMenuBuilder withFilter(Predicate<EntitySelectInteractionEvent > filter) {
                    Checks.notNull(filter, "Filter");
                    this.filter = filter;
                    return this;
                }

                public EntitySelectMenuBuilder withCallback(Consumer<EntitySelectInteractionEvent> callback) {
                    Checks.notNull(callback, "Callback");
                    this.callback = callback;
                    return this;
                }

                @NotNull
                @Override
                public EntitySelectMenu build() {
                    Checks.check(minValues <= maxValues, "Min values cannot be greater than max values!");
                    EnumSet<ChannelType> channelTypes = componentType == Component.Type.CHANNEL_SELECT ? this.channelTypes : EnumSet.noneOf(ChannelType.class);
                    EntitySelectMenuImpl menu = new EntitySelectMenuImpl(customId, placeholder, minValues, maxValues, disabled, componentType, channelTypes);
                    listener.registerCallback(menu, event -> {
                        if (filter.test(event)) {
                            if (maxAge > 0 && System.currentTimeMillis() - event.getInteraction().getTimeCreated().toInstant().toEpochMilli() >= maxAge) {
                                return true;
                            }
                            callback.accept(event);
                            return maxUses > 0 && uses.incrementAndGet() >= maxUses;
                        }
                        return false;
                    });
                    return menu;
                }
            }
        }
    }
}
