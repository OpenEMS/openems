package io.openems.edge.solaredge.ess;

import io.openems.edge.bridge.modbus.api.ElementToChannelConverter;

/**
 * Even if there is no real power from PV and no Charge/Discharge Power, the Inverter Power Channel
 * could remain on minimum power values. These values are ignored.
 *
 * <p>
 * The class optionally creates a {@link ElementToChannelConverterChain}, for
 * use-cases when additionally to the above 'IgnoeMinPower' logic a scale-factor is
 * required.
 */
public class IgnoreMinPowerConverter extends ElementToChannelConverter {

	/**
	 * Generates an ElementToChannelConverter for the use case covered by
	 * {@link IgnoreMinPowerConverter}.
	 *
	 * @param parent    the parent component
	 * @param converter an additional {@link ElementToChannelConverter}
	 * @return the {@link ElementToChannelConverter}
	 */
	public static ElementToChannelConverter from(SolarEdgeEssImpl parent,
			ElementToChannelConverter converter) {
		if (converter == DIRECT_1_TO_1) {
			return new IgnoreMinPowerConverter(parent);
		}
		return ElementToChannelConverter.chain(new IgnoreMinPowerConverter(parent), converter);
	}

	private IgnoreMinPowerConverter(SolarEdgeEssImpl parent) {
		super(value -> {
			// Is value null?
			if (value == null) {
				return null;
			}
			// If there is no PV Production and no Charge/Discharge Power -> ignore minimum values
			if (value instanceof Float f && Math.abs(f) < 50 && parent.getPvProduction() != null && parent.getPvProduction() == 0
					&& parent.getDcDischargePower().orElse(-1) == 0) {
				return 0;
			}
			return value;
		});
	}

}