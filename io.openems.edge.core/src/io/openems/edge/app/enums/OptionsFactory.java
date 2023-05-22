package io.openems.edge.app.enums;

import java.util.Arrays;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.stream.Collectors;

import io.openems.common.session.Language;

/**
 * Options-Factory for setting options with their translations.
 * 
 * <pre>
 * Usage:
 * public static OptionsFactory optionsFactory() {
 * 	return OptionsFactory.of(values());
 * }
 * or:
 * OptionsFactory.of(Enum.class)
 * </pre>
 *
 */
public interface OptionsFactory {

	/**
	 * Creates a {@link OptionsFactory} of the given {@link TranslatableEnum}
	 * values.
	 * 
	 * @param values the values to create a {@link OptionsFactory} from
	 * @return the {@link OptionsFactory}
	 */
	public static OptionsFactory of(TranslatableEnum[] values) {
		return l -> Arrays.stream(values) //
				.map(e -> Map.entry(e.getTranslation(l), e.name())) //
				.collect(Collectors.toSet());
	}

	/**
	 * Creates a {@link OptionsFactory} of the given {@link TranslatableEnum} class.
	 * 
	 * @param <T>       the type of the enum {@link Class}
	 * @param enumClass the {@link Class EnumClass} to get the values from.
	 * @return the {@link OptionsFactory}
	 */
	public static <T extends Enum<T> & TranslatableEnum> OptionsFactory of(Class<T> enumClass) {
		return of(enumClass.getEnumConstants());
	}

	/**
	 * Gets the options of the current instance.
	 * 
	 * @param l the language of the options
	 * @return the options where the key is the label and the value the value
	 */
	public Set<Entry<String, String>> options(Language l);
}