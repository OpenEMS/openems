package io.openems.edge.common.channel.dynamicdoctext;

import io.openems.common.types.OptionsEnum;
import io.openems.edge.common.type.TextProvider;

public interface EnumChannelParameterProvider<V extends Enum<V> & OptionsEnum> extends ParameterProvider {
	/**
	 * Maps specific enum value to a text.
	 * 
	 * @param value One enum value
	 * @param text  Text to display for the specific value
	 * @return myself
	 */
	public EnumChannelParameterProvider<V> when(V value, TextProvider text);

	/**
	 * Maps specific enum values to a text.
	 * 
	 * @param values Multiple enum values as array
	 * @param text   Text to display for the specific values
	 * @return myself
	 */
	public EnumChannelParameterProvider<V> when(V[] values, TextProvider text);

	/**
	 * Text that should be used if no when() matches. If not defined, the enum name
	 * is displayed.
	 * 
	 * @param text Text to display
	 * @return myself
	 */
	public EnumChannelParameterProvider<V> defaultText(TextProvider text);
}
