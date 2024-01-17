package io.openems.edge.battery.pylontech.powercubem2;

import io.openems.edge.battery.protection.BatteryProtectionDefinition;
import io.openems.edge.battery.protection.force.ForceCharge;
import io.openems.edge.battery.protection.force.ForceDischarge;
import io.openems.edge.common.linecharacteristic.PolyLine;

public class PylontechPowercubeM2BatteryProtectionDefinition implements BatteryProtectionDefinition {

	/*
	 * Most values not defined. Those that are defined come from Pylontech engineer
	 * 
	 */
	@Override
	public int getInitialBmsMaxEverChargeCurrent() {
		return 148; // [A]
	}

	@Override
	public int getInitialBmsMaxEverDischargeCurrent() {
		return 148; // [A]
	}

	@Override
	public PolyLine getChargeVoltageToPercent() {
		return PolyLine.empty();
	}

	@Override
	public PolyLine getDischargeVoltageToPercent() {
		return PolyLine.empty();
	}

	@Override
	public PolyLine getChargeTemperatureToPercent() {
		return PolyLine.empty();
	}

	@Override
	public PolyLine getDischargeTemperatureToPercent() {
		return PolyLine.empty();
	}

	@Override
	public ForceDischarge.Params getForceDischargeParams() {
		return new ForceDischarge.Params(3650, 3450, 3449);
	}

	@Override
	public ForceCharge.Params getForceChargeParams() {
		return new ForceCharge.Params(2700, 3000, 3001);
	}

	@Override
	public Double getMaxIncreaseAmperePerSecond() {
		return 5.0; // [A] per second // TODO: This is not provided by Pylontech - check that it
					// functions ok with this limit.
	}
}