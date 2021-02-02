package io.openems.edge.battery.soltaro;

import io.openems.edge.battery.protection.BatteryProtectionDefinition;
import io.openems.edge.battery.protection.ChargeMaxCurrentHandler;
import io.openems.edge.battery.protection.ChargeMaxCurrentHandler.ForceDischargeParams;
import io.openems.edge.battery.protection.DischargeMaxCurrentHandler;
import io.openems.edge.battery.protection.DischargeMaxCurrentHandler.ForceChargeParams;
import io.openems.edge.common.linecharacteristic.PolyLine;

public class SoltaroBatteryProtectionDefinition implements BatteryProtectionDefinition {

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
				.addPoint(3600, 0.01) //
				.addPoint(Math.nextDown(3650), 0.01) //
				.addPoint(3650, 0) //
				.build();
	}

	@Override
	public PolyLine getDischargeVoltageToPercent() {
		return PolyLine.create() //
				.addPoint(2900, 0) //
				.addPoint(Math.nextUp(2900), 0.1) //
				.addPoint(2920, 0.1) //
				.addPoint(3000, 1) //
				.addPoint(3700, 1) //
				.addPoint(Math.nextUp(3700), 0) //
				.build();
	}

	@Override
	public PolyLine getChargeTemperatureToPercent() {
		return PolyLine.create() //
				.addPoint(Math.nextDown(-10), 0) //
				.addPoint(-10, 0.215) //
				.addPoint(0, 0.215) //
				.addPoint(1, 0.325) //
				.addPoint(5, 0.325) //
				.addPoint(6, 0.65) //
				.addPoint(15, 0.65) //
				.addPoint(16, 1) //
				.addPoint(44, 1) //
				.addPoint(45, 0.65) //
				.addPoint(49, 0.65) //
				.addPoint(50, 0.325) //
				.addPoint(54, 0.325) //
				.addPoint(55, 0) //
				.build();
	}

	@Override
	public PolyLine getDischargeTemperatureToPercent() {
		return PolyLine.create() //
				.addPoint(Math.nextDown(-10), 0) //
				.addPoint(-10, 0.215) //
				.addPoint(0, 0.215) //
				.addPoint(1, 1) //
				.addPoint(44, 1) //
				.addPoint(45, 0.865) //
				.addPoint(49, 0.865) //
				.addPoint(50, 0.325) //
				.addPoint(54, 0.325) //
				.addPoint(55, 0) //
				.build();
	}

	@Override
	public ForceDischargeParams getForceDischargeParams() {
		return new ChargeMaxCurrentHandler.ForceDischargeParams(3660, 3640, 3450);
	}

	@Override
	public ForceChargeParams getForceChargeParams() {
		return new DischargeMaxCurrentHandler.ForceChargeParams(2850, 2910, 3000);
	}

	@Override
	public Double getMaxIncreaseAmperePerSecond() {
		return 0.5; // [A] per second
	}
}