package io.openems.edge.battery.soltaro.shared.batteryprotection;

import io.openems.edge.common.linecharacteristic.PolyLine;

public class BatteryProtectionDefinitionSoltaro3kWh extends AbstractBatteryProtectionDefinitionSoltaro {

	@Override
	public PolyLine getChargeVoltageToPercent() {
		return PolyLine.create() //
				.addPoint(3000, 0.1) //
				.addPoint(Math.nextUp(3000), 1) //
				.addPoint(3350, 1) //
				.addPoint(3450, 0.9999) //
				.addPoint(3600, 0.02) //
				.addPoint(Math.nextDown(3650), 0.02) //
				.addPoint(3650, 0) //
				.build();
	}

}