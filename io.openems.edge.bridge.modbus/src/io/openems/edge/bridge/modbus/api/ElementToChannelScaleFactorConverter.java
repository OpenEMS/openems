package io.openems.edge.bridge.modbus.api;

import io.openems.common.types.OpenemsType;

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
		for (OpenemsType openemsType : OpenemsType.values()) {
			// this 'for' + 'switch' is only utilized to get an alert by Eclipse IDE if a
			// new OpenemsType was added. ("The enum constant XXX needs a corresponding case
			// label in this enum switch on OpenemsType")
			switch (openemsType) {
			case BOOLEAN:
			case SHORT:
			case INTEGER:
			case LONG:
			case FLOAT:
			case STRING:
				if (value instanceof Boolean) {
					return (boolean) value;
				}
				if (value instanceof Short) {
					return (short) ((short) value * Math.pow(10, scaleFactor * -1));
				}
				if (value instanceof Integer) {
					return (int) ((int) value * Math.pow(10, scaleFactor * -1));
				}
				if (value instanceof Long) {
					return (long) ((long) value * Math.pow(10, scaleFactor * -1));
				}
				if (value instanceof Float) {
					return (float) ((float) value * Math.pow(10, scaleFactor * -1));
				}
				if (value instanceof String) {
					return (String) value;
				}
			}
			break;
		}
		throw new IllegalArgumentException(
				"Type [" + value.getClass().getName() + "] not supported by SCALE_FACTOR converter");
	}
}
