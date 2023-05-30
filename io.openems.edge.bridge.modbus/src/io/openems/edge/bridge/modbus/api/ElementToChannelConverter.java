package io.openems.edge.bridge.modbus.api;

import java.util.function.Function;

import io.openems.edge.common.converter.StaticConverters;

/**
 * Provides Functions to convert from Element to Channel and back. Also has some
 * static convenience functions to facilitate conversion.
 */
public class ElementToChannelConverter {

	/**
	 * Converts directly 1-to-1 between Element and Channel.
	 */
	public static final ElementToChannelConverter DIRECT_1_TO_1 = new ElementToChannelConverter(//
			// element -> channel
			value -> value, //
			// channel -> element
			value -> value);

	/**
	 * Applies a scale factor of -1. Converts value [1] to [0.1].
	 *
	 * @see ElementToChannelScaleFactorConverter
	 */
	public static final ElementToChannelConverter SCALE_FACTOR_MINUS_1 = new ElementToChannelScaleFactorConverter(-1);

	/**
	 * Applies a scale factor of -2. Converts value [1] to [0.01].
	 *
	 * @see ElementToChannelScaleFactorConverter
	 */
	public static final ElementToChannelConverter SCALE_FACTOR_MINUS_2 = new ElementToChannelScaleFactorConverter(-2);

	/**
	 * Applies a scale factor of -3. Converts value [1] to [0.001].
	 *
	 * @see ElementToChannelScaleFactorConverter
	 */
	public static final ElementToChannelConverter SCALE_FACTOR_MINUS_3 = new ElementToChannelScaleFactorConverter(-3);

	/**
	 * Applies a scale factor of 1. Converts value [1] to [10].
	 *
	 * @see ElementToChannelScaleFactorConverter
	 */
	public static final ElementToChannelConverter SCALE_FACTOR_1 = new ElementToChannelScaleFactorConverter(1);

	/**
	 * Applies a scale factor of 2. Converts value [1] to [100].
	 *
	 * @see ElementToChannelScaleFactorConverter
	 */
	public static final ElementToChannelConverter SCALE_FACTOR_2 = new ElementToChannelScaleFactorConverter(2);

	/**
	 * Applies a scale factor of 3. Converts value [1] to [1000].
	 *
	 * @see ElementToChannelScaleFactorConverter
	 */
	public static final ElementToChannelConverter SCALE_FACTOR_3 = new ElementToChannelScaleFactorConverter(3);

	/**
	 * Converts only positive values from Element to Channel.
	 */
	public static final ElementToChannelConverter KEEP_POSITIVE = new ElementToChannelConverter(//
			// element -> channel
			StaticConverters.KEEP_POSITIVE, //
			// channel -> element
			value -> value);

	/**
	 * Inverts the value from Element to Channel.
	 */
	public static final ElementToChannelConverter INVERT = new ElementToChannelConverter(//
			// element -> channel
			StaticConverters.INVERT, //
			// channel -> element
			StaticConverters.INVERT);

	/**
	 * Sets the value to 'zero' if parameter is true; otherwise
	 * {@link #DIRECT_1_TO_1}.
	 *
	 * <ul>
	 * <li>true: set zero
	 * <li>false: apply {@link #DIRECT_1_TO_1}
	 * </ul>
	 *
	 * @param setZero true to set to null
	 * @return the {@link ElementToChannelConverter}
	 */
	// CHECKSTYLE:OFF
	public static ElementToChannelConverter SET_ZERO_IF_TRUE(boolean setZero) {
		// CHECKSTYLE:ON
		if (setZero) {
			return new ElementToChannelConverter(//
					// element -> channel
					value -> 0, //
					// channel -> element
					value -> 0);
		}
		return DIRECT_1_TO_1;
	}

