package io.openems.edge.battery.soltaro.common.batteryprotection;

import io.openems.edge.common.linecharacteristic.PolyLine;

public class BatteryProtectionDefinitionSoltaro35kWh extends BatteryProtectionDefinitionSoltaro3kWh {

	@Override
	public PolyLine getChargeVoltageToPercent() {
		return PolyLine.create() //
				.addPoint(3000, 0.1) //
				.addPoint(Math.nextUp(3000), 1) //
				.addPoint(3350, 1) //
				.addPoint(3450, 0.9999) //
				.addPoint(3550, 0.01) //
				.addPoint(Math.nextDown(3650), 0.01) //
				.addPoint(3650, 0) //
				.build();
	}
}