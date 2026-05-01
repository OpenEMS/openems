package io.openems.edge.common.channel.dynamicdoctext;

import io.openems.edge.common.type.TextProvider;

public interface NumberChannelParameterProvider<V extends Number> extends ParameterProvider {
	/**
	 * Maps numbers that are between the specified min and max ranges (min <= value
	 * <= max) to a text.
	 *
	 * @param min  The minimum value the number must have (inclusive, min <= value)
	 * @param max  The maximum value the number must have (inclusive, max >= value)
	 * @param text Text to display for the specific value
	 * @return myself
	 */
	public NumberChannelParameterProvider<V> whenIsInRange(V min, V max, TextProvider text);

	/**
	 * Maps numbers that are greater than the specified min number (min <= value) to
	 * a text.
	 *
	 * @param min  The minimum value the number must have (inclusive, min <= value)
	 * @param text Text to display for the specific value
	 * @return myself
	 */
	public NumberChannelParameterProvider<V> whenIsAtLeast(V min, TextProvider text);

	/**
	 * Maps numbers that are lower than the specified max number (value <= max) to a
	 * text.
	 *
	 * @param max  The maximum value the number must have (inclusive, max >= value)
	 * @param text Text to display for the specific value
	 * @return myself
	 */
	public NumberChannelParameterProvider<V> whenIsAtMost(V max, TextProvider text);

	/**
	 * Maps negative numbers to a specific text.
	 *
	 * @param text Text to display for the specific value
	 * @return myself
	 */
	public NumberChannelParameterProvider<V> whenIsNegative(TextProvider text);

	/**
	 * Maps positive numbers to a specific text.
	 *
	 * @param text Text to display for the specific value
	 * @return myself
	 */
	public NumberChannelParameterProvider<V> whenIsPositive(TextProvider text);

	/**
	 * Text that should be used if no when() matches. If not defined, the enum name
	 * is displayed.
	 *
	 * @param text Text to display
	 * @return myself
	 */
	public NumberChannelParameterProvider<V> defaultText(TextProvider text);
}