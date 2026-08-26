package io.openems.edge.bridge.modbus.api;

import io.openems.common.exceptions.InvalidValueException;
import io.openems.edge.bridge.modbus.sunspec.Point.ScaledValuePoint;
import io.openems.edge.common.channel.ChannelId;
import io.openems.edge.common.channel.IntegerReadChannel;
import io.openems.edge.common.component.OpenemsComponent;

/**
 * Converts between Element and Channel by applying a scale factor.
 *
 * <p>
 * (channel = element * 10^scaleFactor)
 *
 * <p>
 * Example: if the Register is in unit [0.1 V] and this converter has a
 * scaleFactor of '2', it converts to unit [1 mV]
 */
public class ElementToChannelScaleFactorConverter extends ElementToChannelConverter {

	private static int getValueOrError(OpenemsComponent component, ChannelId channelId)
			throws InvalidValueException, IllegalArgumentException {
		var channel = (IntegerReadChannel) component.channel(channelId);
		var value = channel.getNextValue().orElse(null);
		if (value != null) {
			return value;
		}
		return channel.value().getOrError();
	}

	public ElementToChannelScaleFactorConverter(OpenemsComponent component, ScaledValuePoint point,
			ChannelId scaleFactorChannel) {
		super(//
				// element -> channel
				value -> {
					if (!point.isDefined(value)) {
						return null;
					}
					try {
						return apply(value, getValueOrError(component, scaleFactorChannel) * -1);
					} catch (InvalidValueException | IllegalArgumentException e) {
						return null;
					}
				}, //

				// channel -> element
				value -> {
					try {
						return apply(value, getValueOrError(component, scaleFactorChannel));
					} catch (InvalidValueException | IllegalArgumentException e) {
						return null;
					}
				});
	}

	public ElementToChannelScaleFactorConverter(int scaleFactor) {
		super(//
				// element -> channel
				value -> apply(value, scaleFactor * -1), //

				// channel -> element
				value -> apply(value, scaleFactor));
	}

	private static Object apply(Object value, int scaleFactor) {
		var factor = Math.pow(10, scaleFactor * -1);
		return switch (value) {
		case null -> null;
		case Boolean b -> b;
		case Short s -> {
			var result = s * factor;
			if (result >= Short.MIN_VALUE && result <= Short.MAX_VALUE) {
				yield Short.valueOf((short) result);
			} else if (result > Integer.MIN_VALUE && result < Integer.MAX_VALUE) {
				yield Integer.valueOf((int) result);
			}
			yield Double.valueOf(Math.round(result));
		}
		case Integer i -> {
			var result = i * factor;
			if (result >= Integer.MIN_VALUE && result <= Integer.MAX_VALUE) {
				yield Integer.valueOf((int) result);
			}
			yield Double.valueOf(Math.round(result));
		}
		case Long l -> {
			var result = l * factor;
			yield Math.round(result);
		}
		case Float f -> {
			var result = f * factor;
			if (result >= Float.MIN_VALUE && result <= Float.MAX_VALUE) {
				yield Float.valueOf((float) result);
			}
			yield Double.valueOf(result);
		}
		case Double d //
			-> Double.valueOf(d * factor);
		case String s //
			-> s;
		default //
			-> throw new IllegalArgumentException(
					"Type [" + value.getClass().getName() + "] not supported by SCALE_FACTOR converter");
		};
	}
}
