package io.openems.edge.battery.fenecon.home;

import io.openems.edge.battery.protection.BatteryProtectionDefinition;
import io.openems.edge.battery.protection.force.ForceCharge;
import io.openems.edge.battery.protection.force.ForceDischarge;
import io.openems.edge.common.linecharacteristic.PolyLine;

public class FeneconHomeBatteryProtection64 implements BatteryProtectionDefinition {

	@Override
	public int getInitialBmsMaxEverChargeCurrent() {
		return 50; // [A]
	}

	@Override
	public int getInitialBmsMaxEverDischargeCurrent() {
		return 50; // [A]
	}

	@Override
	public PolyLine getChargeVoltageToPercent() {
		return PolyLine.create() //
				.addPoint(2000, 0.1) //
				.addPoint(3000, 0.1) //
				.addPoint(Math.nextUp(3000), 1) //
				.addPoint(3450, 1) //
				.addPoint(3540, 0.08) //
				.addPoint(Math.nextDown(3550), 0.08) //
				.addPoint(3550, 0) //
				.build();
	}

	@Override
	public PolyLine getDischargeVoltageToPercent() {
		return PolyLine.create() //
				.addPoint(2000, 0) //
				.addPoint(2900, 0) //
				.addPoint(Math.nextUp(2900), 0.1) //
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
		return new ForceDischarge.Params(3600, 3540, 3450);
	}

	@Override
	public ForceCharge.Params getForceChargeParams() {
		return new ForceCharge.Params(2850, 2950, 3100);
	}

	@Override
	public Double getMaxIncreaseAmperePerSecond() {
		return 0.1; // [A] per second
	}

}
