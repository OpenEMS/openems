package io.openems.edge.battery.bmw;

import io.openems.edge.battery.protection.BatteryProtectionDefinition;
import io.openems.edge.battery.protection.force.ForceCharge;
import io.openems.edge.battery.protection.force.ForceDischarge;
import io.openems.edge.common.linecharacteristic.PolyLine;

public class BmwBatteryProtectionDefinition implements BatteryProtectionDefinition {

	@Override
	public final int getInitialBmsMaxEverChargeCurrent() {
		return 135; // [A]
	}

	@Override
	public final int getInitialBmsMaxEverDischargeCurrent() {
		return 135; // [A]
	}

	@Override
	public PolyLine getChargeVoltageToPercent() {
		// Values are [mV] per Cell. Convert using x * 1000 / 192
		return PolyLine.create() //
				// 750 V -> 135 A
				.addPoint(Math.nextDown(3906), 1) //
				// 750 V Reset-Threshold
				.addPoint(3906, 0.9999) //
				// 770 V -> ~135 A
				.addPoint(4010, 0.9999) //
				// 800 V -> 5 A
				.addPoint(4167, 0.037) //
				// 803 V -> 0 A
				.addPoint(Math.nextDown(4182), 0.0001) //
				.addPoint(4182, 0) //
				.build();
	}

	@Override
	public final PolyLine getDischargeVoltageToPercent() {
		return PolyLine.empty();
	}

	@Override
	public final PolyLine getChargeTemperatureToPercent() {
		return PolyLine.empty();
	}

	@Override
	public final PolyLine getDischargeTemperatureToPercent() {
		return PolyLine.empty();
	}

	@Override
	public final ForceDischarge.Params getForceDischargeParams() {
		return null;
	}

	@Override
	public final ForceCharge.Params getForceChargeParams() {
		return null;
	}

	@Override
	public final Double getMaxIncreaseAmperePerSecond() {
		return 0.1; // [A] per second
	}
}