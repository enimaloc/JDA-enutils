package fr.enimaloc.enutils.jda.register.annotation;

import fr.enimaloc.enutils.jda.utils.AnnotationUtils;
import net.dv8tion.jda.api.interactions.components.text.TextInputStyle;

import java.lang.annotation.*;

import static fr.enimaloc.enutils.jda.utils.AnnotationUtils.*;

@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.TYPE)
public @interface Modal {
    String title() default STR_DEFAULT;

    @Retention(RetentionPolicy.RUNTIME)
    @Target(ElementType.FIELD)
    @interface ActionRow {
        int id() default INT_DEFAULT;
    }

    @Retention(RetentionPolicy.RUNTIME)
    @Target(ElementType.FIELD)
    @interface Component {
        String id() default STR_DEFAULT;
        String label() default STR_DEFAULT;
    }

    @Retention(RetentionPolicy.RUNTIME)
    @Target(ElementType.FIELD)
    @interface TextInput {
        TextInput DEFAULT_TEXT_INPUT = new Modal.TextInput() {
            @Override
            public Class<? extends Annotation> annotationType() {
                return Modal.TextInput.class;
            }

            @Override
            public String placeholder() {
                return AnnotationUtils.STR_DEFAULT;
            }

            @Override
            public TextInputStyle type() {
                return TextInputStyle.PARAGRAPH;
            }
        };

        String placeholder() default STR_DEFAULT;
        TextInputStyle type() default TextInputStyle.PARAGRAPH;
    }
}