	/**
	 * Converts depending on the given parameter.
	 *
	 * <ul>
	 * <li>true: invert value
	 * <li>false: keep value (1-to-1)
	 * </ul>
	 *
	 * @param invert true if Converter should invert
	 * @return the {@link ElementToChannelConverter}
	 */
	// CHECKSTYLE:OFF
	public static ElementToChannelConverter INVERT_IF_TRUE(boolean invert) {
		// CHECKSTYLE:ON
		if (invert) {
			return INVERT;
		}
		return DIRECT_1_TO_1;
	}

	/**
	 * Sets the chain with given {@link ElementToChannelConverter
	 * ElementToChannelConverters}.
	 * 
	 * @param converters to be applied as chain one after the other.
	 * @return {@link ElementToChannelConverter} after applied all converters.
	 */
	public static ElementToChannelConverter chain(ElementToChannelConverter... converters) {
		return new ElementToChannelConverter(
				// element -> channel
				value -> {
					for (var converter : converters) {
						value = converter.elementToChannel(value);
					}
					return value;
				},
				// channel -> element
				value -> {
					for (int i = converters.length - 1; i >= 0; i--) {
						value = converters[i].channelToElement(value);
					}
					return value;
				});
	}

	/**
	 * Converts only negative values from Element to Channel and inverts them (makes
	 * the value positive).
	 */
	public static final ElementToChannelConverter KEEP_NEGATIVE_AND_INVERT = ElementToChannelConverter.chain(INVERT,
			KEEP_POSITIVE);

	/**
	 * Applies {@link ElementToChannelConverter#SCALE_FACTOR_1} and
	 * CONVERT_POSITIVE.
	 */
	public static final ElementToChannelConverter SCALE_FACTOR_1_AND_KEEP_POSITIVE = ElementToChannelConverter
			.chain(SCALE_FACTOR_1, KEEP_POSITIVE);

	/**
	 * Applies {@link ElementToChannelConverter#SCALE_FACTOR_2} and INVERT.
	 */
	public static final ElementToChannelConverter SCALE_FACTOR_2_AND_INVERT = ElementToChannelConverter
			.chain(SCALE_FACTOR_2, INVERT);

	/**
	 * Applies {@link ElementToChannelConverter#SCALE_FACTOR_1} and
	 * CONVERT_NEGATIVE_AND_INVERT.
	 */
	public static final ElementToChannelConverter SCALE_FACTOR_1_AND_KEEP_NEGATIVE_AND_INVERT = ElementToChannelConverter
			.chain(SCALE_FACTOR_1, KEEP_NEGATIVE_AND_INVERT);

	/**
	 * Applies {@link ElementToChannelConverter#SCALE_FACTOR_2} and
	 * CONVERT_POSITIVE.
	 */
	public static final ElementToChannelConverter SCALE_FACTOR_2_AND_KEEP_POSITIVE = ElementToChannelConverter
			.chain(SCALE_FACTOR_2, KEEP_POSITIVE);

	/**
	 * Applies {@link ElementToChannelConverter#SCALE_FACTOR_2} and @see
	 * {@link ElementToChannelConverter#KEEP_NEGATIVE_AND_INVERT}.
	 */
	public static final ElementToChannelConverter SCALE_FACTOR_2_AND_KEEP_NEGATIVE_AND_INVERT = ElementToChannelConverter
			.chain(SCALE_FACTOR_2, KEEP_NEGATIVE_AND_INVERT);

	/**
	 * Applies {@link ElementToChannelConverter#SCALE_FACTOR_2_AND_KEEP_NEGATIVE}
	 * and @see {@link ElementToChannelConverter#INVERT}.
	 */
	public static ElementToChannelConverter SCALE_FACTOR_2_AND_KEEP_NEGATIVE = ElementToChannelConverter
			.chain(SCALE_FACTOR_2_AND_KEEP_NEGATIVE_AND_INVERT, INVERT);

