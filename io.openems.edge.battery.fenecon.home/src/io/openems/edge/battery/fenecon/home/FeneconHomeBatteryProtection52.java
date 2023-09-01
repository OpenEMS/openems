package io.openems.edge.battery.fenecon.home;

import io.openems.edge.battery.protection.BatteryProtectionDefinition;
import io.openems.edge.battery.protection.force.ForceCharge;
import io.openems.edge.battery.protection.force.ForceDischarge;
import io.openems.edge.common.linecharacteristic.PolyLine;

public class FeneconHomeBatteryProtection52 implements BatteryProtectionDefinition {

	@Override
	public int getInitialBmsMaxEverChargeCurrent() {
		return 40; // [A]
	}

	@Override
	public int getInitialBmsMaxEverDischargeCurrent() {
		return 40; // [A]
	}

	@Override
	public PolyLine getChargeVoltageToPercent() {
		return PolyLine.empty();
	}

	@Override
	public PolyLine getDischargeVoltageToPercent() {
		return PolyLine.create() //
				.addPoint(3000, 0.1) //
				.addPoint(3100, 1) //
				.build();
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
		return new ForceDischarge.Params(3550, 3490, 3450);
	}

	@Override
	public ForceCharge.Params getForceChargeParams() {
		return new ForceCharge.Params(2900, 3000, 3100);
	}

	@Override
	public Double getMaxIncreaseAmperePerSecond() {
		return 0.1; // [A] per second
	}

}
