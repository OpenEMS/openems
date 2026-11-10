package io.openems.edge.bridge.modbus.api;

import java.util.function.Function;

/**
 * Provides Functions to convert from Element to Channel and back. Also has some
 * static convenience functions to facilitate conversion.
 */
public class ElementToChannelConverter {

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

	// CHECKSTYLE:OFF: MethodName
	/**
	 * Multiply the given factor with the channel value.
	 * 
	 * @param factor the value to be applied to the Channel value.
	 * @return {@link ElementToChannelConverter}
	 */
	public static ElementToChannelConverter MULTIPLY(double factor) {
		return new ElementToChannelConverter(multiplyFunction(factor), divideFunction(factor));
	}

	/**
	 * Divide the channel value with the given scale.
	 * 
	 * @param scale the value to be applied to the Channel value.
	 * @return {@link ElementToChannelConverter}
	 */
	public static ElementToChannelConverter DIVIDE(double scale) {
		return new ElementToChannelConverter(divideFunction(scale), multiplyFunction(scale));
	}

	/**
	 * Add the given value to the Channel value.
	 * 
	 * @param value to add to the Channel value.
	 * @return {@link ElementToChannelConverter}
	 */
	public static ElementToChannelConverter ADD(double value) {
		return new ElementToChannelConverter(addFunction(value), addFunction(-value));
	}

	/**
	 * Subtract the given value to the Channel value.
	 * 
	 * @param value to subtract to the Channel value.
	 * @return {@link ElementToChannelConverter}
	 */
	public static ElementToChannelConverter SUBTRACT(double value) {
		return ADD(-value);
	}
	// CHECKSTYLE:ON: MethodName

	/**
	 * Multiplication function to be applied for different variable types.
	 * 
	 * @param factor to multiply to the Channel value.
	 * @return an {@link Object} based on the variable type.
	 */
	private static Function<Object, Object> multiplyFunction(double factor) {
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
	private static Function<Object, Object> divideFunction(double scale) {
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
	private static Function<Object, Object> addFunction(double value) {
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
		return switch (value) {
		case null -> null;
		case Boolean b -> b;
		case Short s -> {
			long result = shortFactor.apply(s);
			if (result >= Short.MIN_VALUE && result <= Short.MAX_VALUE) {
				yield Short.valueOf((short) result);
			} else if (result > Integer.MIN_VALUE && result < Integer.MAX_VALUE) {
				yield Integer.valueOf((int) result);
			}
			yield Long.valueOf(result);
		}
		case Integer i -> {
			long result = integerFactor.apply(i);
			if (result >= Integer.MIN_VALUE && result <= Integer.MAX_VALUE) {
				yield Integer.valueOf((int) result);
			}
			yield Long.valueOf(result);
		}
		case Long l //
			-> longFactor.apply(l);
		case Float f -> {
			double result = floatFactor.apply(f);
			if (result >= Float.MIN_VALUE && result <= Float.MAX_VALUE) {
				yield Float.valueOf((float) result);
			}
			yield Double.valueOf(result);
		}
		case Double d //
			-> doubleFactor.apply(d);
		case String s //
			-> s;
		default //
			-> throw new IllegalArgumentException(
					"Type [" + value.getClass().getName() + "] not supported by OFFSET converter");
		};
	}
}