	/**
	 * Applies {@link ElementToChannelConverter#SCALE_FACTOR_1} and INVERT_IF_TRUE.
	 * 
	 * @param invert input value for {@link #INVERT_IF_TRUE(boolean)}
	 * @return the {@link ElementToChannelConverter}
	 */
	// CHECKSTYLE:OFF
	public static final ElementToChannelConverter SCALE_FACTOR_1_AND_INVERT_IF_TRUE(boolean invert) {
		// CHECKSTYLE:ON
		return ElementToChannelConverter.chain(SCALE_FACTOR_1, INVERT_IF_TRUE(invert));
	}

	/**
	 * Applies {@link ElementToChannelConverter#SCALE_FACTOR_2} and INVERT_IF_TRUE.
	 * 
	 * @param invert input value for {@link #INVERT_IF_TRUE(boolean)}
	 * @return the {@link ElementToChannelConverter}
	 */
	// CHECKSTYLE:OFF
	public static final ElementToChannelConverter SCALE_FACTOR_2_AND_INVERT_IF_TRUE(boolean invert) {
		// CHECKSTYLE:ON
		return ElementToChannelConverter.chain(SCALE_FACTOR_2, INVERT_IF_TRUE(invert));
	}

	/**
	 * Applies {@link ElementToChannelConverter#SCALE_FACTOR_3} and INVERT_IF_TRUE.
	 * 
	 * @param invert input value for {@link #INVERT_IF_TRUE(boolean)}
	 * @return the {@link ElementToChannelConverter}
	 */
	// CHECKSTYLE:OFF
	public static final ElementToChannelConverter SCALE_FACTOR_3_AND_INVERT_IF_TRUE(boolean invert) {
		// CHECKSTYLE:ON
		return ElementToChannelConverter.chain(SCALE_FACTOR_3, INVERT_IF_TRUE(invert));
	}

	/**
	 * Applies {@link ElementToChannelConverter#SCALE_FACTOR_MINUS_1} and
	 * INVERT_IF_TRUE.
	 * 
	 * @param invert input value for {@link #INVERT_IF_TRUE(boolean)}
	 * @return the {@link ElementToChannelConverter}
	 */
	// CHECKSTYLE:OFF
	public static final ElementToChannelConverter SCALE_FACTOR_MINUS_1_AND_INVERT_IF_TRUE(boolean invert) {
		// CHECKSTYLE:ON
		return ElementToChannelConverter.chain(SCALE_FACTOR_MINUS_1, INVERT_IF_TRUE(invert));
	}

	private final Function<Object, Object> elementToChannel;
	private final Function<Object, Object> channelToElement;

	/**
	 * This constructs and back-and-forth converter from Element to Channel and
	 * back.
	 *
	 * @param elementToChannel from Element to Channel
	 * @param channelToElement from Channel to Element
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
	 * @param elementToChannel Element to Channel
	 */
	public ElementToChannelConverter(Function<Object, Object> elementToChannel) {
		this.elementToChannel = elementToChannel;
		this.channelToElement = value -> {
			throw new IllegalArgumentException("Backwards-Conversion for [" + value + "] is not implemented.");
		};
	}

	/**
	 * Convert an Element value to a Channel value. If the value can or should not
	 * be converted, this method returns null.
	 *
	 * @param value the Element value
	 * @return the converted value or null
	 */
	public Object elementToChannel(Object value) {
		return this.elementToChannel.apply(value);
	}

	/**
	 * Convert a Channel value to an Element value. If the value can or should not
	 * be converted, this method returns null.
	 *
	 * @param value the Channel value
	 * @return the converted value or null
	 */
	public Object channelToElement(Object value) {
		return this.channelToElement.apply(value);
	}

	/**
	 * Multiply the given factor with the channel value.
	 * 
	 * @param factor the value to be applied to the Channel value.
	 * @return {@link ElementToChannelConverter}
	 */
	public static final ElementToChannelConverter multiply(double factor) {
		return new ElementToChannelConverter(multiplyFunction(factor), divideFunction(factor));
	}

