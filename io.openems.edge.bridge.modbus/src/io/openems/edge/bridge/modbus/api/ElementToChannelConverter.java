package io.openems.edge.bridge.modbus.api;

import java.util.function.Function;

import io.openems.common.types.OpenemsType;

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
			value -> {
				if (value == null) {
					return null;
				}
				for (OpenemsType openemsType : OpenemsType.values()) {
					// this 'for' + 'switch' is only utilized to get an alert by Eclipse IDE if a
					// new OpenemsType was added. ("The enum constant XX needs a corresponding case
					// label in this enum switch on OpenemsType")
					switch (openemsType) {
					case BOOLEAN:
					case SHORT:
					case INTEGER:
					case LONG:
					case FLOAT:
						if (value instanceof Boolean) {
							return value; // impossible
						} else if (value instanceof Short) {
							short shortValue = (Short) value;
							if (shortValue >= 0) {
								return shortValue;
							} else {
								return 0;
							}
						} else if (value instanceof Integer) {
							int intValue = (Integer) value;
							if (intValue >= 0) {
								return intValue;
							} else {
								return 0;
							}
						} else if (value instanceof Long) {
							long longValue = (Long) value;
							if (longValue >= 0) {
								return longValue;
							} else {
								return 0;
							}
						} else if (value instanceof Float) {
							float floatValue = (Float) value;
							if (floatValue >= 0) {
								return floatValue;
							} else {
								return 0;
							}
						}
					}
					break;
				}
				throw new IllegalArgumentException(
						"Converter KEEP_POSITIVE does not accept the type of [" + value + "]");
			}, //
				// channel -> element
			value -> value);

	private final static Function<Object, Object> INVERT_FCT = (value) -> {
		if (value == null) {
			return null;
		}
		for (OpenemsType openemsType : OpenemsType.values()) {
			// this 'for' + 'switch' is only utilized to get an alert by Eclipse IDE if a
			// new OpenemsType was added. ("The enum constant XX needs a corresponding case
			// label in this enum switch on OpenemsType")
			switch (openemsType) {
			case BOOLEAN:
			case SHORT:
			case INTEGER:
			case LONG:
			case FLOAT:
				if (value instanceof Boolean) {
					return Boolean.valueOf(!(boolean) value);
				} else if (value instanceof Short) {
					return Short.valueOf((short) ((short) value * -1));
				} else if (value instanceof Integer) {
					return Integer.valueOf((int) value * -1);
				} else if (value instanceof Long) {
					return Long.valueOf((long) value * -1);
				} else if (value instanceof Float) {
					return Float.valueOf((float) value * -1);
				}
			}
			break;
		}
		throw new IllegalArgumentException("Converter INVERT does not accept the type of [" + value + "]");
	};

	/**
	 * Inverts the value from Element to Channel
	 */
	public final static ElementToChannelConverter INVERT = new ElementToChannelConverter( //
			// element -> channel
			INVERT_FCT, //
			// channel -> element
			INVERT_FCT);

	/**
	 * Converts only negative values from Element to Channel and inverts them (makes
	 * the value positive)
	 */
	public final static ElementToChannelConverter KEEP_NEGATIVE_AND_INVERT = new ElementToChannelConverterChain(INVERT,
			KEEP_POSITIVE);

	/**
	 * Applies SCALE_FACTOR_1 and CONVERT_POSITIVE
	 */
	public final static ElementToChannelConverter SCALE_FACTOR_1_AND_CONVERT_POSITIVE = new ElementToChannelConverterChain(
			SCALE_FACTOR_1, KEEP_POSITIVE);

	/**
	 * Applies SCALE_FACTOR_1 and CONVERT_NEGATIVE_AND_INVERT
	 */
	public final static ElementToChannelConverter SCALE_FACTOR_1_AND_CONVERT_NEGATIVE_INVERT = new ElementToChannelConverterChain(
			SCALE_FACTOR_1, KEEP_NEGATIVE_AND_INVERT);

	/**
	 * Applies SCALE_FACTOR_2 and CONVERT_POSITIVE
	 */
	public final static ElementToChannelConverter SCALE_FACTOR_2_AND_CONVERT_POSITIVE = new ElementToChannelConverterChain(
			SCALE_FACTOR_2, KEEP_POSITIVE);

	/**
	 * Applies SCALE_FACTOR_2 and CONVERT_NEGATIVE_AND_INVERT
	 */
	public final static ElementToChannelConverter SCALE_FACTOR_2_AND_CONVERT_NEGATIVE_INVERT = new ElementToChannelConverterChain(
			SCALE_FACTOR_2, KEEP_NEGATIVE_AND_INVERT);

	private final Function<Object, Object> elementToChannel;
	private final Function<Object, Object> channelToElement;

	public ElementToChannelConverter(Function<Object, Object> elementToChannel,
			Function<Object, Object> channelToElement) {
		this.elementToChannel = elementToChannel;
		this.channelToElement = channelToElement;
	}

	public ElementToChannelConverter(Function<Object, Object> elementToChannel,
			Function<Object, Object> channelToElement, ElementToChannelConverter nextConverter) {
		this.elementToChannel = elementToChannel.andThen(nextConverter.elementToChannel);
		this.channelToElement = channelToElement.andThen(nextConverter.channelToElement);
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
