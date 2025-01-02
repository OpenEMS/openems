package io.openems.edge.goodwe.gridmeter;

import io.openems.edge.bridge.modbus.api.ElementToChannelConverter;

/**
 * If the {@link GoodWeGridMeter} State 'HAS_NO_METER' is set, all values should
 * be considered 'null' instead of 'zero'.
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
	public static ElementToChannelConverter from(GoodWeGridMeter parent, ElementToChannelConverter converter) {
		if (converter == ElementToChannelConverter.DIRECT_1_TO_1) {
			return new IgnoreZeroConverter(parent);
		}
		return ElementToChannelConverter.chain(new IgnoreZeroConverter(parent), converter);
	}

	private IgnoreZeroConverter(GoodWeGridMeter parent) {
		super(value -> {
			// Is value null?
			if (value == null) {
				return null;
			}
			if (value instanceof Integer && (Integer) value != 0) {
				return value;
			}
			if (value instanceof Long && (Long) value != 0L) {
				return value;
			}
			var hasNoMeter = parent.getHasNoMeter();
			if (!hasNoMeter.isDefined() || hasNoMeter.get()) {
				return null;
			}
			return value;
		});
	}

}