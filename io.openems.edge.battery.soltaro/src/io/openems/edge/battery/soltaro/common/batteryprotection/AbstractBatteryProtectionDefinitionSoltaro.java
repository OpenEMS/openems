package io.openems.edge.battery.soltaro.common.batteryprotection;

import io.openems.edge.battery.protection.BatteryProtectionDefinition;
import io.openems.edge.battery.protection.force.ForceCharge;
import io.openems.edge.battery.protection.force.ForceDischarge;
import io.openems.edge.common.linecharacteristic.PolyLine;

public abstract class AbstractBatteryProtectionDefinitionSoltaro implements BatteryProtectionDefinition {

	@Override
	public final int getInitialBmsMaxEverChargeCurrent() {
		return 100; // [A]
	}

	@Override
	public final int getInitialBmsMaxEverDischargeCurrent() {
		return 100; // [A]
	}

	@Override
	public final PolyLine getDischargeVoltageToPercent() {
		return PolyLine.create() //
				.addPoint(3040, 0) //
				.addPoint(Math.nextUp(3040), 0.01) //
				.addPoint(3050, 0.01) //
				.addPoint(3150, 1) //
				.addPoint(3615, 1) //
				.addPoint(Math.nextUp(3615), 1) //
				.build();
	}

	@Override
	public final PolyLine getChargeTemperatureToPercent() {
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
	public final PolyLine getDischargeTemperatureToPercent() {
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
	public final ForceDischarge.Params getForceDischargeParams() {
		return new ForceDischarge.Params(3660, 3640, 3450);
	}

	@Override
	public final ForceCharge.Params getForceChargeParams() {
		return new ForceCharge.Params(3031, 3100, 3130);

	}

	@Override
	public final Double getMaxIncreaseAmperePerSecond() {
		return 0.1; // [A] per second
	}
}