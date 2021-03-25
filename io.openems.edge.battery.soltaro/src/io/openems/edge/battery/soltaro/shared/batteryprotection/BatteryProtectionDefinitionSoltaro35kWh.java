package io.openems.edge.battery.soltaro.shared.batteryprotection;

import io.openems.edge.common.linecharacteristic.PolyLine;

public class BatteryProtectionDefinitionSoltaro35kWh extends BatteryProtectionDefinitionSoltaro3kWh {

	@Override
	public PolyLine getChargeVoltageToPercent() {
		return PolyLine.create() //
				.addPoint(3000, 0.1) //
				.addPoint(Math.nextUp(3000), 1) //
				.addPoint(3350, 1) //
				.addPoint(3450, 0.9999) //

				// Reduce earlier than in BatteryProtectionDefinitionSoltaro3kWh.
				.addPoint(3550, 0.02) //

				.addPoint(Math.nextDown(3650), 0.02) //
				.addPoint(3650, 0) //
				.build();
	}
}