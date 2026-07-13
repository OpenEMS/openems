package io.openems.edge.ess.generic.common.essprotection;

import io.openems.edge.battery.api.Battery;
import io.openems.edge.batteryinverter.api.SymmetricBatteryInverter;

public interface EssProtectionHandler {

	public static record EssProtectionLimits(//
			Integer chargeMaxCurrent, //
			Integer dischargeMaxCurrent) {

		public static final EssProtectionLimits EMPTY = new EssProtectionLimits(null, null);
	}

	/**
	 * Calculates the {@link EssProtectionLimits}.
	 * 
	 * @param battery  the {@link Battery}
	 * @param inverter the {@link SymmetricBatteryInverter}
	 * @return the {@link EssProtectionLimits}
	 */
	public EssProtectionLimits calculateEssProtectionLimits(Battery battery, SymmetricBatteryInverter inverter);

}
