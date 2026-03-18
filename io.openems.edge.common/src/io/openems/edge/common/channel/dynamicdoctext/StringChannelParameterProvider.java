package io.openems.edge.common.channel.dynamicdoctext;

import io.openems.edge.common.type.TextProvider;

public interface StringChannelParameterProvider extends ParameterProvider {
	/**
	 * Maps the specific string value to a text.
	 *
	 * @param value String value that must match exactly
	 * @param text  Text to display
	 * @return myself
	 */
	public StringChannelParameterProvider when(String value, TextProvider text);

	/**
	 * Maps specific string values to a text.
	 *
	 * @param values String values that must match exactly
	 * @param text   Text to display
	 * @return myself
	 */
	public StringChannelParameterProvider when(String[] values, TextProvider text);

	/**
	 * Maps the string value to a text, if the string value is containing the
	 * specified value.
	 *
	 * @param value String that must be included in the value to match =>
	 *              channelValue.contains(value)
	 * @param text  Text to display
	 * @return myself
	 */
	public StringChannelParameterProvider whenStringContains(String value, TextProvider text);

	/**
	 * Text that should be used if no when() matches. If not defined, the enum name
	 * is displayed.
	 *
	 * @param text Text to display
	 * @return myself
	 */
	public StringChannelParameterProvider defaultText(TextProvider text);
}