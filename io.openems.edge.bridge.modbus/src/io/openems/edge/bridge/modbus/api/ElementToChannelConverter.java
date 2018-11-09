package io.openems.edge.bridge.modbus.api;

import java.util.function.Function;

import io.openems.edge.common.converter.StaticConverters;

/**
 * Provides Functions to convert from Element to Channel and back. Also has some
 * static convenience functions to facilitate conversion.
 */
public class ElementToChannelConverter {

	/**
	 * Converts directly 1-to-1 between Element and Channel
	 */
	public final static ElementToChannelConverter DIRECT_1_TO_1 = new ElementToChannelConverter( //
			// element -> channel
			value -> value, //
			// channel -> element
			value -> value);

	/**
	 * Applies a scale factor of -1.
	 * 
	 * @see ElementToChannelScaleFactorConverter
	 */
	public final static ElementToChannelConverter SCALE_FACTOR_MINUS_1 = new ElementToChannelScaleFactorConverter(-1);
	
	/**
	 * Applies a scale factor of -2.
	 * 
	 * @see ElementToChannelScaleFactorConverter
	 */
	public final static ElementToChannelConverter SCALE_FACTOR_MINUS_2 = new ElementToChannelScaleFactorConverter(-2);

	/**
	 * Applies a scale factor of 1.
	 * 
	 * @see ElementToChannelScaleFactorConverter
	 */
	public final static ElementToChannelConverter SCALE_FACTOR_1 = new ElementToChannelScaleFactorConverter(1);

	/**
	 * Applies a scale factor of 2.
	 * 
	 * @see ElementToChannelScaleFactorConverter
	 */
	public final static ElementToChannelConverter SCALE_FACTOR_2 = new ElementToChannelScaleFactorConverter(2);

	/**
	 * Applies a scale factor of 3.
	 * 
	 * @see ElementToChannelScaleFactorConverter
	 */
	public final static ElementToChannelConverter SCALE_FACTOR_3 = new ElementToChannelScaleFactorConverter(3);

	/**
	 * Converts only positive values from Element to Channel
	 */
	public final static ElementToChannelConverter KEEP_POSITIVE = new ElementToChannelConverter( //
			// element -> channel
			value -> StaticConverters.KEEP_POSITIVE, //
			// channel -> element
			value -> value);

	/**
	 * Inverts the value from Element to Channel
	 */
	public final static ElementToChannelConverter INVERT = new ElementToChannelConverter( //
			// element -> channel
			StaticConverters.INVERT, //
			// channel -> element
			StaticConverters.INVERT);

	/**
	 * Depending on the given parameter:
	 * <ul>
	 * <li>true: invert value
	 * <li>false: keep value (1-to-1)
	 * </ul>
	 * 
	 * @param invert
	 * @return
	 */
	public static ElementToChannelConverter INVERT_IF_TRUE(boolean invert) {
		if (invert) {
			return INVERT;
		} else {
			return DIRECT_1_TO_1;
		}
	}

	/**
	 * Converts only negative values from Element to Channel and inverts them (makes
	 * the value positive)
	 */
	public final static ElementToChannelConverter KEEP_NEGATIVE_AND_INVERT = new ElementToChannelConverterChain(INVERT,
			KEEP_POSITIVE);

	/**
	 * Applies {@link ElementToChannelConverter#SCALE_FACTOR_1} and CONVERT_POSITIVE
	 */
	public final static ElementToChannelConverter SCALE_FACTOR_1_AND_KEEP_POSITIVE = new ElementToChannelConverterChain(
			SCALE_FACTOR_1, KEEP_POSITIVE);

	/**
	 * Applies {@link ElementToChannelConverter#SCALE_FACTOR_2} and INVERT
	 */
	public final static ElementToChannelConverter SCALE_FACTOR_2_AND_INVERT = new ElementToChannelConverterChain(
			SCALE_FACTOR_2, INVERT);

	/**
	 * Applies {@link ElementToChannelConverter#SCALE_FACTOR_1} and
	 * CONVERT_NEGATIVE_AND_INVERT
	 */
	public final static ElementToChannelConverter SCALE_FACTOR_1_AND_KEEP_NEGATIVE_AND_INVERT = new ElementToChannelConverterChain(
			SCALE_FACTOR_1, KEEP_NEGATIVE_AND_INVERT);

	/**
	 * Applies {@link ElementToChannelConverter#SCALE_FACTOR_2} and CONVERT_POSITIVE
	 */
	public final static ElementToChannelConverter SCALE_FACTOR_2_AND_KEEP_POSITIVE = new ElementToChannelConverterChain(
			SCALE_FACTOR_2, KEEP_POSITIVE);

	/**
	 * Applies {@link ElementToChannelConverter#SCALE_FACTOR_2} and @see
	 * {@link ElementToChannelConverter#KEEP_NEGATIVE_AND_INVERT}
	 */
	public final static ElementToChannelConverter SCALE_FACTOR_2_AND_KEEP_NEGATIVE_AND_INVERT = new ElementToChannelConverterChain(
			SCALE_FACTOR_2, KEEP_NEGATIVE_AND_INVERT);

	/**
	 * Applies {@link ElementToChannelConverter#SCALE_FACTOR_2_AND_KEEP_NEGATIVE}
	 * and @see {@link ElementToChannelConverter#INVERT}
	 */
	public final static ElementToChannelConverter SCALE_FACTOR_2_AND_KEEP_NEGATIVE = new ElementToChannelConverterChain(
			SCALE_FACTOR_2_AND_KEEP_NEGATIVE_AND_INVERT, INVERT);

	private final Function<Object, Object> elementToChannel;
	private final Function<Object, Object> channelToElement;

	/**
	 * This constructs and back-and-forth converter from Element to Channel and back
	 * 
	 * @param elementToChannel
	 * @param channelToElement
	 */
	public ElementToChannelConverter(Function<Object, Object> elementToChannel,
			Function<Object, Object> channelToElement) {
		this.elementToChannel = elementToChannel;
		this.channelToElement = channelToElement;
	}

	/**
	 * This constructs a forward-only converter from Element to Channel.
	 * Back-conversion throws an Exception.
	 * 
	 * @param elementToChannel
	 */
	public ElementToChannelConverter(Function<Object, Object> elementToChannel) {
		this.elementToChannel = elementToChannel;
		this.channelToElement = (value) -> {
			throw new IllegalArgumentException("Backwards-Conversion for [" + value + "] is not implemented.");
		};
	}

	/**
	 * Convert an Element value to a Channel value. If the value can or should not
	 * be converted, this method returns null.
	 * 
	 * @param value
	 * @return the converted value or null
	 */
	public Object elementToChannel(Object value) {
		return this.elementToChannel.apply(value);
	}

	public Object channelToElement(Object value) {
		return this.channelToElement.apply(value);
	}
}
