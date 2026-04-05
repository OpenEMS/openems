package io.openems.edge.ruhfass.battery.rbti;

import io.openems.edge.battery.protection.force.ForceCharge;
import io.openems.edge.battery.protection.force.ForceDischarge;
import io.openems.edge.common.linecharacteristic.PolyLine;

public class BatteryProtectionDefinition implements io.openems.edge.battery.protection.BatteryProtectionDefinition {

	@Override
	public int getInitialBmsMaxEverChargeCurrent() {
		return 188; // [A]
	}

	@Override
	public int getInitialBmsMaxEverDischargeCurrent() {
		return 188; // [A]
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
	public ForceDischarge.Params getForceDischargeParams() {
		return new ForceDischarge.Params(//
				4170, //
				4160, //
				4150);
	}

	@Override
	public ForceCharge.Params getForceChargeParams() {
		return new ForceCharge.Params(//
				3210, //
				3220, //
				3231);
	}

	@Override
	public Double getMaxIncreaseAmperePerSecond() {
		return 1.; // [A] per second
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
	public PolyLine getChargeSocToPercent() {
		return PolyLine.create() //
				.addPoint(94, 1.0) //
				.addPoint(95, 0.0) //
				.build();

	}

	@Override
	public PolyLine getDischargeSocToPercent() {
		return PolyLine.create() //
				.addPoint(6, 1.0) //
				.addPoint(5, 0.0) //
				.build();
	}
}
