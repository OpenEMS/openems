package io.openems.edge.battery.soltaro;

import io.openems.edge.battery.protection.BatteryProtectionDefinition;
import io.openems.edge.battery.protection.force.ForceCharge;
import io.openems.edge.battery.protection.force.ForceDischarge;
import io.openems.edge.common.linecharacteristic.PolyLine;

public class BatteryProtectionDefinitionSoltaro implements BatteryProtectionDefinition {

	@Override
	public int getInitialBmsMaxEverChargeCurrent() {
		return 80; // [A]
	}

	@Override
	public int getInitialBmsMaxEverDischargeCurrent() {
		return 80; // [A]
	}

	@Override
	public PolyLine getChargeVoltageToPercent() {
		return PolyLine.create() //
				.addPoint(3000, 0.1) //
				.addPoint(Math.nextUp(3000), 1) //
				.addPoint(3450, 1) //
				.addPoint(3600, 0.02) //
				.addPoint(Math.nextDown(3650), 0.02) //
				.addPoint(3650, 0) //
				.build();
	}

	@Override
	public PolyLine getDischargeVoltageToPercent() {
		return PolyLine.create() //
				.addPoint(2900, 0) //
				.addPoint(Math.nextUp(2900), 0.05) //
				.addPoint(2920, 0.05) //
				.addPoint(3000, 1) //
				.addPoint(3700, 1) //
				.addPoint(Math.nextUp(3700), 0) //
				.build();
	}

	@Override
	public PolyLine getChargeTemperatureToPercent() {
		return PolyLine.create() //
				.addPoint(0, 0) //
				.addPoint(Math.nextUp(0), 0.01) //
				.addPoint(18, 1) //
				.addPoint(35, 1) //
				.addPoint(Math.nextDown(40), 0.01) //
				.addPoint(40, 0) //
				.build();
	}

	@Override
	public PolyLine getDischargeTemperatureToPercent() {
		return PolyLine.create() //
				.addPoint(0, 0) //
				.addPoint(Math.nextUp(0), 0.01) //
				.addPoint(12, 1) //
				.addPoint(45, 1) //
				.addPoint(Math.nextDown(55), 0.01) //
				.addPoint(55, 0) //
				.build();
	}

	@Override
	public ForceDischarge.Params getForceDischargeParams() {
		return new ForceDischarge.Params(3660, 3640, 3450);
	}

	@Override
	public ForceCharge.Params getForceChargeParams() {
		return new ForceCharge.Params(2850, 2910, 3000);
	}

	@Override
	public Double getMaxIncreaseAmperePerSecond() {
		return 0.1; // [A] per second
	}
}