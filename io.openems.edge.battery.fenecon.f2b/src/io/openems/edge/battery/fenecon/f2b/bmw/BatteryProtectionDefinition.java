package io.openems.edge.battery.fenecon.f2b.bmw;

import io.openems.edge.battery.protection.force.ForceCharge;
import io.openems.edge.battery.protection.force.ForceDischarge;
import io.openems.edge.common.linecharacteristic.PolyLine;

public class BatteryProtectionDefinition implements io.openems.edge.battery.protection.BatteryProtectionDefinition {
	@Override
	public int getInitialBmsMaxEverChargeCurrent() {
		return 135; // [A]
	}

	@Override
	public int getInitialBmsMaxEverDischargeCurrent() {
		return 135; // [A]
	}

	// Over voltage Protection
	@Override
	public PolyLine getChargeVoltageToPercent() {
		return PolyLine.create() //
				.addPoint(3304, 1) //
				.addPoint(Math.nextUp(3304), 1) //
				.addPoint(4160, 1) //
				.addPoint(4164, 0.2) //
				.addPoint(4174, 0.05) //
				.addPoint(Math.nextDown(4180), 0.01) //
				.addPoint(4180, 0) //
				.build();
	}

	// Low Voltage protection
	@Override
	public PolyLine getDischargeVoltageToPercent() {
		return PolyLine.create() //
				.addPoint(3304, 0) //
				.addPoint(Math.nextUp(3308), 0.01) //
				.addPoint(3314, 0.1) //
				.addPoint(3340, 1) //
				.addPoint(4180, 1) //
				.addPoint(Math.nextUp(4180), 1) //
				.build();
	}

	@Override
	public PolyLine getChargeTemperatureToPercent() {
		return PolyLine.create() //
				.addPoint(-20, 0.01) //
				.addPoint(Math.nextUp(-20), 0.01) //
				.addPoint(25, 1) //
				.addPoint(35, 1) //
				.addPoint(Math.nextDown(44), 0.01) //
				.addPoint(45, 0) //
				.build();
	}

	@Override
	public PolyLine getDischargeTemperatureToPercent() {
		return PolyLine.create() //
				.addPoint(-36, 0.01) //
				.addPoint(Math.nextUp(-36), 0.01) //
				.addPoint(5, 1) //
				.addPoint(35, 1) //
				.addPoint(Math.nextDown(44), 0.01) //
				.addPoint(45, 0) //
				.build();
	}

	@Override
	public ForceDischarge.Params getForceDischargeParams() {
		return new ForceDischarge.Params(4186, 4183, 4180);
	}

	@Override
	public ForceCharge.Params getForceChargeParams() {
		return new ForceCharge.Params(3296, 3298, 3300);

	}

	@Override
	public Double getMaxIncreaseAmperePerSecond() {
		return 1.; // [A] per second
	}
}