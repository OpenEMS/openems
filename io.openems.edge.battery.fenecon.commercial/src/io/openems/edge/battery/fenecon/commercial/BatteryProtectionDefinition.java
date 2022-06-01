package io.openems.edge.battery.fenecon.commercial;

import io.openems.edge.battery.protection.force.ForceCharge;
import io.openems.edge.battery.protection.force.ForceDischarge;
import io.openems.edge.common.linecharacteristic.PolyLine;

public class BatteryProtectionDefinition implements io.openems.edge.battery.protection.BatteryProtectionDefinition {

	@Override
	public int getInitialBmsMaxEverChargeCurrent() {
		return 100; // [A]
	}

	@Override
	public int getInitialBmsMaxEverDischargeCurrent() {
		return 100; // [A]
	}

	// Over voltage Protection
	@Override
	public PolyLine getChargeVoltageToPercent() {
		return PolyLine.create() //
				.addPoint(3000, 0.1) //
				.addPoint(Math.nextUp(3000), 1) //
				.addPoint(3485, 1) //
				.addPoint(3490, 0.9) //
				.addPoint(3570, 0.01) //
				.addPoint(3600, 0.01) //
				.addPoint(Math.nextDown(3600), 0) //
				.addPoint(3600, 0) //
				.build();
	}

	// Low Voltage protection
	@Override
	public PolyLine getDischargeVoltageToPercent() {
		return PolyLine.create() //
				.addPoint(3000, 0) //
				.addPoint(Math.nextUp(3000), 0.1) //
				.addPoint(3030, 0.02) //
				.addPoint(3050, 0.02) //
				.addPoint(3140, 1) //
				.addPoint(3600, 1) //
				.addPoint(Math.nextUp(3600), 1) //
				.build();
	}

	@Override
	public PolyLine getChargeTemperatureToPercent() {
		return PolyLine.create() //
				.addPoint(0, 0) //
				.addPoint(Math.nextUp(0), 0.01) //
				.addPoint(15, 1) //
				.addPoint(50, 1) //
				.addPoint(Math.nextDown(54), 0.01) //
				.addPoint(55, 0) //
				.build();
	}

	@Override
	public PolyLine getDischargeTemperatureToPercent() {
		return PolyLine.create() //
				.addPoint(0, 0) //
				.addPoint(Math.nextUp(0), 0.01) //
				.addPoint(10, 1) //
				.addPoint(50, 1) //
				.addPoint(Math.nextDown(54), 0.01) //
				.addPoint(55, 0) //
				.build();
	}

	@Override
	public ForceDischarge.Params getForceDischargeParams() {
		return new ForceDischarge.Params(3700, 3630, 3610);
	}

	@Override
	public ForceCharge.Params getForceChargeParams() {
		return new ForceCharge.Params(2850, 2950, 3030);
	}

	@Override
	public Double getMaxIncreaseAmperePerSecond() {
		return 0.5; // [A] per second
	}

}
