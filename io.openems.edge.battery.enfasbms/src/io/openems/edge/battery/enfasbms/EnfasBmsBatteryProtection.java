package io.openems.edge.battery.enfasbms;

import io.openems.edge.battery.protection.BatteryProtectionDefinition;
import io.openems.edge.battery.protection.force.ForceCharge;
import io.openems.edge.battery.protection.force.ForceDischarge;
import io.openems.edge.common.linecharacteristic.PolyLine;

public class EnfasBmsBatteryProtection implements BatteryProtectionDefinition {

	@Override
	public int getInitialBmsMaxEverChargeCurrent() {
		return 325; // [A]
	}

	@Override
	public int getInitialBmsMaxEverDischargeCurrent() {
		return 325; // [A]
	}

	@Override
	public PolyLine getChargeVoltageToPercent() {
		return PolyLine.create() //
				.addPoint(4040, 0) //
				.addPoint(4030, 0.1) //
				.addPoint(4020, 1) //
				.build();
	}

	@Override
	public PolyLine getDischargeVoltageToPercent() {
		return PolyLine.create() //
				.addPoint(3000, 0) //
				.addPoint(3025, 0.1) //
				.addPoint(3050, 1) //
				.build();
	}

	@Override
	public ForceDischarge.Params getForceDischargeParams() {
		return new ForceDischarge.Params(//
				4200, //
				4170, //
				4150);
	}

	@Override
	public ForceCharge.Params getForceChargeParams() {
		return new ForceCharge.Params(//
				2900, //
				2950, //
				2975);
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

}