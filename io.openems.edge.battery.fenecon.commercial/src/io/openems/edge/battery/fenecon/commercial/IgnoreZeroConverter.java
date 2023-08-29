package io.openems.edge.battery.fenecon.commercial;

import io.openems.edge.bridge.modbus.api.ElementToChannelConverter;

/**
 * Before the battery is started, values are wrongly received as 'zero' via
 * Modbus. This logic replaces these wrong values in the beginning with 'null'.
 *
 * <p>
 * The class optionally creates a {@link ElementToChannelConverterChain}, for
 * use-cases when additionally to the above 'zero/null' logic a scale-factor is
 * required.
 */
public class IgnoreZeroConverter extends ElementToChannelConverter {

	/**
	 * Generates an ElementToChannelConverter for the use case covered by
	 * {@link IgnoreZeroConverter}.
	 *
	 * @param parent    the parent component
	 * @param converter an additional {@link ElementToChannelConverter}
	 * @return the {@link ElementToChannelConverter}
	 */
	public static ElementToChannelConverter from(BatteryFeneconCommercialImpl parent,
			ElementToChannelConverter converter) {
		if (converter == DIRECT_1_TO_1) {
			return new IgnoreZeroConverter(parent);
		}
		return ElementToChannelConverter.chain(new IgnoreZeroConverter(parent), converter);
	}

	private IgnoreZeroConverter(BatteryFeneconCommercialImpl parent) {
		super(value -> {
			// Is value null?
			if (value == null) {
				return null;
			}
			// If the battery is not started and the value is not zero -> return the value,
			if (value instanceof Integer && (Integer) value != 0) {
				return value;
			}
			if (value instanceof Long && (Long) value != 0L) {
				return value;
			}
			// Is battery status not available or battery not started?
			var isBatteryStarted = parent.getMasterStarted();
			if (!isBatteryStarted.isDefined() || !isBatteryStarted.get()) {
				return null;
			}
			return value;
		});
	}

}