	/**
	 * Divide the channel value with the given scale.
	 * 
	 * @param scale the value to be applied to the Channel value.
	 * @return {@link ElementToChannelConverter}
	 */
	public static final ElementToChannelConverter divide(double scale) {
		return new ElementToChannelConverter(divideFunction(scale), multiplyFunction(scale));
	}

	/**
	 * Add the given value to the Channel value.
	 * 
	 * @param value to add to the Channel value.
	 * @return {@link ElementToChannelConverter}
	 */
	public static final ElementToChannelConverter add(double value) {
		return new ElementToChannelConverter(addFunction(value), addFunction(-value));
	}

	/**
	 * Subtract the given value to the Channel value.
	 * 
	 * @param value to subtract to the Channel value.
	 * @return {@link ElementToChannelConverter}
	 */
	public static final ElementToChannelConverter subtract(double value) {
		return add(-value);
	}

	/**
	 * Multiplication function to be applied for different variable types.
	 * 
	 * @param factor to multiply to the Channel value.
	 * @return an {@link Object} based on the variable type.
	 */
	private static final Function<Object, Object> multiplyFunction(double factor) {
		return value -> apply(value, //
				t -> (long) (t * factor), //
				t -> (long) (t * factor), //
				t -> (long) (t * factor), //
				t -> t * factor, //
				t -> t * factor //
		);
	}

	/**
	 * Division function to be applied for different variable types.
	 * 
	 * @param scale to divide to the Channel value.
	 * @return an {@link Object} based on the variable type.
	 */
	private static final Function<Object, Object> divideFunction(double scale) {
		return value -> apply(value, //
				t -> (long) (t / scale), //
				t -> (long) (t / scale), //
				t -> (long) (t / scale), //
				t -> t / scale, //
				t -> t / scale //
		);
	}

	/**
	 * Summation function to be applied for different variable types.
	 * 
	 * @param value to add to the Channel value.
	 * @return an {@link Object} based on the variable type.
	 */
	private static final Function<Object, Object> addFunction(double value) {
		return v -> apply(v, //
				t -> (long) (t + value), //
				t -> (long) (t + value), //
				t -> (long) (t + value), //
				t -> t + value, //
				t -> t + value //
		);
	}

	private static Object apply(//
			Object value, //
			Function<Short, Long> shortFactor, //
			Function<Integer, Long> integerFactor, //
			Function<Long, Long> longFactor, //
			Function<Float, Double> floatFactor, //
			Function<Double, Double> doubleFactor //
	) {
		if (value == null) {
			return null;
		}
		if (value instanceof Boolean) {
			return (boolean) value;
		}
		if (value instanceof Short s) {
			long result = shortFactor.apply(s);
			if (result >= Short.MIN_VALUE && result <= Short.MAX_VALUE) {
				return Short.valueOf((short) result);
			}
			if (result > Integer.MIN_VALUE && result < Integer.MAX_VALUE) {
				return Integer.valueOf((int) result);
			} else {
				return Long.valueOf(result);
			}
		}
		if (value instanceof Integer i) {
			long result = integerFactor.apply(i);
			if (result >= Integer.MIN_VALUE && result <= Integer.MAX_VALUE) {
				return Integer.valueOf((int) result);
			}
			return Long.valueOf(result);
		}
		if (value instanceof Long l) {
			return longFactor.apply(l);
		}
		if (value instanceof Float f) {
			double result = floatFactor.apply(f);
			if (result >= Float.MIN_VALUE && result <= Float.MAX_VALUE) {
				return Float.valueOf((float) result);
			}
			return Double.valueOf(result);
		}
		if (value instanceof Double d) {
			return doubleFactor.apply(d);
		}
		if (value instanceof String) {
			return value;
		}
		throw new IllegalArgumentException(
				"Type [" + value.getClass().getName() + "] not supported by OFFSET converter");
	}
}
