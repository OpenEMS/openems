package io.openems.edge.bridge.modbus.api;

/**
 * Converts between Element and Channel by applying a scale factor of 2
 * 
 * (channel = element * 10^scaleFactor)
 * 
 * Example: if the Register is in unit [0.1 V] this converter converts to unit
 * [1 mV]
 */
public class ElementToChannelScaleFactorConverter extends ElementToChannelConverter {

	public ElementToChannelScaleFactorConverter(int scaleFactor) {
		super(//
				// element -> channel
				value -> {
					return apply(value, scaleFactor * -1);
				}, //
					// channel -> element
				value -> {
					return apply(value, scaleFactor);
				});
	}

	private static Object apply(Object value, int scaleFactor) {
		if (value == null) {
			return null;
		}
		if (value instanceof Integer) {
			return (int) ((int) value * Math.pow(10, scaleFactor * -1));
		}
		if (value instanceof Long) {
			return (long) ((long) value * Math.pow(10, scaleFactor * -1));
		}
		throw new IllegalArgumentException(
				"Type [" + value.getClass().getName() + "] not supported by SCALE_FACTOR converter");
	}
}
