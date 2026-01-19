package io.openems.edge.ruhfass.battery.batcon;

import io.openems.edge.battery.protection.BatteryProtectionDefinition;
import io.openems.edge.battery.protection.force.ForceCharge;
import io.openems.edge.battery.protection.force.ForceDischarge;
import io.openems.edge.common.linecharacteristic.PolyLine;

public class BatconBatteryProtection implements BatteryProtectionDefinition {

	@Override
	public int getInitialBmsMaxEverChargeCurrent() {
		return 260;
	}

	@Override
	public int getInitialBmsMaxEverDischargeCurrent() {
		return 260;
	}

	@Override
	public PolyLine getChargeVoltageToPercent() {
		return PolyLine.create() //
				.addPoint(4100, 0) //
				.addPoint(4050, 0.1) //
				.addPoint(4000, 1) //
				.build();
	}

	@Override
	public PolyLine getDischargeVoltageToPercent() {
		return PolyLine.create() //
				.addPoint(3400, 0) //
				.addPoint(3450, 0.1) //
				.addPoint(3500, 1) //
				.build();
	}

	@Override
	public ForceDischarge.Params getForceDischargeParams() {
		return new ForceDischarge.Params(//
				4108, //
				4104, //
				4100);
	}

	@Override
	public ForceCharge.Params getForceChargeParams() {
		return new ForceCharge.Params(//
				3392, //
				3396, //
				3400);
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
		return PolyLine.empty();
	}

	@Override
	public PolyLine getDischargeSocToPercent() {
		return PolyLine.empty();
	}

}
