package io.openems.edge.core.appmanager.formly;

import com.google.gson.JsonObject;

import io.openems.edge.core.appmanager.Nameable;
import io.openems.edge.core.appmanager.formly.builder.CheckboxBuilder;
import io.openems.edge.core.appmanager.formly.builder.FieldGroupBuilder;
import io.openems.edge.core.appmanager.formly.builder.InputBuilder;
import io.openems.edge.core.appmanager.formly.builder.RangeBuilder;
import io.openems.edge.core.appmanager.formly.builder.RepeatBuilder;
import io.openems.edge.core.appmanager.formly.builder.SelectBuilder;
import io.openems.edge.core.appmanager.formly.builder.TextBuilder;

/**
 * Source https://formly.dev/examples/introduction.
 */
public class JsonFormlyUtil {

	private JsonFormlyUtil() {
	}

	/**
	 * Creates a JsonObject Formly Checkbox Builder for the given enum.
	 *
	 * @param <T>      the type of the enum
	 * @param property the enum property
	 * @return a {@link CheckboxBuilder}
	 */
	public static <T extends Enum<T>> CheckboxBuilder buildCheckbox(T property) {
		return new CheckboxBuilder(toNameable(property));
	}

	/**
	 * Creates a JsonObject Formly Checkbox Builder for the given enum.
	 *
	 * @param nameable the {@link Nameable} property
	 * @return a {@link CheckboxBuilder}
	 */
	public static CheckboxBuilder buildCheckboxFromNameable(Nameable nameable) {
		return new CheckboxBuilder(nameable);
	}

	/**
	 * Creates a JsonObject Formly Input Builder for the given enum.
	 *
	 * @param <T>      the type of the enum
	 * @param property the enum property
	 * @return a {@link InputBuilder}
	 */
	public static <T extends Enum<T>> InputBuilder buildInput(T property) {
		return new InputBuilder(toNameable(property));
	}

	/**
	 * Creates a JsonObject Formly Input Builder for the given enum.
	 *
	 * @param nameable the {@link Nameable} property
	 * @return a {@link InputBuilder}
	 */
	public static InputBuilder buildInputFromNameable(Nameable nameable) {
		return new InputBuilder(nameable);
	}

	/**
	 * Creates a JsonObject Formly Input Builder for the given enum.
	 *
	 * @param <T>      the type of the enum
	 * @param property the enum property
	 * @return a {@link InputBuilder}
	 */
	public static <T extends Enum<T>> FieldGroupBuilder buildFieldGroup(T property) {
		return new FieldGroupBuilder(toNameable(property));
	}

	/**
	 * Creates a JsonObject Formly Input Builder for the given enum.
	 *
	 * @param nameable the {@link Nameable} property
	 * @return a {@link InputBuilder}
	 */
	public static FieldGroupBuilder buildFieldGroupFromNameable(Nameable nameable) {
		return new FieldGroupBuilder(nameable);
	}

	/**
	 * Creates a JsonObject Formly Select Builder for the given enum.
	 *
	 * @param <T>      the type of the enum
	 * @param property the enum property
	 * @return a {@link SelectBuilder}
	 */
	public static <T extends Enum<T>> SelectBuilder buildSelect(T property) {
		return new SelectBuilder(toNameable(property));
	}

	/**
	 * Creates a JsonObject Formly Select Builder for the given enum.
	 *
	 * @param nameable the {@link Nameable} property
	 * @return a {@link SelectBuilder}
	 */
	public static SelectBuilder buildSelectFromNameable(Nameable nameable) {
		return new SelectBuilder(nameable);
	}

	/**
	 * Creates a JsonObject Formly Range Builder for the given enum.
	 *
	 * @param <T>      the type of the enum
	 * @param property the enum property
	 * @return a {@link RangeBuilder}
	 */
	public static <T extends Enum<T>> RangeBuilder buildRange(T property) {
		return new RangeBuilder(toNameable(property));
	}

	/**
	 * Creates a JsonObject Formly Range Builder for the given enum.
	 *
	 * @param nameable the {@link Nameable} property
	 * @return a {@link RangeBuilder}
	 */
	public static RangeBuilder buildRangeFromNameable(Nameable nameable) {
		return new RangeBuilder(nameable);
	}

	/**
	 * Creates a JsonObject Formly Repeat Builder for the given enum.
	 *
	 * @param <T>      the type of the enum
	 * @param property the enum property
	 * @return a {@link RepeatBuilder}
	 */
	public static <T extends Enum<T>> RepeatBuilder buildRepeat(T property) {
		return new RepeatBuilder(toNameable(property));
	}

	/**
	 * Creates a JsonObject Formly Repeat Builder for the given enum.
	 *
	 * @param nameable the {@link Nameable} property
	 * @return a {@link RepeatBuilder}
	 */
	public static RepeatBuilder buildRepeatFromNameable(Nameable nameable) {
		return new RepeatBuilder(nameable);
	}

	/**
	 * Creates a JsonObject Formly Text Builder for the given enum.
	 *
	 * @return a {@link TextBuilder}
	 */
	public static TextBuilder buildText() {
		return new TextBuilder();
	}

	private static <T extends Enum<T>> Nameable toNameable(T property) {
		return Nameable.of(property.name());
	}

	/**
	 * Creates a new {@link JsonObject} or returns the given {@link JsonObject} if
	 * it is not null.
	 * 
	 * @param o the existing {@link JsonObject}; can be null
	 * @return the existing or created {@link JsonObject}; never null
	 */
	public static final JsonObject single(JsonObject o) {
		if (o != null) {
			return o;
		}
		return new JsonObject();
	}

}